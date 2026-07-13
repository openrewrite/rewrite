/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.parity.corpus;

import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Materializes {@code corpus.yaml} entries into {@code .corpus/}: single-pom entries are fetched
 * from Maven Central through {@link RecordingHttpSender} in RECORD mode (populating the store),
 * reactors are shallow-cloned at their pinned tag.
 */
public class CorpusFetch {

    public static void main(String[] args) throws Exception {
        Set<String> filter = new HashSet<>(Arrays.asList(args));
        CorpusManifest manifest = CorpusManifest.load(CorpusPaths.manifest());
        RecordingHttpSender sender = RecordingHttpSender.record(CorpusPaths.store(),
                new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(60)));

        int failures = 0;
        for (CorpusManifest.Entry entry : manifest.getEntries()) {
            if (!filter.isEmpty() && !filter.contains(entry.getName())) {
                continue;
            }
            if (entry.isDeferred()) {
                System.out.println("[fetch] SKIP (deferred): " + entry.getName());
                continue;
            }
            try {
                if (entry.isPom()) {
                    fetchPom(sender, entry);
                } else if (entry.isReactor()) {
                    cloneReactor(entry);
                } else {
                    throw new IllegalArgumentException("Unknown kind '" + entry.getKind() + "' for " + entry.getName());
                }
            } catch (Exception e) {
                failures++;
                System.err.println("[fetch] FAILED " + entry.getName() + ": " + e);
            }
        }
        if (failures > 0) {
            System.exit(1);
        }
    }

    static Path pomFile(CorpusManifest.Entry entry) {
        String[] gav = requireGav(entry);
        return CorpusPaths.poms().resolve(entry.getName()).resolve(gav[1] + "-" + gav[2] + ".pom");
    }

    static String centralPomUrl(String[] gav) {
        return "https://repo1.maven.org/maven2/" + gav[0].replace('.', '/') + "/" + gav[1] + "/" + gav[2] +
               "/" + gav[1] + "-" + gav[2] + ".pom";
    }

    static String[] requireGav(CorpusManifest.Entry entry) {
        String gav = entry.getSource().getGav();
        if (gav == null) {
            throw new IllegalArgumentException("Entry " + entry.getName() + " has no gav");
        }
        String[] parts = gav.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed gav '" + gav + "' for " + entry.getName());
        }
        return parts;
    }

    private static void fetchPom(RecordingHttpSender sender, CorpusManifest.Entry entry) throws IOException {
        Path target = pomFile(entry);
        String url = centralPomUrl(requireGav(entry));
        try (HttpSender.Response response = sender.send(sender.get(url).build())) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.getCode() + " fetching " + url);
            }
            Files.createDirectories(target.getParent());
            Files.write(target, readAll(response));
        }
        System.out.println("[fetch] pom " + entry.getName() + " -> " + target);
    }

    private static byte[] readAll(HttpSender.Response response) throws IOException {
        try (InputStream is = response.getBody()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int read;
            while ((read = is.read(data)) != -1) {
                buffer.write(data, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    private static void cloneReactor(CorpusManifest.Entry entry) throws IOException, InterruptedException {
        CorpusManifest.Git git = entry.getSource().getGit();
        if (git == null) {
            throw new IllegalArgumentException("Entry " + entry.getName() + " has no git source");
        }
        Path target = CorpusPaths.reactors().resolve(entry.getName());
        if (Files.isDirectory(target.resolve(".git"))) {
            System.out.println("[fetch] reactor " + entry.getName() + " already cloned");
            return;
        }
        Files.createDirectories(target.getParent());
        Process process = new ProcessBuilder("git", "clone", "--depth", "1", "--single-branch",
                "--branch", git.getTag(), git.getUrl(), target.toString())
                .inheritIO()
                .start();
        if (process.waitFor() != 0) {
            throw new IOException("git clone failed for " + entry.getName());
        }
        System.out.println("[fetch] reactor " + entry.getName() + " -> " + target);
    }
}
