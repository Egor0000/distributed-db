package md.utm.isa.distributeddb;

import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.io.*;
import java.net.Socket;

@Service
public class TcpClient {
    public void send(String address, int port, Entity body) throws Exception{
        try (Socket socket = new Socket(address, port)){
            OutputStream output = socket.getOutputStream();

            byte[] data = SerializationUtils.serialize(body);

            if (data != null) {
                output.write(data);
            }

            PrintWriter writer = new PrintWriter(output, true);
            writer.println(address);
        }
    }
}
