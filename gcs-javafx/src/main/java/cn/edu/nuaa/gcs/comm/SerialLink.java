package cn.edu.nuaa.gcs.comm;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SerialLink implements Link {
    private final String portName;
    private final int baudRate;
    private SerialPort serialPort;
    private final BlockingQueue<Byte> rxBuffer = new LinkedBlockingQueue<>(65536);
    private volatile boolean open = false;

    public SerialLink(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
    }

    @Override
    public void open() throws Exception {
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        System.out.println("[Serial] 打开串口: " + portName + " @ " + baudRate);
        if (!serialPort.openPort()) {
            System.out.println("[Serial] 打开失败!");
            throw new RuntimeException("无法打开串口 " + portName);
        }
        System.out.println("[Serial] 串口已打开, 监听数据...");

        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                byte[] data = event.getReceivedData();
                System.out.println("[Serial] 收到数据: " + data.length + " bytes");
                for (byte b : data) {
                    rxBuffer.offer(b);
                }
            }
        });

        open = true;
    }

    @Override
    public byte[] read() throws Exception {
        if (!open) return null;

        if (rxBuffer.isEmpty()) {
            Thread.sleep(50);
            return null;
        }

        int available = rxBuffer.size();
        byte[] data = new byte[available];
        for (int i = 0; i < available; i++) {
            Byte b = rxBuffer.poll(50, TimeUnit.MILLISECONDS);
            if (b == null) break;
            data[i] = b;
        }
        return data;
    }

    @Override
    public void send(byte[] data) throws Exception {
        if (!open) throw new IllegalStateException("Link not open");
        serialPort.writeBytes(data, data.length);
    }

    @Override
    public void close() {
        if (serialPort != null) {
            serialPort.removeDataListener();
            serialPort.closePort();
        }
        rxBuffer.clear();
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open && serialPort != null && serialPort.isOpen();
    }

    @Override
    public String toString() { return portName + "@" + baudRate; }

    public static String[] listPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] names = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            names[i] = ports[i].getSystemPortName();
        }
        return names;
    }
}
