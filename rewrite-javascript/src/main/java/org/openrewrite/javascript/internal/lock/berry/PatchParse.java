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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/plugin-patch sources/tools/parse.ts (Yarn 4.10.3).
 */
final class PatchParse {

    static final int NON_EXECUTABLE_FILE_MODE = 0644;
    static final int EXECUTABLE_FILE_MODE = 0755;

    static final String CONTEXT = "context";
    static final String INSERTION = "insertion";
    static final String DELETION = "deletion";

    private static final Pattern HEADER_REGEXP = V8RegExp.of("^@@ -(\\d+)(,(\\d+))? \\+(\\d+)(,(\\d+))? @@.*");
    private static final Pattern DIFF_LINE = V8RegExp.of("^diff --git a\\/(.*?) b\\/(.*?)\\s*$");
    private static final Pattern INDEX_HASHES = V8RegExp.of("(\\w+)\\.\\.(\\w+)");

    private PatchParse() {
    }

    private static final class FileDeets {
        @Nullable String semverExclusivity;
        @Nullable String diffLineFromPath;
        @Nullable String diffLineToPath;
        @Nullable String oldMode;
        @Nullable String newMode;
        @Nullable String deletedFileMode;
        @Nullable String newFileMode;
        @Nullable String renameFrom;
        @Nullable String renameTo;
        @Nullable String beforeHash;
        @Nullable String afterHash;
        @Nullable String fromPath;
        @Nullable String toPath;
        @Nullable List<Map<String, Object>> hunks;
    }

    static String getPath(String p) {
        String resolved = PosixPath.normalize("/" + p);
        if (resolved.length() > 1 && resolved.endsWith("/")) {
            resolved = resolved.substring(0, resolved.length() - 1);
        }
        return resolved.substring(1);
    }

    static Map<String, Object> parseHunkHeaderLine(String headerLine) {
        Matcher match = HEADER_REGEXP.matcher(Js.trim(headerLine));
        if (!match.find()) {
            throw new JsError(JsError.ERROR, "Bad header line: '" + headerLine + "'");
        }
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("start", Math.max(Js.toNumber(match.group(1)), 1));
        original.put("length", match.group(3) == null ? 1.0 : Js.toNumber(match.group(3)));
        Map<String, Object> patched = new LinkedHashMap<>();
        patched.put("start", Math.max(Js.toNumber(match.group(4)), 1));
        patched.put("length", match.group(6) == null ? 1.0 : Js.toNumber(match.group(6)));
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("original", original);
        header.put("patched", patched);
        return header;
    }

    private static Map<String, Object> emptyHunk(String headerLine) {
        Map<String, Object> hunk = new LinkedHashMap<>();
        hunk.put("header", parseHunkHeaderLine(headerLine));
        hunk.put("parts", new ArrayList<Map<String, Object>>());
        return hunk;
    }

    private static @Nullable String hunkLinetype(String line) {
        if (line.isEmpty()) {
            return CONTEXT;
        }
        switch (line.charAt(0)) {
            case '@':
                return "header";
            case '-':
                return DELETION;
            case '+':
                return INSERTION;
            case ' ':
                return CONTEXT;
            case '\\':
                return "pragma";
            default:
                return null;
        }
    }

    private static final class LinesParser {
        final List<FileDeets> result = new ArrayList<>();
        FileDeets currentFilePatch = new FileDeets();
        @Nullable Map<String, Object> currentHunk;
        @Nullable Map<String, Object> currentHunkMutationPart;

        @SuppressWarnings("unchecked")
        void commitHunk() {
            if (currentHunk != null) {
                if (currentHunkMutationPart != null) {
                    ((List<Map<String, Object>>) currentHunk.get("parts")).add(currentHunkMutationPart);
                    currentHunkMutationPart = null;
                }
                if (currentFilePatch.hunks == null) {
                    throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of null (reading 'push')");
                }
                currentFilePatch.hunks.add(currentHunk);
                currentHunk = null;
            }
        }

        void commitFilePatch() {
            commitHunk();
            result.add(currentFilePatch);
            currentFilePatch = new FileDeets();
        }

        @SuppressWarnings("unchecked")
        List<FileDeets> parse(List<String> lines) {
            boolean parsingHeader = true;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (parsingHeader) {
                    if (line.startsWith("@@")) {
                        parsingHeader = false;
                        currentFilePatch.hunks = new ArrayList<>();
                        i -= 1;
                    } else if (line.startsWith("diff --git ")) {
                        if (currentFilePatch.diffLineFromPath != null && !currentFilePatch.diffLineFromPath.isEmpty()) {
                            commitFilePatch();
                        }
                        Matcher match = DIFF_LINE.matcher(line);
                        if (!match.find()) {
                            throw new JsError(JsError.ERROR, "Bad diff line: " + line);
                        }
                        currentFilePatch.diffLineFromPath = match.group(1);
                        currentFilePatch.diffLineToPath = match.group(2);
                    } else if (line.startsWith("old mode ")) {
                        currentFilePatch.oldMode = Js.trim(Js.slice(line, "old mode ".length()));
                    } else if (line.startsWith("new mode ")) {
                        currentFilePatch.newMode = Js.trim(Js.slice(line, "new mode ".length()));
                    } else if (line.startsWith("deleted file mode ")) {
                        currentFilePatch.deletedFileMode = Js.trim(Js.slice(line, "deleted file mode ".length()));
                    } else if (line.startsWith("new file mode ")) {
                        currentFilePatch.newFileMode = Js.trim(Js.slice(line, "new file mode ".length()));
                    } else if (line.startsWith("rename from ")) {
                        currentFilePatch.renameFrom = Js.trim(Js.slice(line, "rename from ".length()));
                    } else if (line.startsWith("rename to ")) {
                        currentFilePatch.renameTo = Js.trim(Js.slice(line, "rename to ".length()));
                    } else if (line.startsWith("index ")) {
                        Matcher match = INDEX_HASHES.matcher(line);
                        if (!match.find()) {
                            continue;
                        }
                        currentFilePatch.beforeHash = match.group(1);
                        currentFilePatch.afterHash = match.group(2);
                    } else if (line.startsWith("semver exclusivity ")) {
                        currentFilePatch.semverExclusivity = Js.trim(Js.slice(line, "semver exclusivity ".length()));
                    } else if (line.startsWith("--- ")) {
                        currentFilePatch.fromPath = Js.trim(Js.slice(line, "--- a/".length()));
                    } else if (line.startsWith("+++ ")) {
                        currentFilePatch.toPath = Js.trim(Js.slice(line, "+++ b/".length()));
                    }
                    continue;
                }
                String lineType = hunkLinetype(line);
                if (lineType == null) {
                    parsingHeader = true;
                    commitFilePatch();
                    i -= 1;
                } else if ("header".equals(lineType)) {
                    commitHunk();
                    currentHunk = emptyHunk(line);
                } else if ("pragma".equals(lineType)) {
                    if (!line.startsWith("\\ No newline at end of file")) {
                        throw new JsError(JsError.ERROR, "Unrecognized pragma in patch file: " + line);
                    }
                    if (currentHunkMutationPart == null) {
                        throw new JsError(JsError.ERROR, "Bad parser state: No newline at EOF pragma encountered " +
                                                         "without context");
                    }
                    currentHunkMutationPart.put("noNewlineAtEndOfFile", true);
                } else {
                    if (currentHunk == null) {
                        throw new JsError(JsError.ERROR, "Bad parser state: Hunk lines encountered before hunk header");
                    }
                    if (currentHunkMutationPart != null && !lineType.equals(currentHunkMutationPart.get("type"))) {
                        ((List<Map<String, Object>>) currentHunk.get("parts")).add(currentHunkMutationPart);
                        currentHunkMutationPart = null;
                    }
                    if (currentHunkMutationPart == null) {
                        currentHunkMutationPart = new LinkedHashMap<>();
                        currentHunkMutationPart.put("type", lineType);
                        currentHunkMutationPart.put("lines", new ArrayList<String>());
                        currentHunkMutationPart.put("noNewlineAtEndOfFile", false);
                    }
                    ((List<String>) currentHunkMutationPart.get("lines")).add(Js.slice(line, 1));
                }
            }
            commitFilePatch();
            for (FileDeets file : result) {
                if (file.hunks != null) {
                    for (Map<String, Object> hunk : file.hunks) {
                        verifyHunkIntegrity(hunk);
                    }
                }
            }
            return result;
        }
    }

    static List<Map<String, Object>> interpretParsedPatchFile(List<FileDeets> files) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (FileDeets file : files) {
            boolean hasHunks = file.hunks != null && !file.hunks.isEmpty();
            String type = truthy(file.renameFrom) ? "rename" : truthy(file.deletedFileMode) ? "file deletion" :
                    truthy(file.newFileMode) ? "file creation" : hasHunks ? "patch" : "mode change";
            String destinationFilePath = null;
            switch (type) {
                case "rename": {
                    if (!truthy(file.renameFrom) || !truthy(file.renameTo)) {
                        throw new JsError(JsError.ERROR, "Bad parser state: rename from & to not given");
                    }
                    Map<String, Object> effect = effect(type, file.semverExclusivity);
                    effect.put("fromPath", getPath(file.renameFrom));
                    effect.put("toPath", getPath(file.renameTo));
                    result.add(effect);
                    destinationFilePath = file.renameTo;
                    break;
                }
                case "file deletion":
                case "file creation": {
                    boolean deletion = "file deletion".equals(type);
                    String path = deletion ? or(file.diffLineFromPath, file.fromPath) :
                            or(file.diffLineToPath, file.toPath);
                    if (!truthy(path)) {
                        throw new JsError(JsError.ERROR, "Bad parse state: no path given for " + type);
                    }
                    Map<String, Object> effect = effect(type, file.semverExclusivity);
                    effect.put("hunk", hasHunks ? file.hunks.get(0) : null);
                    effect.put("path", getPath(path));
                    effect.put("mode", parseFileMode(deletion ? file.deletedFileMode : file.newFileMode));
                    effect.put("hash", deletion ? file.beforeHash : file.afterHash);
                    result.add(effect);
                    break;
                }
                default:
                    destinationFilePath = or(file.toPath, file.diffLineToPath);
                    break;
            }
            if (truthy(destinationFilePath) && truthy(file.oldMode) && truthy(file.newMode) &&
                !file.oldMode.equals(file.newMode)) {
                Map<String, Object> effect = effect("mode change", file.semverExclusivity);
                effect.put("path", getPath(destinationFilePath));
                effect.put("oldMode", parseFileMode(file.oldMode));
                effect.put("newMode", parseFileMode(file.newMode));
                result.add(effect);
            }
            if (truthy(destinationFilePath) && hasHunks) {
                Map<String, Object> effect = effect("patch", file.semverExclusivity);
                effect.put("path", getPath(destinationFilePath));
                effect.put("hunks", file.hunks);
                effect.put("beforeHash", file.beforeHash);
                effect.put("afterHash", file.afterHash);
                result.add(effect);
            }
        }
        if (result.isEmpty()) {
            throw new JsError(JsError.ERROR, "Unable to parse patch file: No changes found. Make sure the patch is a " +
                                             "valid UTF8 encoded string");
        }
        return result;
    }

    private static Map<String, Object> effect(String type, @Nullable String semverExclusivity) {
        Map<String, Object> effect = new LinkedHashMap<>();
        effect.put("type", type);
        effect.put("semverExclusivity", semverExclusivity);
        return effect;
    }

    private static boolean truthy(@Nullable String s) {
        return s != null && !s.isEmpty();
    }

    // JavaScript's a || b over strings and null
    private static @Nullable String or(@Nullable String a, @Nullable String b) {
        return truthy(a) ? a : b;
    }

    private static int parseFileMode(@Nullable String mode) {
        int parsedMode = Js.toInt32(Js.parseInt(String.valueOf(mode), 8)) & 0777;
        if (parsedMode != NON_EXECUTABLE_FILE_MODE && parsedMode != EXECUTABLE_FILE_MODE) {
            throw new JsError(JsError.ERROR, "Unexpected file mode string: " + mode);
        }
        return parsedMode;
    }

    static List<Map<String, Object>> parsePatchFile(String file) {
        List<String> lines = new ArrayList<>(Arrays.asList(file.split("\n", -1)));
        if (lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return interpretParsedPatchFile(new LinesParser().parse(lines));
    }

    @SuppressWarnings("unchecked")
    static void verifyHunkIntegrity(Map<String, Object> hunk) {
        double originalLength = 0;
        double patchedLength = 0;
        for (Map<String, Object> part : (List<Map<String, Object>>) hunk.get("parts")) {
            int lines = ((List<String>) part.get("lines")).size();
            String type = (String) part.get("type");
            if (CONTEXT.equals(type)) {
                patchedLength += lines;
                originalLength += lines;
            } else if (DELETION.equals(type)) {
                originalLength += lines;
            } else {
                patchedLength += lines;
            }
        }
        Map<String, Object> header = (Map<String, Object>) hunk.get("header");
        double expectedOriginal = (Double) ((Map<String, Object>) header.get("original")).get("length");
        double expectedPatched = (Double) ((Map<String, Object>) header.get("patched")).get("length");
        if (originalLength != expectedOriginal || patchedLength != expectedPatched) {
            throw new JsError(JsError.ERROR, "hunk header integrity check failed (expected @@ " +
                                             format(expectedOriginal) + " " + format(expectedPatched) + " @@, got @@ " +
                                             format(originalLength) + " " + format(patchedLength) + " @@)");
        }
    }

    private static String format(double n) {
        return n < 0 ? Js.numberToString(n) : "+" + Js.numberToString(n);
    }
}
