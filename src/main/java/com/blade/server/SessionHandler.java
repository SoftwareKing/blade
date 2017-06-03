package com.blade.server;

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
public class SessionHandler implements RequestHandler<SessionManager> {

    private SessionManager sessionManager;

    public SessionHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public SessionManager handle(ChannelHandlerContext ctx, Request request, Response response) {
        Session session = getSession(request);
        if (null == session) {
            createSession(request, response);
        } else {
            if (session.expired() < Instant.now().getEpochSecond()) {
                removeSession(session, response);
            }
        }
        return sessionManager;
    }

    private void createSession(Request request, Response response) {

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

        request.cookie(cookie);
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
