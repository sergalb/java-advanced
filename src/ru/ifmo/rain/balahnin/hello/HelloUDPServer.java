package ru.ifmo.rain.balahnin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HelloUDPServer implements HelloServer {
    private static final int TIMEOUT = 100;
    private static final int CAPACITY = 1024;
    private DatagramSocket socket;
    private List<Thread> threadList = new ArrayList<>();

    @Override
    public void start(int port, int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("<threads> should be 1 or more");
        }
        try {
            if (socket == null) {
                socket = new DatagramSocket(port);
            }
            socket.setSoTimeout(TIMEOUT);

            for (int i = 0; i < threads; i++) {
                threadList.add(new Thread(() -> {
                    try {
                        while (true) {
                            byte[] read = new byte[CAPACITY];
                            DatagramPacket packet = new DatagramPacket(read, read.length);
                            socket.receive(packet);

                            byte[] res = getMessage("Hello, " + new String(packet.getData(), packet.getOffset(), packet.getLength()));
                            InetAddress clientIP = packet.getAddress();
                            int clientPort = packet.getPort();

                            DatagramPacket response = new DatagramPacket(res, res.length, clientIP, clientPort);
                            socket.send(response);
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("Very long waiting: " + e.getMessage());
                    } catch (SocketException e) {
                        System.out.println("Socket couldn't be opened: " + e.getMessage());
                    } catch (IOException e) {
                        System.out.println("Error while sending: " + e.getMessage());
                    }
                }));

                threadList.get(i).start();
            }
        } catch (SocketException e) {
            System.out.println("Socket couldn't be opened: " + e.getMessage());
        }
    }

    private static byte[] getMessage(String message) {
        return message.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        for (Thread thread : threadList) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
        socket.close();
    }
}