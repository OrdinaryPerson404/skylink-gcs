package cn.edu.nuaa.gcs.comm;

import cn.edu.nuaa.gcs.comm.MavlinkParser.Telemetry;

import java.util.List;

/**
 * BATTERY_STATUS 诊断工具（独立 main，不依赖 JavaFX）。
 *
 * 用途：直接连接串口，主动请求 BATTERY_STATUS(147) / SYS_STATUS(1) / HIGHRES_IMU(105)，
 * 监听 15 秒响应，打印每条消息详情，最后汇总飞控支持情况。
 *
 * 运行：
 *   mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java
 *     -Dexec.mainClass=cn.edu.nuaa.gcs.comm.BatteryDiagnostic
 *     -Dexec.classpathScope=test -Dexec.args="COM5 115200"
 */
public class BatteryDiagnostic {

    private static final int GCS_SYS = 255, GCS_COMP = 0;
    private static final int DRONE_SYS = 1, DRONE_COMP = 1;

    // 消息计数
    static int heartbeatCount = 0, batteryStatusCount = 0, sysStatusCount = 0,
              highresImuCount = 0, attitudeCount = 0, scaledImuCount = 0,
              rcCount = 0, motorCount = 0, landedCount = 0,
              ackCount = 0, paramValueCount = 0, statusTextCount = 0, otherCount = 0;
    static int crcOkCount = 0, crcFailCount = 0;  // CRC 统计
    static int lastAckCommand = -1, lastAckResult = -1;
    static double lastVoltage = -1, lastCurrent = -1, lastTemp = -1;
    static int lastRemaining = -1, lastCell0 = -1, lastCell1 = -1;

    public static void main(String[] args) throws Exception {
        String port = args.length > 0 ? args[0] : "COM5";
        int baud = args.length > 1 ? Integer.parseInt(args[1]) : 115200;

        System.out.println("============================================");
        System.out.println("  BATTERY_STATUS 诊断工具");
        System.out.println("  串口: " + port + " @ " + baud);
        System.out.println("============================================");

        SerialLink link = new SerialLink(port, baud);
        MavlinkParser parser = new MavlinkParser();  // 有状态流式解析（跨块拼接）
        parser.setLenient(true);  // 诊断模式：跳过 CRC 校验，验证帧布局是否正确
        link.open();
        Thread.sleep(500); // 等串口稳定

        // 启动心跳定时器：每 1 秒发送 HEARTBEAT，保持飞控 mavlinkConnected=true
        // （CF-Drone 飞控收到地面站心跳后才会持续发送完整遥测：ATTITUDE/IMU/MOTOR 等）
        java.util.Timer heartbeatTimer = new java.util.Timer("Diag-Heartbeat", true);
        heartbeatTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                try { link.send(MavlinkEncoder.heartbeat(GCS_SYS, GCS_COMP)); }
                catch (Exception e) { System.err.println("[TX] 心跳发送失败: " + e.getMessage()); }
            }
        }, 0, 1000);  // 立即发第一次，之后每 1000ms 发送
        System.out.println("[TX] 心跳定时器已启动（每 1 秒发送 HEARTBEAT）");
        Thread.sleep(1000); // 等首次心跳到达飞控

        // 请求各类消息（只发一次，飞控收到后回传）
        // 1. 请求 BATTERY_STATUS(147)
        link.send(MavlinkEncoder.requestBatteryStatus(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP));
        System.out.println("[TX] REQUEST BATTERY_STATUS(147) 已发送");
        Thread.sleep(200);

        // 2. 请求 SYS_STATUS(1)
        link.send(MavlinkEncoder.requestMessage(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP, 1));
        System.out.println("[TX] REQUEST SYS_STATUS(1) 已发送");
        Thread.sleep(200);

        // 3. 请求 HIGHRES_IMU(105)
        link.send(MavlinkEncoder.requestMessage(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP, 105));
        System.out.println("[TX] REQUEST HIGHRES_IMU(105) 已发送");
        Thread.sleep(200);

        // 4. 请求参数列表
        link.send(MavlinkEncoder.paramRequestList(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP));
        System.out.println("[TX] PARAM_REQUEST_LIST 已发送");
        Thread.sleep(200);

        // 5. 请求 AUTOPILOT_VERSION
        link.send(MavlinkEncoder.requestAutopilotVersion(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP));
        System.out.println("[TX] REQUEST AUTOPILOT_VERSION 已发送");

        System.out.println("\n[RX] 监听 15 秒（心跳持续发送中）...\n");

        long start = System.currentTimeMillis();
        long lastPrint = 0;
        while (System.currentTimeMillis() - start < 15000) {
            byte[] data = link.read();
            if (data == null || data.length == 0) continue;
            List<Telemetry> frames = parser.feed(data, data.length);
            for (Telemetry t : frames) {
                printTelemetry(t);
            }
            // 每 3 秒打印进度
            long now = System.currentTimeMillis();
            if (now - lastPrint > 3000) {
                lastPrint = now;
                System.out.printf("[..] 已运行 %ds: HEARTBEAT=%d ATTITUDE=%d IMU=%d MOTOR=%d%n",
                    (now - start) / 1000, heartbeatCount, attitudeCount, scaledImuCount, motorCount);
            }
        }

        heartbeatTimer.cancel();  // 停止心跳
        link.close();

        // 汇总
        System.out.println("\n============================================");
        System.out.println("  诊断结果汇总");
        System.out.println("============================================");
        System.out.println("HEARTBEAT(0)       : " + heartbeatCount + " 帧");
        System.out.println("SYS_STATUS(1)      : " + sysStatusCount + " 帧");
        System.out.println("ATTITUDE_QUAT(31)  : " + attitudeCount + " 帧");
        System.out.println("SCALED_IMU(26)     : " + scaledImuCount + " 帧");
        System.out.println("RC_CHANNELS_RAW(35): " + rcCount + " 帧");
        System.out.println("ACTUATOR_TGT(140)  : " + motorCount + " 帧");
        System.out.println("EXT_SYS_STATE(245) : " + landedCount + " 帧");
        System.out.println("HIGHRES_IMU(105)   : " + highresImuCount + " 帧");
        System.out.println("BATTERY_STATUS(147): " + batteryStatusCount + " 帧");
        System.out.println("STATUSTEXT(253)    : " + statusTextCount + " 帧");
        System.out.println("PARAM_VALUE(22)    : " + paramValueCount + " 帧");
        System.out.println("COMMAND_ACK(77)    : " + ackCount + " 帧");
        System.out.println("其它               : " + otherCount + " 帧");
        System.out.println("\nCRC 校验统计: 通过=" + crcOkCount + " 失败=" + crcFailCount
            + " 通过率=" + (crcOkCount + crcFailCount > 0
                ? String.format("%.1f%%", 100.0 * crcOkCount / (crcOkCount + crcFailCount))
                : "N/A"));

        if (lastAckCommand == 512) {
            String[] names = {"ACCEPTED","TEMP_REJECTED","DENIED","UNSUPPORTED","FAILED","IN_PROGRESS"};
            String name = lastAckResult >= 0 && lastAckResult < names.length ? names[lastAckResult] : "UNKNOWN";
            System.out.println("\nREQUEST_MESSAGE ACK: command=512 result=" + lastAckResult + "(" + name + ")");
        }

        System.out.println("\n--- 电池数据（最后一次收到） ---");
        if (batteryStatusCount > 0) {
            System.out.printf("BATTERY_STATUS: 电流=%.2fA 温度=%.1f°C 已消耗=%dmAh 剩余=%d%% 电芯0=%dmV 电芯1=%dmV%n",
                lastCurrent, lastTemp, (int) capacityConsumed(lastCurrent), lastRemaining, lastCell0, lastCell1);
        }
        if (sysStatusCount > 0) {
            System.out.printf("SYS_STATUS: 电压=%.2fV 电流=%.2fA%n", lastVoltage, lastCurrent);
        }

        System.out.println("\n--- 结论 ---");
        if (batteryStatusCount > 0) {
            System.out.println("[OK] 飞控支持 BATTERY_STATUS(147)：电池电流/温度/电芯/容量可读取");
        } else {
            System.out.println("[X] 飞控未回复 BATTERY_STATUS(147)");
        }
        if (sysStatusCount > 0) {
            System.out.println("[OK] 飞控支持 SYS_STATUS(1)：电压/电流/百分比可读取");
        } else {
            System.out.println("[X] 飞控未回复 SYS_STATUS(1)");
        }
        if (highresImuCount > 0) {
            System.out.println("[OK] 飞控支持 HIGHRES_IMU(105)：气压/温度/磁力计可读取");
        } else {
            System.out.println("[X] 飞控未回复 HIGHRES_IMU(105)");
        }
        System.out.println("============================================");
    }

    static double capacityConsumed(double v) { return consumedCap; }
    static double consumedCap = 0;

    private static void printTelemetry(Telemetry t) {
        if (!t.valid) return;
        if (t.crcValid) crcOkCount++; else crcFailCount++;
        String crcTag = t.crcValid ? "" : " [CRC FAIL]";
        switch (t.msgId) {
            case 0:
                heartbeatCount++;
                if (heartbeatCount <= 3) {
                    System.out.printf("[RX] HEARTBEAT%s: customMode=%d armed=%s systemStatus=%d%n",
                        crcTag, t.customMode, ((t.baseMode & 128) != 0), t.systemStatus);
                }
                break;
            case 1:
                sysStatusCount++;
                lastVoltage = t.voltage;
                lastCurrent = t.batteryCurrent;
                System.out.printf("[RX] SYS_STATUS%s: voltage=%.2fV current=%.2fA batteryPct=%d%%%n",
                    crcTag, t.voltage, t.batteryCurrent, (int) t.batteryPct);
                break;
            case 31:
                attitudeCount++;
                if (attitudeCount <= 2) {
                    System.out.printf("[RX] ATTITUDE%s: roll=%.1f pitch=%.1f yaw=%.1f (deg)%n",
                        crcTag, Math.toDegrees(t.roll), Math.toDegrees(t.pitch), Math.toDegrees(t.yaw));
                }
                break;
            case 26:
                scaledImuCount++;
                if (scaledImuCount <= 2) {
                    System.out.printf("[RX] SCALED_IMU%s: acc=(%.2f, %.2f, %.2f) m/s2 gyro=(%.4f, %.4f, %.4f)%n",
                        crcTag, t.accX, t.accY, t.accZ, t.gyroX, t.gyroY, t.gyroZ);
                }
                break;
            case 35:
                rcCount++;
                if (rcCount <= 2) {
                    System.out.printf("[RX] RC_CHANNELS_RAW%s: ch1=%d ch2=%d rssi=%d%n",
                        crcTag, t.rcChannels[0], t.rcChannels[1], t.rssi);
                }
                break;
            case 140:
                motorCount++;
                if (motorCount <= 2) {
                    System.out.printf("[RX] ACTUATOR%s: m1=%.2f m2=%.2f m3=%.2f m4=%.2f%n",
                        crcTag, t.motorOutputs[0], t.motorOutputs[1], t.motorOutputs[2], t.motorOutputs[3]);
                }
                break;
            case 245:
                landedCount++;
                break;
            case 105:
                highresImuCount++;
                System.out.printf("[RX] HIGHRES_IMU%s: absPress=%.1fhPa pressAlt=%.1fm temp=%.1f°C%n",
                    crcTag, t.absPressure, t.pressureAlt, t.imuTemp2);
                break;
            case 147:
                batteryStatusCount++;
                lastCurrent = t.batteryCurrent;
                lastTemp = t.batteryTemp;
                lastRemaining = t.batteryRemaining2;
                lastCell0 = t.cellVoltages[0];
                lastCell1 = t.cellVoltages[1];
                consumedCap = t.capacityConsumed;
                System.out.printf("[RX] BATTERY_STATUS%s: id=%d current=%.2fA temp=%.1f°C consumed=%dmAh remaining=%d%% cell0=%dmV%n",
                    crcTag, t.batteryId, t.batteryCurrent, t.batteryTemp,
                    (int) t.capacityConsumed, t.batteryRemaining2, t.cellVoltages[0]);
                break;
            case 77:
                ackCount++;
                lastAckCommand = t.ackCommand;
                lastAckResult = t.ackResult;
                String[] names = {"ACCEPTED","TEMP_REJECTED","DENIED","UNSUPPORTED","FAILED","IN_PROGRESS"};
                String name = t.ackResult >= 0 && t.ackResult < names.length ? names[t.ackResult] : "UNKNOWN";
                System.out.printf("[RX] COMMAND_ACK%s: command=%d result=%d(%s)%n",
                    crcTag, t.ackCommand, t.ackResult, name);
                break;
            case 22:
                paramValueCount++;
                if (paramValueCount <= 3) {
                    System.out.printf("[RX] PARAM_VALUE%s: name=%s value=%.4f (%d/%d)%n",
                        crcTag, t.paramName, t.paramValue, t.paramIndex, t.paramCount);
                }
                break;
            case 253:
                statusTextCount++;
                if (statusTextCount <= 5) {
                    System.out.printf("[RX] STATUSTEXT%s: severity=%d text=%s%n",
                        crcTag, t.statusSeverity, t.statusText);
                }
                break;
            default:
                otherCount++;
                if (otherCount <= 5) {
                    System.out.printf("[RX] msgId=%d%s%n", t.msgId, crcTag);
                }
                break;
        }
    }
}
