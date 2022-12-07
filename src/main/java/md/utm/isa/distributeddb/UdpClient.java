package md.utm.isa.distributeddb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;

@Slf4j
@Service
public class UdpClient {
    private DatagramSocket socket;

    private InetAddress address;

    private byte[] buf;

    private String id;

    DatagramSocket clientSocket;

    public UdpClient(@Value("${leader}")String leader, @Value("${data.id}") String id) {
        this.id = id;

        try {
            if (id.equals(leader)) {
                clientSocket = new DatagramSocket();
                new Thread(this::send).start();
            }
        } catch (SocketException ex) {
            // Handle exception
        } catch (IOException ex) {
            // Handle exception
        }

    }

    public void send() {
        while (true) {
            try {
                String dateText = new Date().toString();
                byte[] buffer = new byte[256];
                buffer = dateText.getBytes();

                InetAddress group = InetAddress.getByName("224.0.0.0");
                DatagramPacket packet;
                packet = new DatagramPacket(buffer, buffer.length, group, 8888);
                clientSocket.send(packet);
                packet = new DatagramPacket(buffer, buffer.length);
                clientSocket.receive(packet);
                String received = new String(
                        packet.getData(), 0, packet.getLength());
                if (received != null) {
                    log.info("Add {} to {}", System.currentTimeMillis(), Integer.parseInt(received));
                    ServerHeathcheck.healthCehckTimer.set(Integer.parseInt(received), System.currentTimeMillis());
                    log.info("{}", ServerHeathcheck.healthCehckTimer);
                }
                log.info("Response from {}", received);
            } catch (Exception ex) {
                log.error("Error while sending udp packets", ex);
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                // Handle exception
            }
        }
    }
}
