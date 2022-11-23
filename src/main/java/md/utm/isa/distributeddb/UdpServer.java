package md.utm.isa.distributeddb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.*;

@Service
@Slf4j
public class UdpServer {
    private final String udpServerPort;

    private final String id;

    public UdpServer(@Value("${udp-server.port}") String udpServerPort, @Value("${data.id}") String id) {
        this.udpServerPort = udpServerPort;
        this.id = id;

        try {
            new Server().start();
        } catch (Exception ex) {

        }
    }

    private class Server extends Thread {
        private MulticastSocket socket;
        private boolean running;
        private byte[] buf = new byte[256];

        public Server() {
            try {
                socket = new MulticastSocket(8888);
                InetAddress group =
                        InetAddress.getByName("224.0.0.0");
                socket.joinGroup(new InetSocketAddress(group, 8888), NetworkInterface.getByName("docker0"));
                log.info("Started udp server on port {}", udpServerPort);
            } catch (Exception ex) {
                log.error("Failed to start udp server on port {}", udpServerPort, ex);
            }
        }

        public void run() {
            running = true;
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();
                    packet = new DatagramPacket(buf, buf.length, address, port);
                    String received  = new String(packet.getData(), 0, packet.getLength());

                    log.info("Received {}", received);
                    if (received.equals("end")) {
                        running = false;
                        continue;
                    }
                    String response = String.format("Server %s is alive", id);
                    packet.setData(id.getBytes());
                    socket.send(packet);
                } catch (Exception ex) {
                    log.error("Error while reading udp socket ", ex);
                }

            }
            socket.close();
        }
    }
}
