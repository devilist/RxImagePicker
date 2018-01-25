package com.errang.rximagepicker.model;

import java.io.Serializable;

/**
 * Created by zengp on 2017/7/11.
 */

public class Video implements Serializable {

    public String name;
    public String path;
    public String thumbnail;
    public long size;
    public long duration;
    public int width;
    public int height;
    public String mimeType;
    public long addTime;
    public boolean isSelected = false;

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Video)) {
            return false;
        }
        if (o instanceof Video) {
            Video item = (Video) o;
            return this.path.equalsIgnoreCase(item.path) && this.addTime == item.addTime;
        }
        return super.equals(o);
    }

}
