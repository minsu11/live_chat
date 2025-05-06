package com.chat_server.common.validation;

/**
 * packageName    : com.chat_server.common.validation
 * fileName       : Validation
 * author         : parkminsu
 * date           : 25. 3. 14.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 3. 14.        parkminsu       최초 생성
 */
public final class Validation {
    private Validation() {
    }

    public static void checkNull(Object object) {
        checkNull(object, "object");
    }

    public static void checkNull(Object object, String fieldName) {
        if (isNull(object)) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    public static void checkEmpty(String string) {
        checkEmpty(string, "string");
    }

    public static void checkEmpty(String string, String fieldName) {
        if (string.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");

        }
    }

    public static void checkNegative(int number) {
        if (number < 0) {

        }
    }

    private static boolean isNull(Object object) {
        return object == null;
    }


}
