package ma.c2m.scannerc2m.detection;

import android.graphics.Color;

public class LumaMotionDetection implements IMotionDetection {
    private static final int mPixelThreshold = 50;
    private static final int mThreshold = 10000;
    private static int[] mPrevious = null;
    private static int mPreviousWidth = 0;
    private static int mPreviousHeight = 0;

    @Override
    public int[] getPrevious() {
        return ((mPrevious != null) ? mPrevious.clone() : null);
    }

    protected static boolean isDifferent(int[] first, int width, int height) {
        if (first == null) throw new NullPointerException();
        if (mPrevious == null) return false;
        if (first.length != mPrevious.length) return true;
        if (mPreviousWidth != width || mPreviousHeight != height) return true;
        int totDifferentPixels = 0;
        for (int i = 0, ij = 0; i < height; i++) {
            for (int j = 0; j < width; j++, ij++) {
                int pix = (0xff & (first[ij]));
                int otherPix = (0xff & (mPrevious[ij]));
                if (pix < 0) pix = 0;
                if (pix > 255) pix = 255;
                if (otherPix < 0) otherPix = 0;
                if (otherPix > 255) otherPix = 255;
                if (Math.abs(pix - otherPix) >= mPixelThreshold) {
                    totDifferentPixels++;
                    first[ij] = Color.RED;
                }
            }
        }
        if (totDifferentPixels <= 0) totDifferentPixels = 1;
        boolean different = totDifferentPixels > mThreshold;
        different = true;
        return different;
    }

    @Override
    public boolean detect(int[] luma, int width, int height) {
        if (luma == null) throw new NullPointerException();
        int[] original = luma.clone();
        if (mPrevious == null) {
            mPrevious = original;
            mPreviousWidth = width;
            mPreviousHeight = height;
            return false;
        }
        boolean motionDetected = isDifferent(luma, width, height);
        mPrevious = original;
        mPreviousWidth = width;
        mPreviousHeight = height;
        return motionDetected;
    }
}