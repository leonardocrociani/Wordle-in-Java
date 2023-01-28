public class WordUpdater implements Runnable {
    public void run () {
        WordleServerMain.initializeGameAndSetWord();
        System.out.println("WORD UPDATED! NEW SECRET WORD : "+ WordleServerMain.getSecretWord());
    }
}
