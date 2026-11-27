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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A project real pnpm installed with {@code install --lockfile-only} against a stub registry, in
 * {@code src/test/resources/pnpm/pnpm10/<name>/}: the files it ran over, the packuments the registry served in full
 * ({@code packuments-full.json}) and abbreviated ({@code packuments-abbreviated.json}), and in {@code outcomes/}
 * what each release wrote ({@code <release>.yaml}) or failed with ({@code <release>.err}).
 */
final class Pnpm10Recording {
    private static final Pattern PNPM_ERROR = Pattern.compile("ERR_PNPM_[A-Z0-9_]+");
    private static final Pattern PNPM_MESSAGE = Pattern.compile("ERROR\\W+(.+)");

    private final Path dir;

    private Pnpm10Recording(Path dir) {
        this.dir = dir;
    }

    static Pnpm10Recording of(String name) {
        URL url = Pnpm10Recording.class.getResource("/pnpm/pnpm10/" + name);
        if (url == null) {
            throw new IllegalArgumentException(name);
        }
        try {
            return new Pnpm10Recording(Paths.get(url.toURI()));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(name, e);
        }
    }

    /**
     * The regeneration of the recorded project as {@code version} installs it.
     */
    Result regenerate(String version) {
        Map<String, String> manifests = new LinkedHashMap<>();
        Map<String, String> files = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList())) {
                String relative = dir.relativize(path).toString().replace('\\', '/');
                if (relative.startsWith("outcomes/") || relative.startsWith("packuments-") ||
                    "pnpm-lock.yaml".equals(relative)) {
                    continue;
                }
                if (relative.equals("package.json") || relative.endsWith("/package.json")) {
                    manifests.put(relative.equals("package.json") ? "" :
                            relative.substring(0, relative.length() - "/package.json".length()), read(path));
                } else {
                    files.put(relative, read(path));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> full = packuments("packuments-full.json");
        Map<String, String> abbreviated = packuments("packuments-abbreviated.json");
        PnpmLockRegenerator.Inputs inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(Files.isRegularFile(dir.resolve("pnpm-lock.yaml")) ? read(dir.resolve("pnpm-lock.yaml")) : null)
          .manifests(manifests)
          .files(files)
          .paths(PnpmScenarios.paths(files, manifests, ""))
          .env(Collections.emptyMap())
          .now(Instant.parse("2026-11-25T00:00:00Z"))
          .release(PnpmRelease.of(version))
          .build();
        return new PnpmLockRegenerator(inputs, new RegistryMetadata() {
            @Override
            public @Nullable String packument(String registry, String name, boolean fullMetadata) {
                String packument = (fullMetadata ? full : abbreviated).get(name);
                return packument != null ? packument : (fullMetadata ? abbreviated : full).get(name);
            }

            @Override
            public @Nullable String attestations(String registry, String name, String version) {
                return null;
            }

            @Override
            public @Nullable String document(String url) {
                return null;
            }
        }).regenerate();
    }

    /**
     * The lock {@code version} wrote, null where it failed.
     */
    @Nullable
    String lock(String version) {
        Path lock = dir.resolve("outcomes/" + version + ".yaml");
        return Files.isRegularFile(lock) ? read(lock) : null;
    }

    /**
     * The pnpm error code {@code version} failed with, or its message where it gave no code, null where it printed
     * no error.
     */
    @Nullable
    String error(String version) {
        String err = read(dir.resolve("outcomes/" + version + ".err"));
        Matcher m = PNPM_ERROR.matcher(err);
        if (m.find()) {
            return m.group();
        }
        Matcher message = PNPM_MESSAGE.matcher(err);
        return message.find() ? message.group(1).trim() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> packuments(String file) {
        Path path = dir.resolve(file);
        if (!Files.isRegularFile(path)) {
            return Collections.emptyMap();
        }
        Map<String, String> packuments = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry :
                ((Map<String, @Nullable Object>) Js.jsonParse(read(path))).entrySet()) {
            packuments.put(entry.getKey(), Js.jsonStringify(entry.getValue()));
        }
        return packuments;
    }

    private static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
