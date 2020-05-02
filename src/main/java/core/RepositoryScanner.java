package core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class RepositoryScanner {

    //Initial commit hash ->  Repository path
    private Map<String, String> repos;
    public static final String REPOS_FILE = "repos.ddgit";

    /**
     * Gets all the repositories from the file.
     *
     * @param file File where repositories stored as "Path Hash", one per line; the last line of the file must be blank.
     * @return Returns map containing repository paths with their initial commits.
     * @throws FileNotFoundException if there are some problems with REPOS_FILE.
     */
    public static Map<String, String> getFromFile(File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        Map<String, String> repos = new HashMap<>();
        try (scanner) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String hash;
                String name;
                try {
                    String[] repo = line.split(" +");
                    hash = repo[0];
                    name = repo[1];
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalStateException("'" + file.getPath() + "' is not properly formatted.");
                }
                repos.put(hash, name);
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
            Map<String, String> existingRepos = getFromFile(outputFile);
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile, true));
            try (out) {
                for (Map.Entry<String, String> repo : repos.entrySet()) {
                    String hash = repo.getKey();
                    String path = repo.getValue();
                    if (existingRepos.containsKey(hash)) {
                        if (!existingRepos.get(hash).equals(path)) {
                            System.err.println("'" + hash + "'" + " is already in '" + REPOS_FILE + "', but with other path:");
                            System.err.println(REPOS_FILE + ": " + path);
                            System.err.println("Scan result: " + existingRepos.get(hash));
                            System.err.println("Use ddgit reset then ddgit scan to replace old path with current one or edit '" + REPOS_FILE + "' manually.");
                        }
                        continue;
                    }
                    out.write(hash + " " + path + " \n");
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
     * @throws IOException if directories to scan are not specified.
     */
    public void scan(String[] roots) throws IOException {
        if (roots != null && roots.length > 0) {
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
        } else {
            throw new IllegalArgumentException("No root directory was specified.");
        }
    }
}

