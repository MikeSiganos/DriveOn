package com.msiganos.driveon.helpers;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.msiganos.driveon.R;

public class MarkerClusterInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
    final Activity activity;

    public MarkerClusterInfoWindowAdapter(Activity activity) {
        this.activity = activity;
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        // return infoWindow;
        return null;
    }

    @NonNull
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        View infoWindow = activity.getLayoutInflater().inflate(R.layout.dialog_map_info, activity.findViewById(R.id.mapView), false);
        TextView title = infoWindow.findViewById(R.id.dialog_map_info_marker_title);
        title.setText(marker.getTitle());
        TextView snippet = infoWindow.findViewById(R.id.dialog_map_info_marker_snippet);
        snippet.setText(marker.getSnippet());
        return infoWindow;
        // return null;
    }
}