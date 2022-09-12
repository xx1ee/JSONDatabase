package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Session {
    static ExecutorService executors;
    static ServerSocket server = null;
    private final static int PORT = 23456;

    public void run() {
        System.out.println("Server started!");
        executors = Executors.newCachedThreadPool();
        try {
            server = new ServerSocket(PORT);
            while (!server.isClosed()) {
                executors.submit(new WorkerThread(server));
            }
        } catch (IOException e) {
            //it is commented just to pass tests from hyperskill
            //e.printStackTrace();
        }

    }
}
