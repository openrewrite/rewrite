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
package org.openrewrite.golang.internal.modgraph;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Module-zip helpers. A Go module zip stores every file under
 * {@code <module>@<version>/...}; the resolver only needs the {@code .go} files
 * (tests included), keyed by their full entry path, to walk the package import
 * graph.
 */
final class Zips {

    private Zips() {
    }

    /** Extracts every {@code .go} file from a module zip, keyed by entry path. */
    static @Nullable Map<String, byte[]> goFiles(byte[] raw) {
        Map<String, byte[]> out = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(raw))) {
            ZipEntry e;
            byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory() || !e.getName().endsWith(".go")) {
                    continue;
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int n;
                while ((n = zis.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                }
                out.put(e.getName(), bos.toByteArray());
            }
        } catch (IOException ex) {
            return null;
        }
        return out;
    }

    /**
     * Filters extracted zip entries to the {@code .go} files of exactly one
     * package directory ({@code importPath} within {@code modPath@version}) —
     * the package itself, not deeper sub-packages.
     */
    static @Nullable Map<String, byte[]> packageFiles(Map<String, byte[]> entries, String modPath,
                                                      String version, String importPath) {
        String rel = importPath.startsWith(modPath) ? importPath.substring(modPath.length()) : importPath;
        if (rel.startsWith("/")) {
            rel = rel.substring(1);
        }
        String prefix = modPath + "@" + version + "/";
        if (!rel.isEmpty()) {
            prefix += rel + "/";
        }
        Map<String, byte[]> files = new HashMap<>();
        for (Map.Entry<String, byte[]> en : entries.entrySet()) {
            String name = en.getKey();
            if (!name.startsWith(prefix)) {
                continue;
            }
            String tail = name.substring(prefix.length());
            if (tail.indexOf('/') >= 0 || !tail.endsWith(".go")) {
                continue; // a deeper sub-package or non-go file
            }
            files.put(tail, en.getValue());
        }
        return files.isEmpty() ? null : files;
    }
}
