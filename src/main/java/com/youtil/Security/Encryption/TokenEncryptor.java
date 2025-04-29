package com.youtil.Security.Encryption;

public interface TokenEncryptor {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}