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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.StripBom;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Port of @pnpm/lockfile.fs src/read.ts, src/yamlDocuments.ts and src/gitMergeFile.ts (pnpm 11.2.2): the text of
 * pnpm-lock.yaml to a {@link LockfileObject}.
 */
public final class LockfileReader {

    static final String WANTED_LOCKFILE = "pnpm-lock.yaml";
    static final String YAML_DOCUMENT_SEPARATOR = "\n---\n";
    static final String YAML_DOCUMENT_START = "---\n";

    private static final String MERGE_CONFLICT_PARENT = "|||||||";
    private static final String MERGE_CONFLICT_END = ">>>>>>>";
    private static final String MERGE_CONFLICT_THEIRS = "=======";
    private static final String MERGE_CONFLICT_OURS = "<<<<<<<";

    private LockfileReader() {
    }

    @Value
    static class ReadResult {
        @Nullable
        LockfileObject lockfile;

        boolean hadConflicts;
    }

    /**
     * pnpm discards the lock and resolves everything from scratch; the message is what it warns
     * ({@code Ignoring broken lockfile at <dir>: <message>}).
     */
    static final class IgnoredLockfile extends RuntimeException {
        IgnoredLockfile(String message) {
            super(message);
        }
    }

    /**
     * {@code _read} as an install reads the wanted lock; every error {@code readLockfiles} warns about is an
     * {@link IgnoredLockfile}.
     */
    static ReadResult read(String text) {
        return read(text, PnpmRelease.PORTED);
    }

    /**
     * {@link #read(String)} as {@code release} reads the lock.
     */
    static ReadResult read(String text, PnpmRelease release) {
        String lockfileRawContent = extractMainDocument(StripBom.stripBom(text), release);
        if (Js.trim(lockfileRawContent).isEmpty()) {
            return new ReadResult(null, false);
        }
        LockfileObject lockfile;
        boolean hadConflicts;
        try {
            try {
                lockfile = LockfileFormatConverters.convertToLockfileObject(JsYaml.load(lockfileRawContent),
                        release);
                hadConflicts = false;
            } catch (JsYaml.YamlException | LockfileFormatConverters.TypeError e) {
                if (!isDiff(lockfileRawContent)) {
                    throw new IgnoredLockfile("The lockfile at \"" + WANTED_LOCKFILE + "\" is broken: " +
                                              e.getMessage());
                }
                hadConflicts = true;
                lockfile = autofixMergeConflicts(lockfileRawContent, release);
            }
            Object version = lockfile.getLockfileVersion();
            String lockfileSemver = comverToSemver(Js.string(version == null || version == Js.UNDEFINED ?
                    0.0 : version));
            if (semverMajor(lockfileSemver) != semverMajor(comverToSemver(LockfileObject.LOCKFILE_VERSION))) {
                throw new IgnoredLockfile("Lockfile " + WANTED_LOCKFILE + " not compatible with current pnpm");
            }
            return new ReadResult(lockfile, hadConflicts);
        } catch (JsYaml.YamlException | LockfileFormatConverters.TypeError | IllegalArgumentException e) {
            throw new IgnoredLockfile(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    static String comverToSemver(String comver) {
        if (!comver.contains(".")) {
            return comver + ".0.0";
        }
        return comver + ".0";
    }

    /**
     * node-semver's {@code major}; an IllegalArgumentException where node-semver throws its TypeError.
     */
    static long semverMajor(String version) {
        NodeSemver.compare(version, version, NodeSemver.Options.DEFAULT);
        String valid = NodeSemver.valid(version, NodeSemver.Options.DEFAULT);
        if (valid == null) {
            throw new IllegalArgumentException("Invalid Version: " + version);
        }
        return Long.parseLong(valid.substring(0, valid.indexOf('.')));
    }

    /**
     * The main document of a lock that may start with the env document.
     */
    public static String extractMainDocument(String content) {
        return extractMainDocument(content, PnpmRelease.PORTED);
    }

    /**
     * {@link #extractMainDocument(String)} as {@code release} reads it: CRLF line ends are read as LF from 11.1.3
     * ({@link PnpmRelease#readsACrLfLockWithAnEnvDocument}).
     */
    static String extractMainDocument(String content, PnpmRelease release) {
        if (content.contains("\r\n") && release.readsACrLfLockWithAnEnvDocument()) {
            content = content.replace("\r\n", "\n");
        }
        if (!content.startsWith(YAML_DOCUMENT_START)) {
            return content;
        }
        int sep = content.indexOf(YAML_DOCUMENT_SEPARATOR, YAML_DOCUMENT_START.length());
        if (sep == -1) {
            return "";
        }
        return content.substring(sep + YAML_DOCUMENT_SEPARATOR.length());
    }

    /**
     * {@code streamReadFirstYamlDocument} over the lock's text: the env document, null when there is none.
     */
    static @Nullable String readFirstYamlDocument(@Nullable String text) {
        return readFirstYamlDocument(text, PnpmRelease.PORTED);
    }

    /**
     * {@link #readFirstYamlDocument(String)} as {@code release} reads it: CRLF line ends are read as LF from 11.1.3.
     */
    static @Nullable String readFirstYamlDocument(@Nullable String text, PnpmRelease release) {
        if (text == null) {
            return null;
        }
        String buffer = StripBom.stripBom(text);
        if (buffer.contains("\r\n") && release.readsACrLfLockWithAnEnvDocument()) {
            buffer = buffer.replace("\r\n", "\n");
        }
        if (!buffer.startsWith(YAML_DOCUMENT_START)) {
            return null;
        }
        int sep = buffer.indexOf(YAML_DOCUMENT_SEPARATOR, YAML_DOCUMENT_START.length());
        return sep == -1 ? null : buffer.substring(YAML_DOCUMENT_START.length(), sep);
    }

    static boolean isDiff(String fileContent) {
        return fileContent.contains(MERGE_CONFLICT_OURS) && fileContent.contains(MERGE_CONFLICT_THEIRS) &&
               fileContent.contains(MERGE_CONFLICT_END);
    }

    static LockfileObject autofixMergeConflicts(String fileContent) {
        return autofixMergeConflicts(fileContent, PnpmRelease.PORTED);
    }

    static LockfileObject autofixMergeConflicts(String fileContent, PnpmRelease release) {
        List<String> ours = new ArrayList<>();
        List<String> theirs = new ArrayList<>();
        String state = "top";
        for (String line : splitLines(fileContent)) {
            if (line.startsWith(MERGE_CONFLICT_PARENT)) {
                state = "parent";
                continue;
            }
            if (line.startsWith(MERGE_CONFLICT_OURS)) {
                state = "ours";
                continue;
            }
            if (line.equals(MERGE_CONFLICT_THEIRS)) {
                state = "theirs";
                continue;
            }
            if (line.startsWith(MERGE_CONFLICT_END)) {
                state = "top";
                continue;
            }
            if ("top".equals(state) || "ours".equals(state)) {
                ours.add(line);
            }
            if ("top".equals(state) || "theirs".equals(state)) {
                theirs.add(line);
            }
        }
        return LockfileMerger.mergeLockfileChanges(
                LockfileFormatConverters.convertToLockfileObject(JsYaml.load(String.join("\n", ours)), release),
                LockfileFormatConverters.convertToLockfileObject(JsYaml.load(String.join("\n", theirs)), release));
    }

    // fileContent.split(/[\n\r]+/)
    private static List<String> splitLines(String s) {
        return new ArrayList<>(Arrays.asList(s.split("[\\n\\r]+", -1)));
    }
}
