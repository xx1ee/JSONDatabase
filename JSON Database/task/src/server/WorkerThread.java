package server;

import com.google.gson.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class WorkerThread implements Runnable {
    public static JsonObject data;
    public static String Json = "";
    private static JsonObject map;
    final static String filePath = "src/server/data/db.json";
    private static ReadWriteLock lock = new ReentrantReadWriteLock();
    private static Lock readLock = lock.readLock();
    private static Lock writeLock = lock.writeLock();
    private Socket socket;
    private ServerSocket serverSocket;
    public WorkerThread(ServerSocket server) throws IOException {
        serverSocket = server;
        socket = server.accept();
    }
    static class Get {
        String response;
        JsonElement value;

        public Get(JsonElement clientCellIndex) {
            this.response = "OK";
            this.value = clientCellIndex;
        }
    }

    static class Set {
        String response;

        public Set() {
            this.response = "OK";
        }
    }

    static class Exit {
        String response;

        public Exit() {
            this.response = "0";
        }
    }

    static class Error {
        String response;
        String reason;

        public Error() {
            this.response = "ERROR";
            this.reason = "No such key";
        }
    }

    public static String execute(JsonObject map) {
        switch (String.valueOf(map.get("type")).replaceAll("\"", "")) {
            case "get":
                readLock.lock();
                if (map.get("key").isJsonArray()) {
                    JsonArray key_array = map.get("key").getAsJsonArray();
                    JsonObject json_data = data;
                    for (int i = 0; i < key_array.size();i++) {
                        //System.out.println(json_data);
                        //System.out.println(key_array.get(i));
                        if (json_data.get(String.valueOf(key_array.get(i)).replaceAll("\"", "")) == null) {
                            Error error = new Error();
                            Json = new Gson().toJson(error);
                            break;
                        } else {
                            if (i == key_array.size() - 1) {
                                Get get = new Get(json_data.get(String.valueOf(key_array.get(i)).replaceAll("\"", "")));
                                Json = new Gson().toJson(get);
                            } else {
                                json_data =  json_data.getAsJsonObject(String.valueOf(key_array.get(i)).replaceAll("\"", ""));
                            }
                        }
                    }
                } else {
                    if (data.get(String.valueOf(map.get("key")).replaceAll("\"", "")) == null) {
                        Error error = new Error();
                        Json = new Gson().toJson(error);
                    } else {
                        Get get = new Get(data.get(String.valueOf(map.get("key")).replaceAll("\"", "")));
                        Json = new Gson().toJson(get);
                    }
                }
                readLock.unlock();
                break;
            case "set":
                writeLock.lock();
                if (map.get("key").isJsonArray()) {
                    JsonArray key_array = map.get("key").getAsJsonArray();
                    JsonObject json_data = data;
                    for (int i = 0; i < key_array.size(); i++) {
                        if (json_data.get(String.valueOf(key_array.get(i)).replaceAll("\"", "")) == null) {
                            Error error = new Error();
                            Json = new Gson().toJson(error);
                            break;
                        } else {
                            if (i == key_array.size() - 1) {
                                json_data.add(String.valueOf(key_array.get(i)).replaceAll("\"", ""), (map.get("value")));
                                Set set = new Set();
                                Json = new Gson().toJson(set);
                            } else {
                                json_data =  json_data.getAsJsonObject(String.valueOf(key_array.get(i)).replaceAll("\"", ""));
                            }
                        }
                    }
                } else {
                    data.add(String.valueOf(map.get("key")).replaceAll("\"", ""), (map.get("value")));
                    Set set = new Set();
                    Json = new Gson().toJson(set);;
                }
                writeLock.unlock();
                break;
            case "delete":
                writeLock.lock();
                if (map.isJsonArray()) {
                    JsonArray path = map.getAsJsonArray();
                    JsonObject tempDb = data;
                    for (int i = 0; i < path.size(); i++) {
                        if (tempDb.has(path.get(i).getAsString())) {
                            if (i == path.size() - 1) {
                                tempDb.remove(path.get(i).getAsString());
                                Set set = new Set();
                                Json = new Gson().toJson(set);
                                return Json;
                            } else {
                                tempDb = tempDb.get(path.get(i).getAsString()).getAsJsonObject();
                            }
                        } else {
                            Error error = new Error();
                            Json = new Gson().toJson(error);
                            return Json;
                        }
                    }
                } else {
                    if (data.has(map.getAsString())) {
                        data.remove(map.getAsString());
                        Set set = new Set();
                        Json = new Gson().toJson(set);
                        return Json;
                    }
                }
                writeLock.unlock();
                //Error error1 = new Error();
                //Json = new Gson().toJson(error1);
                break;
            case "exit":
                Exit exit = new Exit();
                Json = new Gson().toJson(exit);
                break;
        }
        return Json;
    }


    public static JsonObject FromFile()
    {
        File file = new File(filePath);
        try {
            String content = new String(Files.readAllBytes(Path.of(file.getPath())));
            data = new Gson().fromJson(content, JsonObject.class);
            if (data == null) {
                data = new JsonObject();
            }
        } catch (IOException fnf) {
            fnf.printStackTrace();
        }
        return data;
    }
    public static void ToFile() {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            if (data != null) {
                fileWriter.write(new Gson().toJson(data));
            }
        } catch (IOException fnf) {
            fnf.printStackTrace();
        }
    }
    @Override
    public void run() {
        System.out.println("Server started!");

            try(DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output  = new DataOutputStream(socket.getOutputStream())){
                writeLock.lock();
                data = FromFile();
                writeLock.unlock();
                String messageReceivedJson = input.readUTF();
                map = JsonParser.parseString(messageReceivedJson).getAsJsonObject();
                String messageToSend = execute(map);
                //System.out.println(messageToSend);
                if (messageToSend.contains("0")) {
                    messageToSend.replace("0", "OK");
                    output.writeUTF(messageToSend);
                    System.out.println("Received: " + messageToSend);
                    Session.executors.shutdown();
                    serverSocket.close();
                }
                output.writeUTF(messageToSend);
                writeLock.lock();
                //System.out.println(data);
                ToFile();
                writeLock.unlock();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
