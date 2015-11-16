package fr.xephi.authme.cache.auth;

import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;

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
    private int groupId;
    private String email;
    private String realName;

    public PlayerAuth(String nickname, String ip, long lastLogin, String realName) {
        this(nickname, "", "", -1, ip, lastLogin, 0, 0, 0, "world", "your@email.com", realName);
    }

    public PlayerAuth(String nickname, double x, double y, double z, String world, String realName) {
        this(nickname, "", "", -1, "127.0.0.1", System.currentTimeMillis(), x, y, z, world, "your@email.com", realName);
    }

    public PlayerAuth(String nickname, String hash, String ip, long lastLogin, String realName) {
        this(nickname, hash, "", -1, ip, lastLogin, 0, 0, 0, "world", "your@email.com", realName);
    }

    public PlayerAuth(String nickname, String hash, String ip, long lastLogin, String email, String realName) {
        this(nickname, hash, "", -1, ip, lastLogin, 0, 0, 0, "world", email, realName);
    }

    public PlayerAuth(String nickname, String hash, String salt, String ip, long lastLogin, String realName) {
        this(nickname, hash, salt, -1, ip, lastLogin, 0, 0, 0, "world", "your@email.com", realName);
    }

    public PlayerAuth(String nickname, String hash, String ip, long lastLogin, double x, double y, double z, String world, String email, String realName) {
        this(nickname, hash, "", -1, ip, lastLogin, x, y, z, world, email, realName);
    }

    public PlayerAuth(String nickname, String hash, String salt, String ip, long lastLogin, double x, double y, double z, String world, String email, String realName) {
        this(nickname, hash, salt, -1, ip, lastLogin, x, y, z, world, email, realName);
    }

    public PlayerAuth(String nickname, String hash, String salt, int groupId, String ip, long lastLogin, String realName) {
        this(nickname, hash, salt, groupId, ip, lastLogin, 0, 0, 0, "world", "your@email.com", realName);
    }

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

    public void setName(String nickname) {
        this.nickname = nickname;
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

    public void setQuitLocX(double d) {
        this.x = d;
    }

    public double getQuitLocX() {
        return x;
    }

    public void setQuitLocY(double d) {
        this.y = d;
    }

    public double getQuitLocY() {
        return y;
    }

    public void setQuitLocZ(double d) {
        this.z = d;
    }

    public double getQuitLocZ() {
        return z;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getWorld() {
        return world;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getSalt() {
        return this.salt;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        if (Settings.getPasswordHash == HashAlgorithm.MD5VB) {
            if (salt != null && !salt.isEmpty() && Settings.getPasswordHash == HashAlgorithm.MD5VB) {
                return "$MD5vb$" + salt + "$" + hash;
            }
        }
        return hash;
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
        return ("Player : " + nickname + " | " + realName
                + " ! IP : " + ip
                + " ! LastLogin : " + lastLogin
                + " ! LastPosition : " + x + "," + y + "," + z + "," + world
                + " ! Email : " + email
                + " ! Hash : " + hash
                + " ! Salt : " + salt);
    }

}
