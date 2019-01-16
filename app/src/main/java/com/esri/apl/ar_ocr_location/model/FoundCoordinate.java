package com.esri.apl.ar_ocr_location.model;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;

public class FoundCoordinate {
  private Point point;
  private boolean selected = false;
  /** This acts as a sort of unique key; there should only be one object with this value in a list */
  private String ocrText;

  /**
   *
   * @param text The text recognized by Firebase VisionText
   * @param x The x-coordinate in the text
   * @param y The y-coordinate in the text
   * @throws NumberFormatException
   */
  public FoundCoordinate(String text, double x, double y) {
    this.ocrText = text;
    this.point = new Point(x, y, SpatialReferences.getWgs84());
  }

  public Point getPoint() {
    return this.point;
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public String getOcrText() {
    return ocrText;
  }

/*  @Override
  public boolean equals(Object obj) throws InvalidParameterException {
    if (!(obj instanceof FoundCoordinate)) throw new InvalidParameterException(
              "Parameter must be an instance of " + this.getClass().getSimpleName());
    FoundCoordinate that = (FoundCoordinate)obj;

//    return this.getPoint().getX() == that.getPoint().getX()
//        && this.getPoint().getY() == that.getPoint().getY();
    return this.getOcrText().equalsIgnoreCase(that.getOcrText());
  }*/

  @Override
  public String toString() {
//    String.format(Locale.US, "x: %f, y: %f", point.getX() , point.getY());
    return "x: " + point.getX() + ", y: " + point.getY();
  }
}
