package com.dodotdo.base.connector;

import android.os.AsyncTask;
import android.util.Log;

import com.dodotdo.base.util.USON;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SocketServerConnector extends Connector {
    private static final String TAG = "SocketServerConnector";

    private String host;
    private int port;
    private ConnectionListener connectionListener;
    private ConnectionFailedHandler connectionFailedHandler;

    private PrintWriter out;
    private boolean isConnected = false;

    public void disconnect() {
        isConnected = false;
        runMethods("disconnect", new JSONObject());
    }

    public void reconnect() {
        disconnect();
        new ConnectTask().execute();
    }

    public SocketServerConnector(String host, int port, ConnectionListener connectionListener, ConnectionFailedHandler connectionFailedHandler) {
        this.host = host;
        this.port = port;
        this.connectionListener = connectionListener;
        this.connectionFailedHandler = connectionFailedHandler;
        reconnect();
    }

    @Override
    public void send(String methodName) {
        send(methodName, null, null);
    }

    @Override
    public void send(String methodName, Object data) {
        send(methodName, data, null);
    }

    @Override
    public void send(String methodName, Callback callback) {
        send(methodName, null, callback);
    }

    @Override
    public void send(String methodName, Object data, Callback callback) {
        if (isConnected == true) {

            JSONObject sendData = new JSONObject();

            try {
                sendData.put("methodName", methodName);
                if (data != null) {
                    sendData.put("data", data instanceof JSONObject ? USON.pack((JSONObject) data) : data);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (callback != null) {

                String callbackName = "__CALLBACK_" + sendKey;
                on(callbackName, (callbackData, notUsing) -> {
                    callback.handle(callbackData);
                    off(callbackName);
                });

                try {
                    sendData.put("sendKey", sendKey);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sendKey += 1;
                if (sendKey >= Integer.MAX_VALUE) {
                    sendKey = 0;
                }
            }

            new Thread() {
                public void run() {
                    out.write(sendData + "\r\n");
                    out.flush();
                }
            }.start();

        } else {
            Log.e(TAG, "Socket not connected.");
        }
    }

    public class ConnectTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {

                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 3000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                isConnected = true;
                Log.i(TAG, "Server " + host + " connected");
                connectionListener.handle();

                new Thread() {
                    public void run() {
                        try {

                            while (isConnected == true) {

                                String str = reader.readLine();
                                if (str != null) {

                                    JSONObject json = new JSONObject(str);

                                    String methodName = json.getString("methodName");

                                    Object data = null;
                                    if (json.isNull("data") != true) {
                                        data = json.get("data");
                                    }

                                    if (json.isNull("sendKey") == true) {
                                        runMethods(methodName, data);
                                    } else {
                                        runMethods(methodName, data, json.getInt("sendKey"));
                                    }
                                }

                                // disconnected
                                else {
                                    isConnected = false;
                                    connectionFailedHandler.handle();
                                }
                            }

                        } catch (SocketException e) {
                            e.printStackTrace();

                            // disconnected
                            isConnected = false;
                            connectionFailedHandler.handle();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                connectionFailedHandler.handle();
            } catch (ConnectException e) {
                e.printStackTrace();
                connectionFailedHandler.handle();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                connectionFailedHandler.handle();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
