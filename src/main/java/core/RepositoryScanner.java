package core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class RepositoryScanner {

    // Repository path -> Initial commit hash
    private Map<String, String> repos;

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
            throw new IllegalArgumentException("No root directory was specified");
        }
    }

    /**
     * Reads repos.ddgit file to find Git repositories which were changed or deleted.
     */
    public void update() throws IOException {
        File inputFile = new File("repos.ddgit");
        if (inputFile.exists() && inputFile.isFile() && inputFile.canRead()) {
            BufferedReader in = new BufferedReader(new FileReader(inputFile));
            try (in) {
                String line = in.readLine();
                while (line != null) {
                    String[] repo = line.split(" ");
                    //TODO
                    line = in.readLine();
                }
            }
        } else {
            throw new IOException("\"repos.ddgit\" is unavailable for some reason");
        }
    }

    /**
     * Writes all the repositories found with their root commit hashes to repos.ddgit file.
     *
     * @throws IOException
     */
    private void writeToFile() throws IOException {
        File outputFile = new File("repos.ddgit");
        if (outputFile.exists() && outputFile.canWrite() || outputFile.createNewFile()) {
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
            try (out) {
                for (Map.Entry<String, String> repo : repos.entrySet()) {
                    out.write(repo.getKey() + " " + repo.getValue() + " \n");
                }
            }
        } else {
            throw new IOException("\"repos.ddgit\" is unavailable for some reason");
        }
    }

}

