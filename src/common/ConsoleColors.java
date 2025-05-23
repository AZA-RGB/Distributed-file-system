package common;

public class ConsoleColors {
    // ANSI color codes
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Background colors
    public static final String BLACK_BG = "\u001B[40m";
    public static final String RED_BG = "\u001B[41m";
    public static final String GREEN_BG = "\u001B[42m";
    public static final String YELLOW_BG = "\u001B[43m";
    public static final String BLUE_BG = "\u001B[44m";
    public static final String PURPLE_BG = "\u001B[45m";
    public static final String CYAN_BG = "\u001B[46m";
    public static final String WHITE_BG = "\u001B[47m";

    // Text styles
    public static final String BOLD = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";

    // Method to format text with color and style
    public static String format(String text, String color, String background, String style) {
        return style + color + background + text + RESET;
    }

    // Method to create a bordered message
    public static String createBorderedMessage(String message, String color, String background, String style) {
        String border = style + color + background + "═".repeat(message.length() + 4) + RESET;
        String paddedMessage = style + color + background + "  " + message + "  " + RESET;
        return border + "\n" + paddedMessage + "\n" + border;
    }
}