package com.nonfou.github.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加密工具类
 * 用于加密和解密敏感的 Access Token
 */
@Component
public class EncryptionUtil {

    @Value("${encryption.key:your-32-character-encryption-key}")
    private String encryptionKey;

    @Value("${encryption.iv:your-16-char-iv}")
    private String encryptionIv;

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    /**
     * 加密文本
     *
     * @param plainText 明文
     * @return Base64 编码的密文
     */
    public String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(encryptionIv.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密文本
     *
     * @param encryptedText Base64 编码的密文
     * @return 明文
     */
    public String decrypt(String encryptedText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(encryptionIv.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 验证加密密钥是否有效
     */
    public boolean validateKey() {
        try {
            String test = "test";
            String encrypted = encrypt(test);
            String decrypted = decrypt(encrypted);
            return test.equals(decrypted);
        } catch (Exception e) {
            return false;
        }
    }
}
