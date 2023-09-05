package com.msiganos.driveon.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MarkerClusterItemHelper implements ClusterItem {
    private LatLng position;
    private BitmapDescriptor icon;
    private Integer color;
    private String title;
    private String snippet;
    private Object data;
    private Float zIndex;

    public MarkerClusterItemHelper(LatLng position, BitmapDescriptor icon, String title, String snippet, Integer color, Object data) {
        setPosition(position);
        setIcon(icon);
        setTitle(title);
        setSnippet(snippet);
        setColor(color);
        setData(data);
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    @NonNull
    public BitmapDescriptor getIcon() {
        return icon;
    }

    public void setIcon(BitmapDescriptor icon) {
        this.icon = icon;
    }

    @Nullable
    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    @NonNull
    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    @NonNull
    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Nullable
    @Override
    public Float getZIndex() {
        return zIndex;
    }

    public void setZIndex(Float zIndex) {
        this.zIndex = zIndex;
    }
}