package com.company.wiki.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BcryptHashGenTest {
    @Test
    void printHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Admin1234!");
        System.out.println("=== ADMIN HASH: " + hash + " ===");
        System.out.println("Matches: " + encoder.matches("Admin1234!", hash));
    }
}
