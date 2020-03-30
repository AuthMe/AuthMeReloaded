package fr.xephi.authme.security.crypts;

/**
 * Test for {@link SaltedSha512}.
 */
class SaltedSha512Test extends AbstractEncryptionMethodTest {

    SaltedSha512Test() {
        super(new SaltedSha512(),
            new HashedPassword("dea7a37cecf5384ae8e347fd1411efb51364b6ba1b328695de3b354612c1d7010807e8b7051c40f740e498490e1f133e2c2408327d13fbdd68e1b1f6d548e624", "29f8a3c52147f987fee7ba3e0fb311bd"),  // password
            new HashedPassword("7c06225aac574d2dc7c81a2ed306637adf025715f52083e05bdab014faaa234e24a97d0e69ea0108dfa77cc9228e58be319ee677e679b5d1ad168d40e50a42f6", "8ea37b85d020b98f60c0fe9b8ec9296c"),  // PassWord1
            new HashedPassword("55711adbe03c9616f3505f0d57077fdd528c32243eb6f9840c1a6ff9e553940d6b89790750ebd52ebda63ca793fbe9980d54057af40836820c648750fe22d49c", "9f58079631ef21d32b4710694f1f461b"),  // &^%te$t?Pw@_
            new HashedPassword("29dc5be8702975ea4563ed3de5b145e2d2f1c37ae661bbe0d3e94d964402cf09d539d65f3b90ff6921ea3d40727f76fb38fb34d1e5c2d62238c4e0203efc372f", "048bb76168265d906f1fd1f81d0616a9")); // âË_3(íù*
    }

}
