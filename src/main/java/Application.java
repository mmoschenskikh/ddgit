import core.Cloner;
import core.RepositoryScanner;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Application {

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Deduplicate());
        cmd.execute(args);

        if (args.length == 0) cmd.usage(System.out);
    }

    @CommandLine.Command(name = "java -jar ddgit.jar", subcommands = {Clone.class, Delete.class, Scan.class, Repack.class})
    static class Deduplicate implements Runnable {
        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "clone", description = "Clone a repository into a new directory.")
    static class Clone implements Runnable {
        @CommandLine.Parameters(index = "0", description = "A link to repository to clone.")
        String link;

        @CommandLine.Option(names = "-p", description = "A path to clone in.")
        String path;

        @CommandLine.Option(names = "-d", description = "Enable deduplication, way of deduplication is selected automatically.")
        boolean deduplicate;

        @CommandLine.Option(names = "-a", description = "Use authorized access to GitHub to increase API rate limit (if cloning from GitHub with deduplication).")
        boolean authorize;

        @CommandLine.Option(names = "--dumb", description = "Enable forced dumb deduplication, use only with '-d'.")
        boolean dumb;

        @Override
        public void run() {
            if (link == null) {
                System.err.println("No link specified.");
            } else {
                Cloner.setAuthorized(authorize);
                try {
                    if (deduplicate) {
                        if (!dumb && link.matches("https://github\\.com/(.+)\\.git"))
                            Cloner.DEDUPLICATE_GITHUB.cloneRepo(link, path);
                        else
                            Cloner.DEDUPLICATE_DUMB.cloneRepo(link, path);
                    } else {
                        Cloner.GIT_DEFAULT.cloneRepo(link, path);
                    }
                } catch (InterruptedException | IOException | IllegalStateException e) {
                    System.err.println("Error: " + e.getMessage());
                    System.exit(-1);
                }
                System.out.println("Repository cloned.");
            }
        }
    }

    @CommandLine.Command(name = "repack", description = "Make repository independent from its source repository.")
    static class Repack implements Runnable {
        @CommandLine.Parameters(index = "0", description = "A repository to repack.")
        String directory;

        @Override
        public void run() {
            try {
                Cloner.repackRepo(directory);
                System.out.println("Success.");
            } catch (IOException | InterruptedException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    @CommandLine.Command(name = "delete", description = "Delete a repository.")
    static class Delete implements Runnable {
        @CommandLine.Parameters(index = "0", description = "A directory to delete.")
        String directory;

        @CommandLine.Option(names = "-f", description = "Force delete repository (without confirmation).")
        boolean force = false;

        @Override
        public void run() {
            try {
                if (force) {
                    Cloner.deleteRepo(directory);
                } else {
                    System.out.println("Are you sure you want to delete the repository at "
                            + new File(directory).getAbsolutePath() + "? (y/n)");
                    try (Scanner input = new Scanner(System.in)) {
                        String line = input.nextLine().toLowerCase();
                        if (line.equals("y") || line.equals("yes")) {
                            Cloner.deleteRepo(directory);
                            System.out.println("Repository deleted.");
                        } else {
                            System.out.println("Deletion cancelled.");
                        }
                    }
                }
            } catch (IOException | IllegalStateException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    @CommandLine.Command(name = "scan", description = "Find local repositories to work with.")
    static class Scan implements Runnable {
        @CommandLine.Parameters(description = "Directories to scan for repositories.")
        String[] paths;

        @Override
        public void run() {
            try {
                if (paths != null && paths.length != 0) {
                    System.out.println("This may take a long time...");
                    final int count = RepositoryScanner.scan(paths);
                    System.out.println(count + " new repositories found.");
                } else {
                    System.err.println("No root directory specified.");
                }
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
