package com.esri.apl.ar_ocr_location.view;

import com.esri.apl.ar_ocr_location.model.FoundCoordinate;

public interface GeoCoordinateSelectionListener {
  void onGeoCoordinateSelected(FoundCoordinate foundCoordinate);
}
