import core.Deduplicator;
import core.RepositoryScanner;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Scanner;

public class Application {

    private static final RepositoryScanner scanner = new RepositoryScanner();

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Deduplicate());
        cmd.setExecutionStrategy(new CommandLine.RunAll()); // default is RunLast
        cmd.execute(args);

        if (args.length == 0) {
            cmd.usage(System.out);
        }
    }

    @CommandLine.Command(name = "ddgit", subcommands = {Clone.class, Scan.class, Update.class, Reset.class})
    static class Deduplicate implements Runnable {
        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "clone", description = "Clone a repository into a new directory.")
    static class Clone implements Runnable {
        @CommandLine.Parameters(index = "0", description = "A link to repository to clone")
        String link;

        @Override
        public void run() {
            if (link == null) {
                System.err.println("No link specified.");
            } else {
                try {
                    Deduplicator.cloneRepo(link);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @CommandLine.Command(name = "scan", description = "Find local repositories to work with.")
    static class Scan implements Runnable {
        @CommandLine.Parameters(description = "Directories to scan for repositories")
        String[] paths;

        @Override
        public void run() {
            try {
                System.out.println("This may take a long time...");
                scanner.scan(paths);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @CommandLine.Command(name = "update", description = "Update the list of repositories found by scan.")
    static class Update implements Runnable {
        @Override
        public void run() {
            try {
                scanner.update();
                System.out.println("Repository list updated.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @CommandLine.Command(name = "reset", description = "Clear the list of found repositories.")
    static class Reset implements Runnable {
        @Override
        public void run() {
            System.err.println("Are you sure you want to delete the list of repositories? (y/n)");
            Scanner cliInput = new Scanner(System.in);
            try (cliInput) {
                String line = cliInput.nextLine().toLowerCase();
                if (line.equals("y") || line.equals("yes")) {
                    try {
                        scanner.reset();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Reset aborted");
                }
            }
        }
    }


}
