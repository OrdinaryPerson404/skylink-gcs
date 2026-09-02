package cn.edu.nuaa.gcs.model;

public enum ActionMode {
    CRUISE("巡航"),
    LOITER("悬停"),
    TAKEPHOTO("拍照");

    private final String label;
    ActionMode(String label) { this.label = label; }
    public String getLabel() { return label; }
}
