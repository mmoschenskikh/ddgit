import core.Cloner;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeMeasurementTest {

    private final static String testFolder = "E:\\ddgit_test\\!clone\\";

    //Sample test
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Dumb deduplication, 16 source repositories, no suitable");
        testCase("https://github.com/Kotlin-Polytech/KotlinAsFirst2019", 1, Cloner.DEDUPLICATE_DUMB);
        testCase("https://github.com/remkop/picocli", 5, Cloner.DEDUPLICATE_DUMB);
        testCase("https://github.com/cypress-io/cypress", 1, Cloner.DEDUPLICATE_DUMB);
    }

    private static void testCase(String link, int count, Cloner cloner) throws IOException, InterruptedException {
        final int NUMBER_OF_TESTS = 5;
        long total = 0;
        for (int i = 0; i < NUMBER_OF_TESTS; i++) {
            long current = makeAttempt(link, count, cloner);
            total += current;
        }
        double totalTime = total / 60000000000.0;
        double timesCloned = count * NUMBER_OF_TESTS;
        double oneRepoTime = totalTime * 60 / timesCloned;
        System.out.println("NUMBER OF REPOSITORIES CLONED: " + timesCloned);
        System.out.println("             TOTAL TIME (MIN): " + totalTime);
        System.out.println("          ONE REPO TIME (SEC): " + oneRepoTime);
        System.out.println("************************************************************");
    }

    private static long makeAttempt(String link, int count, Cloner cloner) throws IOException, InterruptedException {
        String name = link.substring(link.lastIndexOf("/") + 1);
        Set<String> reposToDeduplicate = new TreeSet<>(parse(link + "/network/members", name));
        int i = 0;

        long start = System.nanoTime();
        for (String repo : reposToDeduplicate) {
            if (i == count) break;
            cloner.cloneRepo(repo, testFolder + i++);
        }
        long end = System.nanoTime();
        for (i = 0; i < count; i++) {
            Cloner.deleteRepo(testFolder + i);
        }
        return end - start;
    }

    private static Set<String> parse(String link, String name) throws IOException {
        URL url = new URL(link);
        Scanner scanner = new Scanner(url.openConnection().getInputStream());
        Pattern pattern = Pattern.compile("<a href=\"(.+)\">" + name + "</a>");

        Set<String> out = new HashSet<>();
        String line = scanner.nextLine();
        while (scanner.hasNextLine()) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                out.add("https://github.com" + matcher.group(1) + ".git");
            }
            line = scanner.nextLine();
        }
        return out;
    }
}
