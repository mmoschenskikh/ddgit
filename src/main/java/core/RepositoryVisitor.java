package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;

public class RepositoryVisitor implements FileVisitor<Path> {

    /**
     * Stores all the repositories found with their root commit hashes.
     */
    private final Map<String, Path> repositories = new HashMap<>();

    private static String getInitialCommitHash(Path path) throws IOException {
        String[] command = {"git", "log", "--reverse", "--all", "--pretty=oneline"};
        ProcessBuilder builder = new ProcessBuilder().directory(path.toFile()).command(command);
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && line.length() > 40) return line.substring(0, 40);
        }
        return null;
    }

    public Map<String, Path> getRepositories() {
        return repositories;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (attrs.isDirectory() && dir.endsWith(".git")) {
            String initialCommitHash = getInitialCommitHash(dir);
            if (initialCommitHash != null) {
                repositories.put(initialCommitHash, dir);
            }
            return SKIP_SIBLINGS; // Expecting user not to have a repository inside other repository
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        return CONTINUE;
    }
}
