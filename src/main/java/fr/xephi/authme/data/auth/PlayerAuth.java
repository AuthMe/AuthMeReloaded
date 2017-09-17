package fr.xephi.authme.data.auth;

import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.Location;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;


/**
 * AuthMe player data.
 */
public class PlayerAuth {

    /** The player's name in lowercase, e.g. "xephi". */
    private String nickname;
    /** The player's name in the correct casing, e.g. "Xephi". */
    private String realName;
    private HashedPassword password;
    private String email;
    private String lastIp;
    private int groupId;
    private long lastLogin;
    private String registrationIp;
    private Long registrationDate;
    // Fields storing the player's quit location
    private double x;
    private double y;
    private double z;
    private String world;
    private float yaw;
    private float pitch;

    /**
     * Hidden constructor.
     *
     * @see #builder()
     */
    private PlayerAuth() {
    }


    public void setNickname(String nickname) {
        this.nickname = nickname.toLowerCase();
    }

    public String getNickname() {
        return nickname;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setQuitLocation(Location location) {
        x = location.getBlockX();
        y = location.getBlockY();
        z = location.getBlockZ();
        world = location.getWorld().getName();
    }

    public double getQuitLocX() {
        return x;
    }

    public void setQuitLocX(double d) {
        this.x = d;
    }

    public double getQuitLocY() {
        return y;
    }

    public void setQuitLocY(double d) {
        this.y = d;
    }

    public double getQuitLocZ() {
        return z;
    }

    public void setQuitLocZ(double d) {
        this.z = d;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public HashedPassword getPassword() {
        return password;
    }

    public void setPassword(HashedPassword password) {
        this.password = password;
    }

    public String getRegistrationIp() {
        return registrationIp;
    }

    public Long getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlayerAuth)) {
            return false;
        }
        PlayerAuth other = (PlayerAuth) obj;
        return other.getLastIp().equals(this.lastIp) && other.getNickname().equals(this.nickname);
    }

    @Override
    public int hashCode() {
        int hashCode = 7;
        hashCode = 71 * hashCode + (this.nickname != null ? this.nickname.hashCode() : 0);
        hashCode = 71 * hashCode + (this.lastIp != null ? this.lastIp.hashCode() : 0);
        return hashCode;
    }

    @Override
    public String toString() {
        return "Player : " + nickname + " | " + realName
            + " ! IP : " + lastIp
            + " ! LastLogin : " + lastLogin
            + " ! LastPosition : " + x + "," + y + "," + z + "," + world
            + " ! Email : " + email
            + " ! Password : {" + password.getHash() + ", " + password.getSalt() + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String realName;
        private HashedPassword password;
        private String lastIp;
        private String email;
        private int groupId = -1;
        // TODO #792: Remove this default
        private long lastLogin = System.currentTimeMillis();
        private String registrationIp;
        private Long registrationDate;

        private double x;
        private double y;
        private double z;
        private String world;
        private float yaw;
        private float pitch;

        /**
         * Creates a PlayerAuth object.
         *
         * @return the generated PlayerAuth
         */
        public PlayerAuth build() {
            PlayerAuth auth = new PlayerAuth();
            auth.nickname = checkNotNull(name).toLowerCase();
            auth.realName = firstNonNull(realName, "Player");
            auth.password = firstNonNull(password, new HashedPassword(""));
            auth.email = firstNonNull(email, "your@email.com");
            auth.lastIp = firstNonNull(lastIp, "127.0.0.1"); // TODO #792 remove default
            auth.groupId = groupId;
            auth.lastLogin = lastLogin;
            auth.registrationIp = registrationIp;
            auth.registrationDate = registrationDate;

            auth.x = x;
            auth.y = y;
            auth.z = z;
            auth.world = firstNonNull(world, "world");
            auth.yaw = yaw;
            auth.pitch = pitch;
            return auth;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder realName(String realName) {
            this.realName = realName;
            return this;
        }

        public Builder password(HashedPassword password) {
            this.password = password;
            return this;
        }

        public Builder password(String hash, String salt) {
            return password(new HashedPassword(hash, salt));
        }

        public Builder lastIp(String lastIp) {
            this.lastIp = lastIp;
            return this;
        }

        /**
         * Sets the location info based on the argument.
         *
         * @param location the location info to set
         * @return this builder instance
         */
        public Builder location(Location location) {
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.world = location.getWorld().getName();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
            return this;
        }

        public Builder locX(double x) {
            this.x = x;
            return this;
        }

        public Builder locY(double y) {
            this.y = y;
            return this;
        }

        public Builder locZ(double z) {
            this.z = z;
            return this;
        }

        public Builder locWorld(String world) {
            this.world = world;
            return this;
        }

        public Builder locYaw(float yaw) {
            this.yaw = yaw;
            return this;
        }

        public Builder locPitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public Builder lastLogin(long lastLogin) {
            this.lastLogin = lastLogin;
            return this;
        }

        public Builder groupId(int groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder registrationIp(String ip) {
            this.registrationIp = ip;
            return this;
        }

        // NOTE: This value is not read when a user is registered; the current timestamp is taken.
        // Registration IP, however, is taken over.
        public Builder registrationDate(Long date) {
            this.registrationDate = date;
            return this;
        }
    }
}
