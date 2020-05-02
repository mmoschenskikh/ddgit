package core;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Deduplicator {

    private static Map<String, String> repos;

    public static void main(String[] args) throws IOException, GitAPIException, InterruptedException {
        cloneRepo("https://github.com/mmoschenskikh/KotlinAsFirst2019.git");
    }

    public static void cloneRepo(String repository) throws IOException, GitAPIException, InterruptedException {
//        https://github.com/vasilievan/KotlinAsFirst2019.git

        Pattern github = Pattern.compile("https:\\/\\/github\\.com\\/(.+)\\/(.+)\\.git");
        Matcher matcher = github.matcher(repository);

        String owner;
        String name;
        if (matcher.find()) {
            owner = matcher.group(1);
            name = matcher.group(2);
        } else {
            throw new IllegalArgumentException("Wrong GitHub link");
        }

        RepositoryId repo = new RepositoryId(owner, name);
        CommitService service = new CommitService();

        Map<String, String> existingRepos = RepositoryScanner.getFromFile(new File(RepositoryScanner.REPOS_FILE));

        String remoteHash = "";

        for (String hash : existingRepos.keySet()) {
            try {
                RepositoryCommit commit = service.getCommit(repo, hash);
            } catch (RequestException ignored) {
                continue;
            }
            remoteHash = hash;
            break;
        }

        String command = "git clone " + repository;

        if (existingRepos.containsKey(remoteHash)) {
            String parentRepo = existingRepos.get(remoteHash);
            System.out.println("Parent repository " + parentRepo + " found for " + repository);
            command += " --reference " + parentRepo.substring(0, parentRepo.length() - 5);
        } else {
            System.err.println("No parent repository found for " + repository);
        }

        Process p = Runtime.getRuntime().exec(command);
    }

    public static void deleteRepo(String directory) {

    }
}
