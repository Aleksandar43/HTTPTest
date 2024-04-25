package com.sourcico.httptest;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "HTTPTest";
    public static final String SERVER = "https://s7om3fdgbt7lcvqdnxitjmtiim0uczux.lambda-url.us-east-2.on.aws/";

    Button sendButton;
    TextView ipText;

    private class ConnectThread extends Thread{
        @Override
        public void run() {
            try {
                URL url = new URL(SERVER);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept","application/json");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("address", "::1");

                Log.i(TAG, "ConnectThread sending data " + jsonObject);

                OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
                osw.write(jsonObject.toString());

                int responseCode = conn.getResponseCode();
                Log.i(TAG, "ConnectThread response code=" + responseCode);
                if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299){
                    BufferedReader isr = new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream()));
                    //BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());


                    String serverData = isr.readLine();
                    Log.i(TAG, "ConnectThread received data " + serverData);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ipText.setTextColor(Color.parseColor("#008800"));
                            ipText.setText(serverData);
                            sendButton.setEnabled(true);
                        }
                    });
                } else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ipText.setTextColor(Color.parseColor("#000000"));
                            ipText.setText("Server returned response code " + responseCode);
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