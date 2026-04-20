package fr.xephi.authme.process.register.executors;

/**
 * Methods with which a player can be registered.
 * <p>
 * These constants each define a different way of registering a player and define the
 * {@link RegistrationParameters parameters} and {@link RegistrationExecutor executor}
 * classes which perform this registration method. This is essentially a <i>typed enum</i>
 * as passing a constant of this class along with a parameters object to a method can
 * be restricted to the correct parameters type.
 *
 * @param <P> the registration parameters type the method uses
 */
public final class RegistrationMethod<P extends RegistrationParameters> {

    /**
     * Password registration.
     */
    public static final RegistrationMethod<PasswordRegisterParams> PASSWORD_REGISTRATION =
        new RegistrationMethod<>(PasswordRegisterExecutor.class);

    /**
     * Registration with two-factor authentication as login means.
     */
    public static final RegistrationMethod<TwoFactorRegisterParams> TWO_FACTOR_REGISTRATION =
        new RegistrationMethod<>(TwoFactorRegisterExecutor.class);

    /**
     * Email registration: an email address is provided, to which a generated password is sent.
     */
    public static final RegistrationMethod<EmailRegisterParams> EMAIL_REGISTRATION =
        new RegistrationMethod<>(EmailRegisterExecutor.class);

    /**
     * API registration: player and password are provided via an API method.
     */
    public static final RegistrationMethod<ApiPasswordRegisterParams> API_REGISTRATION =
        new RegistrationMethod<>(ApiPasswordRegisterExecutor.class);


    private final Class<? extends RegistrationExecutor<P>> executorClass;

    private RegistrationMethod(Class<? extends RegistrationExecutor<P>> executorClass) {
        this.executorClass = executorClass;
    }

    /**
     * @return the executor class to perform the registration method
     */
    public Class<? extends RegistrationExecutor<P>> getExecutorClass() {
        return executorClass;
    }
}
