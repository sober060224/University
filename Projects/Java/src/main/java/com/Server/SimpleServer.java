package com.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleServer {
    public static void main(String[] args) throws IOException {
        // 创建一个服务器套接字，监听8888端口
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器启动，等待连接");

        // 接受客户端的连接请求
        Socket socket = serverSocket.accept();
        System.out.println("客户端已连接");

        // 创建输入流和输出流对象
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // 读取客户端发送的消息
        String msg = in.readLine();
        System.out.println("收到客户端消息：" + msg);

        // 向客户端回复消息
        out.println("服务器已收到你的消息：" + msg);

        // 关闭资源
        in.close();
        out.close();
        socket.close();
        serverSocket.close();
    }
}
