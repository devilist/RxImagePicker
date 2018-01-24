package com.errang.rximagepicker.model;

import java.util.ArrayList;

/**
 * Created by zengp on 2017/7/11.
 */

public class VideoFolder {
    public String name;
    public String path;
    public String cover;
    public ArrayList<Video> videoList;

    @Override
    public boolean equals(Object o) {
        try {
            VideoFolder other = (VideoFolder) o;
            return this.path.equalsIgnoreCase(other.path) && this.name.equalsIgnoreCase(other.name);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return super.equals(o);
    }

}
