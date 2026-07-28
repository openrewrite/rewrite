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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Patches a classic {@code yarn.lock} (v1). The file is not valid YAML, so this is a targeted text edit
 * over the raw string: the moving package's block header {@code name@<oldRange>} is re-pinned to the new
 * declared range and its {@code version}/{@code resolved}/{@code integrity} lines rewritten, while every
 * other byte is preserved.
 * <p>
 * When a header merges several selectors that resolved to one version ({@code "a@^1, a@~1.2:"}) the moving
 * selector is <em>split</em> out — removed from the merged header (the de-merged remainder kept byte-identical)
 * and re-emitted as a new, {@code sortAlpha}-positioned block — rather than renaming the whole header, which
 * would silently move the other ranges. Serialization mirrors yarn's own {@code _stringify}: {@code sortAlpha}
 * ordering, {@code shouldWrapKey} quoting, and the priority field order.
 */
public final class YarnClassicLockPatcher implements LockPatcher {

    private static final String YARN_HEADER = "# yarn lockfile v1";
    private static final String NPM_REGISTRY = "https://registry.npmjs.org/";
    private static final String YARN_REGISTRY = "https://registry.yarnpkg.com/";

    @Override
    public String patch(LockEditSet edits) {
        String content = edits.getExistingLockContent();
        if (content == null || !content.contains(YARN_HEADER)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "not a yarn v1 lockfile");
        }
        for (PackageEdit edit : edits.getEdits()) {
            content = applyEdit(content, edit, edits.getEditedPackageJson());
        }
        return content;
    }

    private String applyEdit(String content, PackageEdit edit, @Nullable String editedPackageJson) {
        String name = edit.getName();
        String oldConstraint = edit.getOldConstraint();
        if (oldConstraint == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, name, "missing old constraint for " + name);
        }
        String oldDescriptor = name + "@" + oldConstraint;

        Blocks blocks = Blocks.parse(content);
        int bi = blocks.indexOfSelector(oldDescriptor);
        if (bi < 0) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, name, "no yarn block for " + oldDescriptor);
        }

        if (edit.getNewVersion() == null) {
            removeSelector(blocks, bi, oldDescriptor);
            return blocks.reconstruct();
        }

        String newConstraint = LockManifests.declaredConstraint(editedPackageJson, edit.getScope(), name);
        if (newConstraint == null) {
            newConstraint = oldConstraint;
        }
        String newDescriptor = name + "@" + newConstraint;

        List<String> selectors = headerTokens(blocks.get(bi));
        if (selectors.size() == 1) {
            blocks.set(bi, inPlace(blocks.get(bi), newDescriptor, edit));
        } else {
            splitOut(blocks, bi, oldDescriptor, newDescriptor, edit);
        }
        return blocks.reconstruct();
    }

    /** Single-selector block: rename the header and, when the version moved, rewrite the resolution lines. */
    private String inPlace(String block, String newDescriptor, PackageEdit edit) {
        int nl = block.indexOf('\n');
        String body = block.substring(nl);
        String newHeader = maybeWrap(newDescriptor) + ":";
        if (edit.getNewVersion() != null && !edit.getNewVersion().equals(edit.getOldVersion())) {
            body = replaceFieldLine(body, "  version ", "  version " + maybeWrap(edit.getNewVersion()));
            body = replaceFieldLine(body, "  resolved ", "  resolved " + maybeWrap(resolved(edit)));
            body = replaceFieldLine(body, "  integrity ", "  integrity " + maybeWrap(nonNull(edit.getNewIntegrity(), edit)));
        }
        return newHeader + body;
    }

    /** Merged header: drop the moving selector (remainder unchanged) and insert a synthesized new block. */
    private void splitOut(Blocks blocks, int bi, String oldDescriptor, String newDescriptor, PackageEdit edit) {
        blocks.set(bi, removeToken(blocks.get(bi), oldDescriptor));
        String newBlock = synthesize(newDescriptor, edit);
        blocks.insertSorted(newBlock);
    }

    private void removeSelector(Blocks blocks, int bi, String oldDescriptor) {
        if (headerTokens(blocks.get(bi)).size() == 1) {
            blocks.remove(bi);
        } else {
            blocks.set(bi, removeToken(blocks.get(bi), oldDescriptor));
        }
    }

    private String synthesize(String descriptor, PackageEdit edit) {
        StringBuilder sb = new StringBuilder();
        sb.append(maybeWrap(descriptor)).append(":\n");
        sb.append("  version ").append(maybeWrap(nonNull(edit.getNewVersion(), edit))).append('\n');
        sb.append("  resolved ").append(maybeWrap(resolved(edit))).append('\n');
        sb.append("  integrity ").append(maybeWrap(nonNull(edit.getNewIntegrity(), edit)));
        appendDeps(sb, "dependencies", edit.getNewDependencies());
        appendDeps(sb, "optionalDependencies", edit.getNewOptionalDependencies());
        return sb.toString();
    }

    private static void appendDeps(StringBuilder sb, String label, @Nullable Map<String, String> deps) {
        if (deps == null || deps.isEmpty()) {
            return;
        }
        sb.append("\n  ").append(label).append(':');
        for (Map.Entry<String, String> e : new TreeMap<>(deps).entrySet()) {
            sb.append("\n    ").append(maybeWrap(e.getKey())).append(' ').append(maybeWrap(e.getValue()));
        }
    }

    private String resolved(PackageEdit edit) {
        String url = edit.getNewResolved();
        if (url == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    "missing resolved metadata for " + edit.getName());
        }
        if (url.startsWith(NPM_REGISTRY)) {
            url = YARN_REGISTRY + url.substring(NPM_REGISTRY.length());
        }
        if (edit.getNewShasum() != null) {
            url = url + "#" + edit.getNewShasum();
        }
        return url;
    }

    private static String nonNull(@Nullable String value, PackageEdit edit) {
        if (value == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    "missing resolution metadata for " + edit.getName());
        }
        return value;
    }

    // --- header/selector helpers ---------------------------------------------

    private static String headerLine(String block) {
        int nl = block.indexOf('\n');
        return nl < 0 ? block : block.substring(0, nl);
    }

    private static List<String> headerTokens(String block) {
        String header = headerLine(block);
        if (header.endsWith(":")) {
            header = header.substring(0, header.length() - 1);
        }
        return splitSelectors(header);
    }

    private static String removeToken(String block, String descriptor) {
        int nl = block.indexOf('\n');
        String header = block.substring(0, nl);
        String body = block.substring(nl);
        String noColon = header.substring(0, header.length() - 1);
        List<String> kept = new ArrayList<>();
        for (String token : splitSelectors(noColon)) {
            if (!unwrap(token).equals(descriptor)) {
                kept.add(token);
            }
        }
        return String.join(", ", kept) + ":" + body;
    }

    private static String representative(String block) {
        return unwrap(headerTokens(block).get(0));
    }

    /** Split a merged header on {@code ", "} while respecting the double-quotes yarn wraps special selectors in. */
    private static List<String> splitSelectors(String header) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < header.length(); i++) {
            char c = header.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                cur.append(c);
            } else if (!inQuote && c == ',' && i + 1 < header.length() && header.charAt(i + 1) == ' ') {
                out.add(cur.toString());
                cur.setLength(0);
                i++;
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static String replaceFieldLine(String body, String linePrefix, String replacement) {
        String[] lines = body.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(linePrefix)) {
                lines[i] = replacement;
                break;
            }
        }
        return String.join("\n", lines);
    }

    // --- yarn serialization (mirrors yarn's own shouldWrapKey/maybeWrap/sortAlpha) --------------------

    static String maybeWrap(String s) {
        return shouldWrapKey(s) ? jsonQuote(s) : s;
    }

    private static boolean shouldWrapKey(String s) {
        if (s.startsWith("true") || s.startsWith("false")) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ':' || c == '\n' || c == '\\' || c == '"' || c == ',' || c == '[' || c == ']' || Character.isWhitespace(c)) {
                return true;
            }
        }
        if (s.isEmpty()) {
            return true;
        }
        char c0 = s.charAt(0);
        if (c0 >= '0' && c0 <= '9') {
            return true;
        }
        return !((c0 >= 'a' && c0 <= 'z') || (c0 >= 'A' && c0 <= 'Z'));
    }

    private static String jsonQuote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                b.append('\\');
            }
            b.append(c);
        }
        return b.append('"').toString();
    }

    static int sortAlpha(String a, String b) {
        int shortLen = Math.min(a.length(), b.length());
        for (int i = 0; i < shortLen; i++) {
            int diff = a.charAt(i) - b.charAt(i);
            if (diff != 0) {
                return diff;
            }
        }
        return a.length() - b.length();
    }

    private static String unwrap(String token) {
        if (token.length() >= 2 && token.charAt(0) == '"' && token.charAt(token.length() - 1) == '"') {
            return token.substring(1, token.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return token;
    }

    /** The raw-string block model: the leading comment/blank prefix plus each top-level block, verbatim. */
    private static final class Blocks {
        private final String prefix;
        private final List<String> blocks;
        private final boolean trailingNewline;

        private Blocks(String prefix, List<String> blocks, boolean trailingNewline) {
            this.prefix = prefix;
            this.blocks = blocks;
            this.trailingNewline = trailingNewline;
        }

        static Blocks parse(String content) {
            int first = firstBlockOffset(content);
            String prefix = content.substring(0, first);
            String rest = content.substring(first);
            boolean trailing = rest.endsWith("\n");
            String core = trailing ? rest.substring(0, rest.length() - 1) : rest;
            List<String> blocks = new ArrayList<>();
            if (!core.isEmpty()) {
                for (String block : core.split("\n\n", -1)) {
                    blocks.add(block);
                }
            }
            return new Blocks(prefix, blocks, trailing);
        }

        private static int firstBlockOffset(String content) {
            int offset = 0;
            for (String line : content.split("\n", -1)) {
                if (!line.isEmpty() && line.charAt(0) != '#' && !Character.isWhitespace(line.charAt(0)) && line.endsWith(":")) {
                    return offset;
                }
                offset += line.length() + 1;
            }
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "no yarn blocks found");
        }

        String get(int i) {
            return blocks.get(i);
        }

        void set(int i, String block) {
            blocks.set(i, block);
        }

        void remove(int i) {
            blocks.remove(i);
        }

        int indexOfSelector(String descriptor) {
            for (int i = 0; i < blocks.size(); i++) {
                for (String token : headerTokens(blocks.get(i))) {
                    if (unwrap(token).equals(descriptor)) {
                        return i;
                    }
                }
            }
            return -1;
        }

        void insertSorted(String block) {
            String rep = representative(block);
            int at = blocks.size();
            for (int i = 0; i < blocks.size(); i++) {
                if (sortAlpha(representative(blocks.get(i)), rep) > 0) {
                    at = i;
                    break;
                }
            }
            blocks.add(at, block);
        }

        String reconstruct() {
            return prefix + String.join("\n\n", blocks) + (trailingNewline ? "\n" : "");
        }
    }
}
