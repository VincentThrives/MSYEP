package com.vincent.msyep.common;

import java.security.SecureRandom;

/** Small helpers for human-shareable ids and temporary passwords. */
public final class IdGen {

    private static final SecureRandom RND = new SecureRandom();
    private static final String BASE36 = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String PWD_CHARS = "abcdefghijkmnpqrstuvwxyz23456789";

    private IdGen() {}

    /** CUID-like id, e.g. "cmjk7etye000hhrxoh2mzw36z" (25 chars, starts with 'c'). */
    public static String cuid() {
        StringBuilder sb = new StringBuilder(25).append('c');
        for (int i = 0; i < 24; i++) {
            sb.append(BASE36.charAt(RND.nextInt(BASE36.length())));
        }
        return sb.toString();
    }

    /** 8-char temporary password (lowercase + digits, no ambiguous chars). */
    public static String tempPassword() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(PWD_CHARS.charAt(RND.nextInt(PWD_CHARS.length())));
        }
        return sb.toString();
    }
}
