package fr.xephi.authme.command;

import java.util.List;

/**
 */
public class FoundCommandResult {

    /**
     * The command description instance.
     */
    private final CommandDescription commandDescription;
    /**
     * The command arguments.
     */
    private final List<String> arguments;
    /**
     * The labels used to invoke the command. This may be different for the same {@link ExecutableCommand} instance
     * if multiple labels have been defined, e.g. "/authme register" and "/authme reg".
     */
    private final List<String> labels;

    private final double difference;

    private final ResultStatus resultStatus;

    /**
     * Constructor.
     *
     * @param commandDescription The command description.
     * @param arguments          The command arguments.
     * @param labels             The original query reference.
     */
    public FoundCommandResult(CommandDescription commandDescription, List<String> arguments, List<String> labels,
                              double difference, ResultStatus resultStatus) {
        this.commandDescription = commandDescription;
        this.arguments = arguments;
        this.labels = labels;
        this.difference = difference;
        this.resultStatus = resultStatus;
    }

    public FoundCommandResult(CommandDescription commandDescription, List<String> arguments, List<String> labels) {
        this(commandDescription, arguments, labels, 0.0, ResultStatus.SUCCESS);
    }

    /**
     * Get the command description.
     *
     * @return Command description.
     */
    public CommandDescription getCommandDescription() {
        return this.commandDescription;
    }


    /**
     * Get the command arguments.
     *
     * @return The command arguments.
     */
    public List<String> getArguments() {
        return this.arguments;
    }

    /**
     * Get the original query reference.
     *
     * @return Original query reference.
     */
    public List<String> getLabels() {
        return this.labels;
    }

    public double getDifference() {
        return difference;
    }

    public ResultStatus getResultStatus() {
        return resultStatus;
    }

    public enum ResultStatus {

        SUCCESS,

        INCORRECT_ARGUMENTS,

        UNKNOWN_LABEL,

        MISSING_BASE_COMMAND
    }
}
