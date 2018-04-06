package com.pickerandeditor.editor_classes;

/**
 * Created by Burhanuddin Rashid on 1/17/2018.
 */

interface BrushViewChangeListener {
    void onViewAdd(BrushDrawingView brushDrawingView);

    void onViewRemoved(BrushDrawingView brushDrawingView);

    void onStartDrawing();

    void onStopDrawing();
}
