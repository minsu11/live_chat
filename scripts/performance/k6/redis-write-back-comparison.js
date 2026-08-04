import ws from 'k6/ws';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const MODE = __ENV.MODE || 'redis-write-back';
const RUN_ID = __ENV.RUN_ID || `${MODE}-${Date.now()}`;
const RESULT_DIR = __ENV.RESULT_DIR || '/results';

const WS_BASE_URL = (__ENV.WS_BASE_URL || 'ws://host.docker.internal:7070').replace(/\/$/, '');
const SOCKJS_ENDPOINT = __ENV.SOCKJS_ENDPOINT || '/api/ws-chat';
const ROOM_ID = Number(__ENV.ROOM_ID || 0);
const MESSAGE_TYPE = __ENV.MESSAGE_TYPE || 'TEXT';
const MESSAGE_PREFIX = __ENV.MESSAGE_PREFIX || 'perf-message';
const MESSAGES_PER_VU = Number(__ENV.MESSAGES_PER_VU || 20);
const SEND_INTERVAL_MS = Number(__ENV.SEND_INTERVAL_MS || 100);
const DRAIN_SECONDS = Number(__ENV.DRAIN_SECONDS || 30);
const VUS = Number(__ENV.VUS || 10);

const TOKENS = parseCsv(__ENV.ACCESS_TOKENS || __ENV.ACCESS_TOKEN || '');
const USER_IDS = parseCsv(__ENV.USER_IDS || __ENV.USER_ID || '');

const stompConnected = new Rate('stomp_connected');
const allOwnMessagesReceived = new Rate('all_own_messages_received');
const messagesSent = new Counter('chat_messages_sent');
const ownMessagesReceived = new Counter('chat_own_messages_received');
const stompErrors = new Counter('stomp_errors');
const messageE2eLatency = new Trend('chat_message_e2e_latency_ms', true);
const postSendReceiveDelay = new Trend('chat_post_send_receive_delay_ms', true);

const MAX_DURATION_SECONDS = Math.max(
    60,
    Math.ceil((MESSAGES_PER_VU * SEND_INTERVAL_MS) / 1000)
    + DRAIN_SECONDS
    + 30,
);

export const options = {
  summaryTrendStats: [
    'avg',
    'min',
    'med',
    'max',
    'p(90)',
    'p(95)',
    'p(99)',
  ],

  scenarios: {
    default: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: `${MAX_DURATION_SECONDS}s`,
      gracefulStop: '5s',
    },
  },

  thresholds: {
    checks: ['rate>0.95'],
    stomp_connected: ['rate>0.95'],
    all_own_messages_received: ['rate>0.90'],
  },
};

export default function () {
  validateEnvironment();

  const credentialIndex = (__VU - 1) % TOKENS.length;
  const token = TOKENS[credentialIndex];
  const userId = USER_IDS[credentialIndex % USER_IDS.length];
  const sessionId = `${Date.now()}-${__VU}-${Math.random().toString(36).slice(2, 12)}`;
  const serverId = String((__VU * 37) % 1000).padStart(3, '0');
  const socketUrl = `${WS_BASE_URL}${SOCKJS_ENDPOINT}/${serverId}/${sessionId}/websocket`;

  const sentAtByMessageContent = new Map();
  let sentCount = 0;
  let receivedOwnCount = 0;
  let sendCompletedAt = 0;
  let connectMetricRecorded = false;

  const response = ws.connect(socketUrl, {}, (socket) => {
    socket.on('message', (raw) => {
      if (raw === 'o') {
        socket.send(sockJsEncode(buildConnectFrame(token)));
        return;
      }

      for (const stompFrame of decodeSockJsPayload(raw)) {
        const frame = parseStompFrame(stompFrame);
        if (!frame) continue;

        if (frame.command === 'CONNECTED') {
          if (!connectMetricRecorded) {
            stompConnected.add(true);
            connectMetricRecorded = true;
          }

          socket.send(sockJsEncode(buildSubscribeFrame()));

          for (let index = 0; index < MESSAGES_PER_VU; index += 1) {
            socket.setTimeout(() => {
              const messageContent =
                  `${MESSAGE_PREFIX}-${RUN_ID}-vu${__VU}-msg${index}`;

              const body = JSON.stringify({
                roomId: ROOM_ID,
                messageType: MESSAGE_TYPE,
                messageContent,
              });

              sentAtByMessageContent.set(messageContent, Date.now());

              socket.send(sockJsEncode(buildSendFrame(body)));

              sentCount += 1;
              messagesSent.add(1);

              if (index === MESSAGES_PER_VU - 1) {
                sendCompletedAt = Date.now();
              }
            }, (index +1)* SEND_INTERVAL_MS);
          }

          socket.setTimeout(() => {
            allOwnMessagesReceived.add(sentCount > 0 && receivedOwnCount === sentCount);
            socket.close();
          }, MESSAGES_PER_VU * SEND_INTERVAL_MS + DRAIN_SECONDS * 1000);

          return;
        }

        if (frame.command === 'MESSAGE') {


          let payload;
          try {
            payload = JSON.parse(frame.body);
          } catch (error) {
            stompErrors.add(1);
            console.error(
                `MESSAGE JSON parse error vu=${__VU}: body=${frame.body}`,
            );
            return;
          }

          const message = payload?.data || payload;

          const receivedContent =
              message?.content ?? message?.messageContent;

          const sentAt = receivedContent
              ? sentAtByMessageContent.get(receivedContent)
              : undefined;

          const DEBUG = (__ENV.DEBUG || 'false') === 'true';

          if (sentAt === undefined) {
            if (DEBUG) {
              console.log(
                  `Other user's MESSAGE vu=${__VU}: ${JSON.stringify(message)}`
              );
            }
            return;
          }

          const now = Date.now();
          messageE2eLatency.add(now - sentAt);

          if (sendCompletedAt > 0 && now >= sendCompletedAt) {
            postSendReceiveDelay.add(now - sendCompletedAt);
          }

          sentAtByMessageContent.delete(receivedContent);
          receivedOwnCount += 1;
          ownMessagesReceived.add(1);
          return;
        }

        if (frame.command === 'ERROR') {
          stompErrors.add(1);

          console.error(
              `STOMP ERROR vu=${__VU}: ` +
              `headers=${JSON.stringify(frame.headers)} ` +
              `body=${frame.body}`,
          );
        }
      }
    });

    socket.on('error', (error) => {
      stompErrors.add(1);
      console.error(`WebSocket error vu=${__VU}: ${error.error()}`);
    });

    socket.on('close', () => {
      if (!connectMetricRecorded) {
        stompConnected.add(false);
        connectMetricRecorded = true;
      }
    });
  });

  check(response, {
    'SockJS WebSocket upgrade status is 101': (result) => result && result.status === 101,
  });
}

function validateEnvironment() {
  if (!ROOM_ID) {
    throw new Error('ROOM_ID 환경변수가 필요합니다.');
  }
  if (TOKENS.length === 0) {
    throw new Error('ACCESS_TOKEN 또는 ACCESS_TOKENS 환경변수가 필요합니다.');
  }
  if (USER_IDS.length === 0) {
    throw new Error('USER_ID 또는 USER_IDS 환경변수가 필요합니다.');
  }
}

function parseCsv(value) {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function sockJsEncode(stompFrame) {
  return JSON.stringify([stompFrame]);
}

function decodeSockJsPayload(raw) {
  if (typeof raw !== 'string' || raw.length === 0 || raw === 'h') {
    return [];
  }

  if (raw.startsWith('c')) {
    stompErrors.add(1);
    console.error(`SockJS close frame: ${raw}`);
    return [];
  }

  if (!raw.startsWith('a')) {
    return [];
  }

  try {
    const messages = JSON.parse(raw.slice(1));
    return messages.flatMap((message) =>
      message
        .split('\u0000')
        .map((frame) => frame.trim())
        .filter((frame) => frame.length > 0)
        .map((frame) => `${frame}\u0000`),
    );
  } catch (error) {
    stompErrors.add(1);
    console.error(`SockJS payload parse error: ${raw}`);
    return [];
  }
}

function buildConnectFrame(token) {
  return [
    'CONNECT',
    'accept-version:1.2',
    'heart-beat:0,0',
    `Authorization:Bearer ${token}`,
    '',
    '\u0000',
  ].join('\n');
}

function buildSubscribeFrame() {
  const destination =
      `/user/api/sub/chat/rooms/${ROOM_ID}`;

  return [
    'SUBSCRIBE',
    `id:room-${ROOM_ID}-vu-${__VU}`,
    `destination:${destination}`,
    'ack:auto',
    '',
    '\u0000',
  ].join('\n');
}

function buildSendFrame(body) {
  return [
    'SEND',
    'destination:/api/pub/chat/message',
    'content-type:application/json',
    '',
    body,
  ].join('\n') +'\u0000';
}

function parseStompFrame(rawFrame) {
  const normalized = rawFrame.replace(/\u0000+$/, '');
  const separatorIndex = normalized.indexOf('\n\n');
  const headerPart = separatorIndex >= 0 ? normalized.slice(0, separatorIndex) : normalized;
  const body = separatorIndex >= 0 ? normalized.slice(separatorIndex + 2) : '';
  const lines = headerPart.split('\n');
  const command = lines.shift();
  if (!command) return null;

  const headers = {};
  for (const line of lines) {
    const colonIndex = line.indexOf(':');
    if (colonIndex <= 0) continue;
    headers[line.slice(0, colonIndex)] = line.slice(colonIndex + 1);
  }

  return { command, headers, body };
}

export function handleSummary(data) {
  const summaryPath = `${RESULT_DIR}/${RUN_ID}-summary.json`;
  const metric = (name, key) => data.metrics?.[name]?.values?.[key];

  const lines = [
    '',
    `Chatalk metadata mode: ${MODE}`,
    `Run ID: ${RUN_ID}`,
    `Sent: ${metric('chat_messages_sent', 'count') ?? 0}`,
    `Received own messages: ${metric('chat_own_messages_received', 'count') ?? 0}`,
    `E2E avg(ms): ${format(metric('chat_message_e2e_latency_ms', 'avg'))}`,
    `E2E p95(ms): ${format(metric('chat_message_e2e_latency_ms', 'p(95)'))}`,
    `E2E p99(ms): ${format(metric('chat_message_e2e_latency_ms', 'p(99)'))}`,
    `E2E max(ms): ${format(metric('chat_message_e2e_latency_ms', 'max'))}`,
    `Post-send tail max(ms): ${format(metric('chat_post_send_receive_delay_ms', 'max'))}`,
    `STOMP errors: ${metric('stomp_errors', 'count') ?? 0}`,
    `JSON summary: ${summaryPath}`,
    '',
  ].join('\n');

  return {
    stdout: lines,
    [summaryPath]: JSON.stringify(data, null, 2),
  };
}

function format(value) {
  return value === undefined || value === null ? '-' : Number(value).toFixed(2);
}
