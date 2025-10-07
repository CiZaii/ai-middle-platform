package com.ai.middle.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashTest {

    @Test
    void zangzangHashMatches() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches("zangzang", "$2a$10$1dpFZEqIALm1bkqZsQ5cROGvKL7mrhtDppjC5nPUjeSH8by06yW0G"));
    }
}

