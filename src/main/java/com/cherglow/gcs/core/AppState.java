package com.cherglow.gcs.core;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * 全局应用状态（单例）。S1 仅承载连接状态供 TopNav 展示；
 * S4 起由 ConnectionManager 驱动，S2 起 TelemetryBus 挂接于此。
 */
public final class AppState {

    public enum ConnStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    private static final AppState INSTANCE = new AppState();

    public static AppState get() {
        return INSTANCE;
    }

    private AppState() {
    }

    private final ObjectProperty<ConnStatus> connStatus =
            new SimpleObjectProperty<>(this, "connStatus", ConnStatus.DISCONNECTED);

    /** 连接详情文本，如 "UDP 192.168.4.1:14550"；空串表示无详情 */
    private final StringProperty connDetail =
            new SimpleStringProperty(this, "connDetail", "");

    public ObjectProperty<ConnStatus> connStatusProperty() {
        return connStatus;
    }

    public StringProperty connDetailProperty() {
        return connDetail;
    }

    public ConnStatus getConnStatus() {
        return connStatus.get();
    }

    /** 规划页任务有未保存/未上传的修改（飞行页横幅联动） */
    private final BooleanProperty missionDirty =
            new SimpleBooleanProperty(this, "missionDirty", false);

    /** 已规划航点数（飞行页 WP 显示） */
    private final IntegerProperty missionCount =
            new SimpleIntegerProperty(this, "missionCount", 0);

    public BooleanProperty missionDirtyProperty() {
        return missionDirty;
    }

    public IntegerProperty missionCountProperty() {
        return missionCount;
    }
}
