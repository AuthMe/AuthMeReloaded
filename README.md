<p align="center"><img src="http://i61.tinypic.com/291dm49.png"></p>
<p align="center"><strong>The most used authentication plugin for CraftBukkit/Spigot!</strong></p>
<hr>

#####Development tools:

- MAIN REPO (**release sources, issue tracker!**): [Github Main Page](https://github.com/Xephi/AuthMeReloaded)

- DEVELOPMENT TEAM REPO (**latest sources, please send PRs here!**): [Github Development Page](https://github.com/AuthMe/AuthMeReloaded)

- Developers ChatRoom: [![Join the chat at https://gitter.im/Xephi/AuthMeReloaded](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Xephi/AuthMeReloaded?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

- Build Server (**DEVELOPMENT BUILDS**): [Xephi's Jenkins](http://ci.xephi.fr/job/AuthMeReloaded)

- Build status: [![Build Status](https://travis-ci.org/AuthMe/AuthMeReloaded.svg?branch=master)](https://travis-ci.org/AuthMe/AuthMeReloaded)

- Dependencies: [![Dependency Status](https://www.versioneye.com/user/projects/57b182e8d6ffcd0032d7cf2d/badge.svg)](https://www.versioneye.com/user/projects/57b182e8d6ffcd0032d7cf2d)

- Build status (CircleCI): [![Circle CI](https://circleci.com/gh/AuthMe/AuthMeReloaded.svg?style=svg)](https://circleci.com/gh/AuthMe/AuthMeReloaded)
- Alternative Dev Build download link (via CircleCi): <a href="https://circleci-tkn.rhcloud.com/api/v1/project/AuthMe/AuthMeReloaded/tree/master/latest/artifacts/AuthMe.jar">Download</a>
- JitPack (just in case): [![](https://jitpack.io/v/AuthMe/AuthMeReloaded.svg)](https://jitpack.io/#AuthMe/AuthMeReloaded)

- Code Coverage: [![Coverage Status](https://coveralls.io/repos/AuthMe-Team/AuthMeReloaded/badge.svg?branch=master&service=github)](https://coveralls.io/github/AuthMe-Team/AuthMeReloaded?branch=master)

- Issue Tracking : [![Stories in Ready](https://badge.waffle.io/Xephi/AuthMeReloaded.png?label=ready&title=Ready)](https://waffle.io/Xephi/AuthMeReloaded) [![Stories in Bugs](https://badge.waffle.io/Xephi/AuthMeReloaded.png?label=bugs&title=Bugs)](https://waffle.io/Xephi/AuthMeReloaded) [![Stories in In%20Progress](https://badge.waffle.io/Xephi/AuthMeReloaded.png?label=in%20progress&title=In%20Progress)](https://waffle.io/Xephi/AuthMeReloaded)

- JavaDoc: <a href="http://ci.xephi.fr/job/AuthMeReloaded/javadoc/">AuthMe Javadoc</a>

- Maven Repo: <a href="http://ci.xephi.fr/plugin/repository/everything/">AuthMe Repo</a>

#####Statistics:

McStats: http://mcstats.org/plugin/AuthMe

<img src="http://i.mcstats.org/AuthMe/Global+Statistics.borderless.png">

<img src="http://i.mcstats.org/AuthMe/Rank.borderless.png">

<img src="http://i.mcstats.org/AuthMe/Version+Demographics.borderless.png">

#####Development history:
Outdated!
[![Gource AuthMe History Video](http://img.youtube.com/vi/hJRzNfYyd9k/hqdefault.jpg)](https://www.youtube.com/watch?v=hJRzNfYyd9k)

<hr>

#####Compiling Requirements:
>- JDK 1.8
>- Maven
>- Git/Github (Optional)

#####How to compile the project:
>- Clone the project with Git/Github
>- Execute command "mvn clean install"

#####Running Requirements:
>- Java 1.8
>- PaperSpigot, Spigot or CraftBukkit (1.7.10, 1.8.X, 1.9.X, 1.10.X)
>- ProtocolLib (optional, required by the protectInventory feature)

<hr>
###Plugin Description:

#####"The best authentication plugin for the Bukkit/Spigot API!"

Prevent username stealing on your server!<br>
Use it to secure your Offline mode server or to increase your Online mode server's protection!

AuthMeReloaded disallows players who aren't authenticated to do actions like placing blocks, moving,<br>
typing commands or using the inventory. It can also kick players with uncommonly long or short player names or kick players from banned countries.

With the Session Login feature you don't have to execute the authentication command every time you connect to the server! 
Each command and every feature can be enabled or disabled from our well structured configuration file.

You can also create your own translation file and, if you want, you can share it with us! :)

####Features:
<ul>
  <li><strong>E-Mail Recovery System !!!</strong></li>
  <li>Username spoofing protection.</li>
  <li>Countries Whitelist/Blacklist! <a href="http://dev.bukkit.org/bukkit-plugins/authme-reloaded/pages/countries-codes/">(countries codes)</a></li>
  <li><strong>Built-in AntiBot System!</strong></li>
  <li><strong>ForceLogin Feature: Admins can login with all account via console command!</strong></li>
  <li><strong>Avoid the "Logged in from another location" message!</strong></li>
  <li>Session Login!</li>
  <li>Editable translations and messages!</li>
  <li><strong>MySQL and SQLite Backend support!</strong></li>
  <li>Supported password encryption algorithms: MD5, SHA1, SHA256, <a href="https://github.com/CypherX/xAuth/wiki/Password-Hashing">xAuth</a>, <a href="http://en.wikipedia.org/wiki/Whirlpool_(cryptography)">Whirlpool</a></li>
  <li>Supported alternative registration methods:<br>
  <ul>
    <li>PHPBB, VBulletin: MD5VB - PHPBB</li>
    <li>Xenforo: XFBCRYPT</li>
    <li>MyBB: MYBB</li>
    <li>IPB3: IPB3</li>
    <li>IPB4: IPB4</li>
    <li>PhpFusion: PHPFUSION</li>
    <li>Joomla: JOOMLA</li>
    <li>WBB3: WBB3*</li>
    <li>SHA512: SHA512</li>
    <li>DoubleSaltedMD5: SALTED2MD5</li>
    <li>WordPress: WORDPRESS</li>
  </ul></li>
  <li>Custom MySQL tables/columns names (useful with forums databases)</li>
  <li><strong>Cached database queries!</strong></li>
  <li><strong>Fully compatible with Citizens2, CombatTag, CombatTagPlus and ChestShop!</strong></li>
  <li>Compatible with Minecraft mods like <strong>BuildCraft or RedstoneCraft</strong></li>
  <li>Restricted users (associate a Username with an IP)</li>
  <li>Protect player's inventory until a correct Authentication</li>
  <li>Saves the quit location of the player</li>
  <li>Automatic database Backup</li>
  <li>Available languages: en, de, br, cz, pl, fr, uk, ru, hu, sk, es, fi, zhtw, zhhk, zhcn, lt, it, ko, pt, nl, gl, bg, eu, tr, vn (feel free to send new translations)</li>
  <li>Built-in Deprecated FlatFile (auths.db) to SQL (authme.sql) converter!</li>
  <li><strong>Import your old database from other plugins like Rakamak, xAuth, CrazyLogin, RoyalAuth and vAuth!</strong></li>
</ul>

####Configuration
<a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/configure-auth-me/">How to Configure Authme</a>
####Email Recovery Dependency
<a href="http://dev.bukkit.org/server-mods/authme-reloaded/pages/how-to-configure-email-recovery-system/">How to configure email recovery system?</a>
####Commands
[Command list and usage](https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/commands.md)
####Permissions
- authme.player.* - for all user commands
- authme.admin.* - for all admin commands
- [List of all permission nodes](http://github.com/AuthMe-Team/AuthMeReloaded/blob/master/docs/permission_nodes.md)

####How To
- [How to install and set up](http://dev.bukkit.org/server-mods/authme-reloaded/pages/how-to-install-and-initial-configuration/)
- [How to import database from xAuth](http://dev.bukkit.org/server-mods/authme-reloaded/pages/how-to-import-database-from-xauth/)
- [Website integration](http://dev.bukkit.org/server-mods/authme-reloaded/pages/web-site-integration/)
- [Click here for an example of the config file](https://raw.githubusercontent.com/Xephi/AuthMeReloaded/master/src/main/resources/config.yml)
- [How to convert from Rakamak](http://dev.bukkit.org/server-mods/authme-reloaded/pages/how-to-import-database-from-rakamak/)
- Convert from FlatFile (auths.db but not the sqlite one) to MySQL: /authme converter

<hr>

#####GeoIP
This product uses data from the GeoLite API created by MaxMind, available at http://www.maxmind.com

<hr>

#####Donate
<p>Do you like our work? Do you want to buy us a coffee? :)<br>
EUR: <a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&amp;hosted_button_id=QLMM9SNCX825Y"><img src="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif"></a>
USD: <a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&amp;hosted_button_id=PWQMYCP2SAH6L"><img src="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif"></a></p>

#####Sponsor
GameHosting.it is leader in Italy as Game Server Provider. With its own DataCenter offers Anti-DDoS solutions at affordable prices. Game Server of Minecraft based on Multicraft are equipped with the latest technology in hardware.
[![GameHosting](http://www.gamehosting.it/images/bn3.png)](http://www.gamehosting.it)

#####Credits
<p>Team members: look at the <a href="https://github.com/AuthMe/AuthMeReloaded/blob/master/team.txt">team.txt file</a>
<p>Credit for old version of the plugin to: d4rkwarriors, fabe1337, Whoami2 and pomo4ka</p>
<p>Thanks also to: AS1LV3RN1NJA, Hoeze and eprimex</p>
