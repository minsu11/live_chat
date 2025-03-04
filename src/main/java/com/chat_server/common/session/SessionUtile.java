package com.chat_server.common.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Session;

/**
 * packageName    : com.chat_server.common.session
 * fileName       : SessionUtile
 * author         : parkminsu
 * date           : 25. 3. 4.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 3. 4.        parkminsu       최초 생성
 */

public final class SessionUtile {
    public static HttpSession renewSession(HttpServletRequest request) {
        return renewSession(request.getSession());
    }

    // todo session id 재발급

    public static HttpSession  renewSession(HttpSession session) {

        return null;
    }
}
