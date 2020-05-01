import core.RepositoryScanner;

import java.io.IOException;

public class Deduplicator {

    private static final RepositoryScanner scanner = new RepositoryScanner();

    public static void main(String[] args) throws IOException {
        System.out.println("This may take a long time...");
        scanner.update();
    }

}
