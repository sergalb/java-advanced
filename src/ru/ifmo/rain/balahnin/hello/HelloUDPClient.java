package ru.ifmo.rain.balahnin.hello;


import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HelloUDPClient implements HelloClient {
    private static final int TIMEOUT = 42;
    private static final int CAPACITY = 1024;

    private static byte[] getMessage(String prefix, int threadNumber, int requestNumber) {
        String result = prefix + threadNumber + "_" + requestNumber;
        return result.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void run(String name, int port, String prefix, int threads, int requests) {

        if (threads < 1) {
            throw new IllegalArgumentException("<threads> should be 1 or more");
        }
        InetAddress ip;
        try {
            ip = InetAddress.getByName(name);
        } catch (IOException e) {
            System.out.println("Socket couldn't be opened: " + e.getMessage());
            return;
        }


        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int threadNumber = i;
            threadList.add(new Thread(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(TIMEOUT);

                    for (int index = 0; index < requests; index++) {
                        byte[] message = getMessage(prefix, threadNumber, index);
                        DatagramPacket packet = new DatagramPacket(message, message.length, ip, port);

                        String result = "";
                        String expected = "Hello, " + prefix + threadNumber + "_" + index;
                        do {
                            try {
                                socket.send(packet);
                                byte[] buffer = new byte[CAPACITY];
                                DatagramPacket response = new DatagramPacket(buffer, buffer.length);

                                socket.receive(response);
                                result = new String(response.getData(), response.getOffset(), response.getLength());
                            } catch (SocketTimeoutException e) {
                                System.out.println("Very long waiting: " + e.getMessage());
                            } catch (SocketException e) {
                                System.out.println("Socket couldn't be opened: " + e.getMessage());
                            } catch (IOException e) {
                                System.out.println("Error while sending: " + e.getMessage());
                            }
                        } while (!result.equals(expected));
                    }
                } catch (Exception e) {
                    System.out.println("ERROR");
                }
            }));
            threadList.get(i).start();
        }

        for (int i = 0; i < threads; i++) {
            try {
                threadList.get(i).join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}