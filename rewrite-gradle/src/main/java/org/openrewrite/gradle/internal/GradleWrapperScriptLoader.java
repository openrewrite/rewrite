package org.openrewrite.gradle.internal;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.marker.Markup;
import org.openrewrite.remote.Remote;
import org.openrewrite.remote.RemoteResource;
import org.openrewrite.semver.LatestRelease;

import java.io.*;
import java.util.NavigableMap;
import java.util.TreeMap;

import static java.util.Objects.requireNonNull;

public class GradleWrapperScriptLoader {
    @Getter
    private final NavigableMap<String, Version> allVersions = new TreeMap<>(
            new LatestRelease(null));

    public GradleWrapperScriptLoader() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(requireNonNull(
                getClass().getResourceAsStream("/META-INF/rewrite/gradle-wrapper/versions.csv"))))) {
            in.readLine(); // header
            String line;
            while ((line = in.readLine()) != null) {
                String[] row = line.split(",");
                allVersions.put(row[0], new Version(row[0], row[1], row[2]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * When the requested version is unavailable we pick the nearest available.
     */
    public Nearest findNearest(@Nullable String requestedVersion) {
        if (requestedVersion == null) {
            return new Nearest(null, allVersions.lastEntry().getValue());
        }
        return new Nearest(requestedVersion, allVersions.floorEntry(requestedVersion).getValue());
    }

    @SuppressWarnings("resource")
    @Value
    public static class Nearest {
        @Nullable
        String requestedVersion;

        Version resolved;

        public RemoteResource gradlew() {
            InputStream script = getClass().getResourceAsStream("/META-INF/rewrite/gradle-wrapper/unix/" + resolved.getGradlewChecksum() + ".txt");
            return maybeWarn(Remote.builder(GradleWrapper.WRAPPER_SCRIPT_LOCATION)
                    .description(String.format("Unix Gradle wrapper script template for %s", resolved.getVersion()))
                    .build(requireNonNull(script)));
        }

        public RemoteResource gradlewBat() {
            InputStream script = getClass().getResourceAsStream("/META-INF/rewrite/gradle-wrapper/windows/" + resolved.getGradlewBatChecksum() + ".txt");
            return maybeWarn(Remote.builder(GradleWrapper.WRAPPER_BATCH_LOCATION)
                    .description(String.format("Windows Gradle wrapper script template for %s", resolved.getVersion()))
                    .build(requireNonNull(script)));
        }

        public RemoteResource maybeWarn(RemoteResource script) {
            if (!resolved.getVersion().equals(requestedVersion)) {
                return Markup.warn(script, new Exception(
                        "rewrite-gradle does not contain a script for requested version" +
                        (requestedVersion == null ? "" : requestedVersion + ". ") +
                        "Using the script from nearest available version " + resolved.getVersion() + " instead."));
            }
            return script;
        }
    }

    @Value
    public static class Version {
        String version;
        String gradlewChecksum;
        String gradlewBatChecksum;
    }
}
