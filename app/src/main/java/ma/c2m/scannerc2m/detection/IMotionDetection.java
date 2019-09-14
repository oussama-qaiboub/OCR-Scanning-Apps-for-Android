package ma.c2m.scannerc2m.detection;

public interface IMotionDetection {
    public int[] getPrevious();

    public boolean detect(int[] data, int width, int height);
}