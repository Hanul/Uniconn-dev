package com.dodotdo.base.connector;

import com.dodotdo.base.util.USON;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Connector {

    protected final Map<String, List<Method>> methodMap = new HashMap<>();
    protected int sendKey = 0;

    protected void runMethods(String methodName, Object data) {
        List<Method> methods = methodMap.get(methodName);
        if (methods != null) {
            try {
                for (Method method : methods) {
                    method.handle(data instanceof JSONObject ? USON.unpack((JSONObject) data) : data, (Object callbackData) -> {
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    protected void runMethods(String methodName, Object data, int sendKey) {
        List<Method> methods = methodMap.get(methodName);
        if (methods != null) {
            try {
                for (Method method : methods) {
                    method.handle(data instanceof JSONObject ? USON.unpack((JSONObject) data) : data, (Object callbackData) -> {
                        send("__CALLBACK_" + sendKey, callbackData);
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void on(String methodName, Method method) {
        List<Method> methods = this.methodMap.get(methodName);
        if (methods == null) {
            methods = new ArrayList<>();
        }
        methods.add(method);
        this.methodMap.put(methodName, methods);
    }

    public void off(String methodName, Method method) {
        List<Method> methods = this.methodMap.get(methodName);
        if (methods != null) {
            methods.remove(method);
        }
    }

    public void off(String methodName) {
        this.methodMap.remove(methodName);
    }

    public abstract void send(String methodName);

    public abstract void send(String methodName, Object data);

    public abstract void send(String methodName, Callback callback);

    public abstract void send(String methodName, Object data, Callback callback);
}
