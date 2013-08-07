/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * Copied from OpenJDK with permission.
 */

package com.sun.mail.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.logging.Level;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.BASE64EncoderStream;
import com.sun.mail.util.MailLogger;


/**
 * NTLMAuthentication:
 *
 * @author Michael McMahon
 * @author Bill Shannon (adapted for JavaMail)
 */
public class Ntlm {

    private byte[] type1;
    private byte[] type3;

    private SecretKeyFactory fac;
    private Cipher cipher;
    private MD4 md4;
    private String hostname;
    private String ntdomain;
    private String username;
    private String password;

    private MailLogger logger;

    private void init0() {
        type1 = new byte[256];
        type3 = new byte[256];
        System.arraycopy(new byte[] {'N','T','L','M','S','S','P',0,1}, 0,
			    type1, 0, 9);
        type1[12] = (byte) 3;
        type1[13] = (byte) 0xb2;
        type1[28] = (byte) 0x20;
        System.arraycopy(new byte[] {'N','T','L','M','S','S','P',0,3}, 0,
			    type3, 0, 9);
        type3[12] = (byte) 0x18;
        type3[14] = (byte) 0x18;
        type3[20] = (byte) 0x18;
        type3[22] = (byte) 0x18;
        type3[32] = (byte) 0x40;
        type3[60] = (byte) 1;
        type3[61] = (byte) 0x82;

        try {
            fac = SecretKeyFactory.getInstance("DES");
            cipher = Cipher.getInstance("DES/ECB/NoPadding");
            md4 = new MD4();
        } catch (NoSuchPaddingException e) {
            assert false;
        } catch (NoSuchAlgorithmException e) {
            assert false;
        }
    };

    /**
     * Create an NTLM authenticator.
     * Username may be specified as domain\\username in the Authenticator.
     * If this notation is not used, then the domain will be taken
     * from the ntdomain parameter.
     */
    public Ntlm(String ntdomain, String hostname, String username,
				String password, MailLogger logger) {
	int i = hostname.indexOf('.');
	if (i != -1) {
	    hostname = hostname.substring(0, i);
	}
        i = username.indexOf('\\');
        if (i != -1) {
            ntdomain = username.substring(0, i).toUpperCase();
            username = username.substring(i+1);
        } else if (ntdomain == null) {
	    ntdomain = "";
	}
	this.ntdomain = ntdomain;
	this.hostname = hostname;
	this.username = username;
	this.password = password;
	this.logger = logger.getLogger(this.getClass(), "DEBUG NTLM");
        init0();
    }

    private void copybytes(byte[] dest, int destpos, String src, String enc) {
        try {
            byte[] x = src.getBytes(enc);
            System.arraycopy(x, 0, dest, destpos, x.length);
        } catch (UnsupportedEncodingException e) {
            assert false;
        }
    }

    public String generateType1Msg(int flags) {
	// XXX - should set "flags" in generated message
        int dlen = ntdomain.length();
        type1[16]= (byte) (dlen % 256);
        type1[17]= (byte) (dlen / 256);
        type1[18] = type1[16];
        type1[19] = type1[17];
	if (dlen == 0)
	    type1[13] &= ~0x10;

        int hlen = hostname.length();
        type1[24]= (byte) (hlen % 256);
        type1[25]= (byte) (hlen / 256);
        type1[26] = type1[24];
        type1[27] = type1[25];

        copybytes(type1, 32, hostname, "iso-8859-1");
        copybytes(type1, hlen+32, ntdomain, "iso-8859-1");
        type1[20] = (byte) ((hlen+32) % 256);
        type1[21] = (byte) ((hlen+32) / 256);

        byte[] msg = new byte[32 + hlen + dlen];
        System.arraycopy(type1, 0, msg, 0, 32 + hlen + dlen);
	if (logger.isLoggable(Level.FINE))
	    logger.fine("type 1 message: " + toHex(msg));

        String result = null;
	try {
	    result = new String(BASE64EncoderStream.encode(msg), "iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            assert false;
        }
        return result;
    }

    /**
     * Convert a 7 byte array to an 8 byte array (for a des key with parity).
     * Input starts at offset off.
     */
    private byte[] makeDesKey(byte[] input, int off) {
        int[] in = new int[input.length];
        for (int i = 0; i < in.length; i++) {
            in[i] = input[i] < 0 ? input[i] + 256: input[i];
        }
        byte[] out = new byte[8];
        out[0] = (byte)in[off+0];
        out[1] = (byte)(((in[off+0] << 7) & 0xFF) | (in[off+1] >> 1));
        out[2] = (byte)(((in[off+1] << 6) & 0xFF) | (in[off+2] >> 2));
        out[3] = (byte)(((in[off+2] << 5) & 0xFF) | (in[off+3] >> 3));
        out[4] = (byte)(((in[off+3] << 4) & 0xFF) | (in[off+4] >> 4));
        out[5] = (byte)(((in[off+4] << 3) & 0xFF) | (in[off+5] >> 5));
        out[6] = (byte)(((in[off+5] << 2) & 0xFF) | (in[off+6] >> 6));
        out[7] = (byte)((in[off+6] << 1) & 0xFF);
        return out;
    }

    private byte[] calcLMHash() throws GeneralSecurityException {
        byte[] magic = {0x4b, 0x47, 0x53, 0x21, 0x40, 0x23, 0x24, 0x25};
        byte[] pwb = null;
	try {
	    pwb = password.toUpperCase(Locale.ENGLISH).getBytes("iso-8859-1");
	} catch (UnsupportedEncodingException ex) {
	    // should never happen
	    assert false;
	}
        byte[] pwb1 = new byte[14];
        int len = password.length();
        if (len > 14)
            len = 14;
        System.arraycopy(pwb, 0, pwb1, 0, len); /* Zero padded */

        DESKeySpec dks1 = new DESKeySpec(makeDesKey(pwb1, 0));
        DESKeySpec dks2 = new DESKeySpec(makeDesKey(pwb1, 7));

        SecretKey key1 = fac.generateSecret(dks1);
        SecretKey key2 = fac.generateSecret(dks2);
        cipher.init(Cipher.ENCRYPT_MODE, key1);
        byte[] out1 = cipher.doFinal(magic, 0, 8);
        cipher.init(Cipher.ENCRYPT_MODE, key2);
        byte[] out2 = cipher.doFinal(magic, 0, 8);

        byte[] result = new byte [21];
        System.arraycopy(out1, 0, result, 0, 8);
        System.arraycopy(out2, 0, result, 8, 8);
        return result;
    }

    private byte[] calcNTHash() throws GeneralSecurityException {
        byte[] pw = null;
        try {
            pw = password.getBytes("UnicodeLittleUnmarked");
        } catch (UnsupportedEncodingException e) {
            assert false;
        }
        byte[] out = md4.digest(pw);
        byte[] result = new byte[21];
        System.arraycopy(out, 0, result, 0, 16);
        return result;
    }

    /*
     * Key is a 21 byte array.  Split it into 3 7 byte chunks,
     * convert each to 8 byte DES keys, encrypt the text arg with
     * each key and return the three results in a sequential [].
     */
    private byte[] calcResponse(byte[] key, byte[] text)
				throws GeneralSecurityException {
        assert key.length == 21;
        DESKeySpec dks1 = new DESKeySpec(makeDesKey(key, 0));
        DESKeySpec dks2 = new DESKeySpec(makeDesKey(key, 7));
        DESKeySpec dks3 = new DESKeySpec(makeDesKey(key, 14));
        SecretKey key1 = fac.generateSecret(dks1);
        SecretKey key2 = fac.generateSecret(dks2);
        SecretKey key3 = fac.generateSecret(dks3);
        cipher.init(Cipher.ENCRYPT_MODE, key1);
        byte[] out1 = cipher.doFinal(text, 0, 8);
        cipher.init(Cipher.ENCRYPT_MODE, key2);
        byte[] out2 = cipher.doFinal(text, 0, 8);
        cipher.init(Cipher.ENCRYPT_MODE, key3);
        byte[] out3 = cipher.doFinal(text, 0, 8);
        byte[] result = new byte[24];
        System.arraycopy(out1, 0, result, 0, 8);
        System.arraycopy(out2, 0, result, 8, 8);
        System.arraycopy(out3, 0, result, 16, 8);
        return result;
    }

    public String generateType3Msg(String challenge) {
	try {
        /* First decode the type2 message to get the server nonce */
        /* nonce is located at type2[24] for 8 bytes */

        byte[] type2 = null;
	try {
	    type2 = BASE64DecoderStream.decode(challenge.getBytes("us-ascii"));
	} catch (UnsupportedEncodingException ex) {
	    // should never happen
	    assert false;
	}
        byte[] nonce = new byte[8];
        System.arraycopy(type2, 24, nonce, 0, 8);

        int ulen = username.length()*2;
        type3[36] = type3[38] = (byte) (ulen % 256);
        type3[37] = type3[39] = (byte) (ulen / 256);
        int dlen = ntdomain.length()*2;
        type3[28] = type3[30] = (byte) (dlen % 256);
        type3[29] = type3[31] = (byte) (dlen / 256);
        int hlen = hostname.length()*2;
        type3[44] = type3[46] = (byte) (hlen % 256);
        type3[45] = type3[47] = (byte) (hlen / 256);

        int l = 64;
        copybytes(type3, l, ntdomain, "UnicodeLittleUnmarked");
        type3[32] = (byte) (l % 256);
        type3[33] = (byte) (l / 256);
        l += dlen;
        copybytes(type3, l, username, "UnicodeLittleUnmarked");
        type3[40] = (byte) (l % 256);
        type3[41] = (byte) (l / 256);
        l += ulen;
        copybytes(type3, l, hostname, "UnicodeLittleUnmarked");
        type3[48] = (byte) (l % 256);
        type3[49] = (byte) (l / 256);
        l += hlen;

        byte[] lmhash = calcLMHash();
        byte[] lmresponse = calcResponse(lmhash, nonce);
        byte[] nthash = calcNTHash();
        byte[] ntresponse = calcResponse(nthash, nonce);
        System.arraycopy(lmresponse, 0, type3, l, 24);
        type3[16] = (byte) (l % 256);
        type3[17] = (byte) (l / 256);
        l += 24;
        System.arraycopy(ntresponse, 0, type3, l, 24);
        type3[24] = (byte) (l % 256);
        type3[25] = (byte) (l / 256);
        l += 24;
        type3[56] = (byte) (l % 256);
        type3[57] = (byte) (l / 256);

        byte[] msg = new byte[l];
        System.arraycopy(type3, 0, msg, 0, l);
	if (logger.isLoggable(Level.FINE))
	    logger.fine("type 3 message: " + toHex(msg));

        String result = null;
	try {
	    result = new String(BASE64EncoderStream.encode(msg), "iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            assert false;
        }
        return result;

	} catch (GeneralSecurityException ex) {
	    // should never happen
	    logger.log(Level.FINE, "GeneralSecurityException", ex);
	    return "";	// will fail later
	}
    }

    private static char[] hex =
	{ '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };

    private static String toHex(byte[] b) {
	StringBuffer sb = new StringBuffer(b.length * 3);
	for (int i = 0; i < b.length; i++)
	    sb.append(hex[(b[i]>>4)&0xF]).append(hex[b[i]&0xF]).append(' ');
	return sb.toString();
    }
}
