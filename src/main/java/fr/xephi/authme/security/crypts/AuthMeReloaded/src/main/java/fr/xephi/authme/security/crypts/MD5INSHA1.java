package fr.xephi.authme.security.crypts;

import static fr.xephi.authme.security.HashUtils.*;

public class MD5INSHA1 extends UnsaltedMethod {

	@Override
	public String computeHash(String password) {
		return md5(sha1(password));
	}
}
