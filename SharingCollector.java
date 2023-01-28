import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SharingCollector implements Runnable {

    private MulticastSocket MCAST_SOCKET;
    private List<String> notificationList = new ArrayList<>();
    private String myUsername = null;

    public SharingCollector (int recPort, String mcastIp) throws IOException {
        this.MCAST_SOCKET = new MulticastSocket(recPort);
        InetAddress MCAST_ADDRESS = InetAddress.getByName(mcastIp);
        this.MCAST_SOCKET.joinGroup(MCAST_ADDRESS);
    }


    public void run () {
        // Attende ripetutamente notifiche
        while (true) {
            byte[] buff = new byte[1024];
            DatagramPacket notification = new DatagramPacket(buff, buff.length);
            try {
                MCAST_SOCKET.receive(notification);
                // Aggiunge la notifica nella lista
                notificationList.add(new String(notification.getData(), StandardCharsets.UTF_8)); //DA AGGIORNARE POI A MODO
            }
            catch (IOException e) {
                System.out.println("ERROR!");
            }
        }
    }

    public void setMyUsername (String usrnm) {
        this.myUsername = usrnm;
    }

    public void printNotifications (boolean IS_WINDOWS11) {
        // Stampa le notifiche degli altri utenti
        for (String notification : this.notificationList) {
            String[] payload = notification.split("&");
            String username = payload[0];
            if (myUsername!=null && !username.equals(myUsername)) {
                String head = "***** "+username+" *****";
                System.out.println(head);
                for (int i=1; i<payload.length; i++) {
                    printColorfulString(payload[i], head.length(), i, IS_WINDOWS11);
                }
                System.out.print("*".repeat(head.length())+"\n\n");
            }
        }
        this.notificationList.clear();
    }

    void printColorfulString (String tipsWord, float hlen, int index, boolean colors_on) {
        // Funzione di comodo per printNotification()
        int offset_to_center = Math.round((hlen-10)/2);

        String BCKGR_YELLOW = "\u001B[43m", BCKGR_GREEN = "\u001B[42m", BCKGR_BLACK = "\u001B[40m", ANSI_RESET = "\u001B[0m";
        String str = "";

        for (int i=0; i<10; i++) {
            Character ch = tipsWord.charAt(i);
            if (ch == 'x') {
                str = str +(BCKGR_BLACK + " " + ANSI_RESET) ;
            }
            else if (ch == '?') {
                str = str + (BCKGR_YELLOW + " " + ANSI_RESET);
            }
            else {
                str = str + (BCKGR_GREEN + " " + ANSI_RESET);
            }
        }
        if (index > 1) System.out.println(" ".repeat(offset_to_center) + "----------");
        if (colors_on) {
            System.out.println(" ".repeat(offset_to_center) + str);
        } else {
            System.out.println(" ".repeat(offset_to_center) + tipsWord.substring(0,10));
        }
    }
}
