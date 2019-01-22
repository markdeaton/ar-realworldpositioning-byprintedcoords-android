package com.esri.apl.ar_ocr_location.util;

import android.content.Context;
import android.text.TextUtils;

import com.esri.apl.ar_ocr_location.R;


public class FcUpdateUtils {
  // Methods that help create the JSON for adding a feature to the REST based Feature Service
  private static String formatParamForJSON(int param) {
    if (param == Integer.MIN_VALUE) return "null";
    else return String.format("%d", param);
  }
  private static String formatParamForJSON(long param) {
    if (param == Long.MIN_VALUE) return "null";
    else return String.format("%d", param);
  }
  private static String formatParamForJSON(String param) {
    if (TextUtils.isEmpty(param)) return "null";
    else return param;
  }
  private static String formatParamForJSON(double param) {
    if (param == Double.NaN) return "null";
    else return Double.toString(param); //String.format("%f", param);
  }
  private static String formatParamForJSON(float param) {
    if (param == Float.NaN) return "null";
    else return Float.toString(param); //String.format("%f", param);
  }
  // TODO Change this if you want to change the database schema and what's being recorded
  public static String jsonForOneFeature(Context ctx,
                                         double x, double y, double z, long datetime,
                                         double heading, double pitch, double roll) {
    String json = ctx.getString(R.string.add_one_feature_json,
            formatParamForJSON(x),
            formatParamForJSON(y),
            formatParamForJSON(z),
            formatParamForJSON(datetime),
            formatParamForJSON(heading),
            formatParamForJSON(pitch),
            formatParamForJSON(roll)
            /*,
            formatParamForJSON(signal),
            formatParamForJSON(dateTime),
            formatParamForJSON(osName),
            formatParamForJSON(osVersion),
            formatParamForJSON(phoneModel),
            formatParamForJSON(deviceId),
            formatParamForJSON(carrierId)*/
    );
    return json;
  }
}
