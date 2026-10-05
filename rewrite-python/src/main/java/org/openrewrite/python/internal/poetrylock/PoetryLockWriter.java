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
package org.openrewrite.python.internal.poetrylock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Emits a {@link PoetryLock} byte-exactly in poetry's own lockfile style: the {@code @generated}
 * header, alphabetically-ordered packages, {@code files} as a 4-space multiline array with
 * trailing commas, inline tables without inner padding, one blank line between blocks, LF endings
 * and a single trailing newline.
 */
public final class PoetryLockWriter {

    private PoetryLockWriter() {
    }

    public static String write(PoetryLock lock) {
        List<String> blocks = new ArrayList<>();
        blocks.add(lock.getHeader());
        for (PoetryLockPackage pkg : lock.getPackages()) {
            blocks.add(packageBlock(pkg));
        }
        if (lock.getRootExtras() != null) {
            blocks.add(extrasTableBlock(lock.getRootExtras()));
        }
        blocks.add(metadataBlock(lock));

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(blocks.get(i)).append('\n');
        }
        return out.toString();
    }

    private static String packageBlock(PoetryLockPackage pkg) {
        StringBuilder b = new StringBuilder("[[package]]");
        b.append("\nname = ").append(string(pkg.getName()));
        b.append("\nversion = ").append(string(pkg.getVersion()));
        b.append("\ndescription = ").append(string(pkg.getDescription()));
        b.append("\noptional = ").append(pkg.isOptional());
        b.append("\npython-versions = ").append(string(pkg.getPythonVersions()));
        b.append("\ngroups = ").append(inlineStringArray(pkg.getGroups()));
        if (pkg.getMarker() != null) {
            b.append("\nmarkers = ").append(string(pkg.getMarker()));
        } else if (pkg.getGroupMarkers() != null) {
            b.append("\nmarkers = ").append(inlineStringMap(pkg.getGroupMarkers()));
        }
        b.append("\nfiles = ").append(filesArray(pkg.getFiles()));
        if (pkg.getDevelop() != null) {
            b.append("\ndevelop = ").append(pkg.getDevelop());
        }
        if (pkg.getDependencies() != null) {
            b.append("\n\n[package.dependencies]");
            for (PoetryLockDependency dep : pkg.getDependencies()) {
                b.append('\n').append(key(dep.getName())).append(" = ").append(dependencyValue(dep));
            }
        }
        if (pkg.getExtras() != null) {
            b.append("\n\n[package.extras]");
            for (Map.Entry<String, List<String>> e : pkg.getExtras().entrySet()) {
                b.append('\n').append(key(e.getKey())).append(" = ").append(inlineStringArray(e.getValue()));
            }
        }
        if (pkg.getSource() != null) {
            b.append("\n\n[package.source]").append(sourceBody(pkg.getSource()));
        }
        return b.toString();
    }

    private static String sourceBody(PoetryLockSource source) {
        StringBuilder b = new StringBuilder();
        if (source.getType() != null) {
            b.append("\ntype = ").append(string(source.getType()));
        }
        b.append("\nurl = ").append(string(source.getUrl()));
        if (source.getReference() != null) {
            b.append("\nreference = ").append(string(source.getReference()));
        }
        if (source.getResolvedReference() != null) {
            b.append("\nresolved_reference = ").append(string(source.getResolvedReference()));
        }
        if (source.getSubdirectory() != null) {
            b.append("\nsubdirectory = ").append(string(source.getSubdirectory()));
        }
        return b.toString();
    }

    private static String dependencyValue(PoetryLockDependency dep) {
        List<PoetryLockConstraint> constraints = dep.getConstraints();
        boolean allVersionOnly = true;
        for (PoetryLockConstraint c : constraints) {
            if (!c.isVersionOnly()) {
                allVersionOnly = false;
                break;
            }
        }
        if (constraints.size() == 1) {
            PoetryLockConstraint c = constraints.get(0);
            return allVersionOnly ? string(c.getVersion()) : constraintTable(c);
        }
        StringBuilder b = new StringBuilder("[");
        for (PoetryLockConstraint c : constraints) {
            b.append("\n    ").append(allVersionOnly ? string(c.getVersion()) : constraintTable(c)).append(',');
        }
        return b.append("\n]").toString();
    }

    private static String constraintTable(PoetryLockConstraint c) {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        if (c.getVersion() != null) {
            first = pair(b, first, "version", string(c.getVersion()));
        }
        if (c.getPath() != null) {
            first = pair(b, first, "path", string(c.getPath()));
        }
        if (c.getDevelop() != null) {
            first = pair(b, first, "develop", c.getDevelop().toString());
        }
        if (c.getUrl() != null) {
            first = pair(b, first, "url", string(c.getUrl()));
        }
        if (c.getVcs() != null) {
            first = pair(b, first, c.getVcs(), string(c.getVcsUrl()));
        }
        if (c.getBranch() != null) {
            first = pair(b, first, "branch", string(c.getBranch()));
        }
        if (c.getTag() != null) {
            first = pair(b, first, "tag", string(c.getTag()));
        }
        if (c.getRev() != null) {
            first = pair(b, first, "rev", string(c.getRev()));
        }
        if (c.getSubdirectory() != null) {
            first = pair(b, first, "subdirectory", string(c.getSubdirectory()));
        }
        if (c.getExtras() != null) {
            first = pair(b, first, "extras", inlineStringArray(c.getExtras()));
        }
        if (c.getOptional() != null) {
            first = pair(b, first, "optional", c.getOptional().toString());
        }
        if (c.getMarkers() != null) {
            first = pair(b, first, "markers", string(c.getMarkers()));
        }
        if (first) {
            throw new PoetryLockFormatException("Dependency constraint has no keys to emit");
        }
        return b.append('}').toString();
    }

    private static boolean pair(StringBuilder b, boolean first, String key, String rendered) {
        if (!first) {
            b.append(", ");
        }
        b.append(key).append(" = ").append(rendered);
        return false;
    }

    private static String filesArray(List<PoetryLockFile> files) {
        if (files.isEmpty()) {
            return "[]";
        }
        StringBuilder b = new StringBuilder("[");
        for (PoetryLockFile f : files) {
            b.append("\n    {file = ").append(string(f.getFile()))
                    .append(", hash = ").append(string(f.getHash())).append("},");
        }
        return b.append("\n]").toString();
    }

    private static String extrasTableBlock(Map<String, List<String>> extras) {
        StringBuilder b = new StringBuilder("[extras]");
        for (Map.Entry<String, List<String>> e : extras.entrySet()) {
            b.append('\n').append(key(e.getKey())).append(" = ").append(inlineStringArray(e.getValue()));
        }
        return b.toString();
    }

    private static String metadataBlock(PoetryLock lock) {
        StringBuilder b = new StringBuilder("[metadata]");
        b.append("\nlock-version = ").append(string(lock.getLockVersion()));
        if (lock.getPythonVersions() != null) {
            b.append("\npython-versions = ").append(string(lock.getPythonVersions()));
        }
        b.append("\ncontent-hash = ").append(string(lock.getContentHash()));
        return b.toString();
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

    private static String inlineStringMap(Map<String, String> map) {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            first = pair(b, first, key(e.getKey()), string(e.getValue()));
        }
        return b.append('}').toString();
    }

    /** A bare TOML key, quoted only when it contains characters requiring it. */
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
