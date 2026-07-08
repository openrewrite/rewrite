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

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Captures ground truth from real Apache Maven 3.9.16 per corpus entry: {@code dependency:tree
 * -Dverbose} and {@code help:effective-pom}, written to {@code .corpus/ground-truth/<entry>/}.
 * These files are the fix-vs-bug arbiter for ledger triage. Single-pom entries get the
 * effective-pom of the artifact's own pom plus a dependency tree via a generated consumer stub
 * declaring the GAV as sole dependency. Uses an isolated local repository under .corpus/; never
 * touches ~/.m2.
 */
public class GroundTruthCapture {
    private static final String MAVEN_VERSION = "3.9.16";
    private static final String MAVEN_DIST_URL =
            "https://archive.apache.org/dist/maven/maven-3/" + MAVEN_VERSION +
            "/binaries/apache-maven-" + MAVEN_VERSION + "-bin.tar.gz";
    private static final String HELP_PLUGIN = "org.apache.maven.plugins:maven-help-plugin:3.4.0";
    private static final String DEPENDENCY_PLUGIN = "org.apache.maven.plugins:maven-dependency-plugin:3.6.1";
    private static final Duration MVN_TIMEOUT = Duration.ofMinutes(15);

    public static void main(String[] args) throws Exception {
        Set<String> filter = new HashSet<>(Arrays.asList(args));
        CorpusManifest manifest = CorpusManifest.load(CorpusPaths.manifest());
        Path mvn = obtainMaven();
        System.out.println("[capture] using " + mvn);

        List<String> failures = new ArrayList<>();
        for (CorpusManifest.Entry entry : manifest.getEntries()) {
            if (!filter.isEmpty() && !filter.contains(entry.getName())) {
                continue;
            }
            if (entry.isDeferred() || entry.isFetchOnly() || entry.getGroundTruth().isEmpty()) {
                System.out.println("[capture] SKIP " + entry.getName());
                continue;
            }
            try {
                capture(mvn, entry);
            } catch (Exception e) {
                failures.add(entry.getName() + ": " + e);
                System.err.println("[capture] FAILED " + entry.getName() + ": " + e);
            }
        }
        if (!failures.isEmpty()) {
            System.err.println("[capture] failures: " + failures);
            System.exit(1);
        }
    }

    private static void capture(Path mvn, CorpusManifest.Entry entry) throws IOException, InterruptedException {
        Path gtDir = CorpusPaths.groundTruth().resolve(entry.getName());
        Files.createDirectories(gtDir);

        if (entry.isPom()) {
            Path pom = CorpusFetch.pomFile(entry);
            if (!Files.exists(pom)) {
                throw new IOException("Pom not fetched yet: " + pom + " (run corpusFetch first)");
            }
            if (entry.getGroundTruth().contains("effective-pom")) {
                run(mvn, pom, gtDir.resolve("effective-pom.xml"), gtDir.resolve("mvn-effective-pom.log"),
                        HELP_PLUGIN + ":effective-pom", false);
            }
            if (entry.getGroundTruth().contains("dependency-tree")) {
                Path stub = writeConsumerStub(entry, pom);
                run(mvn, stub, gtDir.resolve("dependency-tree.txt"), gtDir.resolve("mvn-dependency-tree.log"),
                        DEPENDENCY_PLUGIN + ":tree", false);
            }
        } else if (entry.isReactor()) {
            Path reactor = CorpusPaths.reactors().resolve(entry.getName()).resolve("pom.xml");
            if (!Files.exists(reactor)) {
                throw new IOException("Reactor not fetched yet: " + reactor + " (run corpusFetch first)");
            }
            if (entry.getGroundTruth().contains("effective-pom")) {
                run(mvn, reactor, gtDir.resolve("effective-pom.xml"), gtDir.resolve("mvn-effective-pom.log"),
                        HELP_PLUGIN + ":effective-pom", false);
            }
            if (entry.getGroundTruth().contains("dependency-tree")) {
                run(mvn, reactor, gtDir.resolve("dependency-tree.txt"), gtDir.resolve("mvn-dependency-tree.log"),
                        DEPENDENCY_PLUGIN + ":tree", true);
            }
        }
        System.out.println("[capture] " + entry.getName() + " -> " + gtDir);
    }

    private static void run(Path mvn, Path pom, Path output, Path log, String goal, boolean append)
            throws IOException, InterruptedException {
        Files.deleteIfExists(output);
        List<String> command = new ArrayList<>(Arrays.asList(
                mvn.toAbsolutePath().toString(),
                "-B", "-f", pom.toAbsolutePath().toString(),
                "-Dmaven.repo.local=" + CorpusPaths.localRepository().toAbsolutePath(),
                goal));
        if (goal.endsWith(":effective-pom")) {
            command.add("-Doutput=" + output.toAbsolutePath());
        } else {
            command.add("-Dverbose");
            command.add("-DoutputFile=" + output.toAbsolutePath());
            if (append) {
                command.add("-DappendOutput=true");
            }
        }
        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
        Process process = builder.start();
        if (!process.waitFor(MVN_TIMEOUT.toMinutes(), TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new IOException("Timed out after " + MVN_TIMEOUT + ": " + String.join(" ", command));
        }
        if (process.exitValue() != 0) {
            throw new IOException("mvn exited " + process.exitValue() + " (see " + log + "): " + String.join(" ", command));
        }
    }

    private static Path writeConsumerStub(CorpusManifest.Entry entry, Path pom) throws IOException {
        String[] gav = CorpusFetch.requireGav(entry);
        boolean pomPackaging = new String(Files.readAllBytes(pom), StandardCharsets.UTF_8)
                .contains("<packaging>pom</packaging>");
        Path stub = CorpusPaths.stubs().resolve(entry.getName()).resolve("pom.xml");
        Files.createDirectories(stub.getParent());
        Files.write(stub, ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                           "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                           "    <modelVersion>4.0.0</modelVersion>\n" +
                           "    <groupId>org.openrewrite.maven.parity</groupId>\n" +
                           "    <artifactId>consumer-of-" + entry.getName() + "</artifactId>\n" +
                           "    <version>1.0</version>\n" +
                           "    <dependencies>\n" +
                           "        <dependency>\n" +
                           "            <groupId>" + gav[0] + "</groupId>\n" +
                           "            <artifactId>" + gav[1] + "</artifactId>\n" +
                           "            <version>" + gav[2] + "</version>\n" +
                           (pomPackaging ? "            <type>pom</type>\n" : "") +
                           "        </dependency>\n" +
                           "    </dependencies>\n" +
                           "</project>\n").getBytes(StandardCharsets.UTF_8));
        return stub;
    }

    /**
     * Uses PATH mvn only when it is exactly 3.9.16 (the pinned parity reference); otherwise
     * downloads the 3.9.16 bin distribution into .corpus/tools/.
     */
    private static Path obtainMaven() throws IOException, InterruptedException {
        Path installed = CorpusPaths.tools().resolve("apache-maven-" + MAVEN_VERSION).resolve("bin").resolve("mvn");
        if (Files.isExecutable(installed)) {
            return installed;
        }
        Path fromPath = mavenFromPath();
        if (fromPath != null) {
            return fromPath;
        }
        Files.createDirectories(CorpusPaths.tools());
        Path archive = CorpusPaths.tools().resolve("apache-maven-" + MAVEN_VERSION + "-bin.tar.gz");
        System.out.println("[capture] downloading " + MAVEN_DIST_URL);
        try (InputStream in = new URL(MAVEN_DIST_URL).openStream()) {
            Files.copy(in, archive, StandardCopyOption.REPLACE_EXISTING);
        }
        Process untar = new ProcessBuilder("tar", "-xzf", archive.toAbsolutePath().toString(),
                "-C", CorpusPaths.tools().toAbsolutePath().toString())
                .inheritIO()
                .start();
        if (untar.waitFor() != 0 || !Files.isExecutable(installed)) {
            throw new IOException("Failed to unpack Maven distribution to " + installed);
        }
        return installed;
    }

    private static @Nullable Path mavenFromPath() {
        try {
            Process process = new ProcessBuilder("mvn", "-v").redirectErrorStream(true).start();
            String out = new String(readAll(process.getInputStream()), StandardCharsets.UTF_8);
            process.waitFor(30, TimeUnit.SECONDS);
            if (out.contains("Apache Maven " + MAVEN_VERSION)) {
                Process which = new ProcessBuilder("which", "mvn").start();
                String path = new String(readAll(which.getInputStream()), StandardCharsets.UTF_8).trim();
                which.waitFor(10, TimeUnit.SECONDS);
                if (!path.isEmpty()) {
                    return Path.of(path);
                }
            }
        } catch (IOException | InterruptedException ignored) {
        }
        return null;
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int read;
        while ((read = is.read(data)) != -1) {
            buffer.write(data, 0, read);
        }
        return buffer.toByteArray();
    }
}
