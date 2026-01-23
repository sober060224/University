package com.Server;

import java.io.*;
import java.net.*;

public class ChatClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 8888);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

        String userInput;
        while ((userInput = in.readLine()) != null) {
            writer.println(userInput);
            if ("exit".equalsIgnoreCase(userInput)) {
                break;
            }
            System.out.println("Received: " + reader.readLine());
        }

        socket.close();
    }
}