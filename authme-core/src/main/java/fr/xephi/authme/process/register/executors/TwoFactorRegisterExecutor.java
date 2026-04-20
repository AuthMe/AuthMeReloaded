package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.crypts.TwoFactor;
import fr.xephi.authme.service.CommonService;
import org.bukkit.Bukkit;

import javax.inject.Inject;

import static fr.xephi.authme.process.register.executors.PlayerAuthBuilderHelper.createPlayerAuth;

/**
 * Executor for two-factor registration.
 */
class TwoFactorRegisterExecutor extends AbstractPasswordRegisterExecutor<TwoFactorRegisterParams> {

    @Inject
    private CommonService commonService;

    @Override
    public boolean isRegistrationAdmitted(TwoFactorRegisterParams params) {
        // nothing to check
        return true;
    }

    @Override
    protected PlayerAuth createPlayerAuthObject(TwoFactorRegisterParams params) {
        return createPlayerAuth(params.getPlayer(), params.getHashedPassword(), null);
    }

    @Override
    public void executePostPersistAction(TwoFactorRegisterParams params) {
        super.executePostPersistAction(params);

        // Note ljacqu 20170317: This two-factor registration type is only invoked when the password hash is configured
        // to two-factor authentication. Therefore, the hashed password is the result of the TwoFactor EncryptionMethod
        // implementation (contains the TOTP secret).
        String hash = params.getHashedPassword().getHash();
        String qrCodeUrl = TwoFactor.getQrBarcodeUrl(params.getPlayerName(), Bukkit.getIp(), hash);
        commonService.send(params.getPlayer(), MessageKey.TWO_FACTOR_CREATE, hash, qrCodeUrl);
    }
}
