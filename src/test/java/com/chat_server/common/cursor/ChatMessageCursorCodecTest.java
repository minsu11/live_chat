package com.chat_server.common.cursor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatMessageCursorCodecTest {

    @Test
    void encodeDecode_success() {
        long epochMillis = 1711111111111L;
        long messageId = 12345L;

        String encoded = ChatMessageCursorCodec.encode(epochMillis, messageId);
        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(encoded);

        assertThat(decoded).isNotNull();
        assertThat(decoded.lastMessageAtEpochMillis()).isEqualTo(epochMillis);
        assertThat(decoded.lastMessageId()).isEqualTo(messageId);
    }

    @Test
    void decode_invalidCursor_returnsNull() {
        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode("not-valid-cursor");

        assertThat(decoded).isNull();
    }

    @Test
    void decode_blankCursor_returnsNull() {
        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(" ");

        assertThat(decoded).isNull();
    }
}
