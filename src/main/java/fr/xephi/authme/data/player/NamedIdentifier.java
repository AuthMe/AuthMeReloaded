package fr.xephi.authme.data.player;

import java.util.Objects;
import java.util.Optional;

public class NamedIdentifier {

    private final String lowercaseName;
    private final String realName;

    public NamedIdentifier(String lowercaseName, String realName) {
        Objects.requireNonNull(lowercaseName);
        this.lowercaseName = lowercaseName;
        this.realName = realName;
    }

    public String getLowercaseName() {
        return lowercaseName;
    }

    public Optional<String> getRealName() {
        return Optional.ofNullable(realName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedIdentifier that = (NamedIdentifier) o;
        return Objects.equals(lowercaseName, that.lowercaseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowercaseName);
    }

    @Override
    public String toString() {
        return "NamedIdentifier{" +
            "lowercaseName='" + lowercaseName + '\'' +
            '}';
    }
}
