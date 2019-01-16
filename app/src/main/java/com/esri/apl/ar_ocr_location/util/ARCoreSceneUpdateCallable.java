package com.esri.apl.ar_ocr_location.util;

import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;

public interface ARCoreSceneUpdateCallable {
    void onSceneUpdate(Scene scene, Session session, Frame frame, FrameTime frameTime);
    void onSceneError(Exception e);
}
