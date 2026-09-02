package cn.edu.nuaa.gcs.planner;

public class GeoUtil {
    private static final double R = 6371000.0;

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    public static double[] lonLatToPixel(double lon, double lat, int zoom) {
        double n = Math.pow(2, zoom);
        double x = (lon + 180) / 360 * 256 * n;
        double latRad = Math.toRadians(lat);
        double y = (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * 256 * n;
        return new double[]{x, y};
    }

    public static double[] pixelToLonLat(double x, double y, int zoom) {
        double n = Math.pow(2, zoom);
        double lon = x / (256 * n) * 360 - 180;
        double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * y / (256 * n))));
        double lat = Math.toDegrees(latRad);
        return new double[]{lon, lat};
    }

    public static int[] pixelToTile(double x, double y) {
        return new int[]{(int) Math.floor(x / 256), (int) Math.floor(y / 256)};
    }
}
