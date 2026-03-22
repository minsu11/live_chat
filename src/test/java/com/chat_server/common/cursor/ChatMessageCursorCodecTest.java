package com.chat_server.common.cursor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatMessageCursorCodecTest {

    @Test
    @DisplayName("정상 커서는 encode/decode 왕복 시 원본 값이 유지된다")
    void shouldRoundTripWhenCursorIsValid() {
        long epochMillis = 1711111111111L;
        long messageId = 12345L;

        String encoded = ChatMessageCursorCodec.encode(epochMillis, messageId);
        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(encoded);

        assertThat(decoded).isNotNull();
        assertThat(decoded.lastMessageAtEpochMillis()).isEqualTo(epochMillis);
        assertThat(decoded.lastMessageId()).isEqualTo(messageId);
    }

    @Test
    @DisplayName("커서가 null이면 decode 결과는 null이다")
    void shouldReturnNullWhenCursorIsNull() {
        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(null);

        assertThat(decoded).isNull();
    }

    @Test
    @DisplayName("커서가 빈 문자열이면 decode 결과는 null이다")
    void shouldReturnNullWhenCursorIsBlank() {
        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(" ");

        assertThat(decoded).isNull();
    }

    @Test
    @DisplayName("Base64 형식이 아니면 decode 결과는 null이다")
    void shouldReturnNullWhenCursorIsNotBase64() {
        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode("not-valid-cursor");

        assertThat(decoded).isNull();
    }

    @Test
    @DisplayName("구분자 콜론이 없으면 decode 결과는 null이다")
    void shouldReturnNullWhenDelimiterIsMissing() {
        String invalid = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("1234567890".getBytes(StandardCharsets.UTF_8));

        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(invalid);

        assertThat(decoded).isNull();
    }

    @Test
    @DisplayName("숫자 파싱이 불가능하면 decode 결과는 null이다")
    void shouldReturnNullWhenNumericParsingFails() {
        String invalid = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("abc:def".getBytes(StandardCharsets.UTF_8));

        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(invalid);

        assertThat(decoded).isNull();
    }

    @Test
    @DisplayName("콜론은 있지만 뒤 값이 비어 있으면 decode 결과는 null이다")
    void shouldReturnNullWhenTailValueIsMissing() {
        String invalid = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("12345:".getBytes(StandardCharsets.UTF_8));

        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(invalid);

        assertThat(decoded).isNull();
    }
}
