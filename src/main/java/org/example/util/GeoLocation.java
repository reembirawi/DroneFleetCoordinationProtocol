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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeoLocation)) return false;
        GeoLocation other = (GeoLocation) o;
        return Double.compare(latitude, other.latitude) == 0 &&
                Double.compare(longitude, other.longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(latitude) * 31 + Double.hashCode(longitude);
    }

    @Override
    public String toString() {
        return "GeoLocation{" + "lat=" + latitude + ", lon=" + longitude + '}';
    }
}