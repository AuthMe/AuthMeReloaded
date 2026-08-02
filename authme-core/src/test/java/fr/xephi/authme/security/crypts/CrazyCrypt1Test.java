package fr.xephi.authme.security.crypts;

/**
 * Test for {@link CrazyCrypt1}.
 */
class CrazyCrypt1Test extends AbstractEncryptionMethodTest {

    CrazyCrypt1Test() {
        super(new CrazyCrypt1(),
            "d5c76eb36417d4e97ec62609619e40a9e549a2598d0dab5a7194fd997a9305af78de2b93f958e150d19dd1e7f821043379ddf5f9c7f352bf27df91ae4913f3e8",  // password
            "49c63f827c88196871e344e589bd46cc4fa6db3c27801bbad5374c0d216381977627c1d76f2114667d5dd117e046f7493eb06e4f461f4f848aa08f6f40a3e934",  // PassWord1
            "6fefb0233bab6e6efb9c16f82cb0d8f569488905e2dae0e7c9dde700e7363da67213d37c44bc15f4a05854c9c21e5688389d416413c7309398aa96cb1f341d08",  // &^%te$t?Pw@_
            "46f51cde7657fdec9848bad0fd8e7fb97783cf5335f94dbb5260899ab0b04022a52d651b1c45345328850178e7165308c8c213040b0864de66018a0b769d37cb"); // âË_3(íù*
    }

}
