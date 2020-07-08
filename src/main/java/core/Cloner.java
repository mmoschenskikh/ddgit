package core;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static core.RepositoryScanner.REPOS_FILE;

public enum Cloner {
    GIT_DEFAULT {
        /**
         * Clones repository using "git clone <i>link</i> <i>directory</i>".
         *
         * @param link a link to repository to clone
         * @param directory a directory to place cloned repository
         * @throws IOException see {@link #runGit(List)}
         * @throws InterruptedException see {@link #runGit(List)}
         * @throws IllegalStateException see {@link #runGit(List)}
         */
        @Override
        public void cloneRepo(String link, String directory) throws IOException, InterruptedException, IllegalStateException {
            List<String> command = new ArrayList<>(Arrays.asList("git", "clone", link));
            if (directory != null)
                command.add(directory);
            if (isBareClone)
                command.add("--bare");
            currentWorkingDirectory = System.getProperty("user.dir");
            Cloner.runGit(command);
        }
    },
    DEDUPLICATE_DUMB {
        /**
         * Clones repository using "git clone <i>link</i> <i>directory</i> --reference <i>sourceRepository</i>".
         * Uses dumb deduplication (every source repository is specified in "--reference").
         * If no source repositories specified, clones repository like {@link Cloner#GIT_DEFAULT}.
         *
         * @param link a link to repository to clone
         * @param directory a directory to place cloned repository
         * @throws IOException if there are some problems with {@link RepositoryScanner#REPOS_FILE} see {@link #runGit(List)}
         * @throws InterruptedException see {@link #runGit(List)}
         * @throws IllegalStateException see {@link #runGit(List)}
         */
        @Override
        public void cloneRepo(String link, String directory) throws IOException, InterruptedException, IllegalStateException {
            List<String> command = new ArrayList<>(Arrays.asList("git", "clone", link));
            if (directory != null)
                command.add(directory);
            if (isBareClone)
                command.add("--bare");
            Map<String, Path> sourceRepositories = RepositoryScanner.getFromFile(REPOS_FILE);
            for (Path repo : sourceRepositories.values()) {
                String repoPath = repo.toAbsolutePath().getParent().toString();
                command.add("--reference");
                command.add(repoPath);
            }
            currentWorkingDirectory = System.getProperty("user.dir");
            Cloner.runGit(command);
        }
    },
    DEDUPLICATE_GITHUB {
        /**
         * Clones repository from GitHub using "git clone <i>link</i> <i>directory</i> --reference <i>sourceRepository</i>".
         * Uses 'smart' deduplication (choosing the right repository to specify in "--reference").
         * If no source repository found, clones repository like {@link Cloner#GIT_DEFAULT}.
         *
         * @param link a link to repository to clone
         * @param directory a directory to place cloned repository
         * @throws IOException if there are some problems with {@link RepositoryScanner#REPOS_FILE} or with {@link TokenHolder#TOKEN_FILE}, also see {@link #runGit(List)}
         * @throws InterruptedException see {@link #runGit(List)}
         * @throws IllegalStateException if there are some problems with authorization, also see {@link #runGit(List)}
         */
        @Override
        public void cloneRepo(String link, String directory) throws IOException, IllegalStateException, InterruptedException {
            Pattern gitHubPattern = Pattern.compile("https://github\\.com/(.+)\\.git");
            Matcher gitHubMatcher = gitHubPattern.matcher(link);
            RepositoryId repositoryId;
            if (gitHubMatcher.find()) {
                String[] repositoryName = gitHubMatcher.group(1).split("/");
                repositoryId = new RepositoryId(repositoryName[0], repositoryName[1]);
            } else {
                throw new IllegalArgumentException("Wrong GitHub link: " + link);
            }
            List<String> command = new ArrayList<>(Arrays.asList("git", "clone", link));
            if (directory != null)
                command.add(directory);
            if (isBareClone)
                command.add("--bare");

            Map<String, Path> sourceRepositories = RepositoryScanner.getFromFile(REPOS_FILE);
            Set<String> sourceHashes = sourceRepositories.keySet();
            Iterator<String> iterator = sourceHashes.iterator();

            String token = "";
            CommitService service = new CommitService();
            if (isAuthorized)
                service.getClient().setOAuth2Token(token = TokenHolder.getToken());

            String currentSourceHash;
            String remoteHash = "";

            outerLoop:
            while (iterator.hasNext()) {
                currentSourceHash = iterator.next();
                final String currentToken = token;
                while (true) {
                    try {
                        service.getCommit(repositoryId, currentSourceHash);
                        break;
                    } catch (IOException e) {
                        String message = e.getMessage();
                        if (message.startsWith("API rate limit")) {
                            if (!isAuthorized)
                                throw new IllegalStateException("API rate limit exceed. Try later or use authorized access.");
                        } else if (message.startsWith("No commit found")) {
                            continue outerLoop;
                        } else if (message.startsWith("Bad credentials")) {
                            System.err.println(token + " is wrong token.");
                            service.getClient().setOAuth2Token(token = TokenHolder.getToken());
                            if (currentToken.equals(token))
                                throw new IllegalStateException("No token can be used. Check if they are valid or try later if their API rate limit exceeded.");
                            continue;
                        }
                        throw new IllegalStateException("Something went wrong: " + message);
                    }
                }
                remoteHash = currentSourceHash;
                break;
            }

            if (sourceRepositories.containsKey(remoteHash)) {
                File sourceRepository = sourceRepositories.get(remoteHash).toFile();
                if (sourceRepository.exists() && sourceRepository.canRead() && sourceRepository.isDirectory()) {
                    System.out.println("Local source repository for " + link + " found: " + sourceRepository.getAbsolutePath());
                    command.add("--reference");
                    command.add(sourceRepository.getParentFile().getAbsolutePath());
                } else {
                    System.err.println("Remove invalid repository " + sourceRepository.getAbsolutePath() + " from " + REPOS_FILE);
                }
            } else {
                System.err.println("No local source repository found for " + link);
            }

            currentWorkingDirectory = System.getProperty("user.dir");
            Cloner.runGit(command);
        }
    };

    private static boolean isAuthorized;
    private static boolean isBareClone;
    private static String currentWorkingDirectory;

    /**
     * Set authorized access for DEDUPLICATE_GITHUB.
     *
     * @param authorized whether to set authorized access.
     */
    public static void setAuthorized(boolean authorized) {
        isAuthorized = authorized;
    }

    /**
     * Tell Cloners to make bare clone (only .git directory, without working directory).
     *
     * @param bareClone whether to make bare clone.
     */
    public static void setBareClone(boolean bareClone) {
        isBareClone = bareClone;
    }

    /**
     * Executes Git commands.
     *
     * @param command list of commands to execute.
     * @throws IOException           if there are some problems when running the process or can't read program's output.
     * @throws InterruptedException  if there are some problems during the execution.
     * @throws IllegalStateException if Git finished execution with some error.
     */
    private static String runGit(List<String> command) throws IOException, InterruptedException, IllegalStateException {
        ProcessBuilder builder = new ProcessBuilder()
                .redirectErrorStream(true)
                .directory(new File(currentWorkingDirectory))
                .command(command);
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder gitOutputBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            gitOutputBuilder.append(line).append('\n');
        }
        int exitCode = process.waitFor();
        String gitOutput = gitOutputBuilder.toString();
        if (exitCode != 0) {
            throw new IllegalStateException("Something went wrong when running Git, the exit code is " + exitCode
                    + "\nGit said:\n" + gitOutput);
        }
        return gitOutput;
    }

    /**
     * Deletes a Git repository.
     *
     * @param directory directory to delete (must be a Git repository).
     * @throws IllegalStateException when not valid directory specified, also see {@link #runGit(List)}.
     * @throws IOException           if there are some problems with access to directory, also see {@link #runGit(List)}.
     * @throws InterruptedException  see {@link #runGit(List)}.
     */
    public static void deleteRepo(String directory) throws IllegalStateException, IOException, InterruptedException {
        RepositoryAction deleteAction = dir -> delete(Path.of(dir));
        workWithRepo(deleteAction, directory, isBareRepository(directory));
    }

    /**
     * Makes repository independent from its source repository.
     *
     * @param directory directory to repack (must be a Git repository).
     * @throws IOException           if there are some problems with access to directory, also see {@link #runGit(List)}.
     * @throws InterruptedException  see {@link #runGit(List)}.
     * @throws IllegalStateException if directory is not a Git repository, also see {@link #runGit(List)}.
     */
    public static void repackRepo(String directory) throws IOException, InterruptedException, IllegalStateException {
        final boolean repoIsBare = isBareRepository(directory);
        RepositoryAction repackAction = dir -> {
            File alternatesFile = repoIsBare
                    ? Path.of(directory, "objects", "info", "alternates").toFile()
                    : Path.of(directory, ".git", "objects", "info", "alternates").toFile();
            if (!alternatesFile.exists()) {
                throw new FileNotFoundException("alternates file not found, looks like repository is already independent.");
            }
            runGit(Arrays.asList("git", "repack", "-ad"));
            alternatesFile.delete();
        };
        workWithRepo(repackAction, directory, repoIsBare);
    }


    /**
     * Tells if the repository bare.
     *
     * @param directory directory to check (must be a Git repository).
     * @throws IOException           if there are some problems with access to directory, also see {@link #runGit(List)}.
     * @throws InterruptedException  see {@link #runGit(List)}.
     * @throws IllegalStateException if directory is not a Git repository, also see {@link #runGit(List)}.
     */
    private static boolean isBareRepository(String directory) throws IOException, InterruptedException, IllegalStateException {
        currentWorkingDirectory = directory;
        try {
            return Boolean.parseBoolean(runGit(Arrays.asList("git", "rev-parse", "--is-bare-repository")).strip());
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Directory is not a Git repository.");
        } catch (IOException e) {
            throw new IOException("Directory do not exist.");
        }
    }

    /**
     * Performs common actions for deleteRepo and repackRepo.
     *
     * @param action    action to perform on repository.
     * @param directory directory to act (must be a Git repository)
     * @param isBare    if the repository bare.
     * @throws IOException           if there are some problems with access to directory, also see {@link #runGit(List)}.
     * @throws InterruptedException  see {@link #runGit(List)}.
     * @throws IllegalStateException if directory is not a Git repository, also see {@link #runGit(List)}.
     */
    private static void workWithRepo(RepositoryAction action, String directory, boolean isBare) throws IOException, InterruptedException, IllegalStateException {
        currentWorkingDirectory = directory;
        try {
            Path gitRootDirectory = isBare
                    ? Path.of(runGit(Arrays.asList("git", "rev-parse", "--absolute-git-dir")).strip())
                    : Path.of(runGit(Arrays.asList("git", "rev-parse", "--show-toplevel")).strip());
            // second runGit() may throw IllegalStateException here (if running this from .git or its subdirectories)

            Path workingDirectory = Path.of(directory);
            if (Files.isSameFile(gitRootDirectory, workingDirectory)) {
                action.act(directory);
                return;
            }
        } catch (IllegalStateException ignored) {
        }
        throw new IllegalStateException("Specify root repository directory, not its subdirectory.");
    }

    /**
     * A single action to perform on repository.
     */
    private interface RepositoryAction {
        void act(String directory) throws IOException, InterruptedException;
    }

    /**
     * Deletes a directory with all of its subfolders and files.
     *
     * @param directory directory to delete.
     * @throws IOException when an I/O error occurs.
     */
    private static void delete(Path directory) throws IOException {
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public abstract void cloneRepo(String link, String directory) throws IOException, InterruptedException, IllegalStateException;
}