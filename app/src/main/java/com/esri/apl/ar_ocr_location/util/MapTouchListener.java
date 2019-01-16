package com.esri.apl.ar_ocr_location.util;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;

import com.esri.apl.ar_ocr_location.model.SelectedGraphicHelper;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;

public class MapTouchListener extends DefaultMapViewOnTouchListener {
  private final static String TAG = "MapTouchListener";
  private final static double TOLERANCE = 5d;
  private SelectedGraphicHelper mOnLocationClick;

  public MapTouchListener(Context context, MapView mapView, SelectedGraphicHelper onClick) {
    super(context, mapView);
    this.mOnLocationClick = onClick;
  }

  @Override
  public boolean onSingleTapConfirmed(MotionEvent e) {
    mMapView.getCallout().dismiss();

    // Identify for points near touched location
    // Note: this will break if more than one graphics overlay exist.
    if (mMapView.getGraphicsOverlays().size() < 1) return false;

    GraphicsOverlay govl = mMapView.getGraphicsOverlays().get(0);

    ListenableFuture<IdentifyGraphicsOverlayResult> fres =
            mMapView.identifyGraphicsOverlayAsync(govl,
                    new Point(Math.round(e.getX()), Math.round(e.getY())),
                    TOLERANCE, false, 1);
    fres.addDoneListener(() -> {
      try {
        if (fres.isDone()) {
          IdentifyGraphicsOverlayResult res = fres.get();
          if (res.getError() != null) throw res.getError();
          Graphic g = res.getGraphics().size() > 0 ? res.getGraphics().get(0) : null;
          mOnLocationClick.itemClicked(g);
        }
      } catch (Exception exc) {
        Log.e(TAG, "Error identifying graphics:" + exc.getLocalizedMessage(), exc);
      }
    });

    return false;
  }
}
