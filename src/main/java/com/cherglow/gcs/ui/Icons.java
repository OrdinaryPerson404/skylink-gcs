package com.cherglow.gcs.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;

/**
 * 内联矢量图标（与原型 HTML 的 24x24 stroke 图标同源）。
 * JavaFX CSS 不支持后代选择器，图标描边统一由 {@link #tint(Node, Color)} 编程控制。
 */
public final class Icons {

    private Icons() {
    }

    public static final String DASHBOARD =
            "M3 3h7v7h-7z M14 3h7v7h-7z M3 14h7v7h-7z M14 14h7v7h-7z";
    public static final String FLY =
            "M12 2L18 12L22 14L12 22L2 14L6 12Z";
    public static final String PLAN =
            "M3 17L8 12L12 16L21 7 M23 7a2 2 0 1 1-4 0a2 2 0 1 1 4 0";
    public static final String SETUP =
            "M12 9a3 3 0 1 0 0.001 0 M12 1v3 M12 20v3 M1 12h3 M20 12h3"
                    + " M4.5 4.5l2 2 M17.5 17.5l2 2 M4.5 19.5l2-2 M17.5 6.5l2-2";
    public static final String DATA =
            "M3 20L7 14L10 17L17 8L21 12";
    public static final String BELL =
            "M18 16v-5a6 6 0 0 0-12 0v5l-2 2h16z M10.5 20a1.5 1.5 0 0 0 3 0";
    public static final String USER =
            "M12 11a4 4 0 1 0 0.001 0 M4.5 21c0-3.5 3.4-5.5 7.5-5.5s7.5 2 7.5 5.5";

    /** 品牌六边形（填充色由调用方设置） */
    public static final String HEX =
            "M12 2l8.66 5v10L12 22l-8.66-5V7z";

    /** 品牌内部旋翼十字 */
    public static final String ROTORS =
            "M12 7v10 M7.5 9.5l9 5 M16.5 9.5l-9 5";

    /** 四旋翼 LOGO（俯视 X 布局，四角旋翼环 + 支臂），描边用 */
    public static final String DRONE =
            "M7 3a4 4 0 1 0 .001 0 M17 3a4 4 0 1 0 .001 0"
                    + " M7 13a4 4 0 1 0 .001 0 M17 13a4 4 0 1 0 .001 0"
                    + " M9.4 9.4L7.6 7.6 M14.6 9.4L16.4 7.6"
                    + " M9.4 14.6L7.6 16.4 M14.6 14.6L16.4 16.4";

    /** 四旋翼 LOGO 机身圆点（填充用） */
    public static final String DRONE_DOT =
            "M12 10.3a1.7 1.7 0 1 0 .001 0";

    /** 四旋翼 LOGO（描边机身 + 彩色中心点） */
    public static Node droneLogo(double size, Color armColor, Color dotColor) {
        SVGPath body = new SVGPath();
        body.setContent(DRONE);
        body.setStroke(armColor);
        body.setStrokeWidth(1.8);
        body.setFill(Color.TRANSPARENT);
        body.setStrokeLineCap(StrokeLineCap.ROUND);
        body.setStrokeLineJoin(StrokeLineJoin.ROUND);
        SVGPath dot = new SVGPath();
        dot.setContent(DRONE_DOT);
        dot.setFill(dotColor);
        dot.setStroke(null);
        double s = size / 24.0;
        body.getTransforms().add(new Scale(s, s));
        dot.getTransforms().add(new Scale(s, s));
        return new Group(body, dot);
    }

    /**
     * 创建描边式图标。外层 Group 保证按缩放后尺寸参与布局。
     */
    public static Node icon(String path, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setStroke(Color.web("#8b949e"));
        svg.setStrokeWidth(2.0);
        svg.setFill(Color.TRANSPARENT);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double s = size / 24.0;
        svg.getTransforms().add(new Scale(s, s));
        return new Group(svg);
    }

    /** 填充式图标（品牌六边形等） */
    public static Node filled(String path, Color fill, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(fill);
        svg.setStroke(null);
        double s = size / 24.0;
        svg.getTransforms().add(new Scale(s, s));
        return new Group(svg);
    }

    /** 编程式改色（active 态高亮等） */
    public static void tint(Node iconNode, Color color) {
        if (iconNode instanceof Group g && !g.getChildren().isEmpty()
                && g.getChildren().get(0) instanceof SVGPath svg) {
            if (color == null) {
                svg.setFill(Color.TRANSPARENT);
                svg.setStroke(Color.web("#8b949e"));
            } else {
                svg.setStroke(color);
            }
        }
    }

    /** 仅对填充式图标改色 */
    public static void tintFill(Node iconNode, Color color) {
        if (iconNode instanceof Group g && !g.getChildren().isEmpty()
                && g.getChildren().get(0) instanceof SVGPath svg) {
            svg.setFill(color);
        }
    }
}
