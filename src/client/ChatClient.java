package client;

import java.net.*;
import java.io.*;
//sending and recieving simultaneously ek sath hogi
public class ChatClient {

    public static void main(String[] args) {

        try {
            Socket socket = new Socket("localhost", 5000);

            //server se message input to client
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            //client to server
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true
            );

            //user jo terminal pe message likhega vo
            BufferedReader userInput = new BufferedReader(
                    new InputStreamReader(System.in)
            );

            //reciever
            Thread readThread = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            //iske bina ek time par read aur write nai ho pata
            readThread.start();

            //Send messages to server
            String input;
            while ((input = userInput.readLine()) != null) {
                out.println(input);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}