package com.dodotdo.base.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;

public class USON {

    public static void extend(JSONArray origin, JSONArray extend) throws JSONException {

        for (int i = 0; i < extend.length(); i += 1) {
            Object value = extend.get(i);

            if (value instanceof Date) {
                origin.put(new Date(((Date) value).getTime()));
            } else if (value instanceof JSONObject) {
                origin.put(copy((JSONObject) value));
            } else if (value instanceof JSONArray) {
                origin.put(copy((JSONArray) value));
            } else {
                origin.put(value);
            }
        }
    }

    public static void extend(JSONObject origin, JSONObject extend) throws JSONException {

        Iterator<String> iterator = extend.keys();

        while (iterator.hasNext()) {

            String name = iterator.next();
            Object value = extend.get(name);

            if (value instanceof Date) {
                origin.put(name, new Date(((Date) value).getTime()));
            } else if (value instanceof JSONObject) {
                origin.put(name, copy((JSONObject) value));
            } else if (value instanceof JSONArray) {
                origin.put(name, copy((JSONArray) value));
            } else {
                origin.put(name, value);
            }
        }
    }

    public static JSONArray copy(JSONArray jsonArray) throws JSONException {
        JSONArray copy = new JSONArray();
        extend(copy, jsonArray);
        return copy;
    }

    public static JSONObject copy(JSONObject json) throws JSONException {
        JSONObject copy = new JSONObject();
        extend(copy, json);
        return copy;
    }

    public static JSONObject pack(JSONObject json) throws JSONException {

        JSONObject result = copy(json);
        JSONArray dateAttrNames = new JSONArray();

        Iterator<String> iterator = result.keys();
        while (iterator.hasNext()) {

            String name = iterator.next();
            Object value = result.get(name);

            if (value instanceof Date) {
                result.put(name, ((Date) value).getTime());
                dateAttrNames.put(name);
            } else if (value instanceof JSONObject) {
                result.put(name, pack((JSONObject) value));
            } else if (value instanceof JSONArray) {

                for (int i = 0; i < ((JSONArray) value).length(); i += 1) {
                    Object v = ((JSONArray) value).get(i);

                    if (v instanceof JSONObject) {
                        ((JSONArray) value).put(i, pack((JSONObject) v));
                    }
                }
            }
        }

        result.put("__D", dateAttrNames);
        return result;
    }

    public static JSONObject unpack(JSONObject json) throws JSONException {

        JSONObject result = copy(json);

        if (!result.isNull("__D")) {
            for (int i = 0; i < ((JSONArray) result.get("__D")).length(); i += 1) {
                String dateAttrName = (String) ((JSONArray) result.get("__D")).get(i);
                result.put(dateAttrName, new Date((Long) result.get(dateAttrName)));
            }
            result.remove("__D");
        }

        Iterator<String> iterator = result.keys();
        while (iterator.hasNext()) {

            String name = iterator.next();
            Object value = result.get(name);

            if (value instanceof JSONObject) {
                result.put(name, unpack((JSONObject) value));
            } else if (value instanceof JSONArray) {

                for (int i = 0; i < ((JSONArray) value).length(); i += 1) {
                    Object v = ((JSONArray) value).get(i);

                    if (v instanceof JSONObject) {
                        ((JSONArray) value).put(i, unpack((JSONObject) v));
                    }
                }
            }
        }

        return result;
    }
}
