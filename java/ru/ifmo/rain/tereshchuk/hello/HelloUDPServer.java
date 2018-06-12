package ru.ifmo.rain.tereshchuk.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {

    private DatagramSocket socket;
    private ExecutorService threadPool;

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println(e.getMessage());
            return;
        }
        threadPool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; ++i) {
            threadPool.execute(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        byte[] receiveData = new byte[1024];
                        DatagramPacket request = new DatagramPacket(receiveData, receiveData.length);
                        socket.receive(request);
                        String requestText = new String(request.getData(), 0, request.getLength());
                        byte[] sendData = ("Hello, " + requestText).getBytes(Charset.defaultCharset());
                        DatagramPacket response = new DatagramPacket(sendData, sendData.length, request.getAddress(), request.getPort());
                        socket.send(response);
                    } catch (IOException e) {
                        //System.err.println(e.getMessage());
                    }
                }
            });
        }
    }

    @Override
    public void close() {
        socket.close();
        threadPool.shutdownNow();
    }
}
