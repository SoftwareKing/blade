package com.blade.server.netty;

import com.blade.kit.UUID;
import com.blade.mvc.Const;
import com.blade.mvc.http.*;
import io.netty.channel.ChannelHandlerContext;

import java.time.Instant;
import java.util.Optional;

/**
 * @author biezhi
 *         2017/6/3
 */
public class SessionHandler implements RequestHandler {

    private SessionManager sessionManager;

    public SessionHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, Request request, Response response) {
        Session session = getSession(request);
        if (null == session) {
            createSession(response);
        } else {
            if (session.expired() < Instant.now().getEpochSecond()) {
                removeSession(session, response);
            }
        }
    }

    private void createSession(Response response) {

        long now = Instant.now().getEpochSecond();
        long expired = now + sessionManager.timeout();

        String sessionId = UUID.UU32();
        Cookie cookie = new Cookie();
        cookie.name(Const.SESSION_COOKIE_NAME);
        cookie.value(sessionId);
        cookie.httpOnly(true);

        HttpSession session = new HttpSession(sessionId);
        session.setCreated(now);
        session.setExpired(expired);
        sessionManager.addSession(session);

        response.cookie(cookie);
    }

    private void removeSession(Session session, Response response) {
        sessionManager.remove(session);
    }

    private Session getSession(Request request) {
        Optional<String> cookieHeader = request.cookie(Const.SESSION_COOKIE_NAME);
        if (!cookieHeader.isPresent()) {
            return null;
        }
        return sessionManager.getSession(cookieHeader.get());
    }

}
