package com.pagerealm.service;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

public interface TotpService {
    GoogleAuthenticatorKey generateSecret();

    String getQrCodeUrl(GoogleAuthenticatorKey secret, String email);

    boolean verifyCode(String secret, int code);
}
