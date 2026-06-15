package com.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class EchoClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 8888);
        System.out.println("已连接到服务器");

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("请输入消息：");
        String userInput = stdIn.readLine();
        out.println(userInput);

        String serverResponse = in.readLine();
        System.out.println("服务器回复：" + serverResponse);

        out.close();
        in.close();
        stdIn.close();
        socket.close();
        System.out.println("连接已关闭");
    }
}
