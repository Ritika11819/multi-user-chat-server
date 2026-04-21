package server;

import service.ChatService;
import java.net.*;
import java.io.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ChatService chatService;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    //token system for smart spam control
    private int tokens = 5;
    private long lastRefillTime = System.currentTimeMillis();

    public ClientHandler(Socket socket, ChatService chatService) {
        this.socket = socket;
        this.chatService = chatService;
    }

    //allow message based on rate (not size)
    private boolean allowMessage() {
        long now = System.currentTimeMillis();

        // refill tokens every 2 seconds
        if (now-lastRefillTime>2000) {
            tokens=Math.min(tokens + 2, 5); // max 5 tokens
            lastRefillTime=now;
        }

        if (tokens>0) {
            tokens--;
            return true;
        }

        return false;
    }

    @Override
    public void run() {
        try {
            in=new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            out=new PrintWriter(
                    socket.getOutputStream(), true
            );

            //Ask username
            out.println("Enter username:");
            username=in.readLine();

            chatService.addClient(username, this);

            String message;

            while ((message = in.readLine()) != null) {

                // agar user exit type kiya toh
                if (message.equalsIgnoreCase("/exit")) {
                    break;
                }

                // show users
                else if (message.equalsIgnoreCase("/users")) {
                    sendMessage(chatService.getActiveUsers());
                }

                // privately message
                else if (message.startsWith("/msg")) {
                    String[] parts = message.split(" ", 3);

                    if (parts.length < 3) {
                        sendMessage("Invalid format. Use: /msg username message");
                    } else {
                        String toUser = parts[1];
                        String msg = parts[2];
                        chatService.sendPrivateMessage(toUser, msg, username);
                    }
                }

                //spam control (rate based)
                else if (!allowMessage()) {
                    sendMessage("⚠ Too many messages! Please slow down.");
                }

                // normal message sabko dikhega
                else {
                    chatService.broadcast(username + ": " + message, this);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            chatService.removeClient(username);
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}