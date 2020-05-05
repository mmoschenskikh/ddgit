package core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

import static core.RepositoryScanner.REPOS_FILE;
import static core.RepositoryScanner.getFromFile;

public class Deduplicator {

    /**
     * Clones repository from GitHub using "git clone <forked_repo> --reference <local_parent_repo>" to prevent duplication.
     * If local_parent_repo is not found, clones the repository as usual.
     *
     * @param repository GitHub repository to clone.
     * @param directory  directory name where cloned repository will be stored.
     * @throws IOException if there are some problems with 'tokens' or 'repos' files.
     */
    public static void cloneRepo(String repository, String directory) throws IOException {

        StringBuilder command = new StringBuilder("git clone " + repository);

        Set<Path> repos = getFromFile(new File(REPOS_FILE));

        for (Path repo : repos) {
            String repoPath = repo.toAbsolutePath().toString();
            command.append(" --reference ").append(repoPath, 0, repoPath.length() - 5);
        }

        if (directory != null && directory.length() > 0) {
            command.append(" ").append(directory);
        }

        Runtime.getRuntime().exec(command.toString());
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
