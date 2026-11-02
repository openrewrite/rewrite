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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.Picomatch;
import org.openrewrite.javascript.internal.lock.PicomatchScan;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Port of tinyglobby 0.2.16 (src/index.ts, crawler.ts, patterns.ts, utils.ts) under the options pnpm 11.2.2's
 * findPackages gives it, with fdir 6.5.0's crawl over the files the host holds instead of the file system.
 */
final class Tinyglobby {

    private static final Picomatch.Options PICOMATCH = new Picomatch.Options(Picomatch.Release.V4_0_4, false, true);

    private static final Pattern ONLY_PARENT_DIRECTORIES = V8RegExp.of("^(\\/?\\.\\.)+$");
    private static final Pattern PARENT_DIRECTORY = V8RegExp.of("^(\\/?\\.\\.)+");
    private static final Pattern ESCAPING_BACKSLASHES = V8RegExp.of("\\\\(?=[()[\\]{}!*+?@|])");

    private @Nullable List<String> commonPath;
    private String root = "";
    private int depthOffset;

    private Tinyglobby() {
    }

    /**
     * {@code glob(patterns, {cwd, ignore, expandDirectories: false})} over files given relative to the working
     * directory: those it returns.
     */
    static List<String> glob(List<String> patterns, List<String> ignore, Collection<String> files) {
        if (patterns.isEmpty()) {
            return new ArrayList<>();
        }
        Tinyglobby props = new Tinyglobby();
        List<String> matchPatterns = new ArrayList<>();
        List<String> ignorePatterns = new ArrayList<>();
        for (String pattern : ignore) {
            if (pattern.isEmpty()) {
                continue;
            }
            if (pattern.charAt(0) != '!' || charAt(pattern, 1) == '(') {
                ignorePatterns.add(props.normalizePattern(pattern, true));
            }
        }
        for (String pattern : patterns) {
            if (pattern.isEmpty()) {
                continue;
            }
            if (pattern.charAt(0) != '!' || charAt(pattern, 1) == '(') {
                matchPatterns.add(props.normalizePattern(pattern, false));
            } else if (charAt(pattern, 1) != '!' || charAt(pattern, 2) == '(') {
                ignorePatterns.add(props.normalizePattern(pattern.substring(1), true));
            }
        }
        Predicate<String> matcher = matcher(matchPatterns);
        Predicate<String> ignored = matcher(ignorePatterns);
        Predicate<String> partialMatcher = getPartialMatcher(matchPatterns);

        List<String> found = new ArrayList<>();
        for (String file : files) {
            if (!props.root.isEmpty() && !file.startsWith(props.root + "/")) {
                continue;
            }
            if (crawled(file, props.root, partialMatcher, ignored) && matcher.test(file) && !ignored.test(file)) {
                found.add(file);
            }
        }
        return found;
    }

    // fdir walks a directory below the root unless the exclude predicate skips it
    private static boolean crawled(String file, String root, Predicate<String> partialMatcher,
                                   Predicate<String> ignored) {
        int from = root.isEmpty() ? 0 : root.length() + 1;
        for (int slash = file.indexOf('/', from); slash >= 0; slash = file.indexOf('/', slash + 1)) {
            String relativePath = file.substring(0, slash);
            if (!partialMatcher.test(relativePath) || ignored.test(relativePath)) {
                return false;
            }
        }
        return true;
    }

    private static int charAt(String s, int i) {
        return i < s.length() ? s.charAt(i) : -1;
    }

    private String normalizePattern(String pattern, boolean isIgnore) {
        String result = pattern;
        if (pattern.endsWith("/")) {
            result = pattern.substring(0, pattern.length() - 1);
        }
        if (ESCAPING_BACKSLASHES.matcher(result).replaceAll("").startsWith("/")) {
            throw new CannotKnow("the projects the workspace pattern \"" + pattern + "\" matches (an absolute " +
                                 "path, which needs the project's own)");
        }
        result = PosixPath.normalize(result);
        if (PARENT_DIRECTORY.matcher(result).find()) {
            throw new CannotKnow("the projects the workspace pattern \"" + pattern + "\" matches (paths outside " +
                                 "the project)");
        }
        List<String> parts = splitPattern(result);
        if (!isIgnore && depthOffset >= 0) {
            if (commonPath == null) {
                commonPath = parts;
            }
            List<String> newCommonPath = new ArrayList<>();
            int length = Math.min(commonPath.size(), parts.size());
            for (int i = 0; i < length; i++) {
                String part = parts.get(i);
                if ("**".equals(part) && (i + 1 >= parts.size() || parts.get(i + 1).isEmpty())) {
                    if (!newCommonPath.isEmpty()) {
                        newCommonPath.remove(newCommonPath.size() - 1);
                    }
                    break;
                }
                if (i == parts.size() - 1 || !part.equals(commonPath.get(i)) || isDynamicPattern(part)) {
                    break;
                }
                newCommonPath.add(part);
            }
            depthOffset = newCommonPath.size();
            commonPath = newCommonPath;
            // buildCrawler drops every backslash of the root
            root = PosixPath.normalize(String.join("/", newCommonPath)).replace("\\", "");
            if (".".equals(root)) {
                root = "";
            }
        }
        return result;
    }

    private static Predicate<String> getPartialMatcher(List<String> patterns) {
        List<List<String>> patternsParts = new ArrayList<>();
        List<List<Predicate<String>>> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            List<String> parts = splitPattern(pattern);
            List<Predicate<String>> partMatchers = new ArrayList<>();
            for (String part : parts) {
                partMatchers.add(matcher(Collections.singletonList(part)));
            }
            patternsParts.add(parts);
            matchers.add(partMatchers);
        }
        return input -> {
            String[] inputParts = input.split("/", -1);
            if ("..".equals(inputParts[0]) && ONLY_PARENT_DIRECTORIES.matcher(input).find()) {
                return true;
            }
            for (int i = 0; i < patternsParts.size(); i++) {
                List<String> patternParts = patternsParts.get(i);
                List<Predicate<String>> matcher = matchers.get(i);
                int minParts = Math.min(inputParts.length, patternParts.size());
                int j = 0;
                while (j < minParts) {
                    String part = patternParts.get(j);
                    if (part.contains("/")) {
                        return true;
                    }
                    if (!matcher.get(j).test(inputParts[j])) {
                        break;
                    }
                    if ("**".equals(part)) {
                        return true;
                    }
                    j++;
                }
                if (j == inputParts.length) {
                    return true;
                }
            }
            return false;
        };
    }

    // picomatch 4.0.4 under tinyglobby's options; its errors are pnpm's
    private static Predicate<String> matcher(List<String> globs) {
        try {
            return Picomatch.matcher(globs, PICOMATCH);
        } catch (JsError e) {
            throw new JsError(JsError.ERROR, e.getMessage());
        }
    }

    static List<String> splitPattern(String path) {
        List<String> parts = PicomatchScan.scan(path, true).getParts();
        if (parts.isEmpty()) {
            List<String> whole = new ArrayList<>();
            whole.add(path);
            return whole;
        }
        return parts;
    }

    static boolean isDynamicPattern(String pattern) {
        PicomatchScan.State scan = PicomatchScan.scan(pattern, false);
        return scan.isGlob() || scan.isNegated();
    }
}
