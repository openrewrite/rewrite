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
package org.openrewrite.coldfusion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The packages ForgeBox, CommandBox's registry, serves. Each version downloads once into a cache and unpacks there,
 * and says where CommandBox installs it in a project, which is the mapping that reaches its components.
 */
public final class ForgeBoxPackages {
    public static final String FORGEBOX = "https://www.forgebox.io/api/v1";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    /**
     * What each coordinate resolved to in this JVM, by registry, cache and coordinate, so that tests resolving the
     * same packages ask ForgeBox once.
     */
    private static final Map<String, Installed> RESOLVED = new ConcurrentHashMap<>();

    private final HttpSender http;
    private final String registry;
    private final Path cache;

    public ForgeBoxPackages(HttpSender http) {
        this(http, FORGEBOX, Paths.get(System.getProperty("user.home"), ".rewrite", "cache", "forgebox"));
    }

    public ForgeBoxPackages(HttpSender http, String registry, Path cache) {
        this.http = http;
        this.registry = registry;
        this.cache = cache;
    }

    @Value
    public static class Installed {
        String slug;
        String version;

        /**
         * Where CommandBox installs the package in a project, from the project's root, as {@code testbox} or
         * {@code modules/cbvalidation}.
         */
        String installPath;

        Path directory;
    }

    /**
     * The package a coordinate names, {@code slug} or {@code slug@range}, at the newest version the range allows. A
     * range is CommandBox's: {@code ^7.0.0}, {@code 7.x}, an exact version, {@code stable} or {@code *} for the newest
     * release, or {@code be} for the newest version of any kind.
     */
    public Installed install(String coordinate) {
        return RESOLVED.computeIfAbsent(registry + "|" + cache + "|" + coordinate, k -> resolve(coordinate));
    }

    private Installed resolve(String coordinate) {
        int at = coordinate.indexOf('@');
        String slug = at < 0 ? coordinate.trim() : coordinate.substring(0, at).trim();
        String range = at < 0 ? "" : coordinate.substring(at + 1).trim();
        JsonNode entry;
        try {
            entry = data(registry + "/entry/" + encode(slug));
        } catch (RuntimeException unreachable) {
            Installed cached = cached(slug, range);
            if (cached == null) {
                throw unreachable;
            }
            return cached;
        }
        JsonNode version = newest(entry.path("versions"), range);
        if (version == null) {
            throw new IllegalArgumentException("ForgeBox has no version of " + slug + " that " +
                                               (range.isEmpty() ? "is a release" : "satisfies " + range));
        }
        String number = version.path("version").asText();
        Path directory = cache.resolve(slug).resolve(number);
        if (!Files.isDirectory(directory)) {
            String url = version.path("downloadURL").asText();
            if ("forgeboxStorage".equals(url)) {
                url = data(registry + "/storage/" + encode(slug) + "/" + encode(number)).asText();
            }
            unpack(url, directory);
        }
        return new Installed(slug, number, installPath(slug, entry.path("typeSlug").asText(""), directory), directory);
    }

    /**
     * The newest version in the cache that the range allows, for when ForgeBox cannot be reached, or null.
     */
    private @Nullable Installed cached(String slug, String range) {
        VersionComparator comparator = comparator(range);
        String newest = null;
        try (Stream<Path> versions = Files.list(cache.resolve(slug))) {
            for (Path directory : (Iterable<Path>) versions::iterator) {
                String number = directory.getFileName().toString();
                if (Files.isDirectory(directory) && !number.contains(".unpacking-") &&
                    comparator.isValid(null, withoutBuild(number)) &&
                    (newest == null || comparator.compare(null, withoutBuild(number), withoutBuild(newest)) > 0)) {
                    newest = number;
                }
            }
        } catch (IOException | UncheckedIOException e) {
            return null;
        }
        if (newest == null) {
            return null;
        }
        Path directory = cache.resolve(slug).resolve(newest);
        return new Installed(slug, newest, installPath(slug, "", directory), directory);
    }

    private static VersionComparator comparator(String range) {
        String selector = range.isEmpty() || "*".equals(range) || "stable".equalsIgnoreCase(range) ?
                "latest.release" : "be".equalsIgnoreCase(range) ? "latest.integration" : range;
        VersionComparator comparator = Semver.validate(selector, null, Semver.Ecosystem.NODE).getValue();
        if (comparator == null) {
            throw new IllegalArgumentException("Not a version range CommandBox takes: " + range);
        }
        return comparator;
    }

    private static @Nullable JsonNode newest(JsonNode versions, String range) {
        VersionComparator comparator = comparator(range);
        JsonNode newest = null;
        for (JsonNode version : versions) {
            String number = withoutBuild(version.path("version").asText());
            if (comparator.isValid(null, number) &&
                (newest == null || comparator.compare(null, number, withoutBuild(newest.path("version").asText())) > 0)) {
                newest = version;
            }
        }
        return newest;
    }

    /**
     * A version without its build metadata, {@code 4.9.1} for {@code 4.9.1+32}, since build metadata never orders
     * versions and the {@code latest.*} selectors leave out a version that has it.
     */
    private static String withoutBuild(String version) {
        int plus = version.indexOf('+');
        return plus < 0 ? version : version.substring(0, plus);
    }

    /**
     * Where CommandBox installs a package: its {@code box.json}'s {@code directory}, else {@code modules} for a ColdBox
     * module, else the project's root, then a directory named {@code packageDirectory} or the slug, unless
     * {@code createPackageDirectory} is false.
     */
    private static String installPath(String slug, String type, Path directory) {
        JsonNode box = JSON.createObjectNode();
        Path manifest = directory.resolve("box.json");
        if (Files.isRegularFile(manifest)) {
            try {
                box = JSON.readTree(manifest.toFile());
            } catch (IOException e) {
                // a package whose box.json does not read installs where its type says
            }
        }
        String base = box.hasNonNull("directory") ? trimSlashes(box.get("directory").asText()) :
                "modules".equals(box.path("type").asText(type)) ? "modules" : "";
        String name = box.path("createPackageDirectory").asBoolean(true) ?
                trimSlashes(box.path("packageDirectory").asText(slug)) : "";
        return base.isEmpty() ? name : name.isEmpty() ? base : base + "/" + name;
    }

    private JsonNode data(String url) {
        try (HttpSender.Response response = http.get(url).withConnectTimeout(TIMEOUT).withReadTimeout(TIMEOUT).send()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("ForgeBox answered " + response.getCode() + " for " + url);
            }
            JsonNode body = JSON.readTree(response.getBody());
            if (body.path("error").asBoolean(false)) {
                throw new IllegalArgumentException("ForgeBox could not answer " + url + ": " + body.path("messages"));
            }
            return body.path("data");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Unpacks a package's zip beside its directory and moves it into place, so a directory in the cache is complete or
     * absent, and another process unpacking the same version at the same time loses the race harmlessly.
     */
    private void unpack(String url, Path directory) {
        Path unpacking = directory.resolveSibling(directory.getFileName() + ".unpacking-" + System.nanoTime());
        try (HttpSender.Response response = http.get(url).withConnectTimeout(TIMEOUT).withReadTimeout(TIMEOUT).send()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Downloading " + url + " answered " + response.getCode());
            }
            Files.createDirectories(unpacking);
            try (InputStream body = response.getBody(); ZipInputStream zip = new ZipInputStream(body)) {
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    Path target = unpacking.resolve(entry.getName()).normalize();
                    if (!target.startsWith(unpacking)) {
                        continue;
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            try {
                Files.move(unpacking, directory, StandardCopyOption.ATOMIC_MOVE);
            } catch (FileAlreadyExistsException | DirectoryNotEmptyException raced) {
                // another process unpacked the same version first
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unpacking " + url + " into " + directory, e);
        } finally {
            deleteRecursively(unpacking);
        }
    }

    private static void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // a partly unpacked directory is left for the next run to remove
        }
    }

    private static String trimSlashes(String path) {
        String trimmed = path.replace('\\', '/');
        while (trimmed.startsWith("/") || trimmed.startsWith("./")) {
            trimmed = trimmed.substring(trimmed.startsWith("/") ? 1 : 2);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String encode(String part) {
        try {
            return URLEncoder.encode(part, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
