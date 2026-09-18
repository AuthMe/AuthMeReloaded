package fr.xephi.authme.security.crypts;

/**
 * Test for {@link Wordpress68}.
 */
class Wordpress68Test extends AbstractEncryptionMethodTest {

    Wordpress68Test() {
        super(new Wordpress68(),
            "$wp$2y$10$Sd3wTap4FgvXoMtgmavCUuRzWLgnTyJxRsjQ0gGfdy8CRfQZK6TXe",  // password
            "$wp$2y$10$nGQi2hxxD2cjWyaQh6cORe0kil0/4W3LPurKwfSG8uxR/oigJ/jqe",  // PassWord1
            "$wp$2y$10$7lAEb3C8bS6hltZWznhhVOOtJIQ9BfrI9NFRiRvIeDM83GOv4MzMi",  // &^%te$t?Pw@_
            "$wp$2y$10$JyXpUo8Gh8EdOQasE/1nHeGhaPiwmSK2egy7ojY47VQugJSRFP1Qy"); // âË_3(íù*
    }
}