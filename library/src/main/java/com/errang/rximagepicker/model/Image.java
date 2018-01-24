package com.errang.rximagepicker.model;

import java.io.Serializable;

/**
 * Created by zengp on 2017/7/8.
 */

public class Image implements Serializable {
    public long id;
    public String name;
    public String path;
    public long size;
    public int width;
    public int height;
    public String mimeType;
    public long addTime;

    public boolean isSelected = false;

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Image)) {
            return false;
        }
        if (o instanceof Image) {
            Image item = (Image) o;
            return  this.id == item.id && this.path.equalsIgnoreCase(item.path) && this.addTime == item.addTime;
        }
        return super.equals(o);
    }
}
