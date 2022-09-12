package client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Args {
    @Parameter(names = "-t")
    String clientCommandRequest = "type";
    @Parameter(names = "-k")
    String clientCellIndex = "key";
    @Parameter(names = "-v")
    String clientValueToStore = "value";

    public static Args parse(String[] args) {

        Args clientArgs = new Args();
        JCommander.newBuilder()
                .addObject(clientArgs)
                .build()
                .parse(args);

        return clientArgs;
    }
}
class Get {
    String type;
    String key;

    public Get(String clientCommandRequest, String clientCellIndex) {
        this.type = clientCommandRequest;
        this.key = clientCellIndex;
    }
}
class Set extends Get{
    String value;

    public Set(String clientCommandRequest, String clientCellIndex, String clientValueToStore) {
        super(clientCommandRequest, clientCellIndex);
        this.value = clientValueToStore;
    }
}
class Delete extends Get{

    public Delete(String clientCommandRequest, String clientCellIndex) {
        super(clientCommandRequest, clientCellIndex);
    }
}
class Input {
    String input;
    public Input(String clientInputFile) {
        this.input = clientInputFile;
    }
}
class Exit{
    String type;

    public Exit(String clientCommandRequest) {
        this.type = clientCommandRequest;
    }
}
public class Main {

    private static final String address = "127.0.0.1";
    private static final int PORT = 23456;
    private static String Json;
    private static JsonObject map;
    final static String filePath = "src/client/data/";
    public static Map<String, String> request = new HashMap<>();
    public static void main(String[] args) throws InterruptedException {
            try {
                //Thread.sleep(1000);
                Socket socket = new Socket(address, PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                System.out.println("Client started!");
                String[] userInput = args;
                if (userInput[0].contains("-in")) {
                    File file = new File(filePath + userInput[1]);
                    try {
                        Json = new String(Files.readAllBytes(Path.of(file.getPath())));
                    } catch (IOException fnf) {
                        fnf.printStackTrace();
                    }
                } else {
                    Args clientArgs = Args.parse(args);
                    switch (clientArgs.clientCommandRequest) {
                        case "get":
                            Get get = new Get(clientArgs.clientCommandRequest, clientArgs.clientCellIndex);
                            Json = new Gson().toJson(get);
                            break;
                        case "set":
                            Set set = new Set(clientArgs.clientCommandRequest, clientArgs.clientCellIndex, clientArgs.clientValueToStore);
                            Json = new Gson().toJson(set);
                            break;
                        case "delete":
                            Delete delete = new Delete(clientArgs.clientCommandRequest, clientArgs.clientCellIndex);
                            Json = new Gson().toJson(delete);
                            break;
                        case "exit":
                            Exit exit = new Exit(clientArgs.clientCommandRequest);
                            Json = new Gson().toJson(exit);
                            break;
                    }
                }
                System.out.print("Sent: ");
                System.out.println(Json);
                output.writeUTF(Json);
                //Gson gson = new Gson();
                //output.writeUTF(gson.toJson(clientArgs));
                String receivedMsg = input.readUTF();
                System.out.println("Received: " + receivedMsg);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

}
