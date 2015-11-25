package fr.xephi.authme.cache.auth;

import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;

/**
 */
public class PlayerAuth {

    private String nickname;
    private String hash;
    private String ip;
    private long lastLogin;
    private double x;
    private double y;
    private double z;
    private String world;
    private String salt;
    private final int groupId;
    private String email;
    private String realName;

    /**
     * Constructor for PlayerAuth.
     *
     * @param nickname  String
     * @param ip        String
     * @param lastLogin long
     * @param realName  String
     */
    public PlayerAuth(String nickname, String ip, long lastLogin, String realName) {
        this(nickname, "", "", -1, ip, lastLogin, 0, 0, 0, "world", "your@email.com", realName);
    }

    /**
     * Constructor for PlayerAuth.
     *
     * @param nickname String
     * @param x        double
     * @param y        double
     * @param z        double
     * @param world    String
     * @param realName String
     */
    public PlayerAuth(String nickname, double x, double y, double z, String world, String realName) {
        this(nickname, "", "", -1, "127.0.0.1", System.currentTimeMillis(), x, y, z, world, "your@email.com", realName);
    }

    /**
     * Constructor for PlayerAuth.
     *
     * @param nickname  String
     * @param hash      String
     * @param ip        String
     * @param lastLogin long
     * @param realName  String
     */
    public PlayerAuth(String nickname, String hash, String ip, long lastLogin, String realName) {
        this(nickname, hash, "", -1, ip, lastLogin, 0, 0, 0, "world", "your@email.com", realName);
    }

    /**
     * Constructor for PlayerAuth.
     *
     * @param nickname  String
     * @param hash      String
     * @param ip        String
     * @param lastLogin long
     * @param email     String
     * @param realName  String
     */
    public PlayerAuth(String nickname, String hash, String ip, long lastLogin, String email, String realName) {
        this(nickname, hash, "", -1, ip, lastLogin, 0, 0, 0, "world", email, realName);
    }

    /**
     * Constructor for PlayerAuth.
     *
     * @param nickname  String
     * @param hash      String
     * @param salt      String
     * @param ip        String
     * @param lastLogin long
     * @param realName  String
     */
    public PlayerAuth(String nickname, String hash, String salt, String ip, long lastLogin, String realName) {
        this(nickname, hash, salt, -1, ip, lastLogin, 0, 0, 0, "world", "your@email.com", realName);
    }

    /**
     * Constructor for PlayerAuth.
     *
     * @param nickname  String
     * @param hash      String
     * @param ip        String
     * @param lastLogin long
     * @param x         double
     * @param y         double
     * @param z         double
     * @param world     String
     * @param email     String
     * @param realName  String
     */
    public PlayerAuth(String nickname, String hash, String ip, long lastLogin, double x, double y, double z, String world, String email, String realName) {
        this(nickname, hash, "", -1, ip, lastLogin, x, y, z, world, email, realName);
    }

    /**
     * Constructor for PlayerAuth.
     *
     * @param nickname  String
     * @param hash      String
     * @param salt      String
     * @param ip        String
     * @param lastLogin long
     * @param x         double
     * @param y         double
     * @param z         double
     * @param world     String
     * @param email     String
     * @param realName  String
     */
    public PlayerAuth(String nickname, String hash, String salt, String ip, long lastLogin, double x, double y, double z, String world, String email, String realName) {
        this(nickname, hash, salt, -1, ip, lastLogin, x, y, z, world, email, realName);
    }

    /**
     * Constructor for PlayerAuth.
     *
     * @param nickname  String
     * @param hash      String
     * @param salt      String
     * @param groupId   int
     * @param ip        String
     * @param lastLogin long
     * @param realName  String
     */
    public PlayerAuth(String nickname, String hash, String salt, int groupId, String ip, long lastLogin, String realName) {
        this(nickname, hash, salt, groupId, ip, lastLogin, 0, 0, 0, "world", "your@email.com", realName);
    }

    /**
     * Constructor for PlayerAuth.
     *
     * @param nickname  String
     * @param hash      String
     * @param salt      String
     * @param groupId   int
     * @param ip        String
     * @param lastLogin long
     * @param x         double
     * @param y         double
     * @param z         double
     * @param world     String
     * @param email     String
     * @param realName  String
     */
    public PlayerAuth(String nickname, String hash, String salt, int groupId, String ip, long lastLogin, double x, double y, double z, String world, String email, String realName) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.salt = salt;
        this.groupId = groupId;
        this.email = email;
        this.realName = realName;
    }

    /**
     * Method set.
     *
     * @param auth PlayerAuth
     */
    public void set(PlayerAuth auth) {
        this.setEmail(auth.getEmail());
        this.setHash(auth.getHash());
        this.setIp(auth.getIp());
        this.setLastLogin(auth.getLastLogin());
        this.setName(auth.getNickname());
        this.setQuitLocX(auth.getQuitLocX());
        this.setQuitLocY(auth.getQuitLocY());
        this.setQuitLocZ(auth.getQuitLocZ());
        this.setSalt(auth.getSalt());
        this.setWorld(auth.getWorld());
        this.setRealName(auth.getRealName());
    }

    /**
     * Method setName.
     *
     * @param nickname String
     */
    public void setName(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Method getNickname.
     *
     * @return String
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Method getRealName.
     *
     * @return String
     */
    public String getRealName() {
        return realName;
    }

    /**
     * Method setRealName.
     *
     * @param realName String
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }

    /**
     * Method getGroupId.
     *
     * @return int
     */
    public int getGroupId() {
        return groupId;
    }

    /**
     * Method getQuitLocX.
     *
     * @return double
     */
    public double getQuitLocX() {
        return x;
    }

    /**
     * Method setQuitLocX.
     *
     * @param d double
     */
    public void setQuitLocX(double d) {
        this.x = d;
    }

    /**
     * Method getQuitLocY.
     *
     * @return double
     */
    public double getQuitLocY() {
        return y;
    }

    /**
     * Method setQuitLocY.
     *
     * @param d double
     */
    public void setQuitLocY(double d) {
        this.y = d;
    }

    /**
     * Method getQuitLocZ.
     *
     * @return double
     */
    public double getQuitLocZ() {
        return z;
    }

    /**
     * Method setQuitLocZ.
     *
     * @param d double
     */
    public void setQuitLocZ(double d) {
        this.z = d;
    }

    /**
     * Method getWorld.
     *
     * @return String
     */
    public String getWorld() {
        return world;
    }

    /**
     * Method setWorld.
     *
     * @param world String
     */
    public void setWorld(String world) {
        this.world = world;
    }

    /**
     * Method getIp.
     *
     * @return String
     */
    public String getIp() {
        return ip;
    }

    /**
     * Method setIp.
     *
     * @param ip String
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Method getLastLogin.
     *
     * @return long
     */
    public long getLastLogin() {
        return lastLogin;
    }

    /**
     * Method setLastLogin.
     *
     * @param lastLogin long
     */
    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Method getEmail.
     *
     * @return String
     */
    public String getEmail() {
        return email;
    }

    /**
     * Method setEmail.
     *
     * @param email String
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Method getSalt.
     *
     * @return String
     */
    public String getSalt() {
        return this.salt;
    }

    /**
     * Method setSalt.
     *
     * @param salt String
     */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /**
     * Method getHash.
     *
     * @return String
     */
    public String getHash() {
        if (Settings.getPasswordHash == HashAlgorithm.MD5VB) {
            if (salt != null && !salt.isEmpty() && Settings.getPasswordHash == HashAlgorithm.MD5VB) {
                return "$MD5vb$" + salt + "$" + hash;
            }
        }
        return hash;
    }

    /**
     * Method setHash.
     *
     * @param hash String
     */
    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
     * Method equals.
     *
     * @param obj Object
     *
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlayerAuth)) {
            return false;
        }
        PlayerAuth other = (PlayerAuth) obj;
        return other.getIp().equals(this.ip) && other.getNickname().equals(this.nickname);
    }

    /**
     * Method hashCode.
     *
     * @return int
     */
    @Override
    public int hashCode() {
        int hashCode = 7;
        hashCode = 71 * hashCode + (this.nickname != null ? this.nickname.hashCode() : 0);
        hashCode = 71 * hashCode + (this.ip != null ? this.ip.hashCode() : 0);
        return hashCode;
    }

    /**
     * Method toString.
     *
     * @return String
     */
    @Override
    public String toString() {
        return ("Player : " + nickname + " | " + realName
            + " ! IP : " + ip
            + " ! LastLogin : " + lastLogin
            + " ! LastPosition : " + x + "," + y + "," + z + "," + world
            + " ! Email : " + email
            + " ! Hash : " + hash
            + " ! Salt : " + salt);
    }

}
