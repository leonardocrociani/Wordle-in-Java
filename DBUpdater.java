public class DBUpdater implements Runnable {
    private Database DB;

    public DBUpdater(Database database) {
        this.DB = database;
    }

    public void run () {
        DB.updateDbFile();
        System.out.println("DB DUMPED");
    }
}
