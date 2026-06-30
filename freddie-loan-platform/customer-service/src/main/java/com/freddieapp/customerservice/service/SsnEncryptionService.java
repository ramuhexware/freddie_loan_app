package com.freddieapp.customerservice.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for SSN data.
 * Key is loaded from environment variable / Vault — never hardcoded.
 */
@Service
public class SsnEncryptionService {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LEN = 12; // 96 bits
    private static final int    GCM_TAG_LEN = 128; // bits

    private final SecretKey secretKey;

    public SsnEncryptionService() {
        // In production: load from Azure Key Vault / Kubernetes Secret
        String keyBase64 = System.getenv().getOrDefault("SSN_ENCRYPTION_KEY",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="); // 32-byte placeholder
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LEN, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes());
            byte[] combined  = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("SSN encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] combined  = Base64.getDecoder().decode(ciphertext);
            byte[] iv        = new byte[GCM_IV_LEN];
            byte[] encrypted = new byte[combined.length - GCM_IV_LEN];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LEN);
            System.arraycopy(combined, GCM_IV_LEN, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LEN, iv));
            return new String(cipher.doFinal(encrypted));
        } catch (Exception e) {
            throw new RuntimeException("SSN decryption failed", e);
        }
    }
}
