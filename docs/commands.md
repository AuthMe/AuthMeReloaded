<!-- AUTO-GENERATED FILE! Do not edit this directly -->
<!-- File auto-generated on Sat Oct 01 23:33:39 CEST 2016. See commands/commands.tpl.md -->

## AuthMe Commands
You can use the following commands to use the features of AuthMe. Mandatory arguments are marked with `< >`
brackets; optional arguments are enclosed in square brackets (`[ ]`).

- **/authme**: The main AuthMeReloaded command. The root for all admin commands.
- **/authme register** &lt;player> &lt;password>: Register the specified player with the specified password.
  <br />Requires `authme.admin.register`
- **/authme unregister** &lt;player>: Unregister the specified player.
  <br />Requires `authme.admin.unregister`
- **/authme forcelogin** [player]: Enforce the specified player to login.
  <br />Requires `authme.admin.forcelogin`
- **/authme password** &lt;player> &lt;pwd>: Change the password of a player.
  <br />Requires `authme.admin.changepassword`
- **/authme lastlogin** [player]: View the date of the specified players last login.
  <br />Requires `authme.admin.lastlogin`
- **/authme accounts** [player]: Display all accounts of a player by his player name or IP.
  <br />Requires `authme.admin.accounts`
- **/authme email** [player]: Display the email address of the specified player if set.
  <br />Requires `authme.admin.getemail`
- **/authme setemail** &lt;player> &lt;email>: Change the email address of the specified player.
  <br />Requires `authme.admin.changemail`
- **/authme getip** &lt;player>: Get the IP address of the specified online player.
  <br />Requires `authme.admin.getip`
- **/authme spawn**: Teleport to the spawn.
  <br />Requires `authme.admin.spawn`
- **/authme setspawn**: Change the player's spawn to your current position.
  <br />Requires `authme.admin.setspawn`
- **/authme firstspawn**: Teleport to the first spawn.
  <br />Requires `authme.admin.firstspawn`
- **/authme setfirstspawn**: Change the first player's spawn to your current position.
  <br />Requires `authme.admin.setfirstspawn`
- **/authme purge** &lt;days> [all]: Purge old AuthMeReloaded data longer than the specified amount of days ago.
  <br />Requires `authme.admin.purge`
- **/authme resetpos** &lt;player/*>: Purge the last know position of the specified player or all of them.
  <br />Requires `authme.admin.purgelastpos`
- **/authme purgebannedplayers**: Purge all AuthMeReloaded data for banned players.
  <br />Requires `authme.admin.purgebannedplayers`
- **/authme switchantibot** [mode]: Switch or toggle the AntiBot mode to the specified state.
  <br />Requires `authme.admin.switchantibot`
- **/authme reload**: Reload the AuthMeReloaded plugin.
  <br />Requires `authme.admin.reload`
- **/authme version**: Show detailed information about the installed AuthMeReloaded version, the developers, contributors, and license.
- **/authme converter** &lt;job>: Converter command for AuthMeReloaded.
  <br />Requires `authme.admin.converter`
- **/authme help** [query]: View detailed help for /authme commands.
- **/login** &lt;password>: Command to log in using AuthMeReloaded.
  <br />Requires `authme.player.login`
- **/login help** [query]: View detailed help for /login commands.
- **/logout**: Command to logout using AuthMeReloaded.
  <br />Requires `authme.player.logout`
- **/logout help** [query]: View detailed help for /logout commands.
- **/register** [password] [verifyPassword]: Command to register using AuthMeReloaded.
  <br />Requires `authme.player.register`
- **/register help** [query]: View detailed help for /register commands.
- **/unregister** &lt;password>: Command to unregister using AuthMeReloaded.
  <br />Requires `authme.player.unregister`
- **/unregister help** [query]: View detailed help for /unregister commands.
- **/changepassword** &lt;oldPassword> &lt;newPassword>: Command to change your password using AuthMeReloaded.
  <br />Requires `authme.player.changepassword`
- **/changepassword help** [query]: View detailed help for /changepassword commands.
- **/email**: The AuthMeReloaded Email command base.
- **/email add** &lt;email> &lt;verifyEmail>: Add a new email address to your account.
  <br />Requires `authme.player.email.add`
- **/email change** &lt;oldEmail> &lt;newEmail>: Change an email address of your account.
  <br />Requires `authme.player.email.change`
- **/email recover** &lt;email> [code]: Recover your account using an Email address by sending a mail containing a new password.
  <br />Requires `authme.player.email.recover`
- **/email help** [query]: View detailed help for /email commands.
- **/captcha** &lt;captcha>: Captcha command for AuthMeReloaded.
  <br />Requires `authme.player.captcha`
- **/captcha help** [query]: View detailed help for /captcha commands.


---

This page was automatically generated on the [AuthMe/AuthMeReloaded repository](https://github.com/AuthMe/AuthMeReloaded/tree/master/docs/) on Sat Oct 01 23:33:39 CEST 2016
