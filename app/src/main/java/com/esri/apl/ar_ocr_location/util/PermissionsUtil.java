package com.esri.apl.ar_ocr_location.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionsUtil {
  private static final String TAG = "PermissionsUtil";
  private static final int PERMISSION_REQUESTS = 1;

  public static String[] getRequiredPermissions(Context ctx) {
    try {
      PackageInfo info =
              ctx.getPackageManager()
                      .getPackageInfo(ctx.getPackageName(), PackageManager.GET_PERMISSIONS);
      String[] ps = info.requestedPermissions;
      if (ps != null && ps.length > 0) {
        return ps;
      } else {
        return new String[0];
      }
    } catch (Exception e) {
      return new String[0];
    }
  }

  public static boolean allPermissionsGranted(Context ctx) {
    for (String permission : getRequiredPermissions(ctx)) {
      if (!isPermissionGranted(ctx, permission)) {
        return false;
      }
    }
    return true;
  }

  public static void getRuntimePermissions(Activity act) {
    List<String> allNeededPermissions = new ArrayList<>();
    for (String permission : getRequiredPermissions(act)) {
      if (!isPermissionGranted(act, permission)) {
        allNeededPermissions.add(permission);
      }
    }

    if (!allNeededPermissions.isEmpty()) {
      ActivityCompat.requestPermissions(
              act, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
    }
  }

  public static boolean isPermissionGranted(Context context, String permission) {
    if (ContextCompat.checkSelfPermission(context, permission)
            == PackageManager.PERMISSION_GRANTED) {
      Log.i(TAG, "Permission granted: " + permission);
      return true;
    }
    Log.i(TAG, "Permission NOT granted: " + permission);
    return false;
  }}
