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

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "HTTPTest";
    public static final String SERVER = "https://s7om3fdgbt7lcvqdnxitjmtiim0uczux.lambda-url.us-east-2.on.aws/";

    Button sendButton;
    ProgressBar progressBar;
    TextView ipText;

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
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    progressBar.setVisibility(View.VISIBLE);
                    ipText.setTextColor(Color.parseColor("#000000"));
                    ipText.setText("Please wait...");
                }
            });

            String addressToSend = getAddressToSend();
            if(addressToSend == null){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                        ipText.setTextColor(Color.parseColor("#000000"));
                        ipText.setText("Did not find any suitable address to send");
                        sendButton.setEnabled(true);

                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                        progressBar.setVisibility(View.INVISIBLE);
                        ipText.setTextColor(Color.parseColor("#000000"));
                        ipText.setText("Opening connection failed!");
                        sendButton.setEnabled(true);

                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                                progressBar.setVisibility(View.INVISIBLE);
                                ipText.setTextColor(Color.parseColor("#008800"));
                                ipText.setText("OK");
                                sendButton.setEnabled(true);
                            }
                        });
                    } else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.INVISIBLE);
                                ipText.setTextColor(Color.parseColor("#FF0000"));
                                ipText.setText("NOT OK");
                                sendButton.setEnabled(true);
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
                            progressBar.setVisibility(View.INVISIBLE);
                            ipText.setTextColor(Color.parseColor("#000000"));
                            ipText.setText("Server returned response code " + responseCode + "\nResponse message: " + responseMessage);
                            sendButton.setEnabled(true);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                //throw new RuntimeException(e);
            } catch (JSONException e) {
                e.printStackTrace();
                //throw new RuntimeException(e);
            } finally {
                conn.disconnect();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
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

        sendButton = findViewById(R.id.button_send);
        ipText = findViewById(R.id.text_ip_address);
        progressBar = findViewById(R.id.progress_bar);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendButton.setEnabled(false);

                ConnectThread connectThread = new ConnectThread();
                connectThread.start();
//                try {
//                    connectThread.join();
//                } catch (InterruptedException e) {
//                    Log.i(TAG, "ConnectThread interrupted");
//                }

                //sendButton.setEnabled(true);
            }
        });
    }


}