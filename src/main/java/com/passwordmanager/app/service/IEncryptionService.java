package com.passwordmanager.app.service;

public interface IEncryptionService {
    String encrypt(String plainText);

    String decrypt(String cipherText);
}
