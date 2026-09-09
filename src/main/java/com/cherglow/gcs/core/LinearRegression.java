package com.cherglow.gcs.core;

/**
 * 多元线性回归引擎（正规方程闭式解）。
 * β = (XᵀX)⁻¹ Xᵀy
 * 输入 X 不含截距列，fit() 内部自动添加一列 1。
 */
public final class LinearRegression {

    private LinearRegression() {
    }

    /** 矩阵转置 m[n][k] → result[k][n] */
    static double[][] transpose(double[][] m) {
        int rows = m.length;
        int cols = m[0].length;
        double[][] r = new double[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                r[j][i] = m[i][j];
            }
        }
        return r;
    }

    /** 矩阵乘法 a[n][k] × b[k][p] → result[n][p] */
    static double[][] multiply(double[][] a, double[][] b) {
        int n = a.length;
        int k = a[0].length;
        int p = b[0].length;
        double[][] r = new double[n][p];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < p; j++) {
                double sum = 0;
                for (int t = 0; t < k; t++) {
                    sum += a[i][t] * b[t][j];
                }
                r[i][j] = sum;
            }
        }
        return r;
    }

    /** 矩阵×向量 a[n][k] × v[k] → result[n] */
    static double[] multiplyVec(double[][] a, double[] v) {
        int n = a.length;
        int k = a[0].length;
        double[] r = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int t = 0; t < k; t++) {
                sum += a[i][t] * v[t];
            }
            r[i] = sum;
        }
        return r;
    }

    /** 高斯-若尔当消元法求 n×n 逆矩阵 */
    static double[][] inverse(double[][] m) {
        int n = m.length;
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                aug[i][j] = m[i][j];
            }
            aug[i][n + i] = 1.0;
        }
        for (int col = 0; col < n; col++) {
            int piv = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(aug[r][col]) > Math.abs(aug[piv][col])) {
                    piv = r;
                }
            }
            if (piv != col) {
                double[] tmp = aug[piv];
                aug[piv] = aug[col];
                aug[col] = tmp;
            }
            double diag = aug[col][col];
            if (Math.abs(diag) < 1e-12) {
                throw new ArithmeticException("矩阵不可逆");
            }
            for (int j = 0; j < 2 * n; j++) {
                aug[col][j] /= diag;
            }
            for (int r = 0; r < n; r++) {
                if (r == col) continue;
                double factor = aug[r][col];
                for (int j = 0; j < 2 * n; j++) {
                    aug[r][j] -= factor * aug[col][j];
                }
            }
        }
        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                inv[i][j] = aug[i][n + j];
            }
        }
        return inv;
    }

    /**
     * 训练多元线性回归模型。
     * X[n][k] 为特征矩阵（不含截距列），y[n] 为目标向量。
     * 返回系数向量 β[k+1]，其中 β[0] 为截距项。
     */
    public static double[] fit(double[][] X, double[] y) {
        int n = X.length;
        int k = X[0].length;
        // 增广: 在 X 前加一列 1 作为截距
        double[][] Xa = new double[n][k + 1];
        for (int i = 0; i < n; i++) {
            Xa[i][0] = 1.0;
            for (int j = 0; j < k; j++) {
                Xa[i][j + 1] = X[i][j];
            }
        }
        double[][] Xt = transpose(Xa);
        double[][] XtX = multiply(Xt, Xa);
        double[] Xty = multiplyVec(Xt, y);
        double[][] XtXinv = inverse(XtX);
        return multiplyVec(XtXinv, Xty);
    }

    /**
     * 使用训练好的系数进行预测。
     * x[k] 为特征向量（不含截距），beta[k+1] 为 fit() 返回的系数。
     */
    public static double predict(double[] x, double[] beta) {
        double sum = beta[0]; // 截距
        for (int i = 0; i < x.length; i++) {
            sum += beta[i + 1] * x[i];
        }
        return sum;
    }
}
