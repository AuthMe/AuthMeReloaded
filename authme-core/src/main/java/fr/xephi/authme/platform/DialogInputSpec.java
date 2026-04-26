package fr.xephi.authme.platform;

import java.util.Objects;

/**
 * Description of one text input displayed in a dialog.
 *
 * @param id the field identifier submitted by the client
 * @param label the translated field label
 * @param maxLength the maximum input length
 */
public record DialogInputSpec(String id, String label, int maxLength) {

    public DialogInputSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
    }
}
