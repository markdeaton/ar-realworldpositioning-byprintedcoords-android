package com.esri.apl.ar_ocr_location.model;


import androidx.databinding.ObservableArrayList;

/**
 * The only way this differs from a normal ArrayList is that its contains()
 * method compares strings insensitive to case.
 */
public class FoundCoordArrayList extends ObservableArrayList<FoundCoordinate> {
  @Override
  public boolean contains(Object o) {
    boolean bRetVal = false;

    if (o instanceof String) {
      String s = o.toString();
      for (FoundCoordinate fc : this) {
        if (fc.getOcrText().equalsIgnoreCase(s)) {
          bRetVal = true;
          break;
        }
      }
    } else if (o instanceof FoundCoordinate) {
      FoundCoordinate that = (FoundCoordinate) o;
      for (FoundCoordinate fc : this) {
        if (fc.getOcrText().equalsIgnoreCase(that.getOcrText())) {
          bRetVal = true;
          break;
        }
      }
    } else bRetVal = super.contains(o);

    return bRetVal;
  }
}
