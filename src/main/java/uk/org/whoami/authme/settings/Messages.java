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

package uk.org.whoami.authme.settings;

import java.io.File;

public class Messages extends CustomConfiguration {

    private static Messages singleton = null;

    public Messages() {
        super(new File(Settings.MESSAGE_FILE+"_"+Settings.messagesLanguage+".yml"));
        loadDefaults();
        loadFile();
        singleton = this;
    }

    private void loadDefaults() {
        this.set("logged_in", "&cAlready logged in!");
        this.set("not_logged_in", "&cNot logged in!");
        this.set("reg_disabled", "&cRegistration is disabled");
        this.set("user_regged", "&cUsername already registered");
        this.set("usage_reg", "&cUsage: /register password ConfirmPassword");
        this.set("usage_log", "&cUsage: /login password");
        this.set("user_unknown", "&cUsername not registered");
        this.set("pwd_changed", "&cPassword changed!");
        this.set("reg_only", "&fRegistered players only! Please visit http://example.com to register");
        this.set("valid_session", "&cSession login");
        this.set("login_msg", "&cPlease login with \"/login password\"");
        this.set("reg_msg", "&cPlease register with \"/register password ConfirmPassword\"");
        this.set("reg_email_msg", "&cPlease register with \"/register <email> <confirmEmail>\"");
        this.set("timeout", "&fLogin Timeout");
        this.set("wrong_pwd", "&cWrong password");
        this.set("logout", "&cSuccessful logout");
        this.set("usage_unreg", "&cUsage: /unregister password");
        this.set("registered", "&cSuccessfully registered!");
        this.set("unregistered", "&cSuccessfully unregistered!");
        this.set("login", "&cSuccessful login!");
        this.set("no_perm", "&cNo Permission");
        this.set("same_nick", "&fSame nick is already playing");
        this.set("reg_voluntarily", "&fYou can register your nickname with the server with the command \"/register password ConfirmPassword\"");
        this.set("reload", "&fConfiguration and database has been reloaded");
        this.set("error", "&fAn error ocurred; Please contact the admin");
        this.set("unknown_user", "&fUser is not in database");
        this.set("unsafe_spawn","&fYour Quit location was unsafe, teleporting you to World Spawn");
        this.set("unvalid_session","&fSession Dataes doesnt corrispond Plaese wait the end of session");
        this.set("max_reg","&fYou have Exceded the max number of Registration for your Account"); 
        this.set("password_error","&fPassword doesnt match");
        this.set("pass_len","&fYour password dind''t reach the minimum length or exeded the max length");
        this.set("vb_nonActiv","&fYour Account isent Activated yet check your Emails!");
        this.set("usage_changepassword", "&fUsage: /changepassword oldPassword newPassword");
        this.set("name_len", "&cYour nickname is too Short or too long");
        this.set("regex", "&cYour nickname contains illegal characters. Allowed chars: REG_EX");
        this.set("add_email","&cPlease add your email with : /email add yourEmail confirmEmail");
        this.set("bad_database_email", "[AuthMe] This /email command only available with MySQL and SQLite, contact an Admin");
        this.set("recovery_email", "&cForgot your password? Please use /email recovery <yourEmail>");
        this.set("usage_captcha", "&cUsage: /captcha <theCaptcha>");
        this.set("wrong_captcha", "&cWrong Captcha, please use : /captcha THE_CAPTCHA");
        this.set("valid_captcha", "&cYour captcha is valid !");
        this.set("kick_forvip", "&cA VIP Player join the full server!");
        this.set("kick_fullserver", "&cThe server is actually full, Sorry!");
        this.set("usage_email_add", "&fUsage: /email add <Email> <confirmEmail> ");
        this.set("usage_email_change", "&Usage: /email change <old> <new> ");
        this.set("usage_email_recovery", "&Usage: /email recovery <Email>");
        this.set("email_add", "[AuthMe] /email add <Email> <confirmEmail>");
        this.set("new_email_invalid", "[AuthMe] New email invalid!");
        this.set("old_email_invalid", "[AuthMe] Old email invalid!");
        this.set("email_invalid", "[AuthMe] Invalid Email !");
        this.set("email_added", "[AuthMe] Email Added !");
        this.set("email_confirm", "[AuthMe] Confirm your Email !");
        this.set("email_changed", "[AuthMe] Email Change !");
        this.set("email_send", "[AuthMe] Recovery Email Send !");
    }

	private void loadFile() {
        this.load();
        this.save();
    }

    public String _(String msg) {
        String loc = (String) this.get(msg);
        if (loc != null) {
            return loc.replace("&", "\u00a7");
        }
        return msg;
    }

    public static Messages getInstance() {
        if (singleton == null) {
            singleton = new Messages();
        }        
        return singleton;
    }

}
