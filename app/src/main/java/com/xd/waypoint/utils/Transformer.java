package com.xd.waypoint.utils;

import android.location.LocationManager;

import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.model.LatLng;

public class Transformer {
    private static final double PI = 3.14159265358979324;
    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;

//    public static LatLng gcj02ToWgs84(double lat, double lon) {
//        double[] gcj02 = wgs84ToGcj02(lat, lon);
//        double dLat = gcj02[0] - lat;
//        double dLon = gcj02[1] - lon;
//        return new LatLng(lat - dLat, lon - dLon);
//    }
    public static LatLng wgs84ToGcj02(double lat, double lon) {

        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        CoordinateConverter converter=new CoordinateConverter()
                .from(CoordinateConverter.CoordType.GPS)
                .coord(new LatLng(lat,lon));
        return converter.convert();


    }
    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320.0 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }
    public static LatLng Gcj02Towgs84(double lat, double lon){

        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (A/ sqrtMagic * Math.cos(radLat) * PI);
        return new LatLng(lat+dLat,lon+dLon);
//        CoordinateConverter converter=new CoordinateConverter()
//                .from(CoordinateConverter.CoordType.)
//                .coord(new LatLng(lat,lon));
    }
}
