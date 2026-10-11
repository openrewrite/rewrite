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
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/plugin-patch sources/tools/apply.ts and tools/UnmatchedHunkError.ts (Yarn 4.10.3), over the
 * package's directory in a {@link ZipArchive}. Every entry the patch reaches keeps its time, so times are not modeled.
 */
final class PatchApply {

    private PatchApply() {
    }

    static final class UnmatchedHunkError extends RuntimeException {
        UnmatchedHunkError(int index) {
            super("Cannot apply hunk #" + (index + 1));
        }
    }

    @SuppressWarnings("unchecked")
    static void applyPatchFile(List<Map<String, Object>> effects, ZipArchive baseFs, String cwd,
                               @Nullable String version) {
        for (Map<String, Object> eff : effects) {
            Object semverExclusivity = eff.get("semverExclusivity");
            if (semverExclusivity != null && version != null &&
                !YarnpkgSemverUtils.satisfiesWithPrereleases(version, (String) semverExclusivity, false)) {
                continue;
            }
            String type = (String) eff.get("type");
            switch (type) {
                case "file deletion":
                    baseFs.unlink(resolve(cwd, (String) eff.get("path")));
                    break;
                case "rename":
                    // ZipFS's renameSync, which FakeFS's movePromise calls
                    throw new JsError(JsError.ERROR, "Unimplemented");
                case "file creation": {
                    Map<String, Object> hunk = (Map<String, Object>) eff.get("hunk");
                    String fileContents = "";
                    if (hunk != null) {
                        Map<String, Object> part = ((List<Map<String, Object>>) hunk.get("parts")).get(0);
                        fileContents = String.join("\n", (List<String>) part.get("lines")) +
                                       (Boolean.TRUE.equals(part.get("noNewlineAtEndOfFile")) ? "" : "\n");
                    }
                    String path = resolve(cwd, (String) eff.get("path"));
                    baseFs.mkdirp(PosixPath.dirname(path));
                    baseFs.writeFile(path, fileContents.getBytes(StandardCharsets.UTF_8), (Integer) eff.get("mode"));
                    break;
                }
                case "patch":
                    applyPatch((List<Map<String, Object>>) eff.get("hunks"), resolve(cwd, (String) eff.get("path")),
                            baseFs);
                    break;
                case "mode change": {
                    String path = resolve(cwd, (String) eff.get("path"));
                    Integer currentMode = baseFs.lstatMode(path);
                    if (currentMode == null) {
                        throw new JsError(JsError.ERROR, "ENOENT: no such file or directory, stat '/" + path + "'",
                                "ENOENT");
                    }
                    int newMode = (Integer) eff.get("newMode");
                    if (isExecutable(newMode) != isExecutable(currentMode)) {
                        continue;
                    }
                    baseFs.chmod(path, newMode);
                    break;
                }
                default:
                    throw new JsError(JsError.ERROR, "Assertion failed: Unexpected effect " + type);
            }
        }
    }

    private static String resolve(String cwd, String path) {
        return PosixPath.normalize(cwd + "/" + path);
    }

    private static boolean isExecutable(int fileMode) {
        return (fileMode & 0100) > 0;
    }

    private static boolean linesAreEqual(String a, String b) {
        return Js.trimEnd(a).equals(Js.trimEnd(b));
    }

    @SuppressWarnings("unchecked")
    private static void applyPatch(List<Map<String, Object>> hunks, String path, ZipArchive baseFs) {
        Integer mode = baseFs.lstatMode(path);
        if (mode == null) {
            throw new JsError(JsError.ERROR, "ENOENT: no such file or directory, stat '/" + path + "'", "ENOENT");
        }
        String fileContents = new String(baseFs.readFile(path), StandardCharsets.UTF_8);
        List<String> fileLines = new ArrayList<>(Arrays.asList(fileContents.split("\n", -1)));

        List<List<Object[]>> result = new ArrayList<>();
        int fixupOffset = 0;
        int maxFrozenLine = 0;
        for (int h = 0; h < hunks.size(); h++) {
            Map<String, Object> hunk = hunks.get(h);
            Map<String, Object> header = (Map<String, Object>) hunk.get("header");
            int patchedStart = ((Double) ((Map<String, Object>) header.get("patched")).get("start")).intValue();
            int originalLength = ((Double) ((Map<String, Object>) header.get("original")).get("length")).intValue();

            int firstGuess = Math.max(maxFrozenLine, patchedStart + fixupOffset);
            int maxPrefixFuzz = Math.max(0, firstGuess - maxFrozenLine);
            int maxSuffixFuzz = Math.max(0, fileLines.size() - firstGuess - originalLength);
            int maxFuzz = Math.max(maxPrefixFuzz, maxSuffixFuzz);

            int offset = 0;
            int location = 0;
            List<Object[]> modifications = null;
            while (offset <= maxFuzz) {
                if (offset <= maxPrefixFuzz) {
                    location = firstGuess - offset;
                    modifications = evaluateHunk(hunk, fileLines, location);
                    if (modifications != null) {
                        offset = -offset;
                        break;
                    }
                }
                if (offset <= maxSuffixFuzz) {
                    location = firstGuess + offset;
                    modifications = evaluateHunk(hunk, fileLines, location);
                    if (modifications != null) {
                        break;
                    }
                }
                offset += 1;
            }
            if (modifications == null) {
                throw new UnmatchedHunkError(h);
            }
            result.add(modifications);
            fixupOffset += offset;
            maxFrozenLine = location + originalLength;
        }

        int diffOffset = 0;
        for (List<Object[]> modifications : result) {
            for (Object[] modification : modifications) {
                switch ((String) modification[0]) {
                    case "splice": {
                        int firstLine = Math.min((Integer) modification[1] + diffOffset, fileLines.size());
                        int numToDelete = (Integer) modification[2];
                        List<String> linesToInsert = (List<String>) modification[3];
                        List<String> deleted = fileLines.subList(firstLine,
                                Math.min(firstLine + numToDelete, fileLines.size()));
                        deleted.clear();
                        fileLines.addAll(firstLine, linesToInsert);
                        diffOffset += linesToInsert.size() - numToDelete;
                        break;
                    }
                    case "pop":
                        if (!fileLines.isEmpty()) {
                            fileLines.remove(fileLines.size() - 1);
                        }
                        break;
                    default:
                        fileLines.add((String) modification[1]);
                        break;
                }
            }
        }
        baseFs.writeFile(path, String.join("\n", fileLines).getBytes(StandardCharsets.UTF_8), mode);
    }

    /**
     * The hunk's modifications at that line, as {@code ["splice", index, numToDelete, linesToInsert]},
     * {@code ["pop"]} or {@code ["push", line]}; null when its context or deletions do not match there.
     */
    @SuppressWarnings("unchecked")
    private static @Nullable List<Object[]> evaluateHunk(Map<String, Object> hunk, List<String> fileLines,
                                                         int offset) {
        List<Object[]> result = new ArrayList<>();
        for (Map<String, Object> part : (List<Map<String, Object>>) hunk.get("parts")) {
            String type = (String) part.get("type");
            List<String> lines = (List<String>) part.get("lines");
            boolean noNewlineAtEndOfFile = Boolean.TRUE.equals(part.get("noNewlineAtEndOfFile"));
            if (PatchParse.CONTEXT.equals(type) || PatchParse.DELETION.equals(type)) {
                for (String line : lines) {
                    String originalLine = offset < fileLines.size() ? fileLines.get(offset) : null;
                    if (originalLine == null || !linesAreEqual(originalLine, line)) {
                        return null;
                    }
                    offset += 1;
                }
                if (PatchParse.DELETION.equals(type)) {
                    result.add(new Object[]{"splice", offset - lines.size(), lines.size(), Collections.emptyList()});
                    if (noNewlineAtEndOfFile) {
                        result.add(new Object[]{"push", ""});
                    }
                }
            } else {
                result.add(new Object[]{"splice", offset, 0, lines});
                if (noNewlineAtEndOfFile) {
                    result.add(new Object[]{"pop"});
                }
            }
        }
        return result;
    }
}
