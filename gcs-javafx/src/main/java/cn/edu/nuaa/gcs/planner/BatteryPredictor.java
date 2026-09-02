package cn.edu.nuaa.gcs.planner;

public class BatteryPredictor {
    private double[] theta;

    public BatteryPredictor() {
        trainDefault();
    }

    private void trainDefault() {
        double[][] X = {
            {1, 2.0, 0.5, 3},
            {1, 3.5, 1.0, 5},
            {1, 5.0, 1.5, 4},
            {1, 1.5, 0.3, 2},
            {1, 4.2, 0.8, 6},
            {1, 6.0, 2.0, 7},
            {1, 3.0, 0.0, 3},
            {1, 7.5, 1.2, 8},
        };
        double[] y = {16, 12, 8, 19, 10, 5, 15, 3};
        theta = normalEquation(X, y);
    }

    public double[] predict(double distanceKm, double loadKg, double windMs) {
        double endurance = theta[0] + theta[1] * distanceKm + theta[2] * loadKg + theta[3] * windMs;
        endurance = Math.max(1, endurance);
        double cost = Math.min(95, 18 + distanceKm * 9 + loadKg * 6 + windMs * 2);
        return new double[]{endurance, cost};
    }

    private static double[] normalEquation(double[][] X, double[] y) {
        int n = X[0].length;
        double[][] XtX = new double[n][n];
        double[] Xty = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < X.length; k++)
                    XtX[i][j] += X[k][i] * X[k][j];
            }
            for (int k = 0; k < X.length; k++)
                Xty[i] += X[k][i] * y[k];
        }
        double[][] inv = invert(XtX);
        double[] result = new double[n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                result[i] += inv[i][j] * Xty[j];
        return result;
    }

    private static double[][] invert(double[][] m) {
        int n = m.length;
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++)
                aug[i][j] = m[i][j];
            aug[i][n + i] = 1;
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++)
                if (Math.abs(aug[r][col]) > Math.abs(aug[pivot][col]))
                    pivot = r;
            if (Math.abs(aug[pivot][col]) < 1e-12)
                throw new RuntimeException("矩阵不可逆（行列式≈0）");
            double[] tmp = aug[col]; aug[col] = aug[pivot]; aug[pivot] = tmp;
            double pivVal = aug[col][col];
            for (int j = 0; j < 2 * n; j++)
                aug[col][j] /= pivVal;
            for (int r = 0; r < n; r++) {
                if (r == col) continue;
                double factor = aug[r][col];
                for (int j = 0; j < 2 * n; j++)
                    aug[r][j] -= factor * aug[col][j];
            }
        }
        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                inv[i][j] = aug[i][n + j];
        return inv;
    }

    public double[] getTheta() { return theta; }
}
