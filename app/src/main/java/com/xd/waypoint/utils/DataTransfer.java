package com.xd.waypoint.utils;

import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;

import dji.common.mission.waypoint.Waypoint;

public class DataTransfer {
    private static final DataTransfer instance=new DataTransfer();
    private ArrayList<Waypoint> waypointList;
    private ArrayList<LatLng> points;

    private DataTransfer(){

    }
    public static DataTransfer getInstance(){
        return instance;
    }

    public void setWaypointList(ArrayList<Waypoint> waypointList) {
        this.waypointList = waypointList;
    }

    public void setPoints(ArrayList<LatLng> points) {
        this.points = points;
    }

    public ArrayList<Waypoint> getWaypointList() {
        return waypointList;
    }

    public ArrayList<LatLng> getPoints() {
        return points;
    }
}
