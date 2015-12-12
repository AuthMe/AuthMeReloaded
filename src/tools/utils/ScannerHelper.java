package utils;

import fr.xephi.authme.util.StringUtils;

import java.util.Scanner;

/**
 * Helper class for retrieving an answer from a Scanner.
 */
public class ScannerHelper {

    // def may be null to force the selection of one of the options
    // options may be null to just select whatever comes in
    public static String getAnswer(String def, Scanner scanner, String... options) {
        while (true) {
            String input = scanner.nextLine();
            if (StringUtils.isEmpty(input) && def != null) {
                return def;
            }

            if (options == null) {
                return input;
            } else {
                for (String option : options) {
                    if (input.equals(option)) {
                        return option;
                    }
                }
            }
            System.out.println("Invalid answer, please try again");
        }
    }
}
