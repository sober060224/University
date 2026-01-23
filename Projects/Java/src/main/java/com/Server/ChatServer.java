package com.Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static List<PrintWriter> clients = new ArrayList<>();

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            System.out.println("Server is starting on port 8888");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clients) {
                    clients.add(writer);
                }

                Thread thread = new Thread(() -> handleClient(socket));
                thread.start();
            }
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            String message;
            while ((message = reader.readLine()) != null) {
                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }
                System.out.println("Received from client: " + message);
                broadcastMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Error occurred while handling client");
        } finally {
            synchronized (clients) {
                clients.removeIf(writer -> writer.checkError());
            }
        }
    }

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