package com.Classroom_ai.Classroom;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For tests of the AI tools, which anyone may use once. The trial gate remembers an address for good, and every test
 * class in a run shares one database, so each test comes from an address nobody else has used.
 */
abstract class ToolsTestSupport extends GenerationTestSupport {

    private static final AtomicInteger NEXT_ADDRESS = new AtomicInteger();
    private static final Pattern TRIAL_COOKIE = Pattern.compile("(?:^|\\s)trial=([^;]+)");

    /** A remote address that no other test has used. */
    static String newIp() {
        int n = NEXT_ADDRESS.incrementAndGet();
        return "10." + ((n >> 8) & 255) + "." + (n & 255) + ".9";
    }

    /** Makes the request arrive from {@code ip}. */
    static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    /** The value of the {@code trial} cookie set by a {@code Set-Cookie} header, or null. */
    static String trialCookieIn(String setCookieHeader) {
        if (setCookieHeader == null) {
            return null;
        }
        Matcher cookie = TRIAL_COOKIE.matcher(setCookieHeader);
        return cookie.find() ? cookie.group(1) : null;
    }
}
