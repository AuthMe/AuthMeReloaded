package fr.xephi.authme.util.datacolumns.sqlimplementation;

public class SampleContext {

    private final boolean isEmailEmpty;
    private final boolean isIsLockedEmpty;
    private final boolean isLastLoginEmpty;

    public SampleContext(boolean isEmailEmpty, boolean isIsLockedEmpty, boolean isLastLoginEmpty) {
        this.isEmailEmpty = isEmailEmpty;
        this.isIsLockedEmpty = isIsLockedEmpty;
        this.isLastLoginEmpty = isLastLoginEmpty;
    }

    public String resolveName(SampleColumns<?> col) {
        if (col == SampleColumns.NAME) {
            return "username";
        } else if (col == SampleColumns.ID) {
            return "id";
        } else if (col == SampleColumns.EMAIL) {
            return isEmailEmpty ? "" : "email";
        } else if (col == SampleColumns.IS_LOCKED) {
            return isIsLockedEmpty ? "" : "is_locked";
        } else if (col == SampleColumns.IS_ACTIVE) {
            return "is_active";
        } else if (col == SampleColumns.LAST_LOGIN) {
            return isLastLoginEmpty ? "" : "last_login";
        } else {
            throw new IllegalStateException("Unknown sample column '" + col + "'");
        }
    }
}
