package com.sourcico.httptest;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "HTTPTest";
    public static final String SERVER = "https://s7om3fdgbt7lcvqdnxitjmtiim0uczux.lambda-url.us-east-2.on.aws/";

    private Button sendButton;
    private ProgressBar progressBar;
    private TextView ipText;

    private Timer delayTimer;
    private TimerTask delayTask;

    private void updateOnSendingStart(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sendButton.setEnabled(false);
        ipText.setTextColor(Color.parseColor("#000000"));
        ipText.setText("");

        delayTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateOnSendingDelay();
                    }
                });
            }
        };
        delayTimer.schedule(delayTask, 3000);
    }

    private void updateOnSendingDelay(){
        progressBar.setVisibility(View.VISIBLE);
        ipText.setTextColor(Color.parseColor("#000000"));
        ipText.setText("Please wait...");
    }

    private void updateOnSendingFinish(String finishText, int color){
        delayTask.cancel();
        delayTask = null;

        progressBar.setVisibility(View.INVISIBLE);
        ipText.setTextColor(color);
        ipText.setText(finishText);
        sendButton.setEnabled(true);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    static {
        System.loadLibrary("getaddr");
    }

    private native String getAddressToSend();

    private class ConnectThread extends Thread{
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateOnSendingStart();
                }
            });

            String addressToSend = getAddressToSend();
            if(addressToSend == null){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateOnSendingFinish("Did not find any suitable address to send",
                                Color.parseColor("#000000"));
                    }
                });
                return;
            }

            HttpURLConnection conn;
            try {
                URL url = new URL(SERVER);
                conn = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                Log.e(TAG, "Opening connection failed!");
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateOnSendingFinish("Opening connection failed!", Color.parseColor("#000000"));
                    }
                });
                return;
            }

            try {
                //conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept","application/json");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("address", addressToSend);

                Log.i(TAG, "ConnectThread sending data " + jsonObject.toString());
                Log.i(TAG, "ConnectThread request method= " + conn.getRequestMethod());

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(jsonObject.toString());

                int responseCode = conn.getResponseCode();
                Log.i(TAG, "ConnectThread response code=" + responseCode);
                if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299){
                    BufferedReader isr = new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream()));
                    String outputData = "";
                    String serverData = isr.readLine();
                    while(serverData != null){
                        outputData += serverData;
                        serverData = isr.readLine();
                    }
                    Log.i(TAG, "ConnectThread received data " + outputData);
                    String finalOutputData = outputData;
                    JSONObject responseJsonObject = new JSONObject(finalOutputData);
                    Boolean nat = (Boolean) responseJsonObject.get("nat");
                    if(nat){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateOnSendingFinish(addressToSend + " ✔", Color.parseColor("#008800"));
                            }
                        });
                    } else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateOnSendingFinish(addressToSend + " ❎", Color.parseColor("#FF0000"));
                            }
                        });
                    }
                } else{
                    String responseMessage = conn.getResponseMessage();
                    BufferedReader isr = new BufferedReader(
                            new InputStreamReader(
                                    conn.getErrorStream()));
                    String errorData = "";
                    String serverData = isr.readLine();
                    while(serverData != null){
                        errorData += serverData;
                        serverData = isr.readLine();
                    }
                    Log.i(TAG, "ConnectThread received error data " + errorData);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateOnSendingFinish("Server returned response code " + responseCode,
                                    Color.parseColor("#000000"));
                        }
                    });
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateOnSendingFinish("An exception with connection occured",
                                    Color.parseColor("#000000"));
                        }
                    });
            } finally {
                conn.disconnect();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        delayTimer = new Timer();

        sendButton = findViewById(R.id.button_send);
        ipText = findViewById(R.id.text_ip_address);
        progressBar = findViewById(R.id.progress_bar);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectThread connectThread = new ConnectThread();
                connectThread.start();
            }
        });
    }


}