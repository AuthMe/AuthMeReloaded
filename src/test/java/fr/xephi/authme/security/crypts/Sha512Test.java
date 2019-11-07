package fr.xephi.authme.security.crypts;

/**
 * Test for {@link Sha512}.
 */
class Sha512Test extends AbstractEncryptionMethodTest {

    Sha512Test() {
        super(new Sha512(),
            "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86",  // password
            "ae9942149995a8171391625b36da134d5e288c721650d7c8d2d464fb49a49f3f551e4916ab1e097d9dd1201b01d69b1dccdefa3d2524a66092fb61b3df6e7e71",  // PassWord1
            "8c4f3df78db191142d819a72c16058b9e1ea41ae9b1649e1184eb89e30344c51c9c71039c483cf2f1b76b51480d8459d7eb3cfbaa24b07f2041d1551af4ead75",  // &^%te$t?Pw@_
            "9db561d04daa6086538444181f1a2ed180bbc5191df2a50c5c1be0c62b510e1dc32936c259e7138d4aa544ce5b60820fa4ead0362aeef730f86d360dc325d824"); // âË_3(íù*
    }

}
