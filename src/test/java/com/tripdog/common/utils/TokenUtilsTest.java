package com.tripdog.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenUtilsTest {

    @Test
    void testValidTokenFormat() {
        String validToken = "1234567890abcdef1234567890abcdef" + System.currentTimeMillis();
        Assertions.assertTrue(TokenUtils.isValidTokenFormat(validToken));
    }

    @Test
    void testInvalidTokenFormatWrongLength() {
        String invalidToken = "1234567890abcdef"; // too short
        Assertions.assertFalse(TokenUtils.isValidTokenFormat(invalidToken));
    }

    @Test
    void testInvalidTokenFormatNonHex() {
        String invalidToken = "ZZ34567890abcdef1234567890abcdef" + System.currentTimeMillis();
        Assertions.assertFalse(TokenUtils.isValidTokenFormat(invalidToken));
    }

    @Test
    void testInvalidTokenFormatMissingTimestamp() {
        String invalidToken = "1234567890abcdef1234567890abcdef"; // missing timestamp
        Assertions.assertFalse(TokenUtils.isValidTokenFormat(invalidToken));
    }
}
