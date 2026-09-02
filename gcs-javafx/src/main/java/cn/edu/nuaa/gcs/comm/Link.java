package cn.edu.nuaa.gcs.comm;

public interface Link {
    void open() throws Exception;
    byte[] read() throws Exception;
    void send(byte[] data) throws Exception;
    void close();
    boolean isOpen();
}
