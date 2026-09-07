package com.cherglow.gcs.core;

import java.util.function.Consumer;

/**
 * 命令通道抽象（S13）：串口 CliLink 与 MAVLink/UDP MavLinkLink 共同实现，
 * 使 ConnectionService 与页面逻辑与传输无关。
 */
public interface CommandLink {

    /** 按需执行（结果并入遥测快照） */
    void requestOnce(String cmd);

    /** 执行命令（危险命令确认由 UI 层负责）；out 回调收原始输出文本 */
    void sendCommand(String cmd, Consumer<String> out);
}
