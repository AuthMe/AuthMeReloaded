package fr.xephi.authme.data.auth;

import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.Location;

import static com.google.common.base.Objects.firstNonNull;
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
    private String ip;
    private int groupId;
    private long lastLogin;
    // Fields storing the player's quit location
    private double x;
    private double y;
    private double z;
    private String world;

    /**
     * @param serialized String
     */
    public PlayerAuth(String serialized) {
        this.deserialize(serialized);
    }

    /**
     * Constructor. Instantiate objects with the {@link #builder() builder}.
     *
     * @param nickname  all lowercase name of the player
     * @param password  password
     * @param groupId   the group id
     * @param ip        the associated ip address
     * @param lastLogin player's last login (timestamp)
     * @param x         quit location: x coordinate
     * @param y         quit location: y coordinate
     * @param z         quit location: z coordinate
     * @param world     quit location: world name
     * @param email     the associated email
     * @param realName  the player's name with proper casing
     */
    private PlayerAuth(String nickname, HashedPassword password, int groupId, String ip, long lastLogin,
                       double x, double y, double z, String world, String email, String realName) {
        this.nickname = nickname.toLowerCase();
        this.password = password;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.groupId = groupId;
        this.email = email;
        this.realName = realName;
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlayerAuth)) {
            return false;
        }
        PlayerAuth other = (PlayerAuth) obj;
        return other.getIp().equals(this.ip) && other.getNickname().equals(this.nickname);
    }

    @Override
    public int hashCode() {
        int hashCode = 7;
        hashCode = 71 * hashCode + (this.nickname != null ? this.nickname.hashCode() : 0);
        hashCode = 71 * hashCode + (this.ip != null ? this.ip.hashCode() : 0);
        return hashCode;
    }

    @Override
    public String toString() {
        return "Player : " + nickname + " | " + realName
            + " ! IP : " + ip
            + " ! LastLogin : " + lastLogin
            + " ! LastPosition : " + x + "," + y + "," + z + "," + world
            + " ! Email : " + email
            + " ! Password : {" + password.getHash() + ", " + password.getSalt() + "}";
    }

    /**
     * Method to serialize PlayerAuth
     *
     * @return String
     */
    public String serialize() {
        StringBuffer str = new StringBuffer();
        char d = ';';
        str.append(this.nickname).append(d);
        str.append(this.realName).append(d);
        str.append(this.ip).append(d);
        str.append(this.email).append(d);
        str.append(this.password.getHash()).append(d);
        str.append(this.password.getSalt()).append(d);
        str.append(this.groupId).append(d);
        str.append(this.lastLogin).append(d);
        str.append(this.world).append(d);
        str.append(this.x).append(d);
        str.append(this.y).append(d);
        str.append(this.z);
        return str.toString();
    }

    /**
     * Method to deserialize PlayerAuth
     * 
     * @param str String
     */
    public void deserialize(String str) {
        String[] args = str.split(";");
        this.nickname = args[0];
        this.realName = args[1];
        this.ip = args[2];
        this.email = args[3];
        this.password = new HashedPassword(args[4], args[5]);
        this.groupId = Integer.parseInt(args[6]);
        this.lastLogin = Long.parseLong(args[7]);
        this.world = args[8];
        this.x = Double.parseDouble(args[9]);
        this.y = Double.parseDouble(args[10]);
        this.z = Double.parseDouble(args[11]);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String realName;
        private HashedPassword password;
        private String ip;
        private String world;
        private String email;
        private int groupId = -1;
        private double x = 0.0f;
        private double y = 0.0f;
        private double z = 0.0f;
        private long lastLogin = System.currentTimeMillis();

        public PlayerAuth build() {
            return new PlayerAuth(
                checkNotNull(name),
                firstNonNull(password, new HashedPassword("")),
                groupId,
                firstNonNull(ip, "127.0.0.1"),
                lastLogin,
                x, y, z,
                firstNonNull(world, "world"),
                firstNonNull(email, "your@email.com"),
                firstNonNull(realName, "Player")
            );
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

        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }

        public Builder location(Location location) {
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.world = location.getWorld().getName();
            return this;
        }

        public Builder locWorld(String world) {
            this.world = world;
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
    }
}
