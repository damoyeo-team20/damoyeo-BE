package com.damoyeo.group.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InviteCodeGeneratorTest {

    private final InviteCodeGenerator generator = new InviteCodeGenerator();

    @Test
    void generatesEightCharacterCodesFromTheAllowedAlphabet() {
        Set<String> codes = new HashSet<>();

        for (int index = 0; index < 100; index++) {
            String code = generator.generate();
            assertThat(code).matches("[A-HJ-NP-Z2-9]{8}");
            codes.add(code);
        }

        assertThat(codes).hasSize(100);
    }
}
