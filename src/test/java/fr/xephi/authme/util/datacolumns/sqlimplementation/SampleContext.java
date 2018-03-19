package fr.xephi.authme.util.datacolumns.sqlimplementation;

public class SampleContext {

    private boolean isEmailEmpty;
    private boolean isIsLockedEmpty;
    private boolean isLastLoginEmpty;

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
        } else if (col == SampleColumns.IP) {
            return "ip";
        } else {
            throw new IllegalStateException("Unknown sample column '" + col + "'");
        }
    }

    public void setEmptyOptions(boolean isEmailEmpty, boolean isIsLockedEmpty, boolean isLastLoginEmpty) {
        this.isEmailEmpty = isEmailEmpty;
        this.isIsLockedEmpty = isIsLockedEmpty;
        this.isLastLoginEmpty = isLastLoginEmpty;
    }

    @Override
    public String toString() {
        return "empty{email=" + isEmailEmpty
            + ", isLocked=" + isIsLockedEmpty
            + ", lastLogin=" + isLastLoginEmpty
            + "}";
    }
}
