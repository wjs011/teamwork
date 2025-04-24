package com.example.demo.utils;

import java.util.UUID;

public class TokenUtils {
    public static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}