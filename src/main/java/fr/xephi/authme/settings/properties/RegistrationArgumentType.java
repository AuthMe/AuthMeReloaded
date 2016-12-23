package fr.xephi.authme.settings.properties;

import fr.xephi.authme.message.MessageKey;

/**
 * Type of arguments used for the login command.
 */
public enum RegistrationArgumentType {

    /** /register [password] */
    PASSWORD(Execution.PASSWORD, 1, MessageKey.REGISTER_NO_REPEAT_MESSAGE),

    /** /register [password] [password] */
    PASSWORD_WITH_CONFIRMATION(Execution.PASSWORD, 2, MessageKey.REGISTER_MESSAGE),

    /** /register [email] */
    EMAIL(Execution.EMAIL, 1, MessageKey.REGISTER_EMAIL_NO_REPEAT_MESSAGE),

    /** /register [email] [email] */
    EMAIL_WITH_CONFIRMATION(Execution.EMAIL, 2, MessageKey.REGISTER_EMAIL_MESSAGE),

    /** /register [password] [email] */
    PASSWORD_WITH_EMAIL(Execution.PASSWORD, 2, MessageKey.REGISTER_PASSWORD_EMAIL_MESSAGE);

    private final Execution execution;
    private final int requiredNumberOfArgs;
    private final MessageKey messageKey;

    /**
     * Constructor.
     *
     * @param execution the registration process
     * @param requiredNumberOfArgs the required number of arguments
     */
    RegistrationArgumentType(Execution execution, int requiredNumberOfArgs, MessageKey messageKey) {
        this.execution = execution;
        this.requiredNumberOfArgs = requiredNumberOfArgs;
        this.messageKey = messageKey;
    }

    /**
     * @return the registration execution that is used for this argument type
     */
    public Execution getExecution() {
        return execution;
    }
    
    public MessageKey getMessageKey(){
    	return messageKey;
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
