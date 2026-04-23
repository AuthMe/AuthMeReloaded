package fr.xephi.authme.platform;

import net.kyori.adventure.key.Key;

/**
 * Identifiers for custom Paper dialog actions handled during the configuration phase.
 */
public final class PaperDialogActionKeys {

    public static final Key PRE_JOIN_LOGIN_SUBMIT = Key.key("authme:prejoin-login/submit");
    public static final Key PRE_JOIN_LOGIN_CANCEL = Key.key("authme:prejoin-login/cancel");
    public static final Key PRE_JOIN_REGISTER_SUBMIT = Key.key("authme:prejoin-register/submit");
    public static final Key PRE_JOIN_REGISTER_CANCEL = Key.key("authme:prejoin-register/cancel");

    private PaperDialogActionKeys() {
    }
}
