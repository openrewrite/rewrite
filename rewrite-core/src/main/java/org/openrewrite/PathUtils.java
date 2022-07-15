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

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class PathUtils {
    private PathUtils() {
    }

    private static final String UNIX_SEPARATOR = "/";

    private static final String WINDOWS_SEPARATOR = "\\";

    /**
     * Compare two paths, returning true if they indicate the same path, regardless of separators.
     * Does not account for comparison of a relative path to an absolute path, but within the context of OpenRewrite
     * all paths should be relative anyway.
     *
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
        return path.contains(WINDOWS_SEPARATOR) ? path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR) : path;
    }

    public static String separatorsToWindows(String path) {
        return path.contains(UNIX_SEPARATOR) ? path.replace(UNIX_SEPARATOR, WINDOWS_SEPARATOR) : path;
    }

    public static String separatorsToSystem(String path) {
        if (FileSystems.getDefault().getSeparator().equals(WINDOWS_SEPARATOR)) {
            return separatorsToWindows(path);
        }
        return separatorsToUnix(path);
    }
}
