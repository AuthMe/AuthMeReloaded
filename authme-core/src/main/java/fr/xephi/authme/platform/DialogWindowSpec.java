package fr.xephi.authme.platform;

import java.util.List;
import java.util.Objects;

/**
 * Resolved dialog text and UX options shared across platform-specific renderers.
 *
 * @param title the dialog title
 * @param inputs the dialog inputs
 * @param primaryButtonLabel the main action button label
 * @param secondaryButtonLabel the optional secondary button label
 * @param showSecondaryButton whether a secondary button should be rendered
 * @param canCloseWithEscape whether the dialog may be dismissed with escape when the platform supports it
 */
public record DialogWindowSpec(String title,
                               List<DialogInputSpec> inputs,
                               String primaryButtonLabel,
                               String secondaryButtonLabel,
                               boolean showSecondaryButton,
                               boolean canCloseWithEscape) {

    public DialogWindowSpec {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(primaryButtonLabel, "primaryButtonLabel");
        Objects.requireNonNull(secondaryButtonLabel, "secondaryButtonLabel");
        inputs = List.copyOf(inputs);
    }
}
