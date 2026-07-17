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
package org.openrewrite.python.internal.uvlock;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Emits a {@link UvLock} model byte-exactly in uv's own lockfile style: fixed key orders,
 * 4-space multiline arrays with trailing commas, single-element {@code requires-dist}
 * arrays inline, one blank line between blocks, LF endings and a single trailing newline.
 * Whether artifacts carry {@code upload-time} is data carried by the model (set on read),
 * so a surgical edit re-emits in the source file's own revision style.
 */
public final class UvLockWriter {

    private UvLockWriter() {
    }

    public static String write(UvLock lock) {
        List<String> blocks = new ArrayList<>();
        blocks.add(headerBlock(lock));
        if (lock.getOptions() != null) {
            blocks.add(optionsBlock(lock.getOptions()));
        }
        if (lock.getManifest() != null) {
            blocks.add(manifestBlock(lock.getManifest()));
        }
        for (UvLockPackage pkg : lock.getPackages()) {
            addPackageBlocks(blocks, pkg);
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

    private static String headerBlock(UvLock lock) {
        StringBuilder b = new StringBuilder();
        b.append("version = ").append(lock.getVersion());
        if (lock.getRevision() != null) {
            b.append("\nrevision = ").append(lock.getRevision());
        }
        if (lock.getRequiresPython() != null) {
            b.append("\nrequires-python = ").append(string(lock.getRequiresPython()));
        }
        if (lock.getResolutionMarkers() != null) {
            b.append("\nresolution-markers = ").append(multilineStringArray(lock.getResolutionMarkers()));
        }
        if (lock.getSupportedMarkers() != null) {
            b.append("\nsupported-markers = ").append(multilineStringArray(lock.getSupportedMarkers()));
        }
        if (lock.getRequiredMarkers() != null) {
            b.append("\nrequired-markers = ").append(multilineStringArray(lock.getRequiredMarkers()));
        }
        if (lock.getConflicts() != null) {
            b.append("\nconflicts = ").append(conflictsArray(lock.getConflicts()));
        }
        return b.toString();
    }

    /**
     * uv emits {@code conflicts} as an array of arrays of inline tables: each set is a multiline
     * bracket run, and the sets are joined by {@code ], [} so the outer brackets abut
     * ({@code [[ … ], [ … ]]}).
     */
    private static String conflictsArray(List<List<UvLockConflictItem>> conflicts) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < conflicts.size(); i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append('[');
            for (UvLockConflictItem item : conflicts.get(i)) {
                b.append("\n    ").append(conflictItem(item)).append(',');
            }
            b.append("\n]");
        }
        return b.append(']').toString();
    }

    private static String conflictItem(UvLockConflictItem item) {
        StringBuilder b = new StringBuilder("{ package = ").append(string(item.getPackageName()));
        if (item.getExtra() != null) {
            b.append(", extra = ").append(string(item.getExtra()));
        }
        if (item.getGroup() != null) {
            b.append(", group = ").append(string(item.getGroup()));
        }
        return b.append(" }").toString();
    }

    private static String optionsBlock(UvLockOptions options) {
        StringBuilder b = new StringBuilder("[options]");
        if (options.getExcludeNewer() != null) {
            b.append("\nexclude-newer = ").append(string(options.getExcludeNewer()));
        }
        return b.toString();
    }

    private static String manifestBlock(UvLockManifest manifest) {
        return "[manifest]\nmembers = " + multilineStringArray(manifest.getMembers());
    }

    private static void addPackageBlocks(List<String> blocks, UvLockPackage pkg) {
        StringBuilder b = new StringBuilder("[[package]]");
        b.append("\nname = ").append(string(pkg.getName()));
        b.append("\nversion = ").append(string(pkg.getVersion()));
        b.append("\nsource = ").append(source(pkg.getSource()));
        if (pkg.getResolutionMarkers() != null) {
            b.append("\nresolution-markers = ").append(multilineStringArray(pkg.getResolutionMarkers()));
        }
        if (pkg.getDependencies() != null) {
            b.append("\ndependencies = ").append(multilineDependencyArray(pkg.getDependencies()));
        }
        if (pkg.getSdist() != null) {
            b.append("\nsdist = ").append(artifact(pkg.getSdist()));
        }
        if (pkg.getWheels() != null) {
            b.append("\nwheels = [");
            for (UvLockArtifact wheel : pkg.getWheels()) {
                b.append("\n    ").append(artifact(wheel)).append(',');
            }
            b.append("\n]");
        }
        blocks.add(b.toString());

        if (pkg.getOptionalDependencies() != null) {
            blocks.add(dependencyGroupsBlock("[package.optional-dependencies]", pkg.getOptionalDependencies()));
        }
        if (pkg.getDevDependencies() != null) {
            blocks.add(dependencyGroupsBlock("[package.dev-dependencies]", pkg.getDevDependencies()));
        }
        UvLockMetadata metadata = pkg.getMetadata();
        if (metadata != null) {
            StringBuilder m = new StringBuilder("[package.metadata]");
            if (metadata.getRequiresDist() != null) {
                m.append("\nrequires-dist = ").append(requirementArray(metadata.getRequiresDist()));
            }
            if (metadata.getProvidesExtras() != null) {
                m.append("\nprovides-extras = ").append(inlineStringArray(metadata.getProvidesExtras()));
            }
            blocks.add(m.toString());

            if (metadata.getRequiresDev() != null) {
                StringBuilder d = new StringBuilder("[package.metadata.requires-dev]");
                for (Map.Entry<String, List<UvLockRequirement>> e : metadata.getRequiresDev().entrySet()) {
                    d.append('\n').append(bareKey(e.getKey())).append(" = ").append(requirementArray(e.getValue()));
                }
                blocks.add(d.toString());
            }
        }
    }

    private static String dependencyGroupsBlock(String header, Map<String, List<UvLockDependency>> groups) {
        StringBuilder b = new StringBuilder(header);
        for (Map.Entry<String, List<UvLockDependency>> e : groups.entrySet()) {
            b.append('\n').append(bareKey(e.getKey())).append(" = ").append(multilineDependencyArray(e.getValue()));
        }
        return b.toString();
    }

    /**
     * Resolved-edge arrays are always multiline, even for a single element.
     */
    private static String multilineDependencyArray(List<UvLockDependency> dependencies) {
        StringBuilder b = new StringBuilder("[");
        for (UvLockDependency dep : dependencies) {
            b.append("\n    ").append(dependency(dep)).append(',');
        }
        b.append("\n]");
        return b.toString();
    }

    /**
     * Declared-metadata arrays are inline with exactly one element (at any width), multiline otherwise.
     */
    private static String requirementArray(List<UvLockRequirement> requirements) {
        if (requirements.isEmpty()) {
            return "[]";
        }
        if (requirements.size() == 1) {
            return "[" + requirement(requirements.get(0)) + "]";
        }
        StringBuilder b = new StringBuilder("[");
        for (UvLockRequirement req : requirements) {
            b.append("\n    ").append(requirement(req)).append(',');
        }
        b.append("\n]");
        return b.toString();
    }

    private static String dependency(UvLockDependency dep) {
        StringBuilder b = new StringBuilder("{ name = ").append(string(dep.getName()));
        if (dep.getVersion() != null) {
            b.append(", version = ").append(string(dep.getVersion()));
        }
        if (dep.getSource() != null) {
            b.append(", source = ").append(source(dep.getSource()));
        }
        if (dep.getExtra() != null) {
            b.append(", extra = ").append(inlineStringArray(dep.getExtra()));
        }
        if (dep.getMarker() != null) {
            b.append(", marker = ").append(string(dep.getMarker()));
        }
        return b.append(" }").toString();
    }

    private static String requirement(UvLockRequirement req) {
        StringBuilder b = new StringBuilder("{ name = ").append(string(req.getName()));
        if (req.getExtras() != null) {
            b.append(", extras = ").append(inlineStringArray(req.getExtras()));
        }
        if (req.getMarker() != null) {
            b.append(", marker = ").append(string(req.getMarker()));
        }
        // editable/url/git/directory are the requirement's direct source; uv emits them after marker, in place of specifier
        if (req.getEditable() != null) {
            b.append(", editable = ").append(string(req.getEditable()));
        }
        if (req.getUrl() != null) {
            b.append(", url = ").append(string(req.getUrl()));
        }
        if (req.getGit() != null) {
            b.append(", git = ").append(string(req.getGit()));
        }
        if (req.getDirectory() != null) {
            b.append(", directory = ").append(string(req.getDirectory()));
        }
        if (req.getSpecifier() != null) {
            b.append(", specifier = ").append(string(req.getSpecifier()));
        }
        if (req.getIndex() != null) {
            b.append(", index = ").append(string(req.getIndex()));
        }
        return b.append(" }").toString();
    }

    private static String artifact(UvLockArtifact artifact) {
        StringBuilder b = new StringBuilder("{ ");
        boolean first = true;
        if (artifact.getUrl() != null) {
            b.append("url = ").append(string(artifact.getUrl()));
            first = false;
        }
        if (artifact.getPath() != null) {
            first = appendKey(b, first, "path", string(artifact.getPath()));
        }
        if (artifact.getHash() != null) {
            first = appendKey(b, first, "hash", string(artifact.getHash()));
        }
        if (artifact.getSize() != null) {
            first = appendKey(b, first, "size", artifact.getSize().toString());
        }
        if (artifact.getUploadTime() != null) {
            first = appendKey(b, first, "upload-time", string(artifact.getUploadTime()));
        }
        if (first) {
            throw new UvLockFormatException("Artifact entry has no keys to emit");
        }
        return b.append(" }").toString();
    }

    private static boolean appendKey(StringBuilder b, boolean first, String key, String rendered) {
        if (!first) {
            b.append(", ");
        }
        b.append(key).append(" = ").append(rendered);
        return false;
    }

    private static String source(UvLockSource source) {
        return "{ " + source.getType().getKey() + " = " + string(source.getValue()) + " }";
    }

    private static String multilineStringArray(List<String> values) {
        StringBuilder b = new StringBuilder("[");
        for (String value : values) {
            b.append("\n    ").append(string(value)).append(',');
        }
        b.append("\n]");
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

    private static String bareKey(String key) {
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            boolean bare = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
            if (!bare) {
                throw new UvLockFormatException("Key requires quoting, which uv never emits: " + key);
            }
        }
        if (key.isEmpty()) {
            throw new UvLockFormatException("Empty key");
        }
        return key;
    }

    /**
     * uv emits basic strings with no escapes; markers use single quotes internally so none are needed.
     */
    private static String string(@Nullable String value) {
        if (value == null) {
            throw new UvLockFormatException("Cannot emit a null string value");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\' || c < 0x20) {
                throw new UvLockFormatException("String value requires escaping, which uv.lock never contains: " + value);
            }
        }
        return "\"" + value + "\"";
    }
}
