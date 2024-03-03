/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

public class PathUtils {
    private PathUtils() {
    }

    private static final char UNIX_SEPARATOR = '/';

    private static final char WINDOWS_SEPARATOR = '\\';

    /**
     * Compare two paths, returning true if they indicate the same path, regardless of separators.
     * Does not account for comparison of a relative path to an absolute path, but within the context of OpenRewrite
     * all paths should be relative anyway.
     * "foo/a.txt" is considered to be equal to "foo\a.txt"
     */
    public static boolean equalIgnoringSeparators(Path a, Path b) {
        return equalIgnoringSeparators(a.normalize().toString(), b.normalize().toString());
    }

    /**
     * Compare two strings representing file paths, returning true if they indicate the same path regardless of separators
     */
    public static boolean equalIgnoringSeparators(String a, String b) {
        return separatorsToSystem(a).equals(separatorsToSystem(b));
    }

    public static String separatorsToUnix(String path) {
        return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
    }

    public static String separatorsToWindows(String path) {
        return path.replace(UNIX_SEPARATOR, WINDOWS_SEPARATOR);
    }

    public static String separatorsToSystem(String path) {
        if (File.separatorChar == WINDOWS_SEPARATOR) {
            return separatorsToWindows(path);
        }
        return separatorsToUnix(path);
    }

    public static boolean matchesGlob(@Nullable Path path, @Nullable String globPattern) {
        if ("**".equals(globPattern)) {
            return true;
        }
        if (globPattern == null || path == null) {
            return false;
        }

        String relativePath = path.toString();
        if (relativePath.isEmpty() && globPattern.isEmpty()) {
            return true;
        }

        List<String> eitherOrPatterns = getEitherOrPatterns(globPattern);
        List<String> excludedPatterns = getExcludedPatterns(globPattern);
        if (eitherOrPatterns.isEmpty() && excludedPatterns.isEmpty()) {
            return matchesGlob(globPattern, relativePath);
        } else if (!eitherOrPatterns.isEmpty()) {
            for (String eitherOrPattern : eitherOrPatterns) {
                if (matchesGlob(Paths.get(relativePath), eitherOrPattern)) {
                    return true;
                }
            }
            return false;
        } else { // If eitherOrPatterns is empty and excludedPatterns is not
            if (!matchesGlob(convertNegationToWildcard(globPattern), relativePath)) {
                return false;
            }
            for (String excludedPattern : excludedPatterns) {
                if (matchesGlob(excludedPattern, relativePath)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean matchesGlob(String pattern, String path) {
        String[] pattTokens = tokenize(pattern);
        String[] pathTokens = tokenize(path);
        int pattIdxStart = 0;
        int pattIdxEnd = pattTokens.length - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = pathTokens.length - 1;

        // Process characters before first **
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            if ("**".equals(pattTokens[pattIdxStart])) {
                break;
            }
            if (!StringUtils.matchesGlob(pathTokens[pathIdxStart], pattTokens[pattIdxStart])) {
                return false;
            }
            pattIdxStart++;
            pathIdxStart++;
        }
        if (pathIdxStart > pathIdxEnd) {
            // Path exhausted
            if (pattIdxStart > pattIdxEnd) {
                return !isFileSeparator(pattern.charAt(pattern.length() - 1));
            }
            if (pattIdxStart == pattIdxEnd && pattTokens[pattIdxStart].equals("*") && isFileSeparator(path.charAt(path.length() - 1))) {
                return true;
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattTokens[i].equals("**")) {
                    return false;
                }
            }
            return true;
        } else if (pattIdxStart > pattIdxEnd) {
            // Path not exhausted, but pattern is. Failure.
            return false;
        }

        // Process characters after last **
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            if ("**".equals(pattTokens[pattIdxEnd])) {
                break;
            }
            if (!StringUtils.matchesGlob(pathTokens[pathIdxEnd], pattTokens[pattIdxEnd])) {
                return false;
            }
            if (pattIdxEnd == (pattTokens.length - 1)
                && (isFileSeparator(pattern.charAt(pattern.length() - 1)) ^ isFileSeparator(path.charAt(path.length() - 1)))) {
                return false;
            }
            pattIdxEnd--;
            pathIdxEnd--;
        }
        if (pathIdxStart > pathIdxEnd) {
            // Path exhausted
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattTokens[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }

        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                if (pattTokens[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == pattIdxStart + 1) {
                // **/** situation, so skip one
                pattIdxStart++;
                continue;
            }
            // Find the pattern between pattIdxStart & pattIdxTmp in path between
            // pathIdxStart & pathIdxEnd
            int patLength = (patIdxTmp - pattIdxStart - 1);
            int strLength = (pathIdxEnd - pathIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    if (!StringUtils.matchesGlob(pathTokens[pathIdxStart + i + j], pattTokens[pattIdxStart + j + 1])) {
                        continue strLoop;
                    }
                }
                foundIdx = pathIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }
            pattIdxStart = patIdxTmp;
            pathIdxStart = foundIdx + patLength;
        }

        for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
            if (!pattTokens[i].equals("**")) {
                return false;
            }
        }
        return true;
    }

    public static String convertNegationToWildcard(String globPattern) {
        // Regular expression to match !(...)
        String negationPattern = "\\!\\((.*?)\\)";
        // Replace all negation patterns with *
        return globPattern.replaceAll(negationPattern, "*");
    }

    public static List<String> getExcludedPatterns(String globPattern) {
        if (!globPattern.contains("!")) {
            return emptyList();
        }

        List<String> excludedPatterns = new ArrayList<>(3);

        // Regular expression to match !(...)
        String negationPattern = "\\!\\((.*?)\\)";
        Pattern pattern = Pattern.compile(negationPattern);
        Matcher matcher = pattern.matcher(globPattern);

        // Find all negation patterns and generate excluded patterns
        while (matcher.find()) {
            String negationContent = matcher.group(1);
            String[] options = negationContent.split("\\|");
            for (String option : options) {
                excludedPatterns.add(globPattern.replace(matcher.group(), option));
            }
        }

        return excludedPatterns;
    }

    public static List<String> getEitherOrPatterns(String globPattern) {
        if (!globPattern.contains("{")) {
            return emptyList();
        }

        List<String> eitherOrPatterns = new ArrayList<>(3);

        // Regular expression to match {...}
        String eitherOrPattern = "\\{(.*?)\\}";
        Pattern pattern = Pattern.compile(eitherOrPattern);
        Matcher matcher = pattern.matcher(globPattern);

        // Find all possible patterns and generate patterns
        while (matcher.find()) {
            String eitherOrContent = matcher.group(1);
            String[] options = eitherOrContent.split("\\,");
            for (String option : options) {
                eitherOrPatterns.add(globPattern.replace(matcher.group(), option));
            }
        }

        return eitherOrPatterns;
    }

    private static String[] tokenize(String path) {
        List<String> tokens = new ArrayList<>();
        int pathIdxStart = 0;
        int pathIdxTmp = 0;
        int pathIdxEnd = path.length() - 1;
        while (pathIdxTmp <= pathIdxEnd) {
            if (isFileSeparator(path.charAt(pathIdxTmp))) {
                tokens.add(path.substring(pathIdxStart, pathIdxTmp));
                pathIdxStart = pathIdxTmp + 1;
            }
            pathIdxTmp++;
        }
        if (pathIdxStart == 0) {
            tokens.add(path);
        } else if (pathIdxStart != pathIdxTmp) {
            tokens.add(path.substring(pathIdxStart, pathIdxTmp));
        }
        return tokens.toArray(new String[0]);
    }

    private static boolean isFileSeparator(char ch) {
        return isFileSeparator(false, ch);
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean isFileSeparator(boolean strict, char ch) {
        return strict
                ? ch == File.separatorChar
                : ch == UNIX_SEPARATOR || ch == WINDOWS_SEPARATOR;
    }
}
