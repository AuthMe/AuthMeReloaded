package fr.xephi.authme.command;

import java.util.List;

/**
 * Result of a command mapping by {@link CommandHandler}. An object of this class represents a successful mapping
 * as well as erroneous ones, as communicated with {@link FoundResultStatus}.
 * <p>
 * Fields other than {@link FoundResultStatus} are available depending, among other factors, on the status:
 * <ul>
 *   <li>{@link FoundResultStatus#SUCCESS} entails that mapping the input to a command was successful. Therefore,
 *       the command description, labels and arguments are set. The difference is 0.0.</li>
 *   <li>{@link FoundResultStatus#INCORRECT_ARGUMENTS}: The received parts could be mapped to a command but the argument
 *       count doesn't match. Guarantees that the command description field is not null; difference is 0.0</li>
 *   <li>{@link FoundResultStatus#UNKNOWN_LABEL}: The labels could not be mapped to a command. The command description
 *       may be set to the most similar command, or it may be null. Difference is above 0.0.</li>
 *   <li>{@link FoundResultStatus#NO_PERMISSION}: The command could be matched properly but the sender does not have
 *       permission to execute it.</li>
 *   <li>{@link FoundResultStatus#MISSING_BASE_COMMAND} should never occur. All other fields may be null and any further
 *       processing of the object should be aborted.</li>
 * </ul>
 */
public class FoundCommandResult {

    /**
     * The command description instance.
     */
    private final CommandDescription commandDescription;
    /**
     * The labels used to invoke the command. This may be different for the same {@link ExecutableCommand} instance
     * if multiple labels have been defined, e.g. "/authme register" and "/authme reg".
     */
    private final List<String> labels;
    /** The command arguments. */
    private final List<String> arguments;
    /** The difference between the matched command and the supplied labels. */
    private final double difference;
    /** The status of the result (see class description). */
    private final FoundResultStatus resultStatus;

    /**
     * Constructor.
     *
     * @param commandDescription The command description.
     * @param labels             The labels used to access the command.
     * @param arguments          The command arguments.
     * @param difference         The difference between the supplied labels and the matched command.
     * @param resultStatus       The status of the result.
     */
    public FoundCommandResult(CommandDescription commandDescription, List<String> labels, List<String> arguments,
                              double difference, FoundResultStatus resultStatus) {
        this.commandDescription = commandDescription;
        this.labels = labels;
        this.arguments = arguments;
        this.difference = difference;
        this.resultStatus = resultStatus;
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

    public FoundResultStatus getResultStatus() {
        return resultStatus;
    }

}
