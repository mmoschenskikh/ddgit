package core;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;

public class RepositoryFinder implements FileVisitor<Path> {

    /**
     * Stores all the repositories found with their root commit hashes.
     */
    public Map<String, String> repos = new HashMap<>();

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (attrs.isDirectory() && dir.endsWith(".git")) {
            String repoDirectory = dir.getParent().toAbsolutePath().toString();
            String initialCommitHash;

            // Getting initial commit hash
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(new File(dir.toAbsolutePath().toString()))
                    .build();
            try (RevWalk walk = new RevWalk(repository)) {
                walk.sort(RevSort.REVERSE);
                walk.markStart(walk.parseCommit(repository.resolve(Constants.HEAD)));
                RevCommit initial = walk.next();
                initialCommitHash = initial.getName();
            }
            repos.put(repoDirectory, initialCommitHash);

            return SKIP_SIBLINGS; // Expecting user not to have a repository inside other repository
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return CONTINUE;
    }
}
