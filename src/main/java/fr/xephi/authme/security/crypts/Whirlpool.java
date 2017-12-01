package fr.xephi.authme.security.crypts;

/**
 * The Whirlpool hashing function.
 * <p/>
 * <p/>
 * <b>References</b>
 * <p/>
 * <p/>
 * The Whirlpool algorithm was developed by <a
 * href="mailto:pbarreto@scopus.com.br">Paulo S. L. M. Barreto</a> and <a
 * href="mailto:vincent.rijmen@cryptomathic.com">Vincent Rijmen</a>.
 * <p/>
 * See P.S.L.M. Barreto, V. Rijmen, ``The Whirlpool hashing function,'' First
 * NESSIE workshop, 2000 (tweaked version, 2003),
 * <https://www.cosic.esat.kuleuven
 * .ac.be/nessie/workshop/submissions/whirlpool.zip>
 *
 * @author Paulo S.L.M. Barreto
 * @author Vincent Rijmen.
 * @version 3.0 (2003.03.12)
 * <p/>
 * ====================================================================
 * =========
 * <p/>
 * Differences from version 2.1:
 * <p/>
 * - Suboptimal diffusion matrix replaced by cir(1, 1, 4, 1, 8, 5, 2,
 * 9).
 * <p/>
 * ====================================================================
 * =========
 * <p/>
 * Differences from version 2.0:
 * <p/>
 * - Generation of ISO/IEC 10118-3 test vectors. - Bug fix: nonzero
 * carry was ignored when tallying the data length (this bug apparently
 * only manifested itself when feeding data in pieces rather than in a
 * single chunk at once).
 * <p/>
 * Differences from version 1.0:
 * <p/>
 * - Original S-box replaced by the tweaked, hardware-efficient
 * version.
 * <p/>
 * ====================================================================
 * =========
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS ''AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHORS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

import java.util.Arrays;

@Deprecated
@Recommendation(Usage.DEPRECATED)
public class Whirlpool extends UnsaltedMethod {

    /**
     * The message digest size (in bits)
     */
    public static final int DIGESTBITS = 512;

    /**
     * The message digest size (in bytes)
     */
    public static final int DIGESTBYTES = DIGESTBITS >>> 3;

    /**
     * The number of rounds of the internal dedicated block cipher.
     */
    protected static final int R = 10;

    /**
     * The substitution box.
     */
    private static final String sbox = "\u1823\uc6E8\u87B8\u014F\u36A6\ud2F5\u796F\u9152" + "\u60Bc\u9B8E\uA30c\u7B35\u1dE0\ud7c2\u2E4B\uFE57" + "\u1577\u37E5\u9FF0\u4AdA\u58c9\u290A\uB1A0\u6B85" + "\uBd5d\u10F4\ucB3E\u0567\uE427\u418B\uA77d\u95d8" + "\uFBEE\u7c66\udd17\u479E\ucA2d\uBF07\uAd5A\u8333" + "\u6302\uAA71\uc819\u49d9\uF2E3\u5B88\u9A26\u32B0" + "\uE90F\ud580\uBEcd\u3448\uFF7A\u905F\u2068\u1AAE" + "\uB454\u9322\u64F1\u7312\u4008\uc3Ec\udBA1\u8d3d" + "\u9700\ucF2B\u7682\ud61B\uB5AF\u6A50\u45F3\u30EF" + "\u3F55\uA2EA\u65BA\u2Fc0\udE1c\uFd4d\u9275\u068A" + "\uB2E6\u0E1F\u62d4\uA896\uF9c5\u2559\u8472\u394c" + "\u5E78\u388c\ud1A5\uE261\uB321\u9c1E\u43c7\uFc04" + "\u5199\u6d0d\uFAdF\u7E24\u3BAB\ucE11\u8F4E\uB7EB" + "\u3c81\u94F7\uB913\u2cd3\uE76E\uc403\u5644\u7FA9" + "\u2ABB\uc153\udc0B\u9d6c\u3174\uF646\uAc89\u14E1" + "\u163A\u6909\u70B6\ud0Ed\ucc42\u98A4\u285c\uF886";

    private static final long[][] C = new long[8][256];
    private static final long[] rc = new long[R + 1];

    static {
        for (int x = 0; x < 256; x++) {
            char c = sbox.charAt(x / 2);
            long v1 = ((x & 1) == 0) ? c >>> 8 : c & 0xff;
            long v2 = v1 << 1;
            if (v2 >= 0x100L) {
                v2 ^= 0x11dL;
            }
            long v4 = v2 << 1;
            if (v4 >= 0x100L) {
                v4 ^= 0x11dL;
            }
            long v5 = v4 ^ v1;
            long v8 = v4 << 1;
            if (v8 >= 0x100L) {
                v8 ^= 0x11dL;
            }
            long v9 = v8 ^ v1;
            /*
             * build the circulant table C[0][x] = S[x].[1, 1, 4, 1, 8, 5, 2,
             * 9]:
             */
            C[0][x] = (v1 << 56) | (v1 << 48) | (v4 << 40) | (v1 << 32) | (v8 << 24) | (v5 << 16) | (v2 << 8) | (v9);
            /*
             * build the remaining circulant tables C[t][x] = C[0][x] rotr t
             */
            for (int t = 1; t < 8; t++) {
                C[t][x] = (C[t - 1][x] >>> 8) | ((C[t - 1][x] << 56));
            }
        }
        /*
         * build the round constants:
         */
        rc[0] = 0L; /*
                     * not used (assigment kept only to properly initialize all
                     * variables)
                     */
        for (int r = 1; r <= R; r++) {
            int i = 8 * (r - 1);
            rc[r] = (C[0][i] & 0xff00000000000000L) ^ (C[1][i + 1] & 0x00ff000000000000L) ^ (C[2][i + 2] & 0x0000ff0000000000L) ^ (C[3][i + 3] & 0x000000ff00000000L) ^ (C[4][i + 4] & 0x00000000ff000000L) ^ (C[5][i + 5] & 0x0000000000ff0000L) ^ (C[6][i + 6] & 0x000000000000ff00L) ^ (C[7][i + 7] & 0x00000000000000ffL);
        }
    }

    /**
     * Global number of hashed bits (256-bit counter).
     */
    protected final byte[] bitLength = new byte[32];

    /**
     * Buffer of data to hash.
     */
    protected final byte[] buffer = new byte[64];

    /**
     * Current number of bits on the buffer.
     */
    protected int bufferBits = 0;

    /**
     * Current (possibly incomplete) byte slot on the buffer.
     */
    protected int bufferPos = 0;

    /**
     * The hashing state.
     */
    protected final long[] hash = new long[8];
    protected final long[] K = new long[8];
    protected final long[] L = new long[8];
    protected final long[] block = new long[8];
    protected final long[] state = new long[8];

    public Whirlpool() {
    }

    protected static String display(byte[] array) {
        char[] val = new char[2 * array.length];
        String hex = "0123456789ABCDEF";
        for (int i = 0; i < array.length; i++) {
            int b = array[i] & 0xff;
            val[2 * i] = hex.charAt(b >>> 4);
            val[2 * i + 1] = hex.charAt(b & 15);
        }
        return String.valueOf(val);
    }

    /**
     * The core Whirlpool transform.
     */
    protected void processBuffer() {
        /*
         * map the buffer to a block:
         */
        for (int i = 0, j = 0; i < 8; i++, j += 8) {
            block[i] = (((long) buffer[j]) << 56) ^ (((long) buffer[j + 1] & 0xffL) << 48) ^ (((long) buffer[j + 2] & 0xffL) << 40) ^ (((long) buffer[j + 3] & 0xffL) << 32) ^ (((long) buffer[j + 4] & 0xffL) << 24) ^ (((long) buffer[j + 5] & 0xffL) << 16) ^ (((long) buffer[j + 6] & 0xffL) << 8) ^ (((long) buffer[j + 7] & 0xffL));
        }
        /*
         * compute and apply K^0 to the cipher state:
         */
        for (int i = 0; i < 8; i++) {
            state[i] = block[i] ^ (K[i] = hash[i]);
        }
        /*
         * iterate over all rounds:
         */
        for (int r = 1; r <= R; r++) {
            /*
             * compute K^r from K^{r-1}:
             */
            for (int i = 0; i < 8; i++) {
                L[i] = 0L;
                for (int t = 0, s = 56; t < 8; t++, s -= 8) {
                    L[i] ^= C[t][(int) (K[(i - t) & 7] >>> s) & 0xff];
                }
            }
            for (int i = 0; i < 8; i++) {
                K[i] = L[i];
            }
            K[0] ^= rc[r];
            /*
             * apply the r-th round transformation:
             */
            for (int i = 0; i < 8; i++) {
                L[i] = K[i];
                for (int t = 0, s = 56; t < 8; t++, s -= 8) {
                    L[i] ^= C[t][(int) (state[(i - t) & 7] >>> s) & 0xff];
                }
            }
            for (int i = 0; i < 8; i++) {
                state[i] = L[i];
            }
        }
        /*
         * apply the Miyaguchi-Preneel compression function:
         */
        for (int i = 0; i < 8; i++) {
            hash[i] ^= state[i] ^ block[i];
        }
    }

    /**
     * Initialize the hashing state.
     */
    public void NESSIEinit() {
        Arrays.fill(bitLength, (byte) 0);
        bufferBits = bufferPos = 0;
        buffer[0] = 0;
        Arrays.fill(hash, 0L);
    }

    /**
     * Delivers input data to the hashing algorithm.
     *
     * @param source     plaintext data to hash.
     * @param sourceBits how many bits of plaintext to process.
     *                   <p>
     *                   This method maintains the invariant: bufferBits &lt; 512
     *                   </p>
     */
    public void NESSIEadd(byte[] source, long sourceBits) {
        /*
         * sourcePos | +-------+-------+------- ||||||||||||||||||||| source
         * +-------+-------+-------
         * +-------+-------+-------+-------+-------+-------
         * |||||||||||||||||||||| buffer
         * +-------+-------+-------+-------+-------+------- | bufferPos
         */
        int sourcePos = 0; // index of leftmost source byte containing data (1
        // to 8 bits).
        int sourceGap = (8 - ((int) sourceBits & 7)) & 7; // space on
        // source[sourcePos].
        int bufferRem = bufferBits & 7; // occupied bits on buffer[bufferPos].
        int b;
        // tally the length of the added data:
        long value = sourceBits;
        for (int i = 31, carry = 0; i >= 0; i--) {
            carry += (bitLength[i] & 0xff) + ((int) value & 0xff);
            bitLength[i] = (byte) carry;
            carry >>>= 8;
            value >>>= 8;
        }
        // process data in chunks of 8 bits:
        while (sourceBits > 8) { // at least source[sourcePos] and
            // source[sourcePos+1] contain data.
            // take a byte from the source:
            b = ((source[sourcePos] << sourceGap) & 0xff) | ((source[sourcePos + 1] & 0xff) >>> (8 - sourceGap));
            if (b < 0 || b >= 256) {
                throw new RuntimeException("LOGIC ERROR");
            }
            // process this byte:
            buffer[bufferPos++] |= b >>> bufferRem;
            bufferBits += 8 - bufferRem; // bufferBits = 8*bufferPos;
            if (bufferBits == 512) {
                // process data block:
                processBuffer();
                // reset buffer:
                bufferBits = bufferPos = 0;
            }
            buffer[bufferPos] = (byte) ((b << (8 - bufferRem)) & 0xff);
            bufferBits += bufferRem;
            // proceed to remaining data:
            sourceBits -= 8;
            sourcePos++;
        }
        // now 0 <= sourceBits <= 8;
        // furthermore, all data (if any is left) is in source[sourcePos].
        if (sourceBits > 0) {
            b = (source[sourcePos] << sourceGap) & 0xff; // bits are
            // left-justified on b.
            // process the remaining bits:
            buffer[bufferPos] |= b >>> bufferRem;
        } else {
            b = 0;
        }
        if (bufferRem + sourceBits < 8) {
            // all remaining data fits on buffer[bufferPos], and there still
            // remains some space.
            bufferBits += sourceBits;
        } else {
            // buffer[bufferPos] is full:
            bufferPos++;
            bufferBits += 8 - bufferRem; // bufferBits = 8*bufferPos;
            sourceBits -= 8 - bufferRem;
            // now 0 <= sourceBits < 8; furthermore, all data is in
            // source[sourcePos].
            if (bufferBits == 512) {
                // process data block:
                processBuffer();
                // reset buffer:
                bufferBits = bufferPos = 0;
            }
            buffer[bufferPos] = (byte) ((b << (8 - bufferRem)) & 0xff);
            bufferBits += (int) sourceBits;
        }
    }

    /**
     * <p>
     * Get the hash value from the hashing state.
     * </p>
     * <p>
     * This method uses the invariant: bufferBits &lt; 512
     * </p>
     * @param digest byte[]
     */
    public void NESSIEfinalize(byte[] digest) {
        // append a '1'-bit:
        buffer[bufferPos] |= 0x80 >>> (bufferBits & 7);
        bufferPos++; // all remaining bits on the current byte are set to zero.
        // pad with zero bits to complete 512N + 256 bits:
        if (bufferPos > 32) {
            while (bufferPos < 64) {
                buffer[bufferPos++] = 0;
            }
            // process data block:
            processBuffer();
            // reset buffer:
            bufferPos = 0;
        }
        while (bufferPos < 32) {
            buffer[bufferPos++] = 0;
        }
        // append bit length of hashed data:
        System.arraycopy(bitLength, 0, buffer, 32, 32);
        // process data block:
        processBuffer();
        // return the completed message digest:
        for (int i = 0, j = 0; i < 8; i++, j += 8) {
            long h = hash[i];
            digest[j] = (byte) (h >>> 56);
            digest[j + 1] = (byte) (h >>> 48);
            digest[j + 2] = (byte) (h >>> 40);
            digest[j + 3] = (byte) (h >>> 32);
            digest[j + 4] = (byte) (h >>> 24);
            digest[j + 5] = (byte) (h >>> 16);
            digest[j + 6] = (byte) (h >>> 8);
            digest[j + 7] = (byte) (h);
        }
    }

    /**
     * Delivers string input data to the hashing algorithm.
     *
     * @param source plaintext data to hash (ASCII text string).
     *               This method maintains the invariant: bufferBits &lt; 512
     */
    public void NESSIEadd(String source) {
        if (source.length() > 0) {
            byte[] data = new byte[source.length()];
            for (int i = 0; i < source.length(); i++) {
                data[i] = (byte) source.charAt(i);
            }
            NESSIEadd(data, 8 * data.length);
        }
    }

    @Override
    public String computeHash(String password) {
        byte[] digest = new byte[DIGESTBYTES];
        NESSIEinit();
        NESSIEadd(password);
        NESSIEfinalize(digest);
        return display(digest);
    }

}
