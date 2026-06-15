package com.Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    // 存储所有客户端的PrintWriter对象列表。这将用于广播消息给所有的客户机。
    private static List<PrintWriter> clients = new ArrayList<>();

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            System.out.println("服务器已经在端口 8888 上启动");

            // 这个循环将一直运行，直到程序被中断。它接受新的客户端连接并为他们创建一个新的处理线程。
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("新客户机已连接: " + socket.getInetAddress());

                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clients) {
                    clients.add(writer);
                }

                Thread thread = new Thread(() -> handleClient(socket));
                thread.start();
            }
        }
    }

    // 这个方法负责处理单个客户机的请求。它从客户端读取行，并将它们广播给所有的其他客户机。如果接收到"exit"消息，它会停止服务该客户的线程。
    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            String message;
            while ((message = reader.readLine()) != null) {
                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }
                System.out.println("从客户机接收: " + message);
                broadcastMessage(message);
            }
        } catch (IOException e) {
            System.out.println("在处理客户机时出错");
        } finally {
            synchronized (clients) {
                clients.removeIf(writer -> writer.checkError());
            }
        }
    }

    // 这个方法遍历所有已连接的客户端，并将消息写入他们的输出流。这用于向所有的客户机广播一条消息。
    private void broadcastMessage(String message) {
        synchronized (clients) {
            for (PrintWriter writer : clients) {
                writer.println(message);
            }
        }
    }

    public static void main(String[] args) {
        try {
            ChatServer server = new ChatServer();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
