package core;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// unsafe
public class TokenHolder {

    private final static String tokenFile = "tokens";
    private final static int TOKEN_LENGTH = 40;

    /**
     * Returns the next token from the file.
     * Store your tokens is 'tokens' file in the program directory.
     * Current token is marked by '*' sign.
     * Don't use your real account's token there. It's totally unsafe.
     *
     * @return GitHub OAuth token.
     * @throws IOException if there are some problems with 'tokens' file.
     */
    public static String getToken() throws IOException {
        List<String> tokens = new ArrayList<>();
        File store = new File(tokenFile);
        BufferedReader br = new BufferedReader(new FileReader(store));
        String line = br.readLine();
        int i = 1;
        int starred = 0;
        while (line != null) {
            if (line.endsWith("*")) {
                starred = i;
            }
            tokens.add(line);
            line = br.readLine();
            i++;
        }

        StringBuilder sb = new StringBuilder(String.join("\n", tokens));
        sb.deleteCharAt(TOKEN_LENGTH * starred + starred - 1);
        int offset = (starred == tokens.size()) ? TOKEN_LENGTH : TOKEN_LENGTH * (starred + 1) + starred;
        sb.insert(offset, '*');

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(store))) {
            bw.write(sb.toString());
        }

        return (starred == tokens.size()) ? tokens.get(0) : tokens.get(starred);
    }
}
