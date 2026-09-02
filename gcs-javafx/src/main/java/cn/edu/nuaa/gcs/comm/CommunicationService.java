package cn.edu.nuaa.gcs.comm;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class CommunicationService {
    private Link link;
    private volatile boolean running = false;
    private Thread listenThread;
    private final ConcurrentLinkedQueue<MavlinkParser.Telemetry> queue = new ConcurrentLinkedQueue<>();
    private Consumer<MavlinkParser.Telemetry> callback;
    private int reconnectAttempts = 0;

    public void setLink(Link link) {
        if (running) stop();
        this.link = link;
    }

    public void setCallback(Consumer<MavlinkParser.Telemetry> cb) {
        this.callback = cb;
    }

    public void start() {
        if (link == null) throw new IllegalStateException("Link not set");
        running = true;
        reconnectAttempts = 0;
        listenThread = new Thread(this::listenLoop, "GCS-Listen");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public void stop() {
        running = false;
        if (listenThread != null) listenThread.interrupt();
        if (link != null) link.close();
    }

    public boolean isConnected() {
        return link != null && link.isOpen() && running;
    }

    private void listenLoop() {
        while (running) {
            try {
                if (!link.isOpen()) {
                    if (reconnectAttempts < 3) {
                        reconnectAttempts++;
                        Thread.sleep((long) Math.pow(2, reconnectAttempts) * 1000);
                        link.open();
                    } else {
                        running = false;
                        break;
                    }
                }
                byte[] data = link.read();
                if (data != null && data.length > 0) {
                    MavlinkParser.Telemetry t = MavlinkParser.parse(data, 0, data.length);
                    if (t.valid) {
                        queue.add(t);
                        if (callback != null) callback.accept(t);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                reconnectAttempts++;
            }
        }
    }

    public MavlinkParser.Telemetry poll() {
        return queue.poll();
    }
}
