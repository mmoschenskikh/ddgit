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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;

public class RepositoryScanner {

    // Path -> Initial commit hash
    private Map<String, String> repos;

    public void scan(String[] roots) throws IOException {
        if (roots != null && roots.length > 0) {
            repos = new HashMap<>();

            for (String root : roots) {
                Path rootPath = Path.of(root);
                Files.walkFileTree(rootPath, new RepositoryFinder());

            }

        } else {
            throw new IllegalArgumentException("No root directory was specified");
        }
    }

    public void update() {

    }

}

class RepositoryFinder implements FileVisitor<Path> {

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (attrs.isDirectory() && dir.endsWith(".git")) {
            String repoDir = dir.getParent().toAbsolutePath().toString();
            String initialCommitHash;

            Repository repository = new FileRepositoryBuilder().setGitDir(new File(dir.toAbsolutePath().toString())).build();
            try (RevWalk walk = new RevWalk(repository)) {
                walk.sort(RevSort.REVERSE);
                walk.markStart(walk.parseCommit(repository.resolve(Constants.HEAD)));
                RevCommit initial = walk.next();

                initialCommitHash = initial.getName();
            }

            System.out.println("FOUND: " + repoDir + " : " + initialCommitHash);
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
