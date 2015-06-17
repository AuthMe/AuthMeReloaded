<hr>
![AuthMeReloaded](http://www.imagestorming.com/media/cys/1328692322/authme.png)
####The most used authentication plugin for CraftBukkit/Spigot!
<hr>
#####Development tools:

- Build status: [![Build Status](https://travis-ci.org/Xephi/AuthMeReloaded.svg?branch=master)](https://travis-ci.org/Xephi/AuthMeReloaded)

- Build Server (Jenkins): http://ci.xephi.fr

- JavaDoc: <a href="http://xephi.github.io/AuthMeReloaded/index.html">AuthMe Javadoc</a></p>

- MavenRepository: <p><a href="http://xephi.fr:8080/plugin/repository/everything/">Maven Repo</a></p>

<hr>

#####Compiling Requirements:
>- JDK 1.7
>- Maven
>- Git/Github (Optional)

#####How to compile the project:
>- Clone the project with Git/Github
>- Execute command "mvn clean install"

#####Running Requirements:
>- Java 1.7 (should work also with Java 1.8)
>- Spigot or CraftBukkit

<hr>
###Plugin Description:

#####"/login|/register plugin!"

Prevent Name stolen ! AutoUpdate names through UUID<br>
AuthMe Reloaded prevents players who aren't logged in from actions like placing blocks, moving, typing commands or seeing the inventory of the current player. </p>
<p>The possibility to set up name spoof protection kicks players with uncommon long or short player names before they could actually join.</p>
<p>Login Sessions make it possible that you don't have to login within a given time period.</p>
<p>Each command and every setting can be enabled or disabled by a easy structured config file. </p>
<p>If you don't like English or don't like my translations you can easily edit almost every message sent by AuthMe!</p>
======================
####Features:
<ul><li><strong>E-Mail Recovery System</strong> !!!
</li><li>Playername spoof protection
</li><li>Countries Selection! <a href="http://dev.bukkit.org/bukkit-plugins/authme-reloaded/pages/countries-codes/">(countries codes)</a>
</li><li>AntiBot Features!
</li><li><strong> Passpartu Admin Feature: Admin can login with all account</strong> more info <a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/how-to-install-and-initial-configuration/">here</a>
</li><li>Protection against "Logged in from another location" messages
</li><li>Login sessions
</li><li>Editable settings &amp; messages
</li><li>MySQL, flatfile and SQLITE support
</li><li>Supported hash algorithms: MD5, SHA1, SHA256, <a href="https://github.com/CypherX/xAuth/wiki/Password-Hashing">xAuth</a>, <a href="http://en.wikipedia.org/wiki/Whirlpool_(cryptography)">Whirlpool</a>
</li><li>Support for PLAINTEXT password storage
</li><li>Support for PHPBB, VBullettin forum registration: <strong>MD5VB - PHPBB</strong>
</li><li>Support for MyBB : <strong>MYBB</strong>
</li><li>Support for IPB3 : <strong>IPB3</strong>
</li><li>Support for PhpFusion : <strong>PHPFUSION</strong>
</li><li>Support for Xenforo SHA1 with : <strong>XFSHA1</strong>
</li><li>Support for Xenforo SHA256 with : <strong>XFSHA256</strong>
</li><li>Support for Joomla with : <strong>JOOMLA</strong>
</li><li>Support for WBB3 with : <strong>WBB3*</strong>
</li><li>Support for SHA512 with : <strong>SHA512</strong>
</li><li>Support DoubleSaltedMD5 password with : <strong>SALTED2MD5</strong>
</li><li>Support WordPress integration, password with : <strong>WORDPRESS</strong>
</li><li>Custom MySQL tables/columns (useable for forums, other scripts)
</li><li>Database queries can be cached
</li><li><em>Compatible with Citizens NPC plugin and CombatTag plugin</em>
</li><li>Compatible with Minecraft mods like <strong>BuildCraft or RedstoneCraft</strong>
</li><li><em>Account restriction through IP and name</em>
</li><li><em>Permissions group switching on un-logged-in</em>
</li><li>Different permission group for Registered and unRegistered users
</li><li>Support for permissions onJoin with transient vault system
</li><li>Cache on file for all inventories and enchants for un-logged-in players
</li><li>Save Quit location to prevent loss of position
</li><li>Possible to use without a Permissions plugin
</li><li><strong>Spoutcraft Login GUI</strong>
</li><li>Automatic backup system of all your user password data
</li><li>Default Language Style: en, de, br, cz, pl, fr, ru, hu, sk, es, zhtw, fi, zhcn, nl ( feel free to send new translations )
</li><li>Convert the FlatFile auths.db to an usefull authme.sql that you can use on a MySQL database !
</li><li>Import your database from Rakamak, xAuth, CrazyLogin, RoyalAuth, vAuth !
</li></ul>
####Configuration
<p><a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/configure-auth-me/">How to Configure Authme</a></p>
####Email Recovery Dependency
<p><a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/how-to-configure-email-recovery-system/">How to configure email recovery system?</a></p>
####Commands
<p><a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/command/">Command list and usage</a></p>
<h2 id="w-permissions">Permissions</h2>
<ul><li>authme.player.* - for all user command
</li><li>authme.admin.* - for all admin command
</li><li>authme.* - for all user and admin command
</li><li><a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/permissions/">List of all single permissions</a>
</li></ul>
####How To
<ul><li><a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/how-to-install-and-initial-configuration/">How to Install and Setup</a>
</li><li><a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/how-to-import-database-from-xauth/">How to import database from xAuth</a>
</li><li><a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/web-site-integration/">WebSite Integration</a>
</li><li><a href="https://raw.githubusercontent.com/Xephi/AuthMeReloaded/master/src/main/resources/config.yml">Click here for an example of the Config file</a>
</li><li><a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/how-to-import-database-from-rakamak/">How to convert from Rakamak</a>
</li><li>Convert from FlatFile (auths.db but not the sqlite one ) to MySQL : /converter
</li></ul>
======================


<h2 id="w-geo-ip">GeoIP</h2>
<p>This product includes and download automatically GeoLite data created by MaxMind, available from <a href="http://www.maxmind.com">http://www.maxmind.com</a></p>
<h2 id="w-donate">Donate</h2>
<p>Do you like my work ? Or just want to buy me a coffee for quickly update?<br>
Use this link in EUR :
<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&amp;hosted_button_id=QLMM9SNCX825Y"><img src="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif" alt="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif" title="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif"></a>
Or this link in USD :
<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&amp;hosted_button_id=PWQMYCP2SAH6L"><img src="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif" alt="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif" title="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif"></a></p>
<p>Credit for old version to d4rkwarriors, fabe1337 , Whoami and pomo4ka</p>
<p>Thanks to : AS1LV3RN1NJA, Hoeze, eprimex</p>
<h2 id="w-official-servers">Official servers</h2>
<p><a href="http://www.minewish.fr/">Minewish Serveur homepage (French server)</a> <br>
<img src="https://minestatus.net/47900-minewish/image/original.png" alt="Minewish" title="Minewish"> <br>
<a href="http://www.rautamiekka.org/?page_id=14">EpriMC homepage (English/Finnish)</a>
<img src="http://minecraft-server-list.com/server/logo/51237.png" alt="EpriMC" title="EpriMC"> <br></p>
