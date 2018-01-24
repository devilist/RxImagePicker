package com.errang.rximagepicker.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by zengp on 2017/7/8.
 */

public class Folder implements Serializable {
    public String name;
    public String path;
    public Image cover;
    public ArrayList<Image> imageList;


    @Override
    public boolean equals(Object o) {
        try {
            Folder other = (Folder) o;
            return this.path.equalsIgnoreCase(other.path) && this.name.equalsIgnoreCase(other.name);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return super.equals(o);
    }
}
