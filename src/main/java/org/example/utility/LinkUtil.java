package org.example.utility;

public class LinkUtil {
    public static boolean isValidLinkFormat(String input) {
        return input != null && input.contains(":") && input.indexOf(":") > 1;
    }
}
