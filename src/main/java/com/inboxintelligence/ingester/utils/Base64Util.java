package com.inboxintelligence.ingester.utils;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * URL-safe Base64 decoding utilities for Gmail API data.
 */
@UtilityClass
public class Base64Util {

    public static String decodeBase64String(String data) {
        if (data == null) {
            return null;
        }

        byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    public static byte[] decodeBase64Bytes(String data) {
        if (data == null) {
            return null;
        }

        return Base64.getUrlDecoder().decode(data);
    }
}
