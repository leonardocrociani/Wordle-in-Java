import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.*;

public class WordleServerMain {

    private static int PORT;
    private static int CLIENT_MCASTPORT;
    private static int WORD_UPDATE_FREQ;
    private static String MCAST_IP;
    private static String SECRET_WORD;
    private static String WORDSFILE_NAME;
    private static int DB_UPDATE_FREQ;
    private static String DBFILE_NAME;
    private static Database DB;

    public static void main(String[] args) throws Exception {

        //inizializzazione variabili da server.properties
        if (!initProperties()) { System.out.println("Errore con inizializzazione variabili da server.properties");System.exit(0); }

        DB = new Database(DBFILE_NAME);

        //inizializzazione database
        if (!DB.initDatabase()) { System.out.println("Errore con inizializzazione database");System.exit(0); }

        //inizializzazione gioco
        if (!initializeGameAndSetWord()) { System.out.println("Errore con inizializzazione gioco");System.exit(0); };

        System.out.println("WORDLE SERVER STARTING! SECRET WORD : "+SECRET_WORD);

        //Cached Thread Pool per servire i client
        ExecutorService service = Executors.newCachedThreadPool();

        //Condivisore risultati
        Sharer sharer = new Sharer(MCAST_IP, CLIENT_MCASTPORT);

        //Updater della parola e dei risultati.
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new WordUpdater(), WORD_UPDATE_FREQ, WORD_UPDATE_FREQ, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(new DBUpdater(DB), DB_UPDATE_FREQ, DB_UPDATE_FREQ, TimeUnit.MINUTES);


        //Quando un client si connette, viene eseguito un nuovo ServerThread
        try (ServerSocket listener = new ServerSocket(PORT)) {
            System.out.println("Server listening on port "+PORT);
            while (true) {
                service.execute(new ServerThread(listener.accept(), sharer, DB, WORDSFILE_NAME));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static String getSecretWord () {
        return SECRET_WORD;
    }

    public static boolean initializeGameAndSetWord () {
        // Decreta casualmente qual è la parola segreta e setta la variabile "hasPlayed" a false per tutti gli utenti.
        long numWords;
        RandomAccessFile wordFile;
        String word;
        Random rand;

        // Random access file, sposto il puntatore con seek () a una parola. Leggo la linea.
        try { wordFile = new RandomAccessFile(Paths.get(WORDSFILE_NAME).toAbsolutePath().toString(), "r"); } catch (FileNotFoundException e) { System.out.println("Error! Words file not found"); return false; }
        try { numWords = wordFile.length()/12; } catch (IOException e) { System.out.println("Error with file"); return false; } //11 -> words length + \n
        rand = new Random();
        long selectedWordPosition = rand.nextLong(numWords);
        try { wordFile.seek(selectedWordPosition*12); } catch (IOException e) { System.out.println("Error seeking file"); return false; }
        try { word = wordFile.readLine(); } catch (IOException e) { System.out.println("Error reading file"); return false; }
        try { wordFile.close(); } catch (IOException e) { e.printStackTrace(); return false; }

        DB.initGame();
        SECRET_WORD = word;
        return true;
    }

    public static boolean initProperties () {
        try (InputStream input = new FileInputStream("server.properties")) {

            Properties prop = new Properties();
            prop.load(input);
            WORD_UPDATE_FREQ = Integer.parseInt(prop.getProperty("wordUpdateFreq"));
            PORT = Integer.parseInt(prop.getProperty("port"));
            CLIENT_MCASTPORT = Integer.parseInt(prop.getProperty("mcastPort"));
            MCAST_IP = prop.getProperty("mcastIp");
            DB_UPDATE_FREQ = Integer.parseInt(prop.getProperty("dbUpdateFreq"));
            DBFILE_NAME = prop.getProperty("dbFile");
            WORDSFILE_NAME = prop.getProperty("wordsFile");

        } catch (IOException ex) {
            System.out.println("Error during properties fetching");
            return false;
        }
        return true;
    }
}
