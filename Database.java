import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;


public class Database {

    private String DBFILE_NAME;
    private Map<String, User> DB;  // < Username , Utente >

    public Database (String dbfilename) {
        DBFILE_NAME = dbfilename;
        DB = new HashMap<String, User>();
    }

    public boolean initDatabase () {
        // Non richiede sincronizzazione. Chiamata 1 sola volta dal ServerMain
        File dbfile = new File(DBFILE_NAME);
        try (FileReader fr = new FileReader(dbfile.getName())) {

            if (dbfile.length() < 10) {	// se il file è vuoto. 10 valore per coprire alcuni casi limite.
                FileWriter fw = new FileWriter(DBFILE_NAME);
                fw.write("[]");
                fw.close();
            }
            JsonElement fileElement = JsonParser.parseReader(fr);
            JsonArray jsonUserArray = fileElement.getAsJsonArray();
            for (JsonElement userElement : jsonUserArray) {
                JsonObject itemJsonObject = userElement.getAsJsonObject();
                String username = itemJsonObject.get("username").getAsString();
                String password = itemJsonObject.get("password").getAsString();
                boolean hasPlayed = itemJsonObject.get("hasPlayed").getAsBoolean();
                int totPlayed = itemJsonObject.get("totPlayed").getAsInt();
                int totWon = itemJsonObject.get("totWon").getAsInt();
                int lastResult = itemJsonObject.get("lastResult").getAsInt();
                int lastStreak = itemJsonObject.get("lastStreak").getAsInt();
                int bestStreak = itemJsonObject.get("bestStreak").getAsInt();
                JsonArray JsonGuessDistribution = itemJsonObject.get("guessDistribution").getAsJsonArray();
                List<Integer> guessDistribution = new ArrayList<>();
                for (JsonElement guess : JsonGuessDistribution) {
                    guessDistribution.add(guess.getAsInt());
                }
                User user = new User(username, password, hasPlayed, totPlayed, totWon, lastResult, lastStreak, bestStreak, guessDistribution);
                DB.put(username, user);
            }
        }
        catch (FileNotFoundException fnfe) {
            System.out.println("DB file not found. Expected : "+ DBFILE_NAME);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public User findOne (String username) {
        return DB.getOrDefault(username, null);
    }

    public void updateOne (User newUserConfig) {
        synchronized (DB) {
            String username = newUserConfig.getUsername();
            DB.replace(username, newUserConfig);
        }
    }

    public void insertOne (User user) {
        synchronized (DB) {
            DB.put(user.getUsername(), user);
        }
    }

    public void initGame () {
        synchronized (DB) {
            for (var entry : DB.entrySet()) {
                User user = DB.get(entry.getKey());
                user.setHasPlayed(false);
                DB.replace(entry.getKey(), user);
            }
        }
    }

    public void updateDbFile () {
        synchronized (DB) {
            //try-with-resources per chiudere gli autocloseable
            try (FileWriter fw = new FileWriter(DBFILE_NAME); JsonWriter writer = new JsonWriter(fw)) {

                writer.setIndent(" ");
                writer.beginArray();
                for (var entry : DB.entrySet()) {
                    User user = DB.get(entry.getKey());
                    writer.beginObject();
                    writer.name("username").value(user.getUsername());
                    writer.name("password").value(user.getPassword());
                    writer.name("hasPlayed").value(user.getHasPlayed());
                    writer.name("totPlayed").value(user.getTotPlayed());
                    writer.name("totWon").value(user.getTotWon());
                    writer.name("lastResult").value(user.getLastResult());
                    writer.name("lastStreak").value(user.getLastStreak());
                    writer.name("bestStreak").value(user.getBestStreak());
                    writer.name("guessDistribution");
                    writer.beginArray();
                    for (int guessed : user.getGuessDistribution()) {
                        writer.value(guessed);
                    }
                    writer.endArray();
                    writer.endObject();
                }
                writer.endArray();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
