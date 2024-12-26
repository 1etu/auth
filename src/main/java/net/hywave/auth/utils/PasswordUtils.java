package net.hywave.auth.utils;

import org.mindrot.jbcrypt.BCrypt;
import java.util.regex.Pattern;

public class PasswordUtils {
    private static final int MIN_LENGTH = 6;
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*\\d.*");

    public static boolean isValidPassword(String password) {
        return password.length() >= MIN_LENGTH && 
               NUMBER_PATTERN.matcher(password).matches();
    }

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
} 