package com.oguzhankarakaya.audiotest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;

    private RecordButton recordButton = null;
    private MediaRecorder recorder = null;

    private PlayButton playButton = null;
    private MediaPlayer player = null;

    private SaveButton saveButton = null;
    private ReadButton readButton = null;

    private AppCompatButton btnRecord, btnStop, btnSendRecord, btnGetRecords;
    private WaveFormView wave;


    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private ProgressDialog progressDialog;

    StorageReference mStorageReference;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference reference = database.getReference("Audio");

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";
        progressDialog = new ProgressDialog(this);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        mStorageReference = FirebaseStorage.getInstance().getReference();

        btnRecord = (AppCompatButton) findViewById(R.id.btnRecord);
        btnStop = (AppCompatButton) findViewById(R.id.btnStop);
        btnSendRecord = (AppCompatButton) findViewById(R.id.btnSendRecord);
        btnGetRecords = (AppCompatButton) findViewById(R.id.btnGetRecords);
        wave = (WaveFormView) findViewById(R.id.wave);
        handler = new Handler();

        /*LinearLayout ll = new LinearLayout(this);
        recordButton = new RecordButton(this);
        ll.addView(recordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        playButton = new PlayButton(this);
        ll.addView(playButton,
                new LinearLayout.LayoutParams(
                        0,
                        0,
                        0));
        setContentView(ll);
        saveButton = new SaveButton(this);
        ll.addView(saveButton,
                new LinearLayout.LayoutParams(
                        0,
                        0,
                        0));
        setContentView(ll);

        readButton = new ReadButton(this);
        ll.addView(readButton,
                new LinearLayout.LayoutParams(
                        0,
                        0,
                        0));
        waveFormView = new WaveFormView(this);
        ll.addView(waveFormView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0));
        setContentView(ll);*/
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                wave.updateAmplitude((float) 0.2, true);
                wave.updateAmplitude((float) 0.5, true);

            }
        }, 100);

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
                btnRecord.setVisibility(View.GONE);
                btnStop.setVisibility(View.VISIBLE);

            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
                btnRecord.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.GONE);
                wave.updateAmplitude(0, false);
            }
        });
        btnSendRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadAudio();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }

    WaveFormView waveFormView;

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.getMaxAmplitude();
        //waveFormView.updateAmplitude(recorder.getMaxAmplitude(), true);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    class RecordButton extends androidx.appcompat.widget.AppCompatButton {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }

    class SaveButton extends androidx.appcompat.widget.AppCompatButton {
        boolean startSaving = true;

        OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadAudio();
                if (startSaving)
                    setText("Saving");
                else
                    setText("Save it");
                startSaving = !startSaving;
            }
        };

        public SaveButton(Context context) {
            super(context);
            setText("Save");
            setOnClickListener(clickListener);
        }
    }

    class ReadButton extends androidx.appcompat.widget.AppCompatButton {
        boolean startRead = true;

        OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                readData();
                if (startRead)
                    setText("Reading");
                else
                    setText("Read it");
                startRead = !startRead;
            }
        };

        public ReadButton(Context context) {
            super(context);
            setText("Read");
            setOnClickListener(clickListener);
        }
    }

    class PlayButton extends androidx.appcompat.widget.AppCompatButton {
        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    setText("Stop playing");
                } else {
                    setText("Start playing");
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start playing");
            setOnClickListener(clicker);
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }

    StorageReference filePath;

    public void uploadAudio() {
        filePath = mStorageReference.child("Audio").child("new.mp3");
        Uri uri = Uri.fromFile(new File(fileName));
        filePath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MainActivity.this, "oldu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public MediaPlayer mMediaplayer;

    public void readData() {
        filePath = mStorageReference.child("Audio").child("new.mp3");
        Log.i("TAG", "readData: ");
        mMediaplayer = new MediaPlayer();
        mMediaplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                try {
                    final String url = uri.toString();
                    mMediaplayer.setDataSource(url);
                    mMediaplayer.setOnPreparedListener(MediaPlayer::start);
                    mMediaplayer.prepareAsync();
                    //mMediaplayer.start();
                    Log.i("TAG", "onSuccess: ");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("TAG", "onSuccess: ");
                }


            }
        });
    }
}