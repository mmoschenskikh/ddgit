package core;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TokenHolder {

    /**
     * A file to store GitHub OAuthTokens. It is not safe to save your real tokens here.
     * Store your tokens in this file in the program directory, one per line.
     * Current token is marked by '*' sign.
     */
    public final static File TOKEN_FILE = new File("tokens");
    private final static int TOKEN_LENGTH = 40;

    /**
     * Returns the next token from the file.
     *
     * @return GitHub OAuth token.
     * @throws IOException if there are some problems with {@link #TOKEN_FILE} file.
     */
    public static String getToken() throws IOException, IllegalStateException {
        if (!TOKEN_FILE.exists())
            throw new FileNotFoundException("Cannot find \"" + TOKEN_FILE.getName() + "\" file.");

        List<String> tokens = new ArrayList<>();
        int starred = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(TOKEN_FILE))) {
            int i = 1;
            String line = reader.readLine();
            while (line != null) {
                if (line.endsWith("*")) {
                    starred = i;
                    line = line.substring(0, line.length() - 1);
                }
                tokens.add(line);
                line = reader.readLine();
                i++;
            }
        }

        StringBuilder builder = new StringBuilder(String.join("\n", tokens));
        if (builder.length() < TOKEN_LENGTH)
            throw new IllegalStateException("\"tokens\" file is not properly formatted");
        int offset = (starred == tokens.size()) ? TOKEN_LENGTH : TOKEN_LENGTH * (starred + 1) + starred;
        builder.insert(offset, '*');

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TOKEN_FILE))) {
            writer.write(builder.toString());
        }
        return (starred == tokens.size()) ? tokens.get(0) : tokens.get(starred);
    }
}
