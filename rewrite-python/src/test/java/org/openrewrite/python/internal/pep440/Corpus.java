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
package org.openrewrite.python.internal.pep440;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the pypa/packaging conformance corpora under {@code src/test/resources/pep440} and
 * {@code pep508}. Lines starting with {@code #} are provenance comments; fields are
 * tab-separated with {@code \t}, {@code \n}, {@code \r} and {@code \\} escaped.
 */
public final class Corpus {
    private Corpus() {
    }

    public static List<String> lines(String resource) {
        List<String> result = new ArrayList<>();
        try (InputStream is = Corpus.class.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalArgumentException("Missing corpus resource: " + resource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    result.add(line);
                }
            }
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    public static List<String[]> rows(String resource) {
        List<String[]> result = new ArrayList<>();
        for (String line : lines(resource)) {
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            for (int i = 0; i < fields.length; i++) {
                fields[i] = unescape(fields[i]);
            }
            result.add(fields);
        }
        return result;
    }

    public static String unescape(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 == s.length()) {
                sb.append(c);
                continue;
            }
            char next = s.charAt(++i);
            switch (next) {
                case 't':
                    sb.append('\t');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                default:
                    sb.append(next);
            }
        }
        return sb.toString();
    }
}
