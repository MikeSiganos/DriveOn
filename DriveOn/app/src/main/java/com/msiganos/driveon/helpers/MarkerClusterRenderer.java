package com.msiganos.driveon.helpers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.msiganos.driveon.R;

public class MarkerClusterRenderer extends DefaultClusterRenderer<MarkerClusterItemHelper> {
    private final Context context;
    private final GoogleMap gMap;
    private Integer color;

    public MarkerClusterRenderer(Context context, GoogleMap map, ClusterManager<MarkerClusterItemHelper> clusterManager) {
        super(context, map, clusterManager);
        this.context = context;
        this.gMap = map;
    }

    @Override
    protected void onBeforeClusterItemRendered(@NonNull MarkerClusterItemHelper item, @NonNull MarkerOptions markerOptions) {
        color = item.getColor();
        markerOptions.icon(item.getIcon());
    }

    @Override
    protected void onClusterItemRendered(@NonNull MarkerClusterItemHelper clusterItem, @NonNull Marker marker) {
        // super.onClusterItemRendered(clusterItem, marker);
        color = clusterItem.getColor();
        marker.setTitle(clusterItem.getTitle());
        marker.setSnippet(clusterItem.getSnippet());
    }

    @Override
    protected void onBeforeClusterRendered(@NonNull Cluster<MarkerClusterItemHelper> cluster, @NonNull MarkerOptions markerOptions) {
        // super.onBeforeClusterRendered(cluster, markerOptions);
        color = cluster.getItems().iterator().next().getColor();
        // markerOptions.icon(BitmapDescriptorFactory.fromBitmap(cluster.getItems().iterator().next().getIcon()));
    }

    @Override
    public int getColor(int clusterSize) {
        // return super.getColor(clusterSize);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        if (clusterSize >= 100) {
            hsv[2] = 0.4f;
        } else if (clusterSize >= 50) {
            hsv[2] = 0.5f;
        } else if (clusterSize >= 10) {
            hsv[2] = 0.6f;
        } else {
            hsv[2] = 0.7f;
        }
        color = Color.HSVToColor(hsv);
        return color;
    }

    @Override
    public void setOnClusterItemInfoWindowClickListener(ClusterManager.OnClusterItemInfoWindowClickListener<MarkerClusterItemHelper> listener) {
        super.setOnClusterItemInfoWindowClickListener(new ClusterManager.OnClusterItemInfoWindowClickListener<MarkerClusterItemHelper>() {
            @Override
            public void onClusterItemInfoWindowClick(MarkerClusterItemHelper item) {
                try {
                    Object objectItem = item.getData();
                    if (objectItem instanceof IncidentHelper) {
                        IncidentHelper incidentHelper = (IncidentHelper) objectItem;
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + incidentHelper.getLatitude() + "," + incidentHelper.getLongitude()));
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.select_app)));
                    } else if (objectItem instanceof TrafficHelper) {
                        TrafficHelper trafficHelper = (TrafficHelper) objectItem;
                        LatLng origin = new LatLng(trafficHelper.getLatitudeStart(), trafficHelper.getLongitudeStart());
                        LatLng dest = new LatLng(trafficHelper.getLatitudeEnd(), trafficHelper.getLongitudeEnd());
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getRouteUrl(origin, dest)));
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.select_app)));
                    } else if (objectItem instanceof LocationHelper) {
                        LocationHelper locationHelper = (LocationHelper) objectItem;
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + locationHelper.getLatitude() + "," + locationHelper.getLongitude()));
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.select_app)));
                    } else {
                        Log.w("ClusterRenderer", "Unknown cluster object " + objectItem);
                    }
                } catch (Exception e) {
                    Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show();
                    Log.e("ClusterRendererException", e.toString());
                }
            }
        });
    }

    @Override
    public void setOnClusterClickListener(ClusterManager.OnClusterClickListener<MarkerClusterItemHelper> listener) {
        super.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MarkerClusterItemHelper>() {
            @Override
            public boolean onClusterClick(Cluster<MarkerClusterItemHelper> cluster) {
                try {
                    gMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(cluster.getPosition())
                                    .zoom(15)
                                    .tilt(80)
                                    .build()
                    ));
                    return true;
                } catch (Exception e) {
                    Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show();
                    Log.e("ClusterException", e.toString());
                    return false;
                }
            }
        });
    }

    @Override
    public void setOnClusterItemClickListener(ClusterManager.OnClusterItemClickListener<MarkerClusterItemHelper> listener) {
        super.setOnClusterItemClickListener(listener);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private String getRouteUrl(LatLng origin, LatLng dest) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode driving
        String travel_mode = "travelmode=driving";
        // Directions action
        String dir_action = "dir_action=navigate";
        // Parameters
        String parameters = str_origin + "&" + str_dest + "&" + travel_mode + "&" + dir_action;
        // Directions URL
        String url = "https://www.google.com/maps/dir/?api=1&" + parameters;
        Log.i("RouteUrl", url);
        return url;
    }
}
