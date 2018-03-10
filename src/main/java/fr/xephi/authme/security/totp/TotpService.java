package fr.xephi.authme.security.totp;

import fr.xephi.authme.data.auth.PlayerAuth;

import javax.inject.Inject;

/**
 * Service for TOTP actions.
 */
public class TotpService {

    @Inject
    private TotpAuthenticator totpAuthenticator;

    public boolean verifyCode(PlayerAuth auth, String totpCode) {
        return totpAuthenticator.checkCode(auth.getTotpKey(), totpCode);
    }
}
