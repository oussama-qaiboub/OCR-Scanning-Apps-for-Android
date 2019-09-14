package ma.c2m.scannerc2m.detection;

public class AggregateLumaMotionDetection implements IMotionDetection {
    private static final int mLeniency = 10;
    private static final int mDebugMode = 2;
    private static final int mXBoxes = 10;
    private static final int mYBoxes = 10;
    private static int[] mPrevious = null;
    private static int mPreviousWidth;
    private static int mPreviousHeight;
    private static State mPreviousState = null;

    @Override
    public int[] getPrevious() {
        return ((mPrevious != null) ? mPrevious.clone() : null);
    }

    protected static boolean isDifferent(int[] first, int width, int height) {
        if (first == null) throw new NullPointerException();
        if (mPrevious == null) return false;
        if (first.length != mPrevious.length) return true;
        if (mPreviousWidth != width || mPreviousHeight != height) return true;
        if (mPreviousState == null) {
            mPreviousState = new State(mPrevious, mPreviousWidth, mPreviousHeight);
            return false;
        }
        State state = new State(first, width, height);
        Comparer comparer = new Comparer(state, mPreviousState, mXBoxes, mYBoxes, mLeniency, mDebugMode);
        boolean different = comparer.isDifferent();
        if (different) {
            comparer.paintDifferences(first);
        }
        mPreviousState = state;
        different=true;
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
        }
        boolean motionDetected = isDifferent(luma, width, height);
        mPrevious = original;
        mPreviousWidth = width;
        mPreviousHeight = height;
        return motionDetected;
    }
}