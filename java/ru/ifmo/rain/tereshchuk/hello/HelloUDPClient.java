package ru.ifmo.rain.tereshchuk.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.concurrent.*;

public class HelloUDPClient implements HelloClient {

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.err.println(e.getMessage());
            return;
        }
        for (int i = 0; i < threads; i++) {
            String threadPrefix = prefix + i + "_";
            threadPool.execute(() -> {
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket();
                    socket.setSoTimeout(60);
                } catch (SocketException e) {
                    System.err.println(e.getMessage());
                    return;
                }
                for (int j = 0; j < requests; j++) {
                    String requestText = threadPrefix + j;
                    try {
                        byte[] sendData = requestText.getBytes(Charset.defaultCharset());
                        socket.send(new DatagramPacket(sendData, sendData.length, address, port));
                        byte[] receiveData = new byte[1024];
                        DatagramPacket response = new DatagramPacket(receiveData, receiveData.length);
                        socket.receive(response);
                        String responseText = new String(response.getData(), 0, response.getLength());
                        if (!responseText.contains(requestText)) {
                            j--;
                        }
                    } catch (IOException e) {
                        j--;
                    }
                }
                socket.close();
            });
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
    }
}