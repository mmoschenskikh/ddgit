package core;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Deduplicator {

    private static Map<String, String> repos;

    public static void cloneRepo(String repository) throws IOException {
        Repository repo = Git.lsRemoteRepository().getRepository();
        String command;
        try (RevWalk walk = new RevWalk(repo)) {
            walk.sort(RevSort.REVERSE);
            walk.markStart(walk.parseCommit(repo.resolve(Constants.HEAD)));
            RevCommit initial = walk.next();
            String hash = initial.getName();

            repos = RepositoryScanner.getFromFile(new File(RepositoryScanner.REPOS_FILE));

            if (repos.containsValue(hash)) {
                command = "git clone " + repository + " --reference ";
            }
        }

        command = "git clone " + repository + " --reference " + repository;
        Process p = Runtime.getRuntime().exec(command);

    }

    public static void deleteRepo(String directory) {

    }
}
