package core;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static core.RepositoryScanner.REPOS_FILE;

public class Deduplicator {

    /**
     * Clones repository from GitHub using "git clone <forked_repo> --reference <local_parent_repo>" to prevent duplication.
     * If local_parent_repo is not found, clones the repository as usual.
     *
     * @param repository GitHub repository to clone.
     * @param directory  directory name where cloned repository will be stored.
     * @throws IOException if there are some problems with 'tokens' or 'repos' files.
     */
    public static void cloneRepo(String repository, String directory, boolean authorized) throws IOException {
        Pattern github = Pattern.compile("https://github\\.com/(.+)/(.+)\\.git");
        Matcher matcher = github.matcher(repository);

        RepositoryId repositoryId;

        if (matcher.find()) {
            String owner = matcher.group(1);
            String name = matcher.group(2);
            repositoryId = new RepositoryId(owner, name);
        } else {
            throw new IllegalArgumentException("Wrong GitHub link");
        }

        CommitService commitService = new CommitService();

        String token = "";
        if (authorized) {
            token = TokenHolder.getToken();
            commitService.getClient().setOAuth2Token(token);
        }

        Map<String, String> existingRepos = RepositoryScanner.getFromFile(new File(REPOS_FILE));

        String remoteHash = "";

        for (String hash : existingRepos.keySet())
            outerloop:{
                try {
                    commitService.getCommit(repositoryId, hash);
                } catch (RequestException e) {
                    if (!e.getMessage().startsWith("No commit")) {
                        if (authorized) {
                            String currentToken = token;
                            while (true) {
                                token = TokenHolder.getToken();
                                commitService.getClient().setOAuth2Token(token);
                                try {
                                    commitService.getCommit(repositoryId, hash);
                                    remoteHash = hash;
                                    break outerloop;
                                } catch (RequestException exception) {
                                    if (exception.getMessage().startsWith("No commit")) {
                                        break;
                                    } else {
                                        if (currentToken.equals(token)) {
                                            throw new IllegalStateException("API rate limit exceed for all tokens. Try later.");
                                        }
                                    }
                                }
                            }
                        } else {
                            throw new IllegalStateException("API rate limit exceeded. Try later or use authorized access.");
                        }
                    }
                    continue;
                }
                remoteHash = hash;
                break;
            }

        String command = "git clone " + repository;

        if (existingRepos.containsKey(remoteHash)) {
            String parentRepo = existingRepos.get(remoteHash);
            File repoDir = new File(parentRepo);
            if (repoDir.exists() && repoDir.isDirectory() && repoDir.canRead()) {
                System.out.println("Parent repository " + parentRepo + " found for " + repository);
                command += " --reference " + parentRepo.substring(0, parentRepo.length() - 5);
            } else {
                System.err.println(parentRepo + " found but doesn't exist. Remove it from " + REPOS_FILE + ".");
            }
        } else {
            System.err.println("No parent repository found for " + repository);
        }

        if (directory != null && directory.length() > 0) {
            command += " " + directory;
        }

        Process p = Runtime.getRuntime().exec(command);
    }

    /**
     * Deletes Git repository.
     *
     * @param directory directory to delete (which must be Git repository).
     * @throws IOException if there are some problems with the directory or with the files within it.
     */
    public static void deleteRepo(String directory) throws IOException {
        File dir = new File(directory);
        String[] list = dir.list((__, name) -> name.equals(".git"));
        if (dir.isDirectory() && list != null && list.length == 1) {
            Files.walk(dir.toPath()).sorted(Comparator.reverseOrder())
                    .map(Path::toFile).forEach(File::delete);
            System.out.println("Repository deleted");
        } else {
            if (dir.exists()) {
                System.err.println(dir.getAbsolutePath() + " is not a Git repository.");
            } else {
                System.err.println(dir.getAbsolutePath() + " doesn't exist.");
            }

        }
    }
}
