package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.data.auth.PlayerAuth;

import static fr.xephi.authme.process.register.executors.PlayerAuthBuilderHelper.createPlayerAuth;

/**
 * Registration executor for password registration.
 */
class PasswordRegisterExecutor extends AbstractPasswordRegisterExecutor<PasswordRegisterParams> {

    @Override
    public synchronized PlayerAuth createPlayerAuthObject(PasswordRegisterParams params) {
        return createPlayerAuth(params.getPlayer(), params.getHashedPassword(), params.getEmail());
    }

}
