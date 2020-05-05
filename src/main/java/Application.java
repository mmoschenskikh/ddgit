import core.Deduplicator;
import core.RepositoryScanner;
import remkop.picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Application {

    private static final RepositoryScanner scanner = new RepositoryScanner();

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Deduplicate());
        cmd.execute(args);

        if (args.length == 0) {
            cmd.usage(System.out);
        }
    }

    @CommandLine.Command(name = "java -jar ddgit.jar", subcommands = {Clone.class, Delete.class, Scan.class, Reset.class})
    static class Deduplicate implements Runnable {
        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "clone", description = "Clone a repository into a new directory.")
    static class Clone implements Runnable {
        @CommandLine.Parameters(index = "0", description = "A link to repository to clone")
        String link;

        @CommandLine.Option(names = "-p", description = "A path to clone in")
        String path;

        @Override
        public void run() {
            if (link == null) {
                System.err.println("No link specified.");
            } else {
                try {
                    Deduplicator.cloneRepo(link, path);
                    System.out.println("Repository cloned.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @CommandLine.Command(name = "delete", description = "Delete a repository")
    static class Delete implements Runnable {
        @CommandLine.Parameters(index = "0", description = "A directory to delete")
        String directory;

        @CommandLine.Option(names = "-f", description = "Force delete repository (without confirmation)")
        boolean force = false;

        @Override
        public void run() {
            if (force) {
                try {
                    Deduplicator.deleteRepo(directory);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Are you sure you want to delete the repository at "
                        + new File(directory).toPath().toAbsolutePath() + "? (y/n)");
                try (Scanner input = new Scanner(System.in)) {
                    String line = input.nextLine().toLowerCase();
                    if (line.equals("y") || line.equals("yes")) {
                        try {
                            Deduplicator.deleteRepo(directory);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Deletion canceled.");
                    }
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
                if (paths != null && paths.length != 0) {
                    System.out.println("This may take a long time...");
                    final int count = scanner.scan(paths);
                    System.out.println(count + " new repositories found.");
                } else {
                    System.err.println("No root directory specified.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @CommandLine.Command(name = "reset", description = "Clear the list of found repositories.")
    static class Reset implements Runnable {
        @Override
        public void run() {
            System.out.println("Are you sure you want to delete the list of repositories? (y/n)");
            try (Scanner input = new Scanner(System.in)) {
                String line = input.nextLine().toLowerCase();
                if (line.equals("y") || line.equals("yes")) {
                    try {
                        scanner.reset();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Reset aborted.");
                }
            }
        }
    }
}
