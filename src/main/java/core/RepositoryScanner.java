package core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class RepositoryScanner {

    /**
     * The file where all the source repositories are stored with their hashes.
     * The format is following: "Hash Path", one per line, Hash must be 40 characters long, then space, then absolute path.
     * The last line of the file must be blank.
     */
    public static final File REPOS_FILE = new File("source_repositories_list");

    /**
     * The file where URLs are stored with paths to suitable source repositories.
     * The format is following: "URL Path", one per line, URL, then space, then absolute path.
     * The last line of the file must be blank.
     * "WK" stands for "well-known".
     */
    public static final File WK_REPOS_FILE = new File("well_known_repositories");

    private static Map<String, Path> repositories;

    /**
     * Gets all the repositories from the file.
     *
     * @param file File where repositories stored with their features (hashes or URLs).
     * @return map containing repositories paths with their features.
     * @throws FileNotFoundException if there are some problems with file.
     */
    public static Map<String, Path> getFromFile(File file) throws FileNotFoundException {
        Map<String, Path> repositories = new HashMap<>();
        if (!file.exists()) throw new FileNotFoundException("\"" + file.getName() + "\" file is not found");
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String feature; // hash or URL to recognize the right repository
                Path path;
                try {
                    String[] repositoryInfo = line.split(" +");
                    feature = repositoryInfo[0];
                    path = Path.of(repositoryInfo[1]);
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalStateException("\"" + file.getName() + "\" is not properly formatted");
                }
                repositories.put(feature, path);
            }
        }
        return repositories;
    }

    /**
     * Writes (appends) all the repositories found with their features (hashes or URLs) to the file.
     *
     * @param file File where repositories will be stored with their features.
     * @throws IOException if there are some problems with the file.
     */
    private static void writeToFile(File file, Map<String, Path> repositoriesToWrite) throws IOException {
        if (file.exists() && file.canWrite() || file.createNewFile()) {
            Map<String, Path> existingRepositories = getFromFile(file);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                for (Map.Entry<String, Path> entry : repositoriesToWrite.entrySet()) {
                    String feature = entry.getKey(); // hash or URL to recognize the right repository
                    Path path = entry.getValue();
                    if (existingRepositories.containsKey(feature)) {
                        if (!existingRepositories.get(feature).equals(path)) {
                            System.err.println("\"" + feature + "\"" + " is already in \"" + file.getName() + "\", but with other path:");
                            System.err.println(file.getName() + ": " + existingRepositories.get(feature).toString());
                            System.err.println("Scan result: " + path.toString());
                        }
                    } else {
                        writer.write(feature + " " + path.toAbsolutePath().toString() + " \n");
                    }
                }
            }
        } else {
            throw new IOException("\"" + file.getName() + "\" is unavailable for some reason.");
        }
    }

    /**
     * Scans the filesystem to find Git repositories.
     *
     * @param roots paths to directories to start scanning from.
     * @return number of repositories found.
     * @throws IOException if directories to scan are not specified.
     */
    public static int scan(String... roots) throws IOException, InterruptedException {
        repositories = new HashMap<>();
        Thread[] threads = new Thread[roots.length];
        for (int i = 0; i < roots.length; i++) {
            String root = roots[i];
            threads[i] = new Thread(() -> {
                RepositoryVisitor visitor = new RepositoryVisitor();
                visitor.setExcludes(
                        List.of(roots).stream()
                                .map(s -> Path.of(s))
                                .filter(it -> {
                                    try {
                                        return !Files.isSameFile(it, Path.of(root)) && it.startsWith(root);
                                    } catch (IOException ignored) {
                                    }
                                    return false;
                                }).collect(Collectors.toList()));
                try {
                    Files.walkFileTree(Path.of(root), visitor);
                } catch (IOException ignored) {
                }
                repositories.putAll(visitor.getRepositories());
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        writeToFile(REPOS_FILE, repositories);
        return repositories.size();
    }
}

