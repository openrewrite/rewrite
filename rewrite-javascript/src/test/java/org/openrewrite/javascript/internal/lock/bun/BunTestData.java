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
package org.openrewrite.javascript.internal.lock.bun;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data tables of bun-v1.3.10's own tests, each written by the script of its name in {@code generate/}.
 */
final class BunTestData {

    private BunTestData() {
    }

    static String resource(String name) {
        try (InputStream in = BunTestData.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException(name + " is missing");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The rows of a tab separated resource.
     */
    static List<String[]> rows(String name) {
        List<String[]> rows = new ArrayList<>();
        for (String line : resource(name).split("\n")) {
            if (!line.isEmpty()) {
                rows.add(line.split("\t", -1));
            }
        }
        return rows;
    }

    static List<String> lines(String name) {
        List<String> lines = new ArrayList<>(Arrays.asList(resource(name).split("\n")));
        lines.removeIf(String::isEmpty);
        return lines;
    }
}
