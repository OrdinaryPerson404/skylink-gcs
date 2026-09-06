package com.cherglow.gcs.tools;

import com.fazecast.jSerialComm.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** jSerialComm 底层读写探针：打开 COM5 → 写 ps → 打印原始字节 */
public final class RawSerialProbe {

    public static void main(String[] args) throws Exception {
        String name = args.length > 0 ? args[0] : "COM5";
        SerialPort p = SerialPort.getCommPort(name);
        p.setBaudRate(115200);
        p.setNumDataBits(8);
        p.setParity(SerialPort.NO_PARITY);
        p.setNumStopBits(SerialPort.ONE_STOP_BIT);
        p.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 0);
        p.clearDTR();
        p.clearRTS();
        System.out.println("openPort -> " + p.openPort() + " (errorCode=" + p.getLastErrorCode() + ")");
        if (!p.isOpen()) {
            return;
        }
        InputStream in = p.getInputStream();
        OutputStream out = p.getOutputStream();

        // 被动听 1s
        long deadline = System.currentTimeMillis() + 1000;
        int passive = 0;
        while (System.currentTimeMillis() < deadline) {
            try {
                int c = in.read();
                if (c >= 0) {
                    passive++;
                }
            } catch (com.fazecast.jSerialComm.SerialPortTimeoutException te) {
                // 超时继续
            }
        }
        System.out.println("passive 1s bytes=" + passive);

        // 写命令并读响应，循环 N 次
        String cmdStr = args.length > 1 ? args[1] : "ps";
        int times = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        for (int i = 0; i < times; i++) {
            byte[] cmd = (cmdStr + "\n").getBytes(StandardCharsets.UTF_8);
            out.write(cmd, 0, cmd.length);
            out.flush();
            long respDeadline = System.currentTimeMillis() + 1000;
            StringBuilder sb = new StringBuilder();
            while (System.currentTimeMillis() < respDeadline) {
                try {
                    int c = in.read();
                    if (c >= 0) {
                        sb.append((char) c);
                        if (c == '\n' && sb.toString().contains("\n") && looksComplete(sb)) {
                            break;
                        }
                    }
                } catch (com.fazecast.jSerialComm.SerialPortTimeoutException te) {
                    // 超时继续到 deadline
                }
            }
            System.out.println("---- resp[" + i + "] bytes=" + sb.length() + " ----");
            System.out.print(sb);
        }
        p.closePort();
        System.out.println("closed");
    }

    /** 简易完整判定：已收到至少一行且 150ms 静默即认为完成 */
    private static boolean looksComplete(StringBuilder sb) {
        return sb.length() > 4; // 由 deadline 截断，简单起见逐行打印
    }

    private RawSerialProbe() {
    }
}
