import core.Cloner;
import core.RepositoryScanner;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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

        @CommandLine.Option(names = {"-p", "--path"}, description = "A path to clone in.")
        String path;

        @CommandLine.Option(names = {"-d", "--deduplicate"}, description = "Enable deduplication, way of deduplication is selected automatically.")
        boolean deduplicate;

        @CommandLine.Option(names = {"-a", "--authorize"}, description = "Use authorized access to GitHub to increase API rate limit (if cloning from GitHub with deduplication).")
        boolean authorize;

        @CommandLine.Option(names = {"--dumb"}, description = "Enable forced dumb deduplication, use only with '-d'.")
        boolean dumb;

        @CommandLine.Option(names = {"-k", "--wk", "--well-known"}, description = "Forcibly look for source repository in well-known repositories list (should be used for frequently cloned repositories), use only with '-d'.")
        boolean wellKnown;

        @CommandLine.Option(names = {"-b", "--bare"}, description = "Make bare clone (no checkout, only .git directory is present).")
        boolean bare;

        @Override
        public void run() {
            if (link == null) {
                System.err.println("No link specified.");
            } else {
                Cloner.setAuthorized(authorize);
                Cloner.setBareClone(bare);
                try {
                    if (deduplicate) {
                        Map<String, Path> wkRepos = new HashMap<>();
                        // Some magic check
                        if (Boolean.logicalOr(
                                Boolean.logicalAnd(authorize, dumb),
                                Boolean.logicalAnd(wellKnown, Boolean.logicalOr(authorize, dumb))
                        )) throw new IllegalArgumentException("Wrong options combination.");

                        try {
                            wkRepos = RepositoryScanner.getFromFile(RepositoryScanner.WK_REPOS_FILE);
                        } catch (FileNotFoundException e) {
                            if (wellKnown)
                                throw new FileNotFoundException(e.getMessage());
                        }
                        if (wellKnown || wkRepos.containsKey(link))
                            Cloner.DEDUPLICATE_WELL_KNOWN.cloneRepo(link, path);
                        else if (!dumb && link.matches("https://github\\.com/(.+)\\.git"))
                            Cloner.DEDUPLICATE_GITHUB.cloneRepo(link, path);
                        else
                            Cloner.DEDUPLICATE_DUMB.cloneRepo(link, path);
                    } else {
                        Cloner.GIT_DEFAULT.cloneRepo(link, path);
                    }
                    System.out.println("Repository cloned.");
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    System.exit(-1);
                }
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
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    @CommandLine.Command(name = "delete", description = "Delete a repository.")
    static class Delete implements Runnable {
        @CommandLine.Parameters(index = "0", description = "A directory to delete.")
        String directory;

        @CommandLine.Option(names = {"-f", "--force"}, description = "Force delete repository (without confirmation).")
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
            } catch (Exception e) {
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
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
