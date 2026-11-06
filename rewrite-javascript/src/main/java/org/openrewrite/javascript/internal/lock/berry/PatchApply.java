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
import org.openrewrite.javascript.internal.lock.V8RegExp;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Port of plugin-patch/tools/apply.ts, including hunk search, newline markers and optional rollback. */
final class PatchApply {
    private static final Pattern TRAILING_SPACE = V8RegExp.of("\\s+$");

    static final class UnmatchedHunk extends RuntimeException {
        UnmatchedHunk(int index) {
            super("Cannot apply hunk #" + (index + 1));
        }
    }

    private PatchApply() {
    }

    @SuppressWarnings("unchecked")
    static void apply(List<Map<String, Object>> effects, ZipArchive archive, String prefix, @Nullable String version) {
        for (Map<String, Object> effect : effects) {
            String range = (String) effect.get("semverExclusivity");
            if (range != null && version != null && !YarnpkgSemverUtils.satisfiesWithPrereleases(version, range, false)) continue;
            String path = prefix + effect.get("path");
            String type = (String) effect.get("type");
            if ("rename".equals(type)) {
                archive.requireNoSymlinkInPath(prefix + effect.get("fromPath"));
                archive.requireNoSymlinkInPath(prefix + effect.get("toPath"));
            } else {
                archive.requireNoSymlinkInPath(path);
            }
            switch (type) {
                case "file deletion":
                    archive.remove(path);
                    break;
                case "rename":
                    archive.rename(prefix + effect.get("fromPath"), prefix + effect.get("toPath"));
                    break;
                case "file creation": {
                    Map<String, Object> hunk = (Map<String, Object>) effect.get("hunk");
                    String content = "";
                    if (hunk != null) {
                        Map<String, Object> part = ((List<Map<String, Object>>) hunk.get("parts")).get(0);
                        content = String.join("\n", (List<String>) part.get("lines")) +
                                (Boolean.TRUE.equals(part.get("noNewlineAtEndOfFile")) ? "" : "\n");
                    }
                    String parent = path.substring(0, path.lastIndexOf('/'));
                    archive.mkdirp(parent);
                    archive.writeFile(path, content.getBytes(StandardCharsets.UTF_8), ((Number) effect.get("mode")).intValue());
                    break;
                }
                case "patch": {
                    ZipArchive.Entry entry = archive.entry(path);
                    String content = patch(new String(entry.data, StandardCharsets.UTF_8),
                            (List<Map<String, Object>>) effect.get("hunks"));
                    entry.data = content.getBytes(StandardCharsets.UTF_8);
                    break;
                }
                case "mode change": {
                    ZipArchive.Entry entry = archive.entry(path);
                    int mode = ((Number) effect.get("newMode")).intValue();
                    // This equality test is intentional: it is the release's apply.ts behavior.
                    if ((mode & 0100) == (entry.mode & 0100)) entry.mode = entry.mode & ~0777 | mode & 0777;
                    break;
                }
                default:
                    throw new IllegalStateException("Unexpected patch effect " + effect.get("type"));
            }
        }
    }

    private static final class Modification {
        final int index;
        final int delete;
        final List<String> insert;
        final int newline; // 1: push empty, -1: pop, 0: splice

        Modification(int index, int delete, List<String> insert, int newline) {
            this.index = index;
            this.delete = delete;
            this.insert = insert;
            this.newline = newline;
        }
    }

    @SuppressWarnings("unchecked")
    private static String patch(String content, List<Map<String, Object>> hunks) {
        List<String> lines = new ArrayList<>(Arrays.asList(content.split("\n", -1)));
        List<List<Modification>> result = new ArrayList<>();
        int fixupOffset = 0;
        int maxFrozenLine = 0;
        for (int n = 0; n < hunks.size(); n++) {
            Map<String, Object> hunk = hunks.get(n);
            Map<String, Object> header = (Map<String, Object>) hunk.get("header");
            Map<String, Object> original = (Map<String, Object>) header.get("original");
            Map<String, Object> patched = (Map<String, Object>) header.get("patched");
            int length = ((Number) original.get("length")).intValue();
            int guess = Math.max(maxFrozenLine, ((Number) patched.get("start")).intValue() + fixupOffset);
            int prefixFuzz = Math.max(0, guess - maxFrozenLine);
            int suffixFuzz = Math.max(0, lines.size() - guess - length);
            int maxFuzz = Math.max(prefixFuzz, suffixFuzz);
            int location = 0;
            int offset;
            List<Modification> modifications = null;
            for (offset = 0; offset <= maxFuzz; offset++) {
                if (offset <= prefixFuzz) {
                    location = guess - offset;
                    modifications = evaluate(hunk, lines, location);
                    if (modifications != null) { offset = -offset; break; }
                }
                if (offset <= suffixFuzz) {
                    location = guess + offset;
                    modifications = evaluate(hunk, lines, location);
                    if (modifications != null) break;
                }
            }
            if (modifications == null) throw new UnmatchedHunk(n);
            result.add(modifications);
            fixupOffset += offset;
            maxFrozenLine = location + length;
        }
        int diffOffset = 0;
        for (List<Modification> modifications : result) {
            for (Modification m : modifications) {
                if (m.newline == 1) {
                    lines.add("");
                } else if (m.newline == -1) {
                    if (!lines.isEmpty()) lines.remove(lines.size() - 1);
                } else {
                    int first = Math.min(lines.size(), Math.max(0, m.index + diffOffset));
                    int end = Math.min(lines.size(), first + m.delete);
                    lines.subList(first, end).clear();
                    lines.addAll(first, m.insert);
                    diffOffset += m.insert.size() - m.delete;
                }
            }
        }
        return String.join("\n", lines);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable List<Modification> evaluate(Map<String, Object> hunk, List<String> file, int offset) {
        List<Modification> result = new ArrayList<>();
        for (Map<String, Object> part : (List<Map<String, Object>>) hunk.get("parts")) {
            String type = (String) part.get("type");
            List<String> lines = (List<String>) part.get("lines");
            boolean noNewline = Boolean.TRUE.equals(part.get("noNewlineAtEndOfFile"));
            if (PatchParse.CONTEXT.equals(type) || PatchParse.DELETION.equals(type)) {
                for (String line : lines) {
                    if (offset >= file.size() || !trimRight(file.get(offset)).equals(trimRight(line))) return null;
                    offset++;
                }
                if (PatchParse.DELETION.equals(type)) {
                    result.add(new Modification(offset - lines.size(), lines.size(), Collections.emptyList(), 0));
                    if (noNewline) result.add(new Modification(0, 0, Collections.emptyList(), 1));
                }
            } else if (PatchParse.INSERTION.equals(type)) {
                result.add(new Modification(offset, 0, lines, 0));
                if (noNewline) result.add(new Modification(0, 0, Collections.emptyList(), -1));
            } else {
                throw new IllegalStateException("Unexpected patch part " + type);
            }
        }
        return result;
    }

    private static String trimRight(String s) {
        return TRAILING_SPACE.matcher(s).replaceAll("");
    }
}
