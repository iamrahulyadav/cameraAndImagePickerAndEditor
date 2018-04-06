package com.pickerandeditor.modelclasses;

import java.io.Serializable;

/**
 * Created by APPZLOGIC on 4/4/2018.
 */

public class ImageModel implements Serializable {

    private String path;
    private Boolean isSelected = false;
    private Boolean isVideo = false;
    private String caption = "";

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
}
