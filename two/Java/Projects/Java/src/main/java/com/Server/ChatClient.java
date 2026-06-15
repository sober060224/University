package com.Server;

import java.io.*;
import java.net.*;

public class ChatClient {
    public static void main(String[] args) throws IOException {
        // 连接到运行在本地主机（localhost）上，端口为8888的服务器。
        Socket socket = new Socket("localhost", 8888);

        // 创建一个PrintWriter对象用于向服务器发送消息，并设置自动刷新。
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

        // 创建一个BufferedReader对象用于从服务器接收消息。
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

        // 创建另一个BufferedReader对象用于读取用户输入。
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

        String userInput;

        // 这个循环会一直运行，直到用户输入"exit"。
        while ((userInput = in.readLine()) != null) {
            // 将用户的输入发送到服务器。
            writer.println(userInput);

            // 如果用户输入是"exit"，则跳出循环。
            if ("exit".equalsIgnoreCase(userInput)) {
                break;
            }

            // 从服务器接收并打印接收到的消息。
            System.out.println("Received: " + reader.readLine());
        }

        // 关闭客户端的Socket连接。
        socket.close();
    }
}