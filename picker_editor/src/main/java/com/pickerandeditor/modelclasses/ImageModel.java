package com.pickerandeditor.modelclasses;

import com.pickerandeditor.editor_classes.PhotoEditor;
import com.pickerandeditor.editor_classes.PhotoEditorView;
import com.pickerandeditor.videoCompressor.K4LVideoTrimmer;

import java.io.Serializable;

/**
 * Created by APPZLOGIC on 4/4/2018.
 */

public class ImageModel{

    private String path;
    private Boolean isSelected = false;
    private Boolean isVideo = false;
    private String caption = "";
    private PhotoEditorView photoEditorView = null;
    private PhotoEditor photoEditor = null;
    private K4LVideoTrimmer k4LVideoTrimmer = null;
    private Boolean operationCompleted = false;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getSelected() {
        return isSelected;
    }

    public void setSelected(Boolean selected) {
        isSelected = selected;
    }

    public Boolean getVideo() {
        return isVideo;
    }

    public void setVideo(Boolean video) {
        isVideo = video;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public PhotoEditorView getPhotoEditorView() {
        return photoEditorView;
    }

    public void setPhotoEditorView(PhotoEditorView photoEditorView) {
        this.photoEditorView = photoEditorView;
    }

    public PhotoEditor getPhotoEditor() {
        return photoEditor;
    }

    public void setPhotoEditor(PhotoEditor photoEditor) {
        this.photoEditor = photoEditor;
    }

    public K4LVideoTrimmer getK4LVideoTrimmer() {
        return k4LVideoTrimmer;
    }

    public void setK4LVideoTrimmer(K4LVideoTrimmer k4LVideoTrimmer) {
        this.k4LVideoTrimmer = k4LVideoTrimmer;
    }

    public Boolean getOperationCompleted() {
        return operationCompleted;
    }

    public void setOperationCompleted(Boolean operationCompleted) {
        this.operationCompleted = operationCompleted;
    }
}
