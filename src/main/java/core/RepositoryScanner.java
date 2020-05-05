package core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class RepositoryScanner {

    public static final String REPOS_FILE = "repos_list";

    private Set<Path> repos;

    /**
     * Gets all the repositories from the file.
     *
     * @param file File where repositories stored as "Hash Path", one per line; the last line of the file must be blank.
     * @return Returns map containing repository paths with their initial commits hashes.
     * @throws FileNotFoundException if there are some problems with REPOS_FILE.
     */
    public static Set<Path> getFromFile(File file) throws FileNotFoundException {
        Set<Path> repos = new HashSet<>();
        if (!file.exists()) {
            return repos;
        }
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                try {
                    repos.add(Paths.get(line));
                } catch (InvalidPathException e) {
                    System.err.println(line + " is wrong path.");
                }
            }
        }
        return repos;
    }

    /**
     * Deletes all content from REPOS_FILE.
     *
     * @throws IOException if there are some problems with REPOS_FILE.
     */
    public void reset() throws IOException {
        new FileWriter(new File(REPOS_FILE)).close();
    }

    /**
     * Writes all the repositories found with their root commit hashes to REPOS_FILE.
     *
     * @throws IOException if there are some problems with REPOS_FILE.
     */
    private void writeToFile() throws IOException {
        File outputFile = new File(REPOS_FILE);
        if (outputFile.exists() && outputFile.canWrite() || outputFile.createNewFile()) {
            Set<Path> existingRepos = getFromFile(outputFile);
            try (BufferedWriter out = new BufferedWriter(new FileWriter(outputFile, true))) {
                for (Path repo : existingRepos) {
                    out.write(repo.toAbsolutePath().toString() + "\n");
                }
            }
        } else {
            throw new IOException("'" + REPOS_FILE + "' is unavailable for some reason.");
        }
    }

    /**
     * Scans the filesystem to find Git repositories.
     *
     * @param roots paths to directories to start scanning from.
     * @return number of repositories found.
     * @throws IOException if directories to scan are not specified.
     */
    public int scan(String[] roots) throws IOException {
        RepositoryFinder rf = new RepositoryFinder();
        for (String root : roots) {
            try {
                Files.walkFileTree(Path.of(root), rf);
            } catch (InvalidPathException e) {
                System.err.println("Invalid path was specified: " + root);
            }
        }
        this.repos = rf.repos;
        writeToFile();
        return repos.size();
    }
}

