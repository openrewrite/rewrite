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
package org.openrewrite.python.internal.pdmlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Emits a {@link PdmLock} byte-exactly in pdm's own lockfile style: the two-line {@code @generated}
 * header, a leading {@code [metadata]} table with {@code [[metadata.targets]]}, then packages with
 * multiline {@code dependencies}/{@code files} arrays, inline tables without inner padding, one
 * blank line between blocks, LF endings and a single trailing newline.
 */
public final class PdmLockWriter {

    private PdmLockWriter() {
    }

    public static String write(PdmLock lock) {
        List<String> blocks = new ArrayList<>();
        blocks.add(lock.getHeader());
        blocks.add(metadataBlock(lock));
        for (Map<String, String> target : lock.getTargets()) {
            blocks.add(targetBlock(target));
        }
        for (PdmLockPackage pkg : lock.getPackages()) {
            blocks.add(packageBlock(pkg));
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(blocks.get(i)).append('\n');
        }
        return out.toString();
    }

    private static String metadataBlock(PdmLock lock) {
        StringBuilder b = new StringBuilder("[metadata]");
        b.append("\ngroups = ").append(inlineStringArray(lock.getGroups()));
        b.append("\nstrategy = ").append(inlineStringArray(lock.getStrategy()));
        b.append("\nlock_version = ").append(string(lock.getLockVersion()));
        b.append("\ncontent_hash = ").append(string(lock.getContentHash()));
        return b.toString();
    }

    private static String targetBlock(Map<String, String> target) {
        StringBuilder b = new StringBuilder("[[metadata.targets]]");
        for (Map.Entry<String, String> e : target.entrySet()) {
            b.append('\n').append(key(e.getKey())).append(" = ").append(string(e.getValue()));
        }
        return b.toString();
    }

    private static String packageBlock(PdmLockPackage pkg) {
        StringBuilder b = new StringBuilder("[[package]]");
        b.append("\nname = ").append(string(pkg.getName()));
        b.append("\nversion = ").append(string(pkg.getVersion()));
        if (pkg.getExtras() != null) {
            b.append("\nextras = ").append(inlineStringArray(pkg.getExtras()));
        }
        if (pkg.getRequiresPython() != null) {
            b.append("\nrequires_python = ").append(string(pkg.getRequiresPython()));
        }
        if (pkg.getEditable() != null) {
            b.append("\neditable = ").append(pkg.getEditable());
        }
        if (pkg.getSubdirectory() != null) {
            b.append("\nsubdirectory = ").append(string(pkg.getSubdirectory()));
        }
        if (pkg.getVcs() != null) {
            b.append('\n').append(pkg.getVcs()).append(" = ").append(string(pkg.getVcsUrl()));
        }
        if (pkg.getRef() != null) {
            b.append("\nref = ").append(string(pkg.getRef()));
        }
        if (pkg.getRevision() != null) {
            b.append("\nrevision = ").append(string(pkg.getRevision()));
        }
        if (pkg.getPath() != null) {
            b.append("\npath = ").append(string(pkg.getPath()));
        }
        if (pkg.getUrl() != null) {
            b.append("\nurl = ").append(string(pkg.getUrl()));
        }
        b.append("\nsummary = ").append(string(pkg.getSummary()));
        if (pkg.getGroups() != null) {
            b.append("\ngroups = ").append(inlineStringArray(pkg.getGroups()));
        }
        if (pkg.getMarker() != null) {
            b.append("\nmarker = ").append(string(pkg.getMarker()));
        }
        if (pkg.getDependencies() != null) {
            b.append("\ndependencies = ").append(multilineStringArray(pkg.getDependencies()));
        }
        if (pkg.getFiles() != null) {
            b.append("\nfiles = ").append(filesArray(pkg.getFiles()));
        }
        return b.toString();
    }

    private static String filesArray(List<PdmLockFile> files) {
        if (files.isEmpty()) {
            return "[]";
        }
        StringBuilder b = new StringBuilder("[");
        for (PdmLockFile f : files) {
            b.append("\n    {").append(f.getFile() != null ? "file = " + string(f.getFile()) : "url = " + string(f.getUrl()))
                    .append(", hash = ").append(string(f.getHash())).append("},");
        }
        return b.append("\n]").toString();
    }

    private static String multilineStringArray(List<String> values) {
        if (values.isEmpty()) {
            return "[]";
        }
        StringBuilder b = new StringBuilder("[");
        for (String value : values) {
            b.append("\n    ").append(string(value)).append(',');
        }
        return b.append("\n]").toString();
    }

    private static String inlineStringArray(List<String> values) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(string(values.get(i)));
        }
        return b.append(']').toString();
    }

    private static String key(String k) {
        boolean bare = !k.isEmpty();
        for (int i = 0; i < k.length(); i++) {
            char c = k.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_')) {
                bare = false;
                break;
            }
        }
        return bare ? k : string(k);
    }

    /** A TOML basic string with tomlkit-compatible escaping (unicode left literal). */
    static String string(String value) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    b.append("\\\"");
                    break;
                case '\\':
                    b.append("\\\\");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                case '\b':
                    b.append("\\b");
                    break;
                case '\f':
                    b.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.append('"').toString();
    }
}
