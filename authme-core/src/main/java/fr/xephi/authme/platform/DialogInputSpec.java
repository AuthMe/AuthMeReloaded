package fr.xephi.authme.platform;

import java.util.Objects;

/**
 * Description of one text input displayed in a dialog.
 */
public final class DialogInputSpec {

    private final String id;
    private final String label;
    private final int maxLength;

    public DialogInputSpec(String id, String label, int maxLength) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.maxLength = maxLength;
    }

    public String id() { return id; }
    public String label() { return label; }
    public int maxLength() { return maxLength; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DialogInputSpec that = (DialogInputSpec) o;
        return maxLength == that.maxLength && Objects.equals(id, that.id)
            && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, maxLength);
    }

    @Override
    public String toString() {
        return "DialogInputSpec[id=" + id + ", label=" + label + ", maxLength=" + maxLength + "]";
    }
}
