package fr.xephi.authme.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Resolved dialog text and UX options shared across platform-specific renderers.
 */
public final class DialogWindowSpec {

    private final String title;
    private final List<DialogInputSpec> inputs;
    private final String primaryButtonLabel;
    private final String secondaryButtonLabel;
    private final boolean showSecondaryButton;
    private final boolean canCloseWithEscape;
    private final String secondaryButtonCommand;
    private final String body;

    public DialogWindowSpec(String title, List<DialogInputSpec> inputs, String primaryButtonLabel,
                            String secondaryButtonLabel, boolean showSecondaryButton,
                            boolean canCloseWithEscape, String secondaryButtonCommand, String body) {
        this.title = Objects.requireNonNull(title, "title");
        Objects.requireNonNull(inputs, "inputs");
        this.inputs = Collections.unmodifiableList(new ArrayList<DialogInputSpec>(inputs));
        this.primaryButtonLabel = Objects.requireNonNull(primaryButtonLabel, "primaryButtonLabel");
        this.secondaryButtonLabel = Objects.requireNonNull(secondaryButtonLabel, "secondaryButtonLabel");
        this.showSecondaryButton = showSecondaryButton;
        this.canCloseWithEscape = canCloseWithEscape;
        this.secondaryButtonCommand = secondaryButtonCommand;
        this.body = body;
    }

    /** Convenience constructor without body text (body = null). */
    public DialogWindowSpec(String title,
                            List<DialogInputSpec> inputs,
                            String primaryButtonLabel,
                            String secondaryButtonLabel,
                            boolean showSecondaryButton,
                            boolean canCloseWithEscape,
                            String secondaryButtonCommand) {
        this(title, inputs, primaryButtonLabel, secondaryButtonLabel,
            showSecondaryButton, canCloseWithEscape, secondaryButtonCommand, null);
    }

    public String title() { return title; }
    public List<DialogInputSpec> inputs() { return inputs; }
    public String primaryButtonLabel() { return primaryButtonLabel; }
    public String secondaryButtonLabel() { return secondaryButtonLabel; }
    public boolean showSecondaryButton() { return showSecondaryButton; }
    public boolean canCloseWithEscape() { return canCloseWithEscape; }
    public String secondaryButtonCommand() { return secondaryButtonCommand; }
    public String body() { return body; }

    /** Returns a copy of this spec with the given body text (may be null). */
    public DialogWindowSpec withBody(String newBody) {
        return new DialogWindowSpec(title, inputs, primaryButtonLabel, secondaryButtonLabel,
            showSecondaryButton, canCloseWithEscape, secondaryButtonCommand, newBody);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DialogWindowSpec that = (DialogWindowSpec) o;
        return showSecondaryButton == that.showSecondaryButton
            && canCloseWithEscape == that.canCloseWithEscape
            && Objects.equals(title, that.title) && Objects.equals(inputs, that.inputs)
            && Objects.equals(primaryButtonLabel, that.primaryButtonLabel)
            && Objects.equals(secondaryButtonLabel, that.secondaryButtonLabel)
            && Objects.equals(secondaryButtonCommand, that.secondaryButtonCommand)
            && Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, inputs, primaryButtonLabel, secondaryButtonLabel,
            showSecondaryButton, canCloseWithEscape, secondaryButtonCommand, body);
    }

    @Override
    public String toString() {
        return "DialogWindowSpec[title=" + title + ", inputs=" + inputs
            + ", primaryButtonLabel=" + primaryButtonLabel + ", secondaryButtonLabel="
            + secondaryButtonLabel + ", showSecondaryButton=" + showSecondaryButton
            + ", canCloseWithEscape=" + canCloseWithEscape + ", secondaryButtonCommand="
            + secondaryButtonCommand + ", body=" + body + "]";
    }
}
