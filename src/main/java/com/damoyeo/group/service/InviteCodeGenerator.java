package com.damoyeo.group.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class InviteCodeGenerator {

    private static final char[] CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LENGTH = 8;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder result = new StringBuilder(LENGTH);
        for (int index = 0; index < LENGTH; index++) {
            result.append(CHARACTERS[random.nextInt(CHARACTERS.length)]);
        }
        return result.toString();
    }
}
