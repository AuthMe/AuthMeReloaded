package fr.xephi.authme.security.crypts;

/**
 * Test for {@link XAuth}.
 */
class XAuthTest extends AbstractEncryptionMethodTest {

    XAuthTest() {
        super(new XAuth(),
            "e54d4916577410d26d2f6e9362445463dab9ffdff9a67ed3b74d3f2312bc8fab84f653fcb88ad8338793ef8a6d0a1162105e46ec24f0dcb52355c634e3e6439f45444b09c715",  // password
            "d54489a4fd4732ee03d56810ab92944096e3d49335266adeecfbc12567abb3ff744761b33a1fcc4d04739e377775c788e4baace3caf35c7b9176b82b1fe3472e4cbdc5a43214",  // PassWord1
            "ce6404c1092fb5abf0a72f9c4327bfe8f4cdc4b8dc90ee6ca35c42b8ae9481b89c2559bb60b99ff2b57a102cfced40b8e2f5ef481400c9e6f79445017fc763b1cc27f4c2df36",  // &^%te$t?Pw@_
            "73074fe3f5503677ab9c5a1885b46a8b6fb249453317da08d86312c20e7b326e84f615e1b594c71129d2d1020400a89838e44653dc02d1799886e522a2789fbe1df6e70b7ffb"); // âË_3(íù*
    }

}
