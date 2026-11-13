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
package org.openrewrite.maven;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * The command line arguments a plain {@code mvn} invocation in a project directory would pick up from
 * {@code .mvn/maven.config}. A {@link MavenParser} that ignores this file resolves poms differently than
 * the project's own build does, most visibly when the build activates a profile that contributes
 * properties, repositories, or a parent.
 * <p>
 * Only the options that change how a pom resolves are modeled: {@code -P}/{@code --activate-profiles} and
 * {@code -D}/{@code --define}. Everything else in the file is ignored rather than rejected, so an
 * unrecognized or future option does not cost the caller the options it does understand.
 *
 * @see MavenParser.Builder#mavenConfig(MavenConfig)
 */
@Value
public class MavenConfig {
    public static final MavenConfig EMPTY = new MavenConfig(emptyList(), emptyList(), emptyMap());

    /**
     * Profiles activated by {@code -P}, in the order they appear. Maven activates these on top of whatever
     * the poms and settings activate on their own.
     */
    List<String> activeProfiles;

    /**
     * Profiles that {@code -P} explicitly deactivates with a {@code !} or {@code -} prefix. These suppress a
     * profile that would otherwise activate itself, so they cannot be expressed by simply omitting them from
     * {@code getActiveProfiles()}.
     */
    List<String> inactiveProfiles;

    /**
     * User properties set by {@code -D}. A flag given without a value is {@code "true"}, matching Maven.
     */
    Map<String, String> properties;

    /**
     * Read {@code <mavenRoot>/.mvn/maven.config}. Maven reads this file from the directory it was invoked in
     * alone, so it is deliberately not searched for in parent or child directories.
     *
     * @param mavenRoot The directory a {@code mvn} invocation would run from, i.e. the one containing {@code .mvn}.
     * @param ctx       Receives an {@link IOException} through {@link ExecutionContext#getOnError()} if the file
     *                  exists but cannot be read.
     * @return The parsed config, or {@link #EMPTY} if there is no {@code .mvn/maven.config} or it cannot be read.
     */
    public static MavenConfig read(Path mavenRoot, ExecutionContext ctx) {
        Path mavenConfig = mavenRoot.resolve(".mvn").resolve("maven.config");
        if (!Files.isRegularFile(mavenConfig)) {
            return EMPTY;
        }
        try {
            return parse(new String(Files.readAllBytes(mavenConfig)));
        } catch (IOException e) {
            ctx.getOnError().accept(new UncheckedIOException("Failed to read maven.config at " + mavenConfig, e));
            return EMPTY;
        }
    }

    public static MavenConfig parse(String mavenConfig) {
        List<String> args = tokenize(mavenConfig);
        List<String> activeProfiles = new ArrayList<>();
        List<String> inactiveProfiles = new ArrayList<>();
        Map<String, String> properties = new LinkedHashMap<>();

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (isOption(arg, "-P", "--activate-profiles")) {
                String value = attachedValue(arg, "-P", "--activate-profiles");
                if (value == null && i + 1 < args.size()) {
                    value = args.get(++i);
                }
                if (value != null) {
                    addProfiles(value, activeProfiles, inactiveProfiles);
                }
            } else if (isOption(arg, "-D", "--define")) {
                String value = attachedValue(arg, "-D", "--define");
                if (value == null && i + 1 < args.size()) {
                    value = args.get(++i);
                }
                if (value != null) {
                    addProperty(value, properties);
                }
            }
        }

        return activeProfiles.isEmpty() && inactiveProfiles.isEmpty() && properties.isEmpty() ?
                EMPTY :
                new MavenConfig(activeProfiles, inactiveProfiles, properties);
    }

    /**
     * Maven has read this file one argument per line since 3.9, and as whitespace-separated arguments before
     * that. Splitting on whitespace within each line accepts both, and quoting recovers the one case the two
     * forms disagree on: an argument whose value contains a space.
     */
    private static List<String> tokenize(String mavenConfig) {
        List<String> args = new ArrayList<>();
        for (String line : mavenConfig.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
                continue;
            }
            StringBuilder arg = new StringBuilder();
            char quote = 0;
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (quote != 0) {
                    if (c == quote) {
                        quote = 0;
                    } else {
                        arg.append(c);
                    }
                } else if (c == '"' || c == '\'') {
                    quote = c;
                } else if (Character.isWhitespace(c)) {
                    if (arg.length() > 0) {
                        args.add(arg.toString());
                        arg.setLength(0);
                    }
                } else {
                    arg.append(c);
                }
            }
            if (arg.length() > 0) {
                args.add(arg.toString());
            }
        }
        return args;
    }

    private static boolean isOption(String arg, String shortOption, String longOption) {
        return arg.startsWith(shortOption) || arg.equals(longOption) || arg.startsWith(longOption + "=");
    }

    /**
     * @return The value attached to the option itself ({@code -Pfoo}, {@code --activate-profiles=foo}), or
     * {@code null} when the option stands alone and its value is the next argument.
     */
    private static @Nullable String attachedValue(String arg, String shortOption, String longOption) {
        if (arg.startsWith(longOption + "=")) {
            return arg.substring(longOption.length() + 1);
        }
        if (arg.startsWith(shortOption) && arg.length() > shortOption.length()) {
            return arg.substring(shortOption.length());
        }
        return null;
    }

    private static void addProfiles(String value, List<String> activeProfiles, List<String> inactiveProfiles) {
        for (String profile : value.split(",")) {
            profile = profile.trim();
            if (profile.isEmpty()) {
                continue;
            }
            char first = profile.charAt(0);
            if (first == '!' || first == '-') {
                String deactivated = profile.substring(1).trim();
                if (!deactivated.isEmpty()) {
                    inactiveProfiles.add(deactivated);
                }
            } else {
                activeProfiles.add(profile);
            }
        }
    }

    private static void addProperty(String value, Map<String, String> properties) {
        int equals = value.indexOf('=');
        if (equals < 0) {
            // `mvn -Dfoo` sets foo to "true"
            properties.put(value, "true");
        } else if (equals > 0) {
            properties.put(value.substring(0, equals), value.substring(equals + 1));
        }
    }
}
