package com.cherglow.gcs.serial;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 串口传输（jSerialComm 封装）。
 * 规格：8N1；DTR/RTS 保持 false（防 ESP32 复位）；按行装配（\n 分隔，剥离 \r）。
 */
public final class SerialTransport implements AutoCloseable {

    private SerialPort port;
    private Thread readerThread;
    private volatile boolean running;
    private volatile Consumer<String> lineListener;
    private volatile Consumer<String> errorListener;

    public static List<String> listPorts() {
        List<String> names = new ArrayList<>();
        for (SerialPort p : SerialPort.getCommPorts()) {
            names.add(p.getSystemPortName());
        }
        return names;
    }

    public void setLineListener(Consumer<String> listener) {
        this.lineListener = listener;
    }

    /** 读写错误回调（与数据行分离，避免错误被当作遥测数据） */
    public void setErrorListener(Consumer<String> listener) {
        this.errorListener = listener;
    }

    /** 打开端口并启动读线程；失败抛 IOException（含错误码原文） */
    public void open(String portName, int baud) throws IOException {
        if (running) {
            throw new IOException("串口已打开");
        }
        SerialPort p = SerialPort.getCommPort(portName);
        p.setBaudRate(baud);
        p.setNumDataBits(8);
        p.setParity(SerialPort.NO_PARITY);
        p.setNumStopBits(SerialPort.ONE_STOP_BIT);
        p.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 0);
        // 必须在 openPort 前设置，避免 DTR/RTS 触发 ESP32 复位
        p.clearDTR();
        p.clearRTS();
        if (!p.openPort()) {
            throw new IOException("打开 " + portName + " 失败（错误码 " + p.getLastErrorCode()
                    + "）；请确认端口未被其他程序占用");
        }
        this.port = p;
        this.running = true;
        readerThread = new Thread(this::readLoop, "serial-reader-" + portName);
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public boolean isOpen() {
        return running;
    }

    public void writeLine(String line) throws IOException {
        if (!running || port == null) {
            throw new IOException("串口未打开");
        }
        byte[] data = (line + "\n").getBytes(StandardCharsets.UTF_8);
        int n = port.writeBytes(data, data.length);
        if (n != data.length) {
            throw new IOException("串口写入不完整：期望 " + data.length + " 字节，实际 " + n);
        }
    }

    private void readLoop() {
        java.io.ByteArrayOutputStream lineBuf = new java.io.ByteArrayOutputStream(256);
        try {
            InputStream in = port.getInputStream();
            while (running) {
                int c;
                try {
                    c = in.read();
                } catch (com.fazecast.jSerialComm.SerialPortTimeoutException te) {
                    continue; // SEMI_BLOCKING 超时属正常空转，继续读到关闭
                }
                if (c == -1) {
                    continue; // 读超时/关闭，继续直到退出
                }
                if (c == '\n') {
                    emit(lineBuf);
                } else if (c != '\r') {
                    lineBuf.write(c);
                }
            }
        } catch (Exception e) {
            Consumer<String> el = errorListener;
            if (running && el != null) {
                el.accept("serial-read: " + e); // 异常拔线等场景
            }
        } finally {
            running = false;
        }
    }

    private void emit(java.io.ByteArrayOutputStream lineBuf) {
        Consumer<String> l = lineListener;
        if (l != null && lineBuf.size() > 0) {
            l.accept(new String(lineBuf.toByteArray(), StandardCharsets.UTF_8)); // 中文标签按 UTF-8 解码
        }
        lineBuf.reset();
    }

    @Override
    public void close() {
        running = false;
        if (port != null && port.isOpen()) {
            port.closePort();
        }
        port = null;
        if (readerThread != null) {
            try {
                readerThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            readerThread = null;
        }
    }
}
