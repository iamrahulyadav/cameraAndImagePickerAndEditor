package com.pickerandeditor.modelclasses;

/**
 * Created by APPZLOGIC on 4/4/2018.
 */

public class ImageModel {

    private String path;
    private Boolean isSelected = false;

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
}
