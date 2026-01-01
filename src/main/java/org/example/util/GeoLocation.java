package org.example.util;

import java.io.Serializable;

public class GeoLocation  implements Serializable {
    private static final long serialVersionUID = 1L;

    private double latitude;
    private double longitude;

    public GeoLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}