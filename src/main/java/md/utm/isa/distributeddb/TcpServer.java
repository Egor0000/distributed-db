package md.utm.isa.distributeddb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class TcpServer {
    private final String port;
    private final DatabaseService databaseService;

    public TcpServer(@Value("${tcp-server.port}") String port, DatabaseService databaseService) {
        this.port = port;
        this.databaseService = databaseService;
        ExecutorService executorService = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        executorService.execute(() -> {
            try {
                init();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    void init() {

        Socket socket = null;
        try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port))) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                socket = serverSocket.accept();

                InputStream input = socket.getInputStream();

                databaseService.saveToDb((Entity) SerializationUtils.deserialize(input.readAllBytes()));
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
