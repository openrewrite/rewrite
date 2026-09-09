/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal.index;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class Netrc {

    private Netrc() {
    }

    @Value
    static class Login {
        @Nullable
        String login;

        @Nullable
        String password;
    }

    static @Nullable Login find(Environment env, String host) {
        Path file = locate(env);
        if (file == null) {
            return null;
        }
        try {
            return find(Files.readAllLines(file, StandardCharsets.UTF_8), host);
        } catch (IOException e) {
            return null;
        }
    }

    static @Nullable Login find(List<String> lines, String host) {
        List<String> tokens = tokenize(lines);
        String login = null;
        String password = null;
        String defaultLogin = null;
        String defaultPassword = null;
        boolean matching = false;
        boolean inDefault = false;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if ("machine".equals(token) && i + 1 < tokens.size()) {
                if (matching) {
                    break;
                }
                matching = host.equals(tokens.get(++i));
                inDefault = false;
            } else if ("default".equals(token)) {
                if (matching) {
                    break;
                }
                inDefault = true;
            } else if (("login".equals(token) || "password".equals(token) || "account".equals(token)) &&
                    i + 1 < tokens.size()) {
                String value = tokens.get(++i);
                if ("login".equals(token)) {
                    if (matching) {
                        login = value;
                    } else if (inDefault) {
                        defaultLogin = value;
                    }
                } else if ("password".equals(token)) {
                    if (matching) {
                        password = value;
                    } else if (inDefault) {
                        defaultPassword = value;
                    }
                }
            }
        }
        if (login != null || password != null) {
            return new Login(login, password);
        }
        if (defaultLogin != null || defaultPassword != null) {
            return new Login(defaultLogin, defaultPassword);
        }
        return null;
    }

    private static @Nullable Path locate(Environment env) {
        String explicit = env.getenv("NETRC");
        if (explicit != null && !explicit.isEmpty()) {
            Path path = Paths.get(explicit);
            return Files.isRegularFile(path) ? path : null;
        }
        Path netrc = env.userHome().resolve(".netrc");
        if (Files.isRegularFile(netrc)) {
            return netrc;
        }
        if (env.osName().toLowerCase(Locale.ROOT).contains("win")) {
            Path winNetrc = env.userHome().resolve("_netrc");
            if (Files.isRegularFile(winNetrc)) {
                return winNetrc;
            }
        }
        return null;
    }

    private static List<String> tokenize(List<String> lines) {
        List<String> tokens = new ArrayList<>();
        boolean inMacdef = false;
        for (String line : lines) {
            if (inMacdef) {
                if (line.trim().isEmpty()) {
                    inMacdef = false;
                }
                continue;
            }
            for (String token : splitLine(line)) {
                if (token.startsWith("#")) {
                    break;
                }
                if ("macdef".equals(token)) {
                    inMacdef = true;
                    break;
                }
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static List<String> splitLine(String line) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '"') {
                int close = line.indexOf('"', i + 1);
                if (close < 0) {
                    tokens.add(line.substring(i + 1));
                    break;
                }
                tokens.add(line.substring(i + 1, close));
                i = close + 1;
            } else {
                int end = i;
                while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
                    end++;
                }
                tokens.add(line.substring(i, end));
                i = end;
            }
        }
        return tokens;
    }
}
