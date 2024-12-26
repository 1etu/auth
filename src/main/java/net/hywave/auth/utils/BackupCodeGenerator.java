package net.hywave.auth.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class BackupCodeGenerator {
    private static final SecureRandom random = new SecureRandom();
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    public static List<String> generateCodes(int count) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            codes.add(generateCode());
        }
        return codes;
    }
    
    private static String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }
} 