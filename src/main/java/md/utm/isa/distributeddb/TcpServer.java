package md.utm.isa.distributeddb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

@Service
public class TcpServer {
    private final String port;

    public TcpServer(@Value("${tcp-server.port}") String port) {
        this.port = port;
        init();
    }

    void init() {

        Socket socket = null;
        try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt("4445"))) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                socket = serverSocket.accept();

                InputStream input = socket.getInputStream();

                System.out.println(SerializationUtils.deserialize(input.readAllBytes()));
            }

        } catch (Exception ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            Socket finalSocket = socket;
            Runtime.getRuntime().addShutdownHook(new Thread(){public void run(){
                try {
                    finalSocket.close();
                    System.out.println("The server is shut down!");
                } catch (IOException e) { /* failed */ }
            }});
        }
    }
}
