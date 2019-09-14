package ma.c2m.scannerc2m;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "C2Mactivity";
    private static final int REQUEST_SIGNUP = 0;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int MY_Storage_REQUEST_CODE = 900;


    String annee, partie, debut, freq , onePage , twoPage;
    Button btncommence;
    EditText Iannee, Ipartie, Idebut, Ifreq ;
    RadioButton IonePage , ItwoPage ;
    RadioGroup grpradio;
    TextView txtError ;
    CheckBox cbdetect;
    public final static String EXTRA_MESSAGE = "c2m.message";
    protected PowerManager.WakeLock mWakeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btncommence = (Button)findViewById(R.id.btn_commence);
        Iannee = (EditText)findViewById(R.id.input_annee);
        Ipartie = (EditText)findViewById(R.id.input_partie);
        Idebut = (EditText)findViewById(R.id.input_debut);
        Ifreq = (EditText)findViewById(R.id.input_freq);
        IonePage = (RadioButton)findViewById(R.id.onePage);
        ItwoPage = (RadioButton)findViewById(R.id.twoPage);
        txtError = (TextView)findViewById(R.id.linkError);
        grpradio = (RadioGroup) findViewById(R.id.grpradio);
        cbdetect=(CheckBox) findViewById(R.id.detectcheckbox);

        //Calendar calendar = Calendar.getInstance();






        btncommence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkInput();
            }
        });




    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_LONG).show();

            } else {

                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
                finish();

            }

        }

        if (requestCode == MY_Storage_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_LONG).show();

            } else {

                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_LONG).show();
                finish();

            }

        }




    }


    @TargetApi(Build.VERSION_CODES.M)
    public void checkInput() {
        Log.d(TAG, "checking");

        if (!validate()) {
            onCommanceFailed();
            return;
        }
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    MY_CAMERA_REQUEST_CODE);
        } else if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_Storage_REQUEST_CODE);
        } else{



        btncommence.setEnabled(false);

        /*final ProgressDialog progressDialog = new ProgressDialog(MainActivity3.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();*/

         annee = Iannee.getText().toString();
         partie =Ipartie.getText().toString();
         debut = Idebut.getText().toString();
         freq = Ifreq.getText().toString();
        // TODO: Implement your own authentication logic here.
        onCommanceSuccess();
    }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                // By default we just finish the Activity and log them in automatically
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }


    public void onCommanceSuccess() {

            btncommence.setEnabled(true);
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
            this.mWakeLock.acquire();


            Toast.makeText(MainActivity.this, "Scan Started", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MotionDetectionActivity.class);
           boolean detect=false;
            Integer year = (Iannee.getText().toString().length() > 0) ? Integer.parseInt(Iannee.getText().toString()) : 0;
            Integer freq = (Ifreq.getText().toString().length() > 0) ? Integer.parseInt(Ifreq.getText().toString()) : 0;
            Integer step;
            int selectedId = grpradio.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton radioStep = (RadioButton) findViewById(selectedId);
                step = (radioStep.getText().toString().length() > 0) ? Integer.parseInt(radioStep.getText().toString()) : 0;
            } else {
                step = 0;
            }
            if(cbdetect.isChecked())
            {
                detect=true;
            }
            Integer part = (Ipartie.getText().toString().length() > 0) ? Integer.parseInt(Ipartie.getText().toString()) : 0;
            intent.putExtra("year", year);
            intent.putExtra("step", step);
            intent.putExtra("freq", freq);
            intent.putExtra("part", part);
            intent.putExtra("detect", detect);
            startActivity(intent);


    }



    public void onCommanceFailed() {
        Toast.makeText(getBaseContext(), "erreur sur la commence de scan", Toast.LENGTH_LONG).show();

        btncommence.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        annee = Iannee.getText().toString();
        partie =Ipartie.getText().toString();
        debut = Idebut.getText().toString();
        freq = Ifreq.getText().toString();

        if (annee.isEmpty()) {
            Iannee.setError("s'il vous plait remplir le champ année");
            valid = false;
        } else {
            Iannee.setError(null);
        }

        if (partie.isEmpty()) {
            Ipartie.setError("s'il vous plait remplir le champ partie");
            valid = false;
        } else {
            Ipartie.setError(null);
        }

        if (debut.isEmpty()) {
            Idebut.setError("s'il vous plait remplir le champ début");
            valid = false;
        } else {
            Idebut.setError(null);
        }

        if (freq.isEmpty()) {
            Ifreq.setError("s'il vous plait remplir le champ fréq");
            valid = false;
        } else {
            Ifreq.setError(null);
        }
        if(!IonePage.isChecked() && !ItwoPage.isChecked())
        {
            txtError.setText("s'il vous plait cocher le nomber de pages");
            valid = false;
        } else {
            txtError.setText("");
        }

        return valid;
    }



}
