# AuthMeReloaded
**"The best authentication plugin for the Bukkit modding API!"**

<img src="wallpaper.png?raw=true" alt="AuthMeLogo"/>

| Type              | Badges                                                                                                                                                                                                                                                                                                                                                                                |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **General:**      | ![](https://tokei.rs/b1/github/AuthMe/AuthMeReloaded?category=code) ![](https://tokei.rs/b1/github/AuthMe/AuthMeReloaded?category=files)                                                                                                                                                                                                                                              |
| **Code quality:** | [![Code Climate](https://codeclimate.com/github/AuthMe/AuthMeReloaded/badges/gpa.svg)](https://codeclimate.com/github/AuthMe/AuthMeReloaded) [![Coverage status](https://coveralls.io/repos/AuthMe-Team/AuthMeReloaded/badge.svg?branch=master&service=github)](https://coveralls.io/github/AuthMe-Team/AuthMeReloaded?branch=master)                                                 |
| **Jenkins CI:**   | [![Jenkins Status](https://img.shields.io/website-up-down-green-red/http/shields.io.svg?label=ci.codemc.org)](https://ci.codemc.org/) [![Build Status](https://ci.codemc.org/buildStatus/icon?job=AuthMe/AuthMeReloaded)](https://ci.codemc.org/job/AuthMe/job/AuthMeReloaded) ![Build Tests](https://img.shields.io/jenkins/t/https/ci.codemc.org/job/AuthMe/job/AuthMeReloaded.svg) |
| **Other CIs:**    | [![Build Status](https://www.travis-ci.com/AuthMe/AuthMeReloaded.svg?branch=master)](https://www.travis-ci.com/AuthMe/AuthMeReloaded)                                                                                                                                                                                                                                                             |

## Description

Prevent username stealing on your server!<br>
Use it to secure your Offline mode server or to increase your Online mode server's protection!

AuthMeReloaded disallows players who aren't authenticated to do actions like placing blocks, moving,<br>
typing commands or using the inventory. It can also kick players with uncommonly long or short player names or kick players from banned countries.

With the Session Login feature you don't have to execute the authentication command every time you connect to the server! 
Each command and every feature can be enabled or disabled from our well structured configuration file.

You can also create your own translation file and, if you want, you can share it with us! :)

#### Features:
<ul>
  <li><strong>E-Mail Recovery System!</strong></li>
  <li>Username spoofing protection.</li>
  <li>Countries Whitelist/Blacklist! <a href="https://dev.maxmind.com/geoip/legacy/codes/iso3166/">(country codes)</a></li>
  <li><strong>Built-in AntiBot System!</strong></li>
  <li><strong>ForceLogin Feature: Admins can login with all account via console command!</strong></li>
  <li><strong>Avoid the "Logged in from another location" message!</strong></li>
  <li>Two-factor (2FA) support!</li>
  <li>Session Login!</li>
  <li>Editable translations and messages!</li>
  <li><strong>MySQL and SQLite Backend support!</strong></li>
  <li>Supported password encryption algorithms: SHA256, ARGON2, BCRYPT, PBKDF2, <a href="https://github.com/CypherX/xAuth/wiki/Password-Hashing">xAuth</a></li>
  <li>Supported alternative registration methods:<br>
  <ul>
    <li>PHPBB, VBulletin: PHPBB - MD5VB</li>
    <li>Xenforo: XFBCRYPT</li>
    <li>MyBB: MYBB</li>
    <li>IPB3: IPB3</li>
    <li>IPB4: IPB4</li>
    <li>PhpFusion: PHPFUSION</li>
    <li>Joomla: JOOMLA</li>
    <li>WBB3: WBB3*</li>
    <li>SHA512: SALTEDSHA512</li>
    <li>DoubleSaltedMD5: SALTED2MD5</li>
    <li>WordPress: WORDPRESS</li>
    <li><a href="https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/hash_algorithms.md">List of all supported hashes</a></li>
  </ul></li>
  <li>Custom MySQL tables/columns names (useful with forum databases)</li>
  <li><strong>Cached database queries!</strong></li>
  <li><strong>Fully compatible with Citizens2, CombatTag, CombatTagPlus!</strong></li>
  <li>Compatible with Minecraft mods like <strong>BuildCraft or RedstoneCraft</strong></li>
  <li>Restricted users (associate a username with an IP)</li>
  <li>Protect player's inventory until correct authentication (requires ProtocolLib)</li>
  <li>Saves the quit location of the player</li>
  <li>Automatic database backup</li>
  <li>Available languages: <a href="https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/translations.md">translations</a></li>
  <li>Built-in Deprecated FlatFile (auths.db) to SQL (authme.sql) converter!</li>
  <li><strong>Import your old database from other plugins like Rakamak, xAuth, CrazyLogin, RoyalAuth and vAuth!</strong></li>
</ul>

#### Configuration
[How to configure AuthMe](https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/config.md)
#### Commands
[Command list and usage](https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/commands.md)
#### Permissions
- authme.player.* - for all user commands
- authme.admin.* - for all admin commands
- [List of all permission nodes](http://github.com/AuthMe/AuthMeReloaded/blob/master/docs/permission_nodes.md)

#### How To
- [How to use the converter](https://github.com/AuthMe/AuthMeReloaded/wiki/Converters)
- [How to import database from xAuth](https://dev.bukkit.org/projects/authme-reloaded/pages/how-to-import-database-from-xauth)
- [Website integration](https://github.com/AuthMe/AuthMeReloaded/tree/master/samples/website_integration)
- [How to convert from Rakamak](https://dev.bukkit.org/projects/authme-reloaded/pages/how-to-import-database-from-rakamak)
- Convert between database types (e.g. SQLite to MySQL): /authme converter


## Links and Contacts

 - **Support:**
   - [GitHub issue tracker](https://github.com/AuthMe/AuthMeReloaded/issues)
   - [Discord](https://discord.gg/Vn9eCyE)
   - [BukkitDev page](https://dev.bukkit.org/projects/authme-reloaded)
   - [Spigot page](https://www.spigotmc.org/resources/authmereloaded.6269/)

- **Dev resources:**
  - <a href="https://ci.codemc.org/job/AuthMe/job/AuthMeReloaded/javadoc/">JavaDocs</a>
  - <a href="http://repo.codemc.org/repository/maven-public/">Maven Repository</a>
  ```xml
    <repositories>
        <repository>
            <id>codemc-repo</id>
            <url>https://repo.codemc.org/repository/maven-public/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>fr.xephi</groupId>
            <artifactId>authme</artifactId>
            <version>5.6.0-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
  ```

- **Statistics:**
    ![Graph](https://bstats.org/signatures/bukkit/AuthMe.svg)

## Requirements

##### Compiling requirements:
>- JDK 11 (JDK 17 is recommended)
>- Maven
>- Git/Github (Optional)

##### How to compile the project:
>- Clone the project with Git/GitHub
>- Execute command "mvn clean package"

##### Running requirements:
>- Java 8 (Java 17 is recommended)
>- Paper or Spigot (1.8.X and up)<br>
   (In case you use Thermos, Cauldron or similar, you have to update the SpecialSource library to support Java 8 plugins.
   HowTo: https://github.com/games647/FastLogin/issues/111#issuecomment-272331347)
>- ProtocolLib (optional, required by some features)

## Credits

##### Contributors:
Team members: <a href="https://github.com/AuthMe/AuthMeReloaded/wiki/Development-team">developers</a>, <a href="https://github.com/AuthMe/AuthMeReloaded/wiki/Translators">translators</a>

Credits for the old version of the plugin: d4rkwarriors, fabe1337, Whoami2 and pomo4ka

Thanks also to: AS1LV3RN1NJA, Hoeze and eprimex

##### GeoIP License:
This product uses data from the GeoLite API created by MaxMind, available at https://www.maxmind.com
