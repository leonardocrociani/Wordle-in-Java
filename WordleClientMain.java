import com.sun.jdi.IntegerType;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.Scanner;

public class WordleClientMain {

    private static int SERVER_PORT;
    private static int MCAST_PORT;
    private static String HASHALGO;
    private static String SERVER_IP;
    private static String MCAST_IP;
    private static boolean COLORS;

    private static boolean LEGEND_PRINTED = false;

    private static SharingCollector sharingCollector;
    private static Thread sharingCollectorThread;
    private static Socket socket;
    private static MessageDigest messageDigest;

    private static Scanner in;  // Scanner di dati dalla socket
    private static PrintWriter out; // PrintWriter di dati sulla socket

    public static void main(String[] args) {
        if (!initProperties()) return;
        try {
            messageDigest = MessageDigest.getInstance(HASHALGO);
            sharingCollector = new SharingCollector(MCAST_PORT, MCAST_IP);
            sharingCollectorThread = new Thread(sharingCollector);
            sharingCollectorThread.start();
        } catch (Exception e) { printl("Can't find Hashing Algorithm specified."); return; }

        Scanner scanner = new Scanner(System.in);
        printl("WORDLE STARTING! Run 'help()' for istructions");

        while (true) {
            System.out.print("wordle > ");
            String cmd = scanner.nextLine();
            if (cmd.equals("exit()")) {
                out.println("EXIT");
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                printl("Bye!");
                System.exit(0);
            }
            evaluate(cmd);
        }
    }

    private static void printl(String s) { System.out.println(s);}

    private static String[] getCredentials(String cmd, int nCred) throws Exception {
        // Prende le credenziali da un comando che le prevede.
        String[] credentials = new String[nCred];
        try {
            if (!cmd.contains("(") || !cmd.contains(")")) { throw new Exception(); }

            String content = cmd.substring(cmd.indexOf("(")+1, cmd.indexOf(")")).trim();
            if (content.length() < 3 && nCred >= 2) { throw new Exception(); }
            if (content.length() < 1 && nCred == 1) { throw new Exception(); }

            for (int i=0; i<nCred; i++) {
                credentials[i] = content.split(",")[i].trim();
            }

        } catch (Exception e) {
            throw new Exception();
        }
        return credentials;
    }

    private static void evaluate (String cmd) {
        // Valutazione dei comandi
        if (cmd.contains(";")) {
            printl("Error: Don't use character ';'");
        }
        else if (cmd.contains("register")) {
            String[] UP;
            try { UP = getCredentials(cmd, 2); } catch (Exception e) { printl("Error. Retry with different parameters"); return; }
            register(UP[0], UP[1]);
        }
        else if (cmd.contains("login")) {
            String[] UP;
            try { UP = getCredentials(cmd, 2); } catch (Exception e) { printl("Error. Retry with different parameters"); return; }
            login(UP[0], UP[1]);
        }
        else if (cmd.contains("logout")) {
            try {
                if (!cmd.contains("(") || !cmd.contains(")")) { throw new Exception(); }
                //String content = cmd.substring(cmd.indexOf("(")+1, cmd.indexOf(")")).trim();
                //il content potrebbe essere usato per quanto veniva richiesto dalle specifiche. D'altra parte, fornire l'username non è necessario per un-loggare l'utente
                //if (content.length() != 0) { throw new Exception(); }
            } catch (Exception e) {
                printl("Invalid command");
                return;
            }
            logout();
        }
        else if (cmd.equals("playWordle()")) {
            playWordle();
        }
        else if (cmd.equals("sendWord()")) {
            sendWord();
        }
        else if (cmd.equals("sendMeStatistics()")) {
            sendMeStatistics();
        }
        else if (cmd.equals("share()")) {
            share();
        }
        else if (cmd.equals("showMeSharing()")) {
            sharingCollector.printNotifications(COLORS);
        }
        else if (cmd.equals("help()")) {
            System.out.print("Commands list:\n" +
                    "register(username,password)\n" +
                    "login(username,password)\n" +
                    "logout() - not necessary to provide username\n" +
                    "playWordle() - to initialize the game\n" +
                    "sendWord() - runnable only if you've already asked for initialization\n" +
                    "sendMeStatistics()  - to receive your games statistics\n" +
                    "share() - runnable only if you've won\n" +
                    "showMeSharing() - runnable only if authenticated\n" +
                    "help()\n" +
                    "exit()\n"
            );

        }
        else {
            printl("Command not found. Run 'help()' for the list of available commands.");
        }
    }

    public static boolean initProperties () {
        // Ottiene le proprietà dal file client.properties
        try (InputStream input = new FileInputStream("client.properties")) {

            Properties prop = new Properties();
            prop.load(input);
            SERVER_PORT = Integer.parseInt(prop.getProperty("serverPort"));
            SERVER_IP = prop.getProperty("serverIp");
            HASHALGO = prop.getProperty("hashAlgo");
            MCAST_IP = prop.getProperty("mcastIp");
            MCAST_PORT = Integer.parseInt(prop.getProperty("mcastPort"));
            if (Integer.parseInt(prop.getProperty("colors")) == 1) {
                COLORS = true;
            } else  COLORS = false;

        } catch (IOException ex) {
            System.out.println("Error during properties fetching");
            return true;
        }
        return true;
    }

    private static void connect () {
        // Si connette al server e inizializza lo Scannner e il PrintWriter per dialogare con il server
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) { System.out.print("Unable to connect. Try later..."); System.exit(0);}
    }

    private static void register (String username, String password) {

        if (socket == null) connect();

        //Hashing Password
        StringBuilder sb = new StringBuilder();
        byte[] bytes = messageDigest.digest(password.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        String hashedPsw = sb.toString();

        out.println("REGISTER;"+username+";"+hashedPsw); //hash

        int retCode = 0;
        try {retCode = in.nextInt();} catch (Exception e) { ; }

        if (retCode == 200) { printl("Successfully registered. Now you can play WORDLE!"); sharingCollector.setMyUsername(username); }  //ti logga anche
        else if (retCode == 409) { printl("User already exist in Database!"); }
        else if (retCode == 403) { printl("Already authenticated!");}
        else { printl("Something went wrong. Please, try again later."); }
    }

    private static void login (String username, String password) {

        if (socket == null) connect();

        //Hashing Password
        StringBuilder sb = new StringBuilder();
        byte[] bytes = messageDigest.digest(password.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        String hashedPsw = sb.toString();

        out.println("LOGIN;"+username+";"+hashedPsw);

        int retCode = 0;
        try {retCode = in.nextInt();} catch (Exception e) { ; }
        if (retCode == 200) { printl("Ok, logged. Now you can play WORDLE!"); sharingCollector.setMyUsername(username); }
        else if (retCode == 401) printl("Wrong credentials!");
        else if (retCode == 403) printl("Already authenticated");
        else if (retCode == 409) printl("User not found. Please, register.");
        else printl("Something went wrong. Please, try again later. CODE " + retCode);

    }

    private static void logout () {

        if (socket == null) { printl("Autheticate first."); return; }

        out.println("LOGOUT");

        int retCode = 0;
        try {retCode = in.nextInt();} catch (Exception e) { ; }
        if (retCode == 200) printl("Successful un-logged!");
        else printl("Something went wrong. Please, try again later.");
    }

    private static void playWordle () {

        if (socket == null) { printl("Autheticate first."); return; }

        out.println("PLAYWORDLE");

        int retCode = 0;
        try {retCode = in.nextInt();} catch (Exception e) { ; }
        if (retCode == 200) { printl("Ok, now you can send words!"); } //ti logga anche
        else if (retCode == 202) printl("Already asked for initialization. User 'sendWord()' to send words.");
        else if (retCode == 403) printl("Please, log in before playing");
        else if (retCode == 409) printl("You have already played for today. Wait a bit for the next word!");
        else printl("Something went wrong. Please, try again later.");
    }

    private static void sendWord () {

        if (socket == null) { printl("Autheticate first."); return; }

        Scanner wordGetter = new Scanner(System.in);
        String BCKGR_YELLOW = "\u001B[43m", BCKGR_GREEN = "\u001B[42m", ANSI_RESET = "\u001B[0m";

        System.out.print("guessed word > ");
        String gw = wordGetter.nextLine();

        out.println("GUESSWORD;"+gw);

        int retCode = 0;
        try {retCode = in.nextInt();} catch (Exception e) { printl("Unable to get return code"); }
        if (retCode == 200) printl("You've Won! The word is correct!");
        else if (retCode == 202) printl("The word you provided is not part of the database");
        else if (retCode == 203) {
            String tipsWord = in.next();

            if (!LEGEND_PRINTED) {
                printLegend(COLORS);
                LEGEND_PRINTED = true;
            }

            if (COLORS) {
                String colorfulTW = "";
                for (int i=0; i<tipsWord.length(); i++) {
                    Character ch = tipsWord.charAt(i);
                    if (ch == 'x') {
                        colorfulTW = colorfulTW + gw.charAt(i);
                    }
                    else if (ch == '?') {
                        colorfulTW = colorfulTW + (BCKGR_YELLOW + gw.charAt(i) + ANSI_RESET);
                    } else {
                        colorfulTW = colorfulTW + (BCKGR_GREEN + gw.charAt(i) + ANSI_RESET);
                    }
                }
                printl("The word is incorrect. Here is your suggest: "+colorfulTW);
            }

            else {
                printl("The word is incorrect. Here is your suggest: "+tipsWord);
            }
        }
        else if (retCode == 400) printl("Error! First you have to initialize the game with 'playWordle()'");
        else if (retCode == 403) printl("You have to log-in before playing");
        else if (retCode == 405) printl("You have 0 attempts left. Sorry. Wait a bit for the next word!");
        else if (retCode == 406) printl("Hey! You have already Won!");
        else if (retCode == 409) printl("You have already played for today.");
        else printl("Something went wrong. Please, try again later.");
    }

    private static void printLegend(boolean isWindows11) {
        if (isWindows11) {
            String BCKGR_YELLOW = "\u001B[43m", BCKGR_GREEN = "\u001B[42m", ANSI_RESET = "\u001B[0m";
            System.out.print("LEGEND. Each letter has one of the following color:\n" +
                    " - No color : if the letter is not contained in the secret word.\n"+
                    " - "+(BCKGR_YELLOW + "Yellow" + ANSI_RESET)+ ": if the letter is contained but has a different position\n" +
                    " - "+(BCKGR_GREEN + "Green" + ANSI_RESET)+": if the letter is contained and is in the correct position\n");
        }
        else {
            System.out.print("LEGEND. Each letter has one of the following symbol:\n" +
                    " - 'x' : if the letter is not contained in the secret word.\n"+
                    " - '?' : if the letter is contained but has a different position\n" +
                    " - '+' : if the letter is contained and is in the correct position\n");
        }
    }

    private static void sendMeStatistics () {

        if (socket == null) { printl("Autheticate first."); return; }

        out.println("SENDMESTATISTICS");

        int retCode = 0;
        try {retCode = in.nextInt();} catch (Exception e) { ; }
        if (retCode != 200) { printl("Something went wrong. Please, try again later."); return; }

        printl("Games played : "+in.nextInt());
        printl("Games won : "+in.nextInt());
        int lastRes = in.nextInt();
        if (lastRes == -1) printl("Last result : None");
        else printl("Last result : "+(lastRes==1?"Win":"Lost"));
        printl("Last streak length : "+in.nextInt());
        printl("Best streak length : "+in.nextInt());
        printl("Attempts distribution : ");
        while (in.hasNextLine()) {
            int res = in.nextInt();
            if (res == -1) break;
            System.out.println(res);
        }
    }

    private static void share() {

        if (socket == null) { printl("Autheticate first."); return; }

        out.println("SHARE");

        int retCode = 0;
        try {retCode = in.nextInt();} catch (Exception e) { ; }
        if (retCode == 200) printl("Ok, game shared.");
        else if (retCode == 403) printl("Please, authenticate first!");
        else if (retCode == 405) printl("You can't share a game that you haven't won!");
        else if (retCode == 406) printl("You have already shared you results!");
        else printl("Something went wrong. Please, try again later.");
    }
}
