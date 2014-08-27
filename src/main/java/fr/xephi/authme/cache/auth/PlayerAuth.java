package fr.xephi.authme.cache.auth;

import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;

public class PlayerAuth {

    private String nickname = "";
    private String hash = "";
    private String ip = "198.18.0.1";
    private long lastLogin = 0;
    private double x = 0;
    private double y = 0;
    private double z = 0;
    private String world = "world";
    private String salt = "";
    private String vBhash = null;
    private int groupId = -1;
    private String email = "your@email.com";

    public PlayerAuth(String nickname, String hash, String ip, long lastLogin,
            String email) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.email = email;

    }

    public PlayerAuth(String nickname, double x, double y, double z,
            String world) {
        this.nickname = nickname;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.lastLogin = System.currentTimeMillis();

    }

    public PlayerAuth(String nickname, String hash, String ip, long lastLogin,
            double x, double y, double z, String world, String email) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.email = email;

    }

    public PlayerAuth(String nickname, String hash, String salt, int groupId,
            String ip, long lastLogin, double x, double y, double z,
            String world, String email) {
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

    }

    public PlayerAuth(String nickname, String hash, String salt, int groupId,
            String ip, long lastLogin) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.salt = salt;
        this.groupId = groupId;

    }

    public PlayerAuth(String nickname, String hash, String salt, String ip,
            long lastLogin) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.salt = salt;

    }

    public PlayerAuth(String nickname, String hash, String salt, String ip,
            long lastLogin, double x, double y, double z, String world,
            String email) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.salt = salt;
        this.email = email;

    }

    public PlayerAuth(String nickname, String ip, long lastLogin) {
        this.nickname = nickname;
        this.ip = ip;
        this.lastLogin = lastLogin;

    }

    public PlayerAuth(String nickname, String hash, String ip, long lastLogin) {
        this.nickname = nickname;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.hash = hash;
    }

    public String getIp() {
        if (ip == null || ip.isEmpty())
            ip = "127.0.0.1";
        return ip;
    }

    public String getNickname() {
        return nickname;
    }

    public String getHash() {
        if (Settings.getPasswordHash == HashAlgorithm.MD5VB) {
            if (salt != null && !salt.isEmpty() && Settings.getPasswordHash == HashAlgorithm.MD5VB) {
                vBhash = "$MD5vb$" + salt + "$" + hash;
                return vBhash;
            }
        }
        return hash;
    }

    public String getSalt() {
        return this.salt;
    }

    public int getGroupId() {
        return groupId;
    }

    public double getQuitLocX() {
        return x;
    }

    public double getQuitLocY() {
        return y;
    }

    public double getQuitLocZ() {
        return z;
    }

    public String getEmail() {
        return email;
    }

    public void setQuitLocX(double d) {
        this.x = d;
    }

    public void setQuitLocY(double d) {
        this.y = d;
    }

    public void setQuitLocZ(double d) {
        this.z = d;
    }

    public long getLastLogin() {
        try {
            if (Long.valueOf(lastLogin) == null)
                lastLogin = 0L;
        } catch (NullPointerException e) {
            lastLogin = 0L;
        }
        return lastLogin;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSalt(String salt) {
        this.salt = salt;
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

    public void setWorld(String world) {
        this.world = world;
    }

    public String getWorld() {
        return world;
    }

    @Override
    public String toString() {
        String s = "Player : " + nickname + " ! IP : " + ip + " ! LastLogin : " + lastLogin + " ! LastPosition : " + x + "," + y + "," + z + "," + world + " ! Email : " + email + " ! Hash : " + hash + " ! Salt : " + salt;
        return s;

    }

    public void setName(String nickname) {
        this.nickname = nickname;
    }

}
