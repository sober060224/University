package com.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器启动，等待连接");

        Socket socket = serverSocket.accept();
        System.out.println("客户端已连接");

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        String msg = in.readLine();
        System.out.println("收到客户端消息：" + msg);

        out.println("服务器已收到你的消息：" + msg);

        in.close();
        out.close();
        socket.close();
        serverSocket.close();
    }
}
