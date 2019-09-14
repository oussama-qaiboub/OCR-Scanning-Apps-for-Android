package ma.c2m.scannerc2m.data;

public abstract class Preferences {
    private Preferences() {
    }

    public static boolean USE_RGB = true;
    public static boolean USE_LUMA = false;
    public static boolean USE_STATE = false;
    public static boolean SAVE_PREVIOUS = true;
    public static boolean SAVE_ORIGINAL = false;
    public static boolean SAVE_CHANGES = false;
    public static int PICTURE_DELAY = 5000;
}