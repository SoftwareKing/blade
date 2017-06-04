package com.blade.server;

import com.blade.Environment;
import com.blade.kit.UUID;
import com.blade.mvc.http.*;
import io.netty.channel.ChannelHandlerContext;

import java.time.Instant;
import java.util.Optional;

import static com.blade.mvc.Const.ENV_KEY_SESSION_KEY;
import static com.blade.mvc.Const.ENV_KEY_SESSION_TIMEOUT;

/**
 * @author biezhi
 *         2017/6/3
 */
public class SessionHandler implements RequestHandler<SessionManager> {

    private final SessionManager sessionManager;
    private final String sessionKey;
    private final int timeout;

    public SessionHandler(SessionManager sessionManager, Environment environment) {
        this.sessionManager = sessionManager;
        this.sessionKey = environment.get(ENV_KEY_SESSION_KEY, "SESSION");
        this.timeout = environment.getInt(ENV_KEY_SESSION_TIMEOUT, 1800);
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
        long expired = now + timeout;

        String sessionId = UUID.UU32();
        Cookie cookie = new Cookie();
        cookie.name(sessionKey);
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
        Optional<String> cookieHeader = request.cookie(sessionKey);
        if (!cookieHeader.isPresent()) {
            return null;
        }
        return sessionManager.getSession(cookieHeader.get());
    }

}
