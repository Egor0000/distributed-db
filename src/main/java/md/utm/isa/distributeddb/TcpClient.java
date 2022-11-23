package md.utm.isa.distributeddb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.io.*;
import java.net.Socket;

@Service
@Slf4j
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
