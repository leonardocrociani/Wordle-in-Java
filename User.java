import java.util.ArrayList;
import java.util.List;

public class User {

    private String username, password;
    private boolean hasPlayed;
    private int totPlayed, totWon, lastResult, lastStreak, bestStreak;   //lastResult : won or lost.
    private List<Integer> guessDistribution;

    User (String username, String password) {
        //per l'inserimento la prima volta.
        this.username = username;
        this.password = password;
        this.totPlayed  = 0;
        this.totWon = 0;
        this.lastResult = -1;   //none
        this.lastStreak = 0;
        this.bestStreak = 0;
        this.guessDistribution = new ArrayList<>();
        this.hasPlayed = false;
    }

    User (String username, String password, boolean hasPlayed, int totPlayed, int totWon, int lastResult, int lastBestWon, int absoluteBestWon, List<Integer> guessDistribution) {
        // Per la deserializzazione dal file json
        this.username = username;
        this.password = password;
        this.totPlayed  = totPlayed;
        this.totWon = totWon;
        this.lastResult = lastResult;   
        this.lastStreak = lastBestWon;
        this.bestStreak = absoluteBestWon;
        this.guessDistribution = guessDistribution;
        this.hasPlayed = hasPlayed;
    }

    public String getPassword() { return this.password; }
    public String getUsername() {
        return this.username;
    }
    public boolean getHasPlayed () { return this.hasPlayed; }
    public void setHasPlayed (boolean val) { this.hasPlayed = val; }
    public int getTotPlayed() {
        return totPlayed;
    }
    public int getTotWon() { return totWon; }
    public int getLastResult() { return lastResult; }
    public int getLastStreak() {
        return lastStreak;
    }
    public int getBestStreak() {
        return bestStreak;
    }
    public List<Integer> getGuessDistribution() {
        return guessDistribution;
    }
    public void incrementPlayed () { this.totPlayed++; }
    public void incrementWon () { this.totWon++; }
    public void setLastResult (int val) { this.lastResult = val; }
    public void incrementLastStreak () { this.lastStreak++; }
    public void setBestStreak (int lastBestWon) { this.bestStreak = lastBestWon; }
    public void resetLastStreak () { this.lastStreak = 1; }
    public void guessDistributionAdd (int r) {
        this.guessDistribution.add(r);
    }

}