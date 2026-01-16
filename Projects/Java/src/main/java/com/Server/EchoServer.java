package com.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器已启动，等待客户端连接……");

        Socket clientSocket = serverSocket.accept();
        System.out.println("客户端已连接" + clientSocket.getInetAddress());

        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String inputLine = in.readLine();
        System.out.println("收到客户端信息：" + inputLine);
        out.println("服务器已收到：" + inputLine);

        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }
}
