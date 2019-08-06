package fr.xephi.authme.data.auth;

import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.Location;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * AuthMe player data.
 */
public class PlayerAuth {

    /** Default email used in the database if the email column is defined to be NOT NULL. */
    public static final String DB_EMAIL_DEFAULT = "your@email.com";
    /** Default last login value used in the database if the last login column is NOT NULL. */
    public static final long DB_LAST_LOGIN_DEFAULT = 0;
    /** Default last ip value used in the database if the last IP column is NOT NULL. */
    public static final String DB_LAST_IP_DEFAULT = "127.0.0.1";

    /** The player's name in lowercase, e.g. "xephi". */
    private String nickname;
    /** The player's name in the correct casing, e.g. "Xephi". */
    private String realName;
    private HashedPassword password;
    private String totpKey;
    private String email;
    private String lastIp;
    private int groupId;
    private Long lastLogin;
    private String registrationIp;
    private long registrationDate;
    // Fields storing the player's quit location
    private double x;
    private double y;
    private double z;
    private String world;
    private float yaw;
    private float pitch;
    private UUID uuid;

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

    public Long getLastLogin() {
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

    public long getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(long registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getTotpKey() {
        return totpKey;
    }

    public void setTotpKey(String totpKey) {
        this.totpKey = totpKey;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlayerAuth)) {
            return false;
        }
        PlayerAuth other = (PlayerAuth) obj;
        return Objects.equals(other.lastIp, this.lastIp) && Objects.equals(other.nickname, this.nickname);
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
            + " ! Password : {" + password.getHash() + ", " + password.getSalt() + "}"
            + " ! UUID : " + uuid;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String realName;
        private HashedPassword password;
        private String totpKey;
        private String lastIp;
        private String email;
        private int groupId = -1;
        private Long lastLogin;
        private String registrationIp;
        private Long registrationDate;

        private double x;
        private double y;
        private double z;
        private String world;
        private float yaw;
        private float pitch;
        private UUID uuid;

        /**
         * Creates a PlayerAuth object.
         *
         * @return the generated PlayerAuth
         */
        public PlayerAuth build() {
            PlayerAuth auth = new PlayerAuth();
            auth.nickname = checkNotNull(name).toLowerCase();
            auth.realName = Optional.ofNullable(realName).orElse("Player");
            auth.password = Optional.ofNullable(password).orElse(new HashedPassword(""));
            auth.totpKey = totpKey;
            auth.email = DB_EMAIL_DEFAULT.equals(email) ? null : email;
            auth.lastIp = lastIp; // Don't check against default value 127.0.0.1 as it may be a legit value
            auth.groupId = groupId;
            auth.lastLogin = isEqualTo(lastLogin, DB_LAST_LOGIN_DEFAULT) ? null : lastLogin;
            auth.registrationIp = registrationIp;
            auth.registrationDate = registrationDate == null ? System.currentTimeMillis() : registrationDate;

            auth.x = x;
            auth.y = y;
            auth.z = z;
            auth.world = Optional.ofNullable(world).orElse("world");
            auth.yaw = yaw;
            auth.pitch = pitch;
            auth.uuid = uuid;
            return auth;
        }

        private static boolean isEqualTo(Long value, long defaultValue) {
            return value != null && defaultValue == value;
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

        public Builder totpKey(String totpKey) {
            this.totpKey = totpKey;
            return this;
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

        public Builder lastLogin(Long lastLogin) {
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

        public Builder registrationDate(long date) {
            this.registrationDate = date;
            return this;
        }

        public Builder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }
    }
}
