package fr.xephi.authme.command;

import java.util.List;

/**
 * Result of a command mapping by {@link CommandHandler}. An object of this class represents a successful mapping
 * as well as erroneous ones, as communicated with {@link ResultStatus}.
 * <p />
 * Fields other than {@link ResultStatus} are available depending, among other factors, on the status:
 * <ul>
 *   <li>{@link ResultStatus#SUCCESS} entails that mapping the input to a command was successful. Therefore,
 *       the command description, labels and arguments are set. The difference is 0.0.</li>
 *   <li>{@link ResultStatus#INCORRECT_ARGUMENTS}: The received parts could be mapped to a command but the argument
 *       count doesn't match. Guarantees that the command description field is not null; difference is 0.0</li>
 *   <li>{@link ResultStatus#UNKNOWN_LABEL}: The labels could not be mapped to a command. The command description may
 *       be set to the most similar command, or it may be null. Difference is above 0.0.</li>
 *   <li>{@link ResultStatus#MISSING_BASE_COMMAND} should never occur. All other fields may be null and any further
 *       processing of the object should be aborted.</li>
 * </ul>
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

    public CommandDescription getCommandDescription() {
        return this.commandDescription;
    }

    public List<String> getArguments() {
        return this.arguments;
    }

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
