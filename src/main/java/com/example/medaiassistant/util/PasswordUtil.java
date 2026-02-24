package com.example.medaiassistant.util;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class PasswordUtil {
    private static final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    
    // 推荐参数配置
    private static final int ITERATIONS = 2;
    private static final int MEMORY = 65536; // 64MB
    private static final int PARALLELISM = 1;
    
    public static String hashPassword(String password) {
        try {
            return argon2.hash(ITERATIONS, MEMORY, PARALLELISM, password.toCharArray());
        } finally {
            argon2.wipeArray(password.toCharArray());
        }
    }
    
    public static boolean verifyPassword(String hash, String password) {
        return argon2.verify(hash, password.toCharArray());
    }
}
