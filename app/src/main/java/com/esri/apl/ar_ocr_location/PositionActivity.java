package com.esri.apl.ar_ocr_location;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.esri.apl.ar_ocr_location.util.ARCoreSceneUpdateCallable;
import com.esri.apl.ar_ocr_location.util.ARCoreSource;
import com.esri.apl.ar_ocr_location.util.ARUtils;
import com.esri.apl.ar_ocr_location.util.FcUpdateUtils;
import com.esri.apl.ar_ocr_location.util.PermissionsUtil;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointBuilder;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.FirstPersonCameraController;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.mapping.view.ViewpointChangedEvent;
import com.esri.arcgisruntime.mapping.view.ViewpointChangedListener;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PositionActivity extends AppCompatActivity implements ARCoreSceneUpdateCallable {
  private static final String TAG = "PositionActivity";
  private static final int UPDATE_PERIOD_MS = 3000;

  private SceneView mArcGISSceneView;
  private ArSceneView mArCoreSceneView;
  private TextView txtPose;
  private CheckBox chkUpdateLocation;
  private static Timer mTmrUpdateLocation;

  private Point mTagLocation;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_position);

    double x = getIntent().getDoubleExtra(getString(R.string.extraname_x), Double.NaN);
    double y = getIntent().getDoubleExtra(getString(R.string.extraname_y), Double.NaN);
    mTagLocation = new Point(x, y, SpatialReferences.getWgs84());

    txtPose = (TextView)findViewById(R.id.txtPose);

    mArcGISSceneView = (SceneView)findViewById(R.id.arcgis_sceneview);
    mArCoreSceneView = (ArSceneView)findViewById(R.id.arcore_sceneview);

    if (PermissionsUtil.allPermissionsGranted(this)) {
      initArCoreSceneView();
      initArcGISSceneView();
    } else {
      PermissionsUtil.getRuntimePermissions(this);
    }

    chkUpdateLocation = (CheckBox)findViewById(R.id.chkSaveLocation);
    chkUpdateLocation.setOnCheckedChangeListener(mOnCheckUpdateLocChanged);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mArcGISSceneView != null) mArcGISSceneView.pause();
    if (mArCoreSceneView != null) mArCoreSceneView.pause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mArcGISSceneView != null) mArcGISSceneView.resume();
    startArSession();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mArCoreSceneView != null) mArCoreSceneView.destroy();
  }

  private void initArcGISSceneView() {
    ArcGISScene scene = new ArcGISScene(Basemap.createTopographic());
    ArcGISTiledElevationSource elevSrc = new ArcGISTiledElevationSource(
            getString(R.string.base_surface_default));
    Surface surface = scene.getBaseSurface();
    surface.getElevationSources().add(elevSrc);
    mArcGISSceneView.setScene(scene);

    mArcGISSceneView.setARModeEnabled(true);

    Point pt = mTagLocation;
//    Point pt = new Point(-117.195369, 34.056203, SpatialReferences.getWgs84());

    elevSrc.addDoneLoadingListener(new Runnable() {
      @Override
      public void run() {
        if (elevSrc.getLoadStatus() != LoadStatus.LOADED) {
          Log.e(TAG, "Failed to load elevation surface", elevSrc.getLoadError());
          return;
        }
        ListenableFuture<Double> future = surface.getElevationAsync(pt);
        future.addDoneListener(new Runnable() {
          @Override
          public void run() {
            try {
              final double OBSERVER_OFFSET = 30; // Viewpoint meters above ground
              double elev = future.get();
              PointBuilder pb = new PointBuilder(pt);
              pb.setZ(elev + OBSERVER_OFFSET);
              Camera cameraStartLoc
                      = new Camera(pb.toGeometry(), 0, 0, 0);
              FirstPersonCameraController fpcController = new FirstPersonCameraController();
              fpcController.setInitialPosition(cameraStartLoc);
//              fpcController.setTranslationFactor(1000);

              startArSession();

              ARCoreSource motionSource = new ARCoreSource(mArCoreSceneView.getScene(),
                      mArCoreSceneView.getSession(), cameraStartLoc,
                      PositionActivity.this);
              fpcController.setDeviceMotionDataSource(motionSource);

              fpcController.setFramerate(FirstPersonCameraController.FirstPersonFramerate.BALANCED);
              mArcGISSceneView.setCameraController(fpcController);

              // To update position and orientation of the camera with device sensors use:
              motionSource.startAll();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
      }
    });


    mArcGISSceneView.addViewpointChangedListener(new ViewpointChangedListener() {
      @Override
      public void viewpointChanged(ViewpointChangedEvent viewpointChangedEvent) {
        Log.d(TAG, "Viewpoint changed");
      }
    });
  }
  private void initArCoreSceneView() {
    startArSession();
  }

  boolean installRequested;
  private void startArSession() {
    if (mArCoreSceneView != null) {
      if (mArCoreSceneView.getSession() == null) {
        // If the session wasn't created yet, don't resume rendering.
        // This can happen if ARCore needs to be updated or permissions are not granted yet.
        try {
          Session session = ARUtils.createArSession(this, installRequested);
          if (session == null) {
            installRequested = ARUtils.hasCameraPermission(this);
            return;
          } else {
            mArCoreSceneView.setupSession(session);
          }
        } catch (UnavailableException e) {
          ARUtils.handleSessionException(this, e);
        }
      }
      try {
        mArCoreSceneView.resume();
      } catch (CameraNotAvailableException e) {
        ARUtils.displayError(this, "The camera cannot be acquired.", e);
      }
    }
  }

  @Override
  public void onSceneError(Exception e) {
    String sErr = "";
    if (e instanceof CameraNotAvailableException) sErr = "Could not acquire camera";
    ARUtils.displayError(this, sErr, e);
  }

  @Override
  public void onSceneUpdate(Scene scene, Session session, Frame frame, FrameTime frameTime) {
/*    Vector3 pos = scene.getCamera().getWorldPosition();
    Quaternion rot = scene.getCamera().getWorldRotation();
    String sPose = getString(R.string.pose, pos.x, pos.y, pos.z, rot.x, rot.y, rot.z, rot.w);
    txtPose.setText(sPose);*/
    // Just update the location bar, if it's visible
    if (frame.getCamera().getTrackingState() == TrackingState.TRACKING
            && txtPose.getVisibility() == View.VISIBLE) {
      Camera cam = mArcGISSceneView.getCurrentViewpointCamera();
      txtPose.setText(getString(R.string.location_text,
              cam.getLocation().getX(),
              cam.getLocation().getY(),
              cam.getLocation().getZ(),
              cam.getHeading()));
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (PermissionsUtil.allPermissionsGranted(this)) {
      initArCoreSceneView();
      initArcGISSceneView();
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  private CompoundButton.OnCheckedChangeListener mOnCheckUpdateLocChanged =
          new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
              if (isChecked) {
                mTmrUpdateLocation = new Timer();
                mTmrUpdateLocation.schedule(new TTUpdateLocation(), UPDATE_PERIOD_MS, UPDATE_PERIOD_MS);
              } else mTmrUpdateLocation.cancel();
            }
          };

  private class TTUpdateLocation extends TimerTask {
    @Override
    public void run() {
      Camera cam = mArcGISSceneView.getCurrentViewpointCamera();
      Point pt = cam.getLocation();
      String jsonAdds = "[" +
              FcUpdateUtils.jsonForOneFeature(PositionActivity.this,
                      pt.getX(), pt.getY(), pt.getZ(),
                      System.currentTimeMillis(),
                      cam.getHeading(), cam.getPitch(), cam.getRoll())
              + "]";

      // Post via REST
      String svcUrl = getString(R.string.fc_location_url)  + "/addFeatures";
      List<Long> successes = new ArrayList<>();
      OkHttpClient http = new OkHttpClient();

      FormBody.Builder fBuild = new FormBody.Builder()
              .add("f", "json")
              .add("rollbackOnFailure", "false")
              .add("features", jsonAdds);
      RequestBody reqBody = fBuild.build();
      Request req = new Request.Builder()
              .url(svcUrl)
              .post(reqBody)
//              .header("referer", getString(R.string.agol_request_referer))
              .build();

      try {
        Response resp = http.newCall(req).execute();

        // Examine results and return list of rowids of successes
        String jsonRes = resp.body().string();
        JSONObject res = new JSONObject(jsonRes);

        // If error, exit
        if (res.has("error")) {
          throw new IOException("Error posting features: " + res.getJSONObject("error")
                  .getJSONArray("details").join("; "));
        }

        // Otherwise, parse the results
        JSONArray addResults = res.getJSONArray("addResults");
      } catch (Exception e) {

      }
    }
  }
}
