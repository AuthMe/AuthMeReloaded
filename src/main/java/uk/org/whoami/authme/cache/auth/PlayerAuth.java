/*
 * Copyright 2011 Sebastian Köhler <sebkoehler@whoami.org.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.whoami.authme.cache.auth;

import uk.org.whoami.authme.security.PasswordSecurity;
import uk.org.whoami.authme.settings.Settings;


public class PlayerAuth {

    private String nickname;
    private String hash;
    private String ip = "198.18.0.1";
    private long lastLogin;
    private int x = 0;
    private int y = 0;
    private int z = 0;
    private String salt = "";
    private String vBhash = null;
    private int groupId;
    private String email = "your@email.com";
    
    public PlayerAuth(String nickname, String hash, String ip, long lastLogin) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
    }
    
    public PlayerAuth(String nickname, String hash, String ip, long lastLogin, String email) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.email = email;
    }
    
    public PlayerAuth(String nickname, int x, int y, int z) {
        this.nickname = nickname;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public PlayerAuth(String nickname, String hash, String ip, long lastLogin, int x, int y, int z, String email) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.x = x;
        this.y = y;
        this.z = z;
        this.email = email;
    }
    
    //
    // This constructor is needed for Vbulletin board Auth!
    //
    public PlayerAuth(String nickname, String hash, String salt, int groupId, String ip, long lastLogin, int x, int y, int z, String email) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.x = x;
        this.y = y;
        this.z = z;   
        this.salt = salt;
        this.groupId = groupId;
        this.email = email; 
    }
    
    // IPB Constructor with groups
    public PlayerAuth(String nickname, String hash, String salt, int groupId , String ip, long lastLogin) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;  
        this.salt = salt;
        this.groupId = groupId;
    }
    
    // IPB Constructor without groups
    public PlayerAuth(String nickname, String hash, String salt, String ip, long lastLogin) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;  
        this.salt = salt;
    }
    
    public PlayerAuth(String nickname, String hash, String salt, String ip, long lastLogin, int x, int y, int z, String email) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.x = x;
        this.y = y;
        this.z = z;   
        this.salt = salt;
        this.email = email; 
    }

    
    public PlayerAuth(String nickname, String hash, String ip, long lastLogin, int x, int y, int z) {
        this.nickname = nickname;
        this.hash = hash;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.x = x;
        this.y = y;
        this.z = z;
        this.email = "your@email.com";
    }


	public String getIp() {
        return ip;
    }

    public String getNickname() {
        return nickname;
    }

    public String getHash() {
        if(!salt.isEmpty() && Settings.getPasswordHash == PasswordSecurity.HashAlgorithm.MD5VB) {
        	vBhash = "$MD5vb$"+salt+"$"+hash;
            // Compose Vbullettin Hash System!
            return vBhash;
        }
        else {
        	return hash;
        } 
    }
    
    //Get Salt
    public String getSalt() {
    	return this.salt;
    }
    
    //
    // GroupId for unactivated User on Vbullettin Board
    //
    public int getGroupId() {
        return groupId;
    }
    
    public int getQuitLocX() {
        return x;
    }
    public int getQuitLocY() {
        return y;
    }
    public int getQuitLocZ() {
        return z;
    }
    public String getEmail() {
    	return email;
    }
    public void setQuitLocX(int x) {
        this.x = x;
    }
    public void setQuitLocY(int y) {
        this.y = y;
    }
    public void setQuitLocZ(int z) {
        this.z = z;
    }  
    public long getLastLogin() {
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
}
