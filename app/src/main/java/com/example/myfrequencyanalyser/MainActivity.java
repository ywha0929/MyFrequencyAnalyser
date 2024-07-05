package com.example.myfrequencyanalyser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {
    Button btnStart;
    Button btnStop;
    TextView textThreshold;
    TextView textFrequencyWidth;
    TextView textLeft;
    TextView textRight;
    SeekBar seekFrequency;
    SeekBar seekWidth;
    Boolean isPlay = false;

    Thread thread;
    Thread converter;
    LinkedBlockingQueue<Short> data = new LinkedBlockingQueue<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        Recorder recorder = new Recorder();
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPlay = true;
                int ret = recorder.startRecording(getApplicationContext());
                recorder.setContinueRecording(true);
                if(ret ==0 ){
                    thread = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                recorder.processData(data);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }

//                            recorder.writeAudioData("buffer");


                        }
                    });
                    int blockSize = 4410;
                    double y[] = new double[blockSize];
                    for (int i = 0; i < blockSize; i++) {
                        y[i] = 0;
                    }

                    converter = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            long offset = 0;
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            while(isPlay) {
                                int threshold = Integer.parseInt("10000");
                                Python python = Python.getInstance();

                                PyObject sys = python.getModule("sys");
                                PyObject io = python.getModule("io");
                                PyObject textOutputStream = io.callAttr("StringIO");
                                sys.put("stdout", textOutputStream);

                                int interval = 441 * 2;
                                short[] left = new short[interval];
                                short[] right = new short[interval];

                                for(int i = 0; i< interval; i++) {
                                    try {
                                        left[i] = data.take();
                                        right[i] = data.take();
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
//                                    Short thisData = data.poll();
//                                    while(thisData == null){
//                                        thisData = data.poll();
//                                    }
//                                    left[i] = thisData;
//                                    thisData = data.poll();
//                                    while(thisData == null) {
//                                        thisData = data.poll();
//                                    }
//                                    right[i] = thisData;

                                }
//                                if(data.size() < 8820) {
//                                    continue;
//                                }
//                                while(dataUsageCheck == true) {
//                                    try {
//                                        Thread.sleep(1);
//                                    } catch (InterruptedException e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                }

//                                List<Short> left = new ArrayList<>();
//                                List<Short> right = new ArrayList<>();
//                                for(int i = 0; i< 8820; i++) {
//                                    switch(i%2) {
//                                        case 0: left.add(data.get(0));
//                                            data.remove(0);
//                                            break;
//                                        case 1: right.add(data.get(0));
//                                            data.remove(0);
//                                            break;
//                                    }
//                                }

                                PyObject FFT = python.getModule("fft").callAttr("fftDirect",left,right,threshold);
//                                left = null;
//                                right = null;


                                String output = textOutputStream.callAttr("getvalue").toString();
//                                offset = Integer.parseInt(  output.substring(0,output.indexOf('.')-1)  );
                                textLeft.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        textLeft.setText(output.substring(0,output.indexOf(']')+2));
                                    }
                                });
                                textRight.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        textRight.setText(output.substring(output.indexOf(']')+2));
                                    }
                                });
                                offset += 8820;


                            }

                        }

                    });

                    thread.start();
                    converter.start();
                }
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recorder.setContinueRecording(false);
                isPlay = false;
                try {
                    thread.join();
                    converter.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    private void checkPermission() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ) {

        } else {
            requestPermission();
        }
    }
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 30){
            if (!Environment.isExternalStorageManager()){
//                Intent getpermission = new Intent();
//                getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                startActivity(getpermission);
            }
        }
        String[] permissions = new String[] {android.Manifest.permission.RECORD_AUDIO};
        ActivityCompat.requestPermissions(this,permissions,1);
    }
}