package fr.xephi.authme.security;

import org.junit.Test;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link HashUtils}.
 */
public class HashUtilsTest {

    /**
     * List of passwords whose hash is provided to the class to test against.
     */
    private static final String[] GIVEN_PASSWORDS = {"", "password", "PassWord1", "&^%te$t?Pw@_"};

    @Test
    public void shouldHashMd5() {
        // given
        String[] correctHashes =  {
            "d41d8cd98f00b204e9800998ecf8427e", // empty string
            "5f4dcc3b5aa765d61d8327deb882cf99", // password
            "f2126d405f46ed603ff5b2950f062c96", // PassWord1
            "0833dcd2bc741f90c46bbac5498fd08f"  // &^%te$t?Pw@_
        };

        // when
        List<String> result = new ArrayList<>();
        for (String password : GIVEN_PASSWORDS) {
            result.add(HashUtils.md5(password));
        }

        // then
        assertThat(result, contains(correctHashes));
    }

    @Test
    public void shouldHashSha1() {
        // given
        String[] correctHashes =  {
            "da39a3ee5e6b4b0d3255bfef95601890afd80709", // empty string
            "5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8", // password
            "285d0c707f9644b75e1a87a62f25d0efb56800f0", // PassWord1
            "a42ef8e61e890af80461ca5dcded25cbfcf407a4"  // &^%te$t?Pw@_
        };

        // when
        List<String> result = new ArrayList<>();
        for (String password : GIVEN_PASSWORDS) {
            result.add(HashUtils.sha1(password));
        }

        // then
        assertThat(result, contains(correctHashes));
    }

    @Test
    public void shouldHashSha256() {
        // given
        String[] correctHashes =  {
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", // empty string
            "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", // password
            "c04265d72b749debd67451c083785aa572742e3222e86884de16485fa14b55e7", // PassWord1
            "005e3d7439d3e9a60a9d74aa1c763b36bfebec8e434ab6c5efab3df37eb2dae6"  // &^%te$t?Pw@_
        };

        // when
        List<String> result = new ArrayList<>();
        for (String password : GIVEN_PASSWORDS) {
            result.add(HashUtils.sha256(password));
        }

        // then
        assertThat(result, contains(correctHashes));
    }


    @Test
    public void shouldHashSha512() {
        // given
        String[] correctHashes =  {
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e", // empty string
            "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86", // password
            "ae9942149995a8171391625b36da134d5e288c721650d7c8d2d464fb49a49f3f551e4916ab1e097d9dd1201b01d69b1dccdefa3d2524a66092fb61b3df6e7e71", // PassWord1
            "8c4f3df78db191142d819a72c16058b9e1ea41ae9b1649e1184eb89e30344c51c9c71039c483cf2f1b76b51480d8459d7eb3cfbaa24b07f2041d1551af4ead75"  // &^%te$t?Pw@_
        };

        // when
        List<String> result = new ArrayList<>();
        for (String password : GIVEN_PASSWORDS) {
            result.add(HashUtils.sha512(password));
        }

        // then
        assertThat(result, contains(correctHashes));
    }

    @Test
    public void shouldRetrieveMd5Instance() {
        // given
        MessageDigestAlgorithm algorithm = MessageDigestAlgorithm.MD5;

        // when
        MessageDigest digest = HashUtils.getDigest(algorithm);

        // then
        assertThat(digest.getAlgorithm(), equalTo("MD5"));
    }

    @Test
    public void shouldCheckForValidBcryptHashStart() {
        // given / when / then
        assertThat(HashUtils.isValidBcryptHash(""), equalTo(false));
        assertThat(HashUtils.isValidBcryptHash("$2"), equalTo(false));
        assertThat(HashUtils.isValidBcryptHash("#2ae5fc78"), equalTo(false));
        assertThat(HashUtils.isValidBcryptHash("$2afsdaf"), equalTo(false));
        assertThat(HashUtils.isValidBcryptHash("$fdfasdfasdfasdfasdfasdfasdfasdfasdfasdfsadfasdfasdfasdfasdf"), equalTo(false));
        assertThat(HashUtils.isValidBcryptHash("$2y$asdfasdfasdfasdfasdfasdfasdfasdfasdfsadfasdfasdfasdfasdf"), equalTo(true));
    }

    @Test
    public void shouldCompareStrings() {
        // given / when / then
        assertThat(HashUtils.isEqual("test", "test"), equalTo(true));
        assertThat(HashUtils.isEqual("test", "Test"), equalTo(false));
        assertThat(HashUtils.isEqual("1234", "1234."), equalTo(false));
        assertThat(HashUtils.isEqual("ພາສາຫວຽດນາມ", "ພາສາຫວຽດນາມ"), equalTo(true));
        assertThat(HashUtils.isEqual("test", "tëst"), equalTo(false));
    }
}
