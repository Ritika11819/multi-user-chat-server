package server;

import service.ChatService;
import service.ChatServiceImpl;
import java.net.*;
import java.util.concurrent.*;

public class ChatServer {

    public static void main(String[] args) {
        try {

            //server wait karega
            ServerSocket server = new ServerSocket(5000);
            System.out.println("Server started...");
            System.out.println("Waiting for clients...");

            //iska matlab ye ki ek common chat server will be shared across all users
            ChatService chatService = new ChatServiceImpl();
            //agar ye line clienthandler class me hoti toh sab individually chat
            // krte aur common group nahi banta

            //thread pool(scalability ke liye)
            ExecutorService pool = Executors.newFixedThreadPool(10);

            //jab connection accept karega server client ki
            while (true) {
                Socket socket = server.accept();
                System.out.println("Client connected!"); // client connect hogya server se

                //server new client ko ek thread de dega
                ClientHandler client = new ClientHandler(socket, chatService);

                //aur vo thread run karna shuru ho jaygi
                pool.execute(client);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}