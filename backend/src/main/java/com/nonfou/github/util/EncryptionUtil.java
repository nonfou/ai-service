package com.nonfou.github.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 加密工具类
 * 使用 AES/GCM/NoPadding + 随机 IV，避免固定 IV 带来的重放和明文推断问题。
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String KEY_ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH_BYTES = 12;

    @Value("${encryption.key:}")
    private String encryptionKey;

    /**
     * 旧版配置字段，现仅用于兼容性提示。
     */
    @Value("${encryption.iv:}")
    private String deprecatedIv;

    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKeySpec keySpec;

    @PostConstruct
    public void init() {
        if (encryptionKey == null || encryptionKey.isBlank() ||
                encryptionKey.startsWith("your-32-character")) {
            throw new IllegalStateException("必须配置 encryption.key，并保证其随机性");
        }

        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new IllegalStateException("encryption.key 长度需为 16/24/32 字节");
        }

        if (deprecatedIv != null && !deprecatedIv.isBlank()) {
            log.warn("配置项 encryption.iv 已弃用，将忽略该值。系统会为每次加密生成随机 IV。");
        }

        this.keySpec = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    /**
     * 加密文本
     *
     * @param plainText 明文
     * @return Base64(IV + CipherText)
     */
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密文本
     */
    public String decrypt(String encryptedText) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            if (payload.length < IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("密文格式无效");
            }

            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
            byte[] cipherBytes = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] decrypted = cipher.doFinal(cipherBytes);
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
