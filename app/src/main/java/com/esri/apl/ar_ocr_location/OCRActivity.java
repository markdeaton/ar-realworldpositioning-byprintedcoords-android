// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.esri.apl.ar_ocr_location;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import com.esri.apl.ar_ocr_location.model.FoundCoordinate;
import com.esri.apl.ar_ocr_location.textrecognition.CameraSource;
import com.esri.apl.ar_ocr_location.textrecognition.CameraSourcePreview;
import com.esri.apl.ar_ocr_location.textrecognition.TextRecognitionProcessor;
import com.esri.apl.ar_ocr_location.util.PermissionsUtil;
import com.esri.apl.ar_ocr_location.view.GeoCoordinateSelectionListener;
import com.esri.apl.ar_ocr_location.view.GeoCoordinatesListAdapter;
import com.esri.apl.ar_ocr_location.view.GraphicOverlay;
import com.esri.apl.ar_ocr_location.viewmodel.MainViewModel;
import com.esri.arcgisruntime.geometry.AngularUnit;
import com.esri.arcgisruntime.geometry.AngularUnitId;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.google.android.gms.common.annotation.KeepName;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.gospelware.compassviewlib.CompassView;
import com.gospelware.compassviewlib.OnRotationChangeListener;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

/** Demo app showing the various features of ML Kit for Firebase. This class is used to
 * set up continuous frame processing on frames from a camera source. */
@KeepName
public final class OCRActivity extends AppCompatActivity
    implements OnRequestPermissionsResultCallback {
  private static final String TAG = "OCRActivity";

  private CameraSource cameraSource = null;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;

  public MainViewModel mainViewModel;

  RecyclerView lstFoundLocations;

  private Pattern mPtnCoords;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");

    mainViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
GraphicsOverlay go = new GraphicsOverlay();
    setContentView(R.layout.activity_ocr);

    // Camera view
    preview = (CameraSourcePreview) findViewById(R.id.firePreview);
    if (preview == null) Log.d(TAG, "Preview is null");

    graphicOverlay = (GraphicOverlay) findViewById(R.id.fireFaceOverlay);
    if (graphicOverlay == null) {
      Log.d(TAG, "graphicOverlay is null");
    }


    if (PermissionsUtil.allPermissionsGranted(this)) {
      createCameraSource();
    } else {
      PermissionsUtil.getRuntimePermissions(this);
    }

    // Set up list view
    lstFoundLocations = (RecyclerView) findViewById(R.id.lstLocations);
    lstFoundLocations.setAdapter(new GeoCoordinatesListAdapter(
            mainViewModel.getAryFoundCoords(), mOnGeoCoordinateSelected));
    // Prepare for coordinate pattern recognition
    mPtnCoords = Pattern.compile("([+-]?\\d+\\.?\\d+)\\s*,\\s*([+-]?\\d+\\.?\\d+)");
  }


  private void createCameraSource() {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      cameraSource = new CameraSource(this, graphicOverlay);
    }

    Log.i(TAG, "Using Text Detector Processor");
    cameraSource.setMachineLearningFrameProcessor(new TextRecognitionProcessor(this));
  }

  /**
   * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  private void startCameraSource() {
    if (cameraSource != null) {
      try {
        if (preview == null) {
          Log.d(TAG, "resume: Preview is null");
        }
        if (graphicOverlay == null) {
          Log.d(TAG, "resume: graphOverlay is null");
        }
        preview.start(cameraSource, graphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        cameraSource.release();
        cameraSource = null;
      }
    }
  }

  /** Check to see whether various combinations of elements on a line geocode for a known location
   *  or specify coordinates.
   *  All calculations here are one-based instead of zero-based.
   *
   * @param line A line (one or more pieces) of recognized text
   */
  public void evaluateLocation(FirebaseVisionText.Line line) {
    String ocrText = line.getText();
    List<FoundCoordinate> aryFoundCoords = mainViewModel.getAryFoundCoords();

    if (aryFoundCoords.contains(ocrText)) return;

    Matcher matcher = mPtnCoords.matcher(ocrText);

    while (matcher.find()) {
      try {
        if (matcher.groupCount() != 2) continue;
        String sCoord1 = matcher.group(1), sCoord2 = matcher.group(2);
        Double dCoord1 = Double.parseDouble(sCoord1);
        Double dCoord2 = Double.parseDouble(sCoord2);
        // Domain validation
        if ((dCoord1 > 180 || dCoord1 < -180) || (dCoord2 > 180 || dCoord2 < -180))
          throw new InvalidParameterException("Coordinate out of range");
        // Can we determine which is x and which is y?
        if ((dCoord1 > 90 || dCoord1 < -90) && (dCoord2 <= 90 || dCoord2 >= -90)) {
          // coord 1 is x
          FoundCoordinate fc = new FoundCoordinate(ocrText, dCoord1, dCoord2);
          aryFoundCoords.add(fc);
        } else if ((dCoord2 > 90 || dCoord2 < -90) && (dCoord1 <= 90 || dCoord1 >= -90)) {
          // coord 1 is y
          FoundCoordinate fc = new FoundCoordinate(ocrText, dCoord2, dCoord1);
          aryFoundCoords.add(fc);
        } else { // Don't know which is x and which is y; put both in list
          FoundCoordinate fc1 = new FoundCoordinate(ocrText, dCoord1, dCoord2);
          aryFoundCoords.add(fc1);
          if (!dCoord1.equals(dCoord2)) {
            FoundCoordinate fc2 = new FoundCoordinate(ocrText, dCoord2, dCoord1);
            aryFoundCoords.add(fc2);
          }
        }
      } catch (Exception e) {
        Log.w(TAG, "Problem parsing input string '" + ocrText + "'.", e);
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    startCameraSource();
  }

  /** Stops the camera. */
  @Override
  protected void onPause() {
    super.onPause();
    if (preview != null) preview.stop();
    if (cameraSource != null) {
      cameraSource.stop();
      cameraSource.release();
    }
  }

/*
  @Override
  public void onDestroy() {
    super.onDestroy();
    if (cameraSource != null) {
      cameraSource.release();
    }
  }
*/

  private GeoCoordinateSelectionListener mOnGeoCoordinateSelected = new GeoCoordinateSelectionListener() {
    TextView txtRotation;
    TextView txtDist;
    // Note: seekbar has 20 positions, 0-19. Each represents 1/4 foot from 1/4 foot to 5 feet.
    // But ultimately we need to report this in both feet and meters, and will use the value in meters.
//    private float tagDistMeters; // Distance chosen from coordinate text marker tag
    FoundCoordinate selectedCoord;
    CompassView compass;
    SeekBar trkDist;

    @Override
    public void onGeoCoordinateSelected(FoundCoordinate foundCoordinate) {
      selectedCoord = foundCoordinate;
      // Let user specify dist & bearing to tag; then continue
      AlertDialog dlg = new AlertDialog.Builder(OCRActivity.this)
              .setTitle(foundCoordinate.toString())
//              .setMessage("What's your distance and bearing to this text marker?")
              .setPositiveButton(android.R.string.ok, okListener)
              .setNegativeButton(android.R.string.cancel, null)
              .setView(R.layout.dlg_dist_bearing)
              .setCancelable(true)
              .create();
      dlg.show();

      txtRotation = (TextView)dlg.findViewById(R.id.txtRotation);
      txtDist = (TextView)dlg.findViewById(R.id.txtDistToTag);
      trkDist = (SeekBar)dlg.findViewById(R.id.trkDistToTag);
      trkDist.setOnSeekBarChangeListener(onDist);
      onDist.onProgressChanged(trkDist, trkDist.getProgress(), false);
      compass = (CompassView)dlg.findViewById(R.id.compass);
      compass.setRotationChangedListener(onRot);
      compass.setCompassRotation(0);
    }

    private DialogInterface.OnClickListener okListener = (dialogInterface, i) -> {
      // Find the device's location and activate the map / AR tracking activity
      Point ptTag = selectedCoord.getPoint();
      int offsetToDeg = compass.getCompassRotation();
      // We now have degrees *to* the coordinate marker; we want degrees *from* it.
      int offsetFromDeg = (offsetToDeg + 180) % 360;
      float distFromTag = feetToMeters(seekBarToFeet(trkDist.getProgress()));
      Point ptDevice = GeometryEngine.moveGeodetic(ptTag,
              distFromTag, new LinearUnit(LinearUnitId.METERS),
              offsetFromDeg, new AngularUnit(AngularUnitId.DEGREES),
              GeodeticCurveType.GEODESIC);
//      mainViewModel.setCurrentUserLocation(ptDevice);

      Intent intent = new Intent(OCRActivity.this, PositionActivity.class);
      intent.putExtra(getString(R.string.extraname_x), ptDevice.getX());
      intent.putExtra(getString(R.string.extraname_y), ptDevice.getY());

      startActivity(intent);
    };
    private OnRotationChangeListener onRot = (oldRotation, newRotation) -> {
      txtRotation.setText(getString(R.string.rotation_degrees, newRotation));
    };
    private SeekBar.OnSeekBarChangeListener onDist = new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        float feet = seekBarToFeet(progress);
        float meters = feetToMeters(feet);
        txtDist.setText(getString(R.string.tag_dist_val, feet, meters));
      }
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) { }
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) { }
    };
    private float seekBarToFeet(int progress) {
      final float incrementVal = 1/4f; // 1/4 foot per stop
      return (progress * incrementVal) + incrementVal;
    }
    private float feetToMeters(float feet) {
      final float METERS_PER_FOOT = 0.3048f;
      float meters = feet * METERS_PER_FOOT;
      return meters;
    }
  };



  @Override
  public void onRequestPermissionsResult(
          int requestCode, String[] permissions, int[] grantResults) {
    Log.i(TAG, "Permission granted!");
    if (PermissionsUtil.allPermissionsGranted(this)) {
      createCameraSource();
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }
}
