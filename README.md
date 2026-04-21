# 💬 Multi User Chat Server

A real-time multi-user chat application built in Java using socket programming and multithreading.

---

## 🚀 Features
- Multiple clients can connect simultaneously  
- Real-time message broadcasting  
- Private messaging using `/msg`  
- View active users using `/users`  
- Graceful exit using `/exit`  
- Thread-safe client handling using ConcurrentHashMap  

---

## 🛠️ Tech Stack
- Java  
- Socket Programming  
- Multithreading  
- Collections Framework  

---

## 📂 Project Structure
src/
 ├── client/
 │    └── ChatClient.java
 ├── server/
 │    ├── ChatServer.java
 │    └── ClientHandler.java
 └── service/
      ├── ChatService.java
      └── ChatServiceImpl.java

---

## ▶️ How to Run

1. Run `ChatServer.java`  
2. Run multiple instances of `ChatClient.java`  
3. Enter username and start chatting  

---

## 💡 Commands
- `/users` → Show all active users  
- `/msg username message` → Send private message  
- `/exit` → Leave the chat  

---

## 🧠 Architecture
- Client-Server model  
- Thread-per-client handling  
- Shared service layer for managing users  
- ConcurrentHashMap for thread safety  

---

## 🔥 Key Concepts Used
- Multithreading  
- Concurrency  
- Socket Programming  
- OOP (Interfaces, Abstraction)  
- Exception Handling  

---

## 📌 Future Improvements
- GUI (JavaFX / Swing)  
- Message persistence (database)  
- File sharing  
- Group chats  

---

## 🙌 Author
Ritika Jiandani

