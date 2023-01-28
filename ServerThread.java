import java.io.*;
import java.net.Socket;
import java.util.*;

public class ServerThread implements Runnable {

    private String WORDS_FILE;
    private Socket socket;
    private Sharer sharer;
    private String clientUsername;
    private String secretWord;
    private int attemptsLeft;
    private boolean isAuthenticated = false;
    private boolean hasAskedForGameInit = false;
    private boolean hasWin = false;
    private boolean sharedRes = false;
    private Database DB;
    //ArrayList in cui vengono inseriti i tentativi fatti codificati in stringhe che compongono una notifica inviabile
    private List<String> attempts_list_for_notification = new ArrayList<>();

    ServerThread (Socket socket, Sharer sharer, Database database, String wordsFile) {
        this.socket = socket;
        this.sharer = sharer;
        this.secretWord = WordleServerMain.getSecretWord();
        this.DB = database;
        this.WORDS_FILE = wordsFile;
    }

    public void run () {
        Scanner in;
        PrintWriter out;

        printl("Client connected");

        try {
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) { printl("Unable to create comunication with client. Please, try again later."); return; }

        while (in.hasNextLine()) {
            String line = in.nextLine();
            String[] content = line.trim().split(";");
            String cmd = content[0];
            if (cmd.equals("REGISTER")) {
                serveRegister(out, content[1], content[2]);
            }
            else if (cmd.equals("LOGIN")) {
                serveLogin(out, content[1], content[2]);
            }
            else if (cmd.equals("LOGOUT")) {
                serveLogout(out);
            }
            else if (cmd.equals("PLAYWORDLE")) {
                startWordle(out);
            }
            else if (cmd.equals("GUESSWORD")) {
                guessWord(out, content[1]);
            }
            else if (cmd.equals("SENDMESTATISTICS")) {
                sendMeStatistics(out);
            }
            else if (cmd.equals("SHARE")) {
                share(out);
            }
            else if (cmd.equals("EXIT")) {
                in.close();
                out.close();
                return;
            }
            else printl("Error Server");
        }
    }

    private void printl(String s) { System.out.println(s); }

    private void serveRegister (PrintWriter out, String username, String password) {

        User user = this.DB.findOne(username);

        if (isAuthenticated) {
            out.println(403);
            return;
        }

        if (user == null) {
            // Utente non presente nel database
            this.DB.insertOne(new User(username, password));
            clientUsername = username;
            out.println(200);
            isAuthenticated = true;
        }
        else {
            // Un utente già registrato non può registrarsi di nuovo!
            out.println(409);
        }
    }

    private void serveLogin (PrintWriter out, String username, String password) {

        User user = this.DB.findOne(username);

        if (isAuthenticated) {
            out.println(403);
            return;
        }

        if (user == null) {
            // Non esiste
            out.println(409);
            return;
        }

       if (password.equals(user.getPassword())) {
            clientUsername = username;
            out.println(200);
            isAuthenticated = true;
       }
       else {
           out.println(401);
       }
    }

    private void serveLogout (PrintWriter out) {
        isAuthenticated = false;
        hasAskedForGameInit = false;
        hasWin = false;
        User user = this.DB.findOne(clientUsername);
        user.setHasPlayed(true);       //"ogni tentativo di indovinare la parola si ritiene concluso se l'utente chiede il logout"
        this.DB.updateOne(user);
        out.println(200);
    }

    private void startWordle (PrintWriter out) {

        if (!isAuthenticated) {
            out.println(403);
            return;
        }

        if (hasAskedForGameInit) {
            out.println(202);
            return;
        }

        User user = this.DB.findOne(clientUsername);

        if (user.getHasPlayed()) {
            out.println(409);
            return;
        }

        //Non ha già giocato e quindi può giocare.
        hasAskedForGameInit = true;
        user.setHasPlayed(true);
        attemptsLeft = 12;
        user.incrementPlayed();
        this.DB.updateOne(user);
        out.println(200);
    }

    private void guessWord (PrintWriter out, String providedWord) {

        if (!hasAskedForGameInit) {
            out.println(400);
            return;
        }

        if (!isAuthenticated) {
            out.println(403);
            return;
        }

        checkSecretWordChanges(); //nel caso la parola cambiasse quando l'utente sta giocando

        if (attemptsLeft == 0) {
            out.println(405);
            return;
        }

        if (hasWin) {
            out.println(406);
            return;
        }

        User user = this.DB.findOne(clientUsername);

        if (providedWord.equals(secretWord)) {
            out.println(200);
            user.incrementWon();
            if (user.getLastResult() == 1 || user.getLastResult() == -1) {
                user.incrementLastStreak();
            } else if (user.getLastResult() == 0) {
                user.resetLastStreak();
            }
            if (user.getLastStreak() >= user.getBestStreak()) {
                user.setBestStreak(user.getLastStreak());
            }
            user.setLastResult(1);
            attemptsLeft--;
            user.guessDistributionAdd(12-attemptsLeft);
            this.DB.updateOne(user);
            hasWin = true;
        }
        else {
            if (isWordPresent(providedWord)) {
                out.println(203);
                String tipsWord = generateTipsWord(providedWord);
                out.println(tipsWord);
                attemptsLeft--;
                if (attemptsLeft == 0) {
                    user.setLastResult(0);
                }
                attempts_list_for_notification.add(tipsWord);
                this.DB.updateOne(user);
            }
            else {
                out.println(202);
            }
        }

    }

    private boolean binarySearch (long l, long r, RandomAccessFile wf, String gw) throws IOException {
        if (l > r) return false;
        long mid = (l + r) / 2 ;
        wf.seek(mid * 12);
        String readWord = wf.readLine();
        if (gw.equals(readWord)) {
            return true;
        }
        if (gw.compareTo(readWord) < 0) {
            return binarySearch(l, mid-1, wf, gw);
        }
        return binarySearch(mid+1, r, wf, gw);
    }

    private boolean isWordPresent (String word) {
        RandomAccessFile wfile;
        long numWords;
        try {
            wfile = new RandomAccessFile(WORDS_FILE, "r");
            numWords = wfile.length()/12;
        } catch (IOException e) {
            printl("Error with words file");
            return false;
        }
        boolean res;
        try {
            res = binarySearch(0,numWords-1, wfile, word);
        } catch (IOException e) {
            printl("Error! during binarysearch on words file");
            return false;
        }

        try {
            wfile.close();
        } catch (IOException e) {
            printl("Unable to close words file!");
            return false;
        }
        return res;
    }

    private String generateTipsWord (String providedWord) {
        HashMap<Character,Integer> charMap = new HashMap<>();
        for (int i=0; i<secretWord.length(); i++) {
            Character chr = secretWord.charAt(i);
            charMap.put(chr, charMap.getOrDefault(chr,0)+1);
        }

        Character[] tipsWord = new Character[10];
        for (int i=0; i<10; i++) {
            // Inizializzazione -> La parola è tutta grigia
            tipsWord[i] = 'x';
        }

        for (int i=0; i<10; i++) {
            // Metto innanzitutto i caratteri che matchano la posizione
            int index = secretWord.indexOf(providedWord.charAt(i));
            if (index == i) {
                tipsWord[i] = '+';
                int val = charMap.get(secretWord.charAt(i));
                charMap.put(secretWord.charAt(i), --val);
            }
        }

        for (int i=0; i<10; i++) {
            Character prChr = providedWord.charAt(i);
            int val = charMap.getOrDefault(prChr, -1);
            if (val > 0) {    // è presente nella parola segreta e c'è bisogno di marcarlo.
                tipsWord[i] = '?';
                charMap.put(prChr, --val);
            }
        }

        String toRet = "";
        for (int i=0; i<10; i++) { toRet = toRet + tipsWord[i]; }
        return toRet;
    }

    private void sendMeStatistics (PrintWriter out) {

        if (!isAuthenticated) {
            out.println(403);
            return;
        }

        User user = this.DB.findOne(clientUsername);

        out.println(200);
        out.println(user.getTotPlayed());
        out.println(user.getTotWon());
        out.println(user.getLastResult());
        out.println(user.getLastStreak());
        out.println(user.getBestStreak());
        for (int val : user.getGuessDistribution()) {
            out.println(val);
        }
        out.println(-1);
    }

    private void share (PrintWriter out) {

        if (!isAuthenticated) {
            out.println(403);
            return;
        }

        checkSecretWordChanges();

        if (sharedRes) {
            out.println(406);   //already shared result for this word()
            return;
        }

        if (!hasWin) {
            out.println(405);
            return;
        }
        sharer.notify(attempts_list_for_notification, clientUsername);
        sharedRes = true;
        out.println(200);
    }

    private void checkSecretWordChanges () {
        String currentSecretWord = WordleServerMain.getSecretWord();
        if (!currentSecretWord.equals(this.secretWord)) {
            attempts_list_for_notification.clear();
            this.secretWord = currentSecretWord;
            attemptsLeft = 12;
            hasWin = false;
            sharedRes = false;
        }
    }
}
