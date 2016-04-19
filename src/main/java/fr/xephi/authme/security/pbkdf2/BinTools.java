package fr.xephi.authme.security.pbkdf2;

/**
 * <p>
 * Free auxiliary functions. Copyright (c) 2007 Matthias G&auml;rtner
 * </p>
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * </p>
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * </p>
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * </p>
 * <p>
 * For Details, see <a
 * href="http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"
 * >http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html</a>.
 * </p>
 *
 * @author Matthias G&auml;rtner
 * @version 1.0
 */
public class BinTools {

    private static final String HEX_CHARS = "0123456789ABCDEF";

    private BinTools() {
    }

    /**
     * Simple binary-to-hexadecimal conversion.
     *
     * @param b Input bytes. May be <code>null</code>.
     *
     * @return Hexadecimal representation of b. Uppercase A-F, two characters
     * per byte. Empty string on <code>null</code> input.
     */
    public static String bin2hex(final byte[] b) {
        if (b == null) {
            return "";
        }
        StringBuffer stringBuffer = new StringBuffer(2 * b.length);
        for (byte aB : b) {
            int v = (256 + aB) % 256;
            stringBuffer.append(HEX_CHARS.charAt((v / 16) & 15));
            stringBuffer.append(HEX_CHARS.charAt((v % 16) & 15));
        }
        return stringBuffer.toString();
    }

    /**
     * Convert hex string to array of bytes.
     *
     * @param s String containing hexadecimal digits. May be <code>null</code>.
     *          On odd length leading zero will be assumed.
     *
     * @return Array on bytes, non-<code>null</code>.
     * @throws IllegalArgumentException when string contains non-hex character
     */
    public static byte[] hex2bin(final String s) {
        String m = s;
        if (s == null) {
            // Allow empty input string.
            m = "";
        } else if (s.length() % 2 != 0) {
            // Assume leading zero for odd string length
            m = "0" + s;
        }
        byte r[] = new byte[m.length() / 2];
        for (int i = 0, n = 0; i < m.length(); n++) {
            char h = m.charAt(i++);
            char l = m.charAt(i++);
            r[n] = (byte) (hex2bin(h) * 16 + hex2bin(l));
        }
        return r;
    }

    /**
     * Convert hex digit to numerical value.
     *
     * @param c 0-9, a-f, A-F allowed.
     *
     * @return 0-15
     * @throws IllegalArgumentException on non-hex character
     */
    public static int hex2bin(char c) {
        if (c >= '0' && c <= '9') {
            return (c - '0');
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 'A' + 10);
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 'a' + 10);
        }
        throw new IllegalArgumentException("Input string may only contain hex digits, but found '" + c + "'");
    }

    // Note ljacqu 20160313: This appears to be a test method that was present in the third-party source.
    // We can keep it for troubleshooting in the future.
    private static void testUtils() {
        byte b[] = new byte[256];
        byte bb = 0;
        for (int i = 0; i < 256; i++) {
            b[i] = bb++;
        }
        String s = bin2hex(b);
        byte c[] = hex2bin(s);
        String t = bin2hex(c);
        if (!s.equals(t)) {
            throw new AssertionError("Mismatch");
        }
    }
}
