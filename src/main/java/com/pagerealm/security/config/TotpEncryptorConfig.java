package com.pagerealm.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class TotpEncryptorConfig {

    @Value("${TOTP_ENCRYPT_KEY}")
    private String totpSecret;
    @Value("${TOTP_ENCRYPT_SALT}")
    private String totpEncryptSalt;

    @Bean
    public TextEncryptor totpTextEncryptor() {
        return Encryptors.text(totpSecret, totpEncryptSalt);
    }

}
