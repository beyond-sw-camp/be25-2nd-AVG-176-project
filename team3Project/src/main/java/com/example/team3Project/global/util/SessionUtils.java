package com.example.team3Project.global.util;

import com.example.team3Project.domain.user.dto.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionUtils {

    private static final String LOGIN_USER_KEY = "loginUser";

    private SessionUtils() {
    }

    public static void setLoginUser(HttpServletRequest request, SessionUser sessionUser) {
        HttpSession session = request.getSession(true);
        session.setAttribute(LOGIN_USER_KEY, sessionUser);
        log.info("세션 저장: userId={}, username={}", sessionUser.getId(), sessionUser.getUsername());
    }

    public static SessionUser getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (SessionUser) session.getAttribute(LOGIN_USER_KEY);
    }

    public static Long getLoginUserId(HttpServletRequest request) {
        SessionUser loginUser = getLoginUser(request);
        return loginUser != null ? loginUser.getId() : null;
    }

    public static String getLoginUsername(HttpServletRequest request) {
        SessionUser loginUser = getLoginUser(request);
        return loginUser != null ? loginUser.getUsername() : null;
    }

    public static boolean isLoggedIn(HttpServletRequest request) {
        return getLoginUser(request) != null;
    }

    public static void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            log.info("세션 무효화 완료");
        }
    }
}
