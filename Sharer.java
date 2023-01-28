import java.net.*;
import java.util.List;

public class Sharer {

    private InetAddress MCAST_ADDRESS;
    private DatagramSocket SOCKET;
    private int CLIENTPORT;

    public Sharer (String mcastip, int clientPort) throws SocketException, UnknownHostException {
        this.CLIENTPORT = clientPort;
        this.MCAST_ADDRESS = InetAddress.getByName(mcastip);
        this.SOCKET = new DatagramSocket(0);
    }

    public void notify (List<String> attempts_list, String username) {
        // Manda in multicast le notifiche
        String msg = username;

        for (String tipsWord : attempts_list) {
            msg = msg + ("&"+tipsWord);
        }
        msg = msg + "&++++++++++";

        try {
            this.SOCKET.send(new DatagramPacket(msg.getBytes(), msg.length(), MCAST_ADDRESS, CLIENTPORT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
