import core.RepositoryScanner;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import java.io.IOException;
import java.util.Scanner;

public class Application {

    private static final RepositoryScanner scanner = new RepositoryScanner();

    @Option(name = "-s", aliases = {"--scan"}, handler = StringArrayOptionHandler.class)
    private String[] roots;

    @Option(name = "-u", aliases = {"--update"})
    private boolean update;

    @Option(name = "-r", aliases = {"--reset"})
    private boolean reset;

    public static void main(String[] args) throws IOException {
        new Application().launch(args);
    }

    private void launch(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java -jar recoder.jar -ie EncodingI -oe EncodingO InputName OutputName");
            parser.printUsage(System.err);
            return;
        }

        if (update) {
            scanner.update();
        }
        if (roots != null) {
            System.out.println("This may take a long time...");
            scanner.scan(roots);
        }
        if (reset) {
            System.err.println("Are you sure you want to delete the list of repositories? (y/n)");
            Scanner cliInput = new Scanner(System.in);
            try (cliInput) {
                String line = cliInput.nextLine().toLowerCase();
                if (line.equals("y") || line.equals("yes")) {
                    scanner.reset();
                } else {
                    System.out.println("Reset aborted");
                }
            }
        }
    }

}
