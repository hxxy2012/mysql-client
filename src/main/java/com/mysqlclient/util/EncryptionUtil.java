package com.mysqlclient.util;

import org.jasypt.util.text.AES256TextEncryptor;

/**
 * 加密工具类 - 使用AES-256加密
 */
public class EncryptionUtil {
    private static final String ENCRYPTION_KEY = "MySQLClient2024SecurePassword";
    private static final AES256TextEncryptor encryptor;

    static {
        encryptor = new AES256TextEncryptor();
        encryptor.setPassword(ENCRYPTION_KEY);
    }

    /**
     * 加密文本
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        try {
            return encryptor.encrypt(plainText);
        } catch (Exception e) {
            System.err.println("Encryption failed: " + e.getMessage());
            return plainText;
        }
    }

    /**
     * 解密文本
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return "";
        }
        try {
            return encryptor.decrypt(encryptedText);
        } catch (Exception e) {
            // 如果解密失败，可能是旧的Base64编码，尝试Base64解码
            try {
                return new String(java.util.Base64.getDecoder().decode(encryptedText));
            } catch (Exception ex) {
                return encryptedText;
            }
        }
    }

    /**
     * 验证加密是否工作正常
     */
    public static boolean testEncryption() {
        String testText = "test123";
        String encrypted = encrypt(testText);
        String decrypted = decrypt(encrypted);
        return testText.equals(decrypted);
    }
}
