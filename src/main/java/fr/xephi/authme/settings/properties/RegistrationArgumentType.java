package fr.xephi.authme.settings.properties;

/**
 * Type of arguments used for the login command.
 */
public enum RegistrationArgumentType {

    /** /register [password] */
    PASSWORD(Execution.PASSWORD, 1),

    /** /register [password] [password] */
    PASSWORD_WITH_CONFIRMATION(Execution.PASSWORD, 2),

    /** /register [email] */
    EMAIL(Execution.EMAIL, 1),

    /** /register [email] [email] */
    EMAIL_WITH_CONFIRMATION(Execution.EMAIL, 2);

    // TODO #427: PASSWORD_WITH_EMAIL(PASSWORD, 2);

    private final Execution execution;
    private final int requiredNumberOfArgs;

    /**
     * Constructor.
     *
     * @param execution the registration process
     * @param requiredNumberOfArgs the required number of arguments
     */
    RegistrationArgumentType(Execution execution, int requiredNumberOfArgs) {
        this.execution = execution;
        this.requiredNumberOfArgs = requiredNumberOfArgs;
    }

    /**
     * @return the registration execution that is used for this argument type
     */
    public Execution getExecution() {
        return execution;
    }

    /**
     * @return number of arguments required to process the register command
     */
    public int getRequiredNumberOfArgs() {
        return requiredNumberOfArgs;
    }

    /**
     * Registration execution (the type of registration).
     */
    public enum Execution {

        PASSWORD,
        EMAIL

    }
}
