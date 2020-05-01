package core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RepositoryScanner {

    // Repository path -> Initial commit hash
    private Map<String, String> repos;
    private static final String REPOS_FILE = "repos.ddgit";

    /**
     * Scans the filesystem to find Git repositories.
     *
     * @param roots Paths to directories to start scanning from
     * @throws IOException
     */
    public void scan(String[] roots) throws IOException {
        if (roots != null && roots.length > 0) {
            RepositoryFinder rf = new RepositoryFinder();

            for (String root : roots) {
                Files.walkFileTree(Path.of(root), rf);
            }

            this.repos = rf.repos;
            writeToFile();
        } else {
            throw new IllegalArgumentException("No root directory was specified.");
        }
    }

    /**
     * Reads REPOS_FILE to find Git repositories which were changed or deleted.
     */
    public void update() throws IOException {
        File inputFile = new File(REPOS_FILE);
        if (inputFile.exists() && inputFile.isFile() && inputFile.canRead()) {
            repos = getFromFile(inputFile);
            Set<Map.Entry<String, String>> entries = repos.entrySet();
            Iterator<Map.Entry<String, String>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String path = entry.getKey();
                String hash = entry.getValue();
                File currentPath = new File(path);
                if (currentPath.exists() && currentPath.isDirectory() && currentPath.canRead()) {
                    String currentHash = RepositoryFinder.getInitialCommitHash(currentPath.toPath());
                    if (!hash.equals(currentHash)) {
                        entry.setValue(currentHash);
                    }
                } else {
                    iterator.remove();
                }
            }
            reset();
            writeToFile();
        } else {
            throw new IOException("'" + REPOS_FILE + "' is unavailable for some reason.");
        }
    }

    /**
     * Deletes all content from REPOS_FILE.
     *
     * @throws IOException
     */
    public void reset() throws IOException {
        new FileWriter(new File(REPOS_FILE)).close();
    }

    /**
     * Writes all the repositories found with their root commit hashes to REPOS_FILE.
     *
     * @throws IOException
     */
    private void writeToFile() throws IOException {
        File outputFile = new File(REPOS_FILE);
        if (outputFile.exists() && outputFile.canWrite() || outputFile.createNewFile()) {
            Map<String, String> existingRepos = getFromFile(outputFile);
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile, true));
            try (out) {
                for (Map.Entry<String, String> repo : repos.entrySet()) {
                    String path = repo.getKey();
                    String hash = repo.getValue();
                    if (existingRepos.containsKey(path)) {
                        if (!existingRepos.get(path).equals(hash)) {
                            System.err.println("'" + path + "'" + " is already in '" + REPOS_FILE + "', but with other hash:");
                            System.err.println(REPOS_FILE + ": " + hash);
                            System.err.println("Scan result: " + existingRepos.get(path));
                            System.err.println("Use --update to replace old hash with current one or edit '" + REPOS_FILE + "' manually.");
                        }
                        continue;
                    }
                    out.write(path + " " + hash + " \n");
                }
            }
        } else {
            throw new IOException("'" + REPOS_FILE + "' is unavailable for some reason.");
        }
    }

    /**
     * Gets all the repositories from the file.
     *
     * @param file File where repositories stored as "Path Hash", one per line; the last line of the file must be blank.
     * @return Returns map containing repository paths with their initial commits.
     * @throws FileNotFoundException
     */
    private Map<String, String> getFromFile(File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        Map<String, String> repos = new HashMap<>();
        try (scanner) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String name;
                String hash;
                try {
                    String[] repo = line.split(" +");
                    name = repo[0];
                    hash = repo[1];
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalStateException("'" + file.getPath() + "' is not properly formatted.");
                }
                repos.put(name, hash);
            }
        }
        return repos;
    }
}
