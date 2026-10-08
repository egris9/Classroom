package com.Classroom_ai.Classroom.tools;

import com.Classroom_ai.Classroom.auth.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Lets an anonymous visitor use the AI tools once. A visitor is known by the {@code trial} cookie and by the address
 * the connection came from; having used the tools under either one counts as having used them. The address is the
 * socket's, not {@code X-Forwarded-For}, which any caller can forge, unless {@code tools.trial.trust-forwarded} says
 * a proxy we run sets it. Only salted hashes are stored, and only hash prefixes are ever logged.
 */
@Component
public class TrialGate {

    static final String COOKIE = "trial";
    private static final Duration COOKIE_LIFETIME = Duration.ofDays(365);
    private static final int MAX_COOKIE_LENGTH = 100;
    private static final Logger log = LoggerFactory.getLogger(TrialGate.class);

    private final TrialUseRepository uses;
    private final String salt;
    private final boolean trustForwarded;

    public TrialGate(TrialUseRepository uses,
                     @Value("${tools.trial.salt:}") String salt,
                     @Value("${tools.trial.trust-forwarded:false}") boolean trustForwarded) {
        if (salt == null || salt.isBlank()) {
            throw new IllegalStateException("tools.trial.salt is empty. Set the TOOLS_TRIAL_SALT environment variable.");
        }
        this.uses = uses;
        this.salt = salt;
        this.trustForwarded = trustForwarded;
    }

    /**
     * Lets a signed-in user through. Lets a visitor through once, remembering them and handing them a cookie if they
     * had none; after that, throws {@link TrialUsedException}. Call it before any work is done for the visitor.
     */
    public void admit(HttpServletRequest request, HttpServletResponse response, User userOrNull) {
        if (userOrNull != null) {
            return;
        }
        String cookie = cookieOf(request);
        boolean issueCookie = cookie == null;
        if (issueCookie) {
            cookie = UUID.randomUUID().toString();
        }
        String cookieHash = hash(cookie);
        String ipHash = hash(addressOf(request));
        if (uses.existsByCookieHashOrIpHash(cookieHash, ipHash)) {
            log.debug("Trial already used by visitor {}", ipHash.substring(0, 8));
            throw new TrialUsedException();
        }
        try {
            uses.saveAndFlush(new TrialUse(cookieHash, ipHash));
        } catch (DataIntegrityViolationException raced) {
            throw new TrialUsedException();
        }
        log.debug("Trial used by visitor {}", ipHash.substring(0, 8));
        if (issueCookie) {
            response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE, cookie)
                    .httpOnly(true)
                    .secure(request.isSecure())
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(COOKIE_LIFETIME)
                    .build().toString());
        }
    }

    /** Whether {@link #admit} would let this visitor through now. Changes nothing. */
    public boolean isAvailable(HttpServletRequest request) {
        String cookie = cookieOf(request);
        String cookieHash = hash(cookie != null ? cookie : UUID.randomUUID().toString());
        return !uses.existsByCookieHashOrIpHash(cookieHash, hash(addressOf(request)));
    }

    private static String cookieOf(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            String value = cookie.getValue();
            if (COOKIE.equals(cookie.getName()) && value != null && !value.isBlank()
                    && value.length() <= MAX_COOKIE_LENGTH) {
                return value;
            }
        }
        return null;
    }

    private String addressOf(HttpServletRequest request) {
        if (trustForwarded) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // The last entry is the one our own proxy added; the ones before it are whatever the caller sent.
                String[] hops = forwarded.split(",");
                return hops[hops.length - 1].strip();
            }
        }
        return request.getRemoteAddr();
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((salt + value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
