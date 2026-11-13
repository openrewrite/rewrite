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
import org.openrewrite.Validated;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Collections.singletonList;

/**
 * The packages ForgeBox, CommandBox's registry, serves. Each version downloads once into a cache and unpacks there,
 * and says where CommandBox installs it in a project, which is the mapping that reaches its components.
 */
public final class ForgeBoxPackages {
    public static final String FORGEBOX = "https://www.forgebox.io/api/v1";
    private static final String GITHUB = "https://github.com/";

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
     * release, or {@code be} for the newest version of any kind. An exact version may be one no semantic version range
     * names, as {@code 1.4.8.0} or {@code 2.0}.
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
        Map<String, JsonNode> listed = new LinkedHashMap<>();
        for (JsonNode version : entry.path("versions")) {
            listed.putIfAbsent(version.path("version").asText(), version);
        }
        String number = newest(new ArrayList<>(listed.keySet()), range);
        if (number == null) {
            throw new IllegalArgumentException("ForgeBox has no version of " + slug + " that " +
                                               (range.isEmpty() ? "is a release" : "satisfies " + range));
        }
        JsonNode version = listed.get(number);
        Path directory = cache.resolve(slug).resolve(number);
        if (!Files.isDirectory(directory)) {
            String url = version.path("downloadURL").asText();
            // ForgeBox spells it in several cases, and CommandBox compares it as CFML does, ignoring case
            if ("forgeboxStorage".equalsIgnoreCase(url)) {
                url = data(registry + "/storage/" + encode(slug) + "/" + encode(number)).asText();
            }
            String repository = gitHubRepository(url);
            if (repository != null) {
                unpackGitHub(repository, directory);
            } else if (!unpack(url, directory, false)) {
                throw new IllegalStateException("Downloading " + url + " answered 404");
            }
        }
        Path root = packageRoot(directory);
        return new Installed(slug, number, installPath(slug, entry.path("typeSlug").asText(""), root), root);
    }

    /**
     * The GitHub repository a download names as CommandBox writes one, {@code owner/repo} with a {@code #ref} or not,
     * or null. CommandBox takes a download with no colon and two names around a slash as one, or one written
     * {@code github:owner/repo}.
     */
    private static @Nullable String gitHubRepository(String download) {
        String repository = download.regionMatches(true, 0, "github:", 0, 7) ? download.substring(7) : download;
        if (repository.indexOf(':') >= 0 || repository.startsWith("@")) {
            return null;
        }
        StringBuilder names = new StringBuilder();
        int count = 0;
        for (String name : repository.trim().split("/")) {
            if (!name.isEmpty()) {
                names.append(count++ == 0 ? "" : "/").append(name);
            }
        }
        return count == 2 ? names.toString() : null;
    }

    /**
     * Unpacks a GitHub repository at its ref, as CommandBox installs one: it clones the repository and checks out the
     * ref, or {@code master}, else {@code main}, when there is none. GitHub's archive of a ref holds the same files,
     * in one directory named for the repository and the ref.
     */
    private void unpackGitHub(String repository, Path directory) {
        int hash = repository.indexOf('#');
        String path = hash < 0 ? repository : repository.substring(0, hash);
        List<String> refs = hash < 0 ? Arrays.asList("master", "main") : singletonList(repository.substring(hash + 1));
        for (String ref : refs) {
            if (unpack(GITHUB + path + "/archive/" + ref + ".zip", directory, true)) {
                return;
            }
        }
        throw new IllegalStateException("GitHub has no archive of " + path + " at " + String.join(" or ", refs));
    }

    /**
     * The newest version in the cache that the range allows, for when ForgeBox cannot be reached, or null.
     */
    private @Nullable Installed cached(String slug, String range) {
        List<String> numbers = new ArrayList<>();
        try (Stream<Path> versions = Files.list(cache.resolve(slug))) {
            for (Path directory : (Iterable<Path>) versions::iterator) {
                String number = directory.getFileName().toString();
                if (Files.isDirectory(directory) && !number.contains(".unpacking-")) {
                    numbers.add(number);
                }
            }
        } catch (IOException | UncheckedIOException e) {
            return null;
        }
        String newest = newest(numbers, range);
        if (newest == null) {
            return null;
        }
        Path root = packageRoot(cache.resolve(slug).resolve(newest));
        return new Installed(slug, newest, installPath(slug, "", root), root);
    }

    /**
     * Where a package's files are: the directory it unpacked into, else the first folder in that, by name, holding a
     * {@code box.json}, which is where CommandBox finds a package whose archive holds it in a folder.
     */
    private static Path packageRoot(Path directory) {
        if (Files.isRegularFile(directory.resolve("box.json"))) {
            return directory;
        }
        try (Stream<Path> children = Files.list(directory)) {
            return children.filter(child -> Files.isRegularFile(child.resolve("box.json")))
                    .min(Comparator.naturalOrder())
                    .orElse(directory);
        } catch (IOException | UncheckedIOException e) {
            return directory;
        }
    }

    private static VersionComparator comparator(String range) {
        String selector = range.isEmpty() || "*".equals(range) || "stable".equalsIgnoreCase(range) ?
                "latest.release" : "be".equalsIgnoreCase(range) ? "latest.integration" : range;
        Validated<VersionComparator> validated = Semver.validate(selector, null, Semver.Ecosystem.NODE);
        VersionComparator comparator = validated.isValid() ? validated.getValue() : null;
        if (comparator == null) {
            throw new IllegalArgumentException("Not a version range CommandBox takes: " + range);
        }
        return comparator;
    }

    /**
     * The version the range names exactly, else the newest the range allows. Some versions, as {@code 1.4.8.0}, are
     * named by no semantic version range.
     */
    private static @Nullable String newest(List<String> numbers, String range) {
        for (String number : numbers) {
            if (!range.isEmpty() && withoutBuild(number).equals(withoutBuild(range))) {
                return number;
            }
        }
        VersionComparator comparator = comparator(range);
        String newest = null;
        for (String number : numbers) {
            if (comparator.isValid(null, withoutBuild(number)) &&
                (newest == null || comparator.compare(null, withoutBuild(number), withoutBuild(newest)) > 0)) {
                newest = number;
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
     * Where CommandBox installs a package: its {@code box.json}'s {@code directory}, else the folder for its type, else
     * the project's root, then a directory named {@code packageDirectory}, else the slug its {@code box.json} gives, else
     * the one asked for, unless {@code createPackageDirectory} is false. CommandBox takes an empty value as none, which
     * its {@code box.json} template writes.
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
        String installDirectory = box.path("directory").asText("");
        String packageType = box.path("type").asText(type);
        boolean createPackageDirectory = box.path("createPackageDirectory").asBoolean(true);
        String base;
        if (!installDirectory.isEmpty()) {
            base = trimSlashes(installDirectory);
        } else {
            base = typeDirectory(packageType);
            // CommandBox drops these into their type's folder whatever their box.json says
            if ("contentbox-widgets".equals(packageType) || "plugins".equals(packageType) ||
                "interceptors".equals(packageType)) {
                createPackageDirectory = false;
            }
        }
        String packageDirectory = box.path("packageDirectory").asText("");
        String named = box.path("slug").asText("");
        String name = createPackageDirectory ?
                trimSlashes(!packageDirectory.isEmpty() ? packageDirectory : !named.isEmpty() ? named : slug) : "";
        return base.isEmpty() ? name : name.isEmpty() ? base : base + "/" + name;
    }

    /**
     * The folder CommandBox installs a type of package in within a project, or the project's root.
     */
    private static String typeDirectory(String type) {
        switch (type) {
            case "modules":
                return "modules";
            case "contentbox-modules":
                return "modules/contentbox/modules_user";
            case "contentbox-themes":
            case "contentbox-layouts":
                return "modules/contentbox/themes";
            case "contentbox-widgets":
                return "modules/contentbox/widgets";
            case "plugins":
                return "plugins";
            case "interceptors":
                return "interceptors";
            case "jars":
                return "lib";
            default:
                return "";
        }
    }

    /**
     * Gets a URL, following a redirect to https, which HttpURLConnection does not follow from http, as some of Ortus's
     * downloads redirect.
     */
    private HttpSender.Response get(String url) {
        String at = url;
        for (int redirects = 0; ; redirects++) {
            HttpSender.Response response = http.get(at).withConnectTimeout(TIMEOUT).withReadTimeout(TIMEOUT).send();
            String location = response.getCode() / 100 == 3 && redirects < 5 ? header(response, "Location") : null;
            String next = null;
            if (location != null) {
                try {
                    next = URI.create(at).resolve(location).toString();
                } catch (IllegalArgumentException notAUri) {
                    // the redirect is answered as it stands
                }
            }
            if (next == null || !next.startsWith("https://")) {
                return response;
            }
            response.close();
            at = next;
        }
    }

    private static @Nullable String header(HttpSender.Response response, String name) {
        for (Map.Entry<String, List<String>> header : response.getHeaders().entrySet()) {
            if (name.equalsIgnoreCase(header.getKey()) && !header.getValue().isEmpty()) {
                return header.getValue().get(0);
            }
        }
        return null;
    }

    private JsonNode data(String url) {
        try (HttpSender.Response response = get(url)) {
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
     * absent, and another process unpacking the same version at the same time loses the race harmlessly. A zip that
     * holds the package in one directory has that directory left out. Returns false when there is no zip at the URL.
     */
    private boolean unpack(String url, Path directory, boolean inOneDirectory) {
        Path unpacking = directory.resolveSibling(directory.getFileName() + ".unpacking-" + System.nanoTime());
        try (HttpSender.Response response = get(url)) {
            if (response.getCode() == 404) {
                return false;
            }
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Downloading " + url + " answered " + response.getCode());
            }
            Files.createDirectories(unpacking);
            try (InputStream body = response.getBody(); ZipInputStream zip = new ZipInputStream(body)) {
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    String name = entry.getName();
                    if (inOneDirectory) {
                        int slash = name.indexOf('/');
                        if (slash < 0 || slash == name.length() - 1) {
                            continue;
                        }
                        name = name.substring(slash + 1);
                    }
                    Path target = unpacking.resolve(name).normalize();
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
            return true;
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
