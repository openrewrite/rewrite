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
package org.openrewrite.javascript.internal.lock;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of Node.js {@code path.posix} (resolve, normalize, join, relative, dirname, basename) and
 * {@code walk-up-path}, over a virtual filesystem whose working directory is {@code /}.
 */
public final class PosixPath {

    public static final String CWD = "/";

    private PosixPath() {
    }

    private static String normalizeString(String path, boolean allowAboveRoot) {
        StringBuilder res = new StringBuilder();
        int lastSegmentLength = 0;
        int lastSlash = -1;
        int dots = 0;
        char code = 0;
        for (int i = 0; i <= path.length(); ++i) {
            if (i < path.length()) {
                code = path.charAt(i);
            } else if (code == '/') {
                break;
            } else {
                code = '/';
            }

            if (code == '/') {
                if (lastSlash == i - 1 || dots == 1) {
                    // noop
                } else if (dots == 2) {
                    if (res.length() < 2 || lastSegmentLength != 2 ||
                        res.charAt(res.length() - 1) != '.' || res.charAt(res.length() - 2) != '.') {
                        if (res.length() > 2) {
                            int lastSlashIndex = res.lastIndexOf("/");
                            if (lastSlashIndex == -1) {
                                res.setLength(0);
                                lastSegmentLength = 0;
                            } else {
                                res.setLength(lastSlashIndex);
                                lastSegmentLength = res.length() - 1 - res.lastIndexOf("/");
                            }
                            lastSlash = i;
                            dots = 0;
                            continue;
                        } else if (res.length() != 0) {
                            res.setLength(0);
                            lastSegmentLength = 0;
                            lastSlash = i;
                            dots = 0;
                            continue;
                        }
                    }
                    if (allowAboveRoot) {
                        res.append(res.length() > 0 ? "/.." : "..");
                        lastSegmentLength = 2;
                    }
                } else {
                    if (res.length() > 0) {
                        res.append('/').append(path, lastSlash + 1, i);
                    } else {
                        res.append(path, lastSlash + 1, i);
                    }
                    lastSegmentLength = i - lastSlash - 1;
                }
                lastSlash = i;
                dots = 0;
            } else if (code == '.' && dots != -1) {
                ++dots;
            } else {
                dots = -1;
            }
        }
        return res.toString();
    }

    public static String resolve(String... args) {
        StringBuilder resolvedPath = new StringBuilder();
        boolean resolvedAbsolute = false;
        for (int i = args.length - 1; i >= -1 && !resolvedAbsolute; i--) {
            String path = i >= 0 ? args[i] : CWD;
            if (path.isEmpty()) {
                continue;
            }
            resolvedPath.insert(0, path + "/");
            resolvedAbsolute = path.charAt(0) == '/';
        }
        String normalized = normalizeString(resolvedPath.toString(), !resolvedAbsolute);
        if (resolvedAbsolute) {
            return "/" + normalized;
        }
        return !normalized.isEmpty() ? normalized : ".";
    }

    public static String normalize(String path) {
        if (path.isEmpty()) {
            return ".";
        }
        boolean isAbsolute = path.charAt(0) == '/';
        boolean trailingSeparator = path.charAt(path.length() - 1) == '/';
        String normalized = normalizeString(path, !isAbsolute);
        if (normalized.isEmpty()) {
            if (isAbsolute) {
                return "/";
            }
            return trailingSeparator ? "./" : ".";
        }
        if (trailingSeparator) {
            normalized += "/";
        }
        return isAbsolute ? "/" + normalized : normalized;
    }

    public static boolean isAbsolute(String path) {
        return !path.isEmpty() && path.charAt(0) == '/';
    }

    public static String join(String... args) {
        String joined = null;
        for (String arg : args) {
            if (!arg.isEmpty()) {
                joined = joined == null ? arg : joined + "/" + arg;
            }
        }
        return joined == null ? "." : normalize(joined);
    }

    public static String relative(String from, String to) {
        if (from.equals(to)) {
            return "";
        }
        from = resolve(from);
        to = resolve(to);
        if (from.equals(to)) {
            return "";
        }
        int fromStart = 1;
        int fromEnd = from.length();
        int fromLen = fromEnd - fromStart;
        int toStart = 1;
        int toLen = to.length() - toStart;
        int length = Math.min(fromLen, toLen);
        int lastCommonSep = -1;
        int i = 0;
        for (; i < length; i++) {
            char fromCode = from.charAt(fromStart + i);
            if (fromCode != to.charAt(toStart + i)) {
                break;
            } else if (fromCode == '/') {
                lastCommonSep = i;
            }
        }
        if (i == length) {
            if (toLen > length) {
                if (to.charAt(toStart + i) == '/') {
                    return to.substring(toStart + i + 1);
                }
                if (i == 0) {
                    return to.substring(toStart + i);
                }
            } else if (fromLen > length) {
                if (from.charAt(fromStart + i) == '/') {
                    lastCommonSep = i;
                } else if (i == 0) {
                    lastCommonSep = 0;
                }
            }
        }
        StringBuilder out = new StringBuilder();
        for (i = fromStart + lastCommonSep + 1; i <= fromEnd; ++i) {
            if (i == fromEnd || from.charAt(i) == '/') {
                out.append(out.length() == 0 ? ".." : "/..");
            }
        }
        return out + to.substring(toStart + lastCommonSep);
    }

    public static String dirname(String path) {
        if (path.isEmpty()) {
            return ".";
        }
        boolean hasRoot = path.charAt(0) == '/';
        int end = -1;
        boolean matchedSlash = true;
        for (int i = path.length() - 1; i >= 1; --i) {
            if (path.charAt(i) == '/') {
                if (!matchedSlash) {
                    end = i;
                    break;
                }
            } else {
                matchedSlash = false;
            }
        }
        if (end == -1) {
            return hasRoot ? "/" : ".";
        }
        if (hasRoot && end == 1) {
            return "//";
        }
        return path.substring(0, end);
    }

    public static String basename(String path) {
        int start = 0;
        int end = -1;
        boolean matchedSlash = true;
        for (int i = path.length() - 1; i >= 0; --i) {
            if (path.charAt(i) == '/') {
                if (!matchedSlash) {
                    start = i + 1;
                    break;
                }
            } else if (end == -1) {
                matchedSlash = false;
                end = i + 1;
            }
        }
        return end == -1 ? "" : path.substring(start, end);
    }

    public static List<String> walkUp(String path) {
        List<String> paths = new ArrayList<>();
        for (path = resolve(path); !path.isEmpty(); ) {
            paths.add(path);
            String pp = dirname(path);
            if (pp.equals(path)) {
                break;
            }
            path = pp;
        }
        return paths;
    }
}
