package ma.c2m.scannerc2m;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

//import com.googlecode.tesseract.android.TessBaseAPI;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.Line;
import com.google.android.gms.vision.text.TextRecognizer;

import ma.c2m.scannerc2m.data.GlobalData;
import ma.c2m.scannerc2m.data.Preferences;
import ma.c2m.scannerc2m.detection.AggregateLumaMotionDetection;
import ma.c2m.scannerc2m.detection.IMotionDetection;
import ma.c2m.scannerc2m.detection.LumaMotionDetection;
import ma.c2m.scannerc2m.detection.RgbMotionDetection;
import ma.c2m.scannerc2m.image.ImageProcessing;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;

public class MotionDetectionActivity extends SensorsActivity {
    private static final String TAG = "MotionDetectionActivity";
    private static SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static boolean inPreview = false;
    protected TextView textView;
    protected TextView textView2;
    private int freq;
    private Integer year;
    private Integer part;
    private Integer step;
    private static Boolean detectactive;
    private ImageView imgsaved;
    private ImageView tiflash;
    private static long mReferenceTime = 0;
    private static IMotionDetection detector = null;
    MotionDetectionActivity thisActivity;
    static int count = 0;
    private boolean focused = false;
private static TextRecognizer textRecognizer;
    static MediaPlayer  mp;

    private static boolean focused_done=false;
    protected PowerManager.WakeLock mWakeLock;
    private static volatile AtomicBoolean processing = new AtomicBoolean(false);
    private RelativeLayout rlCenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
        rlCenter = (RelativeLayout) findViewById(R.id.rlCenter);
        rlCenter.setVisibility(View.INVISIBLE);
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        mp = MediaPlayer.create(this, R.raw.saved);
        Intent intent = getIntent();
        this.count = 0;
        this.year = intent.getIntExtra("year", 0);
        this.part = intent.getIntExtra("part", 0);
        this.freq = intent.getIntExtra("freq", 0);
        this.step = intent.getIntExtra("step", 0);
        this.detectactive= intent.getBooleanExtra("detect", false);
        if (this.step == 2) {
            rlCenter.setVisibility(View.VISIBLE);
        }
        this.freq = (this.freq != 0) ? (this.freq * 1000) : Preferences.PICTURE_DELAY;
        Toast.makeText(MotionDetectionActivity.this, year + " " + freq + " " + step, Toast.LENGTH_SHORT).show();
        textView = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        preview = (SurfaceView) findViewById(R.id.preview);
        imgsaved = (ImageView) findViewById(R.id.savedimg);
        tiflash = (ImageView) findViewById(R.id.flashimgbtn);

        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        thisActivity = this;
        if (Preferences.USE_RGB) {
            detector = new RgbMotionDetection();
        } else if (Preferences.USE_LUMA) {
            detector = new LumaMotionDetection();
        } else {
            detector = new AggregateLumaMotionDetection();
        }


        thisActivity.tiflash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 Camera.Parameters cp = thisActivity.camera.getParameters();
                if (cp.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {// turn on flash
                    thisActivity.tiflash.setImageDrawable(getResources().getDrawable(R.drawable.ic_flash_on));
                    cp.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(cp);


                } else if (cp.getFlashMode().equals(android.hardware.Camera.Parameters.FLASH_MODE_ON) || cp.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    // turn off flash
                    thisActivity.tiflash.setImageDrawable(getResources().getDrawable(R.drawable.ic_flash_off));
                    cp.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(cp);
                }
            }
        });
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        super.onPause();
        camera.setPreviewCallback(null);
        if (inPreview)
            camera.stopPreview();
        inPreview = false;
        camera.release();
        camera = null;
    }


    @Override
    public void onResume() {
        super.onResume();
        camera = Camera.open();
    }

    private PreviewCallback previewCallback = new PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null)
                return;
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null)
                return;
            if (!GlobalData.isPhoneInMotion()) {
                        if (focused)
                        {
                            long now = System.currentTimeMillis();
                            if (now > (mReferenceTime + freq)) {
                                mReferenceTime = now;
                                DetectionThread thread = new DetectionThread(data, size.width, size.height, thisActivity);
                                thread.start();
                            } else {
                                int still = (int) Math.abs((now - mReferenceTime) / 1000);
                                thisActivity.textView.setTextColor(Color.RED);
                                thisActivity.textView.setText("Wait " + still + " sec");
                                thisActivity.imgsaved.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            thisActivity.textView.setTextColor(Color.CYAN);
                            thisActivity.textView.setText("Not focused !");
                            thisActivity.imgsaved.setVisibility(View.INVISIBLE);

                        }
                    }




            }




    };
    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                Camera.Parameters parameters = camera.getParameters();

                List<String> supportedFocusModes = parameters.getSupportedSceneModes();
                boolean hasFocus = supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
                if(hasFocus)
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                else
                    Toast.makeText(thisActivity,"Your device does not support auto focus",Toast.LENGTH_LONG);

                List<String> supportedHdrModes = parameters.getSupportedSceneModes();
                boolean hassencehdr = supportedHdrModes != null && supportedHdrModes.contains(Camera.Parameters.SCENE_MODE_HDR);
                if(hassencehdr)
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
                else
                    Toast.makeText(thisActivity,"Your device does not support hdr",Toast.LENGTH_LONG);

                camera.setParameters(parameters);
                camera.setPreviewDisplay(previewHolder);
                camera.setPreviewCallback(previewCallback);

                Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        focused = success;
                        Log.e("Tibas", "autofocus complete: " + success);
                    }
                };

                camera.autoFocus(autoFocusCallback);
            } catch (Throwable t) {
                //Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = getBestPreviewSize(width, height, parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                Log.d(TAG, "Using width=" + size.width + " height="
                        + size.height);
            }
            List<String> supportedFocusModes = parameters.getSupportedSceneModes();
            boolean hasFocus = supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
            if(hasFocus)
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            else
                Toast.makeText(thisActivity,"Your device does not support auto focus",Toast.LENGTH_LONG);

            List<String> supportedHdrModes = parameters.getSupportedSceneModes();
            boolean hassencehdr = supportedHdrModes != null && supportedHdrModes.contains(Camera.Parameters.SCENE_MODE_HDR);
            if(hassencehdr)
            parameters. setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
            else
                Toast.makeText(thisActivity,"Your device does not support hdr",Toast.LENGTH_LONG);

            camera.setParameters(parameters);
            camera.startPreview();
            Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    focused = success;
                    Log.e("Tibas", "autofocus complete: " + success);
                }
            };
            camera.autoFocus(autoFocusCallback);
            inPreview = true;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };



    private static Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        Log.e("Tibas", "size.width  : " + result.width + " size.height "
                + result.height);
        return result;
    }

    private static final class DetectionThread extends Thread {
        private byte[] data;
        private int width;
        private int height;
        private MotionDetectionActivity activity;

        public DetectionThread(byte[] data, int width, int height, MotionDetectionActivity activity) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.activity = activity;
        }

        @Override
        public void run() {
            if (!processing.compareAndSet(false, true))
                return;
            try {
                int[] pre = null;
                if (Preferences.SAVE_PREVIOUS)
                    pre = detector.getPrevious();
                int[] img = null;
                if (Preferences.USE_RGB) {
                    img = ImageProcessing.decodeYUV420SPtoRGB(data, width, height);
                } else {
                    img = ImageProcessing.decodeYUV420SPtoLuma(data, width, height);
                }
                int[] org = null;
                if (Preferences.SAVE_ORIGINAL && img != null)
                    org = img.clone();
                if (img != null && detector.detect(img, width, height)) {
                    Bitmap previous = null;
                    if (Preferences.SAVE_PREVIOUS && pre != null) {
                        if (Preferences.USE_RGB)
                            previous = ImageProcessing.rgbToBitmap(pre, width, height);
                        else
                            previous = ImageProcessing.lumaToGreyscale(pre, width, height);
                    }
                    Bitmap original = null;
                    if (Preferences.SAVE_ORIGINAL && org != null) {
                        if (Preferences.USE_RGB)
                            original = ImageProcessing.rgbToBitmap(org, width, height);
                        else
                            original = ImageProcessing.lumaToGreyscale(org, width, height);
                    }
                    Bitmap bitmap = null;
                    if (Preferences.SAVE_CHANGES) {
                        if (Preferences.USE_RGB)
                            bitmap = ImageProcessing.rgbToBitmap(img, width, height);
                        else
                            bitmap = ImageProcessing.lumaToGreyscale(img, width, height);
                    }
                    Log.i(TAG, "Saving.. previous=" + previous + " original="
                            + original + " bitmap=" + bitmap);
                    Looper.prepare();
                    new SavePhotoTask(activity).execute(previous, original, bitmap);
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.textView2.setTextColor(Color.RED);
                            activity.textView2.setText("Erreur !");

                            new android.os.Handler().postDelayed(new Runnable() {
                                public void run() {
                                    activity.textView2.setTextColor(Color.BLUE);
                                    activity.textView2.setText("");
                                    activity.imgsaved.setVisibility(View.INVISIBLE);
                                }
                            }, 1000);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                processing.set(false);
            }
            processing.set(false);
        }
    }

    ;

    private static final class SavePhotoTask extends
            AsyncTask<Bitmap, Integer, Integer> {
        MotionDetectionActivity activity;

        SavePhotoTask(MotionDetectionActivity activity) {
            this.activity = activity;
        }

        @Override
        protected Integer doInBackground(Bitmap... data) {
            for (int i = 0; i < data.length; i++) {
                Bitmap bitmap = data[i];
                String name = String.valueOf(System.currentTimeMillis());
                if (bitmap != null)
                    save(name, bitmap);

            }
            return 1;
        }


        private void save(String name, Bitmap bitmap) {


            try {
                File directory = new File(Environment.getExternalStorageDirectory() + "/"
                        + activity.year + "/" + activity.part);
                directory.mkdirs();
                if (activity.step == 2) {
                    activity.count++;
                    int first=activity.count;
                    File firstHalf = new File(directory, activity.count
                            + ".jpg");
                    activity.count++;
                    int second=activity.count;
                    File secondHalf = new File(directory, activity.count
                            + ".jpg");
                    FileOutputStream firstHalfFos = new FileOutputStream(firstHalf.getPath());
                    FileOutputStream secondHalfFos = new FileOutputStream(secondHalf.getPath());
                    Bitmap bm2 = Bitmap.createBitmap(bitmap, 0, 0, (bitmap.getWidth() / 2), bitmap.getHeight());
                    Bitmap bm1 = Bitmap.createBitmap(bitmap, (bitmap.getWidth() / 2), 0, (bitmap.getWidth() / 2), bitmap.getHeight());
                    bm1.compress(Bitmap.CompressFormat.JPEG, 100, firstHalfFos);
                    bm2.compress(Bitmap.CompressFormat.JPEG, 100, secondHalfFos);
                    firstHalfFos.close();
                    secondHalfFos.close();
                    try {
                        if (textRecognizer.isOperational() && bm1 != null && detectactive) {
                            Frame imageFrame = new Frame.Builder()

                                    .setBitmap(bm1)                 // your image bitmap
                                    .build();

                            String imageText = "";

                            SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

                            for (int i = 0; i < textBlocks.size(); i++) {
                                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                                imageText += "\n" + textBlock.getValue();                   // return string
                            }
                            if (textBlocks.size() == 0) {
                                Log.e("Tibas", "Scan Failed: Found nothing to scan!");

                            } else {
                                try {

                                    File file = new File(directory, first + " 's detected text.txt");

                                    BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

                                    writer.write(imageText);
                                    writer.newLine();
                                    writer.flush();
                                    writer.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            Log.e("Tibas", "Could not set up the detector!");
                        }
                    }
                    catch(Exception e)
                    {
                        Log.e("Tibas", "Detecting text Exception", e);
                    }

                    try {
                        if (textRecognizer.isOperational() && bm2 != null && detectactive) {
                            Frame imageFrame = new Frame.Builder()

                                    .setBitmap(bm2)                 // your image bitmap
                                    .build();

                            String imageText = "";

                            SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

                            for (int i = 0; i < textBlocks.size(); i++) {
                                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                                imageText += "\n" + textBlock.getValue();                   // return string
                            }
                            if (textBlocks.size() == 0) {
                                Log.e("Tibas", "Scan Failed: Found nothing to scan!");

                            } else {
                                try {

                                    File file = new File(directory, second + " 's detected text.txt");

                                    BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

                                    writer.write(imageText);
                                    writer.newLine();
                                    writer.flush();
                                    writer.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            Log.e("Tibas", "Could not set up the detector!");
                        }
                    }
                    catch(Exception e)
                    {
                        Log.e("Tibas", "Detecting text Exception", e);
                    }

                } else {
                    activity.count++;
                    File photo = new File(directory, activity.count + ".jpg");
                    if (photo.exists()) {
                        photo.delete();
                    }


                    try {
                    FileOutputStream fos = new FileOutputStream(photo.getPath());
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                        fos.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        if (textRecognizer.isOperational() && bitmap != null && detectactive) {
                            Frame imageFrame = new Frame.Builder()

                                    .setBitmap(bitmap)                 // your image bitmap
                                    .build();

                            String imageText = "";

                            SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

                            for (int i = 0; i < textBlocks.size(); i++) {
                                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                                imageText += "\n" + textBlock.getValue();                   // return string
                            }
                            if (textBlocks.size() == 0) {
                                Log.e("Tibas", "Scan Failed: Found nothing to scan!");

                            } else {
                                try {

                                    File file = new File(Environment.getExternalStorageDirectory() + "/"
                                            + activity.year + "/" + activity.part, activity.count + " 's detected text.txt");

                                    BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

                                    writer.write(imageText);
                                    writer.newLine();
                                    writer.flush();
                                    writer.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            Log.e("Tibas", "Could not set up the detector!");
                        }
                    }
                    catch(Exception e)
                        {
                            Log.e("Tibas", "Detecting text Exception", e);
                        }










                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        activity.textView2.setTextColor(Color.GREEN);

                       if (mp.isPlaying()) {
                            mp.stop();
                        }
                        mp.start();

                        activity.textView2.setText("Picture saved successfully!");
                        focused_done=false;
                        activity.imgsaved.setVisibility(View.VISIBLE);
                        new android.os.Handler().postDelayed(new Runnable() {
                            public void run() {
                                activity.textView2.setTextColor(Color.BLUE);
                                activity.textView2.setText("");
                                activity.imgsaved.setVisibility(View.INVISIBLE);
                            }
                        }, 1000);
                    }
                });
            } catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }
        }
    }
}