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
import org.openrewrite.javascript.semver.NodeSemver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/deps.path src/index.ts (pnpm 11.2.2): the grammar of a dependency path, {@code name@version}
 * followed by an optional {@code (patch_hash=...)} and the peer suffix {@code (peer@1.0.0)(other@2.0.0)}.
 */
final class DepPath {

    private static final Pattern RUNTIME_DEP_PATH_RE = Pattern.compile("^(?:node|bun|deno)@runtime:");
    private static final Pattern FILENAME_UNSAFE = Pattern.compile("[\\\\/:*?\"<>|#]");

    private DepPath() {
    }

    /**
     * Where the patch hash and the peer suffix start, -1 for each that is absent.
     */
    @Value
    static class SuffixIndex {
        int peersIndex;
        int patchHashIndex;
    }

    /**
     * {@code parse}'s result.
     */
    @Value
    static class DependencyPath {
        @Nullable
        String name;

        @Nullable
        String peerDepGraphHash;

        @Nullable
        String version;

        @Nullable
        String nonSemverVersion;

        @Nullable
        String patchHash;
    }

    /**
     * {@code createPeerDepGraphHash}'s peer id: a {@code name@version} pair, or an already rendered dependency path.
     */
    @Value
    static class PeerId {
        @Nullable
        String name;

        @Nullable
        String version;

        @Nullable
        String depPath;

        static PeerId of(String name, String version) {
            return new PeerId(name, version, null);
        }

        static PeerId of(String depPath) {
            return new PeerId(null, null, depPath);
        }
    }

    static boolean isAbsolute(String dependencyPath) {
        return dependencyPath.isEmpty() || dependencyPath.charAt(0) != '/';
    }

    static SuffixIndex indexOfDepPathSuffix(String depPath) {
        if (!depPath.endsWith(")")) {
            return new SuffixIndex(-1, -1);
        }
        int open = 1;
        for (int i = depPath.length() - 2; i >= 0; i--) {
            char c = depPath.charAt(i);
            if (c == '(') {
                open--;
            } else if (c == ')') {
                open++;
            } else if (open == 0) {
                if (depPath.substring(i + 1).startsWith("(patch_hash=")) {
                    return new SuffixIndex(depPath.indexOf('(', i + 2), i + 1);
                }
                return new SuffixIndex(i + 1, -1);
            }
        }
        return new SuffixIndex(-1, -1);
    }

    static String removeSuffix(String relDepPath) {
        SuffixIndex index = indexOfDepPathSuffix(relDepPath);
        if (index.getPatchHashIndex() != -1) {
            return relDepPath.substring(0, index.getPatchHashIndex());
        }
        if (index.getPeersIndex() != -1) {
            return relDepPath.substring(0, index.getPeersIndex());
        }
        return relDepPath;
    }

    static String getPkgIdWithPatchHash(String depPath) {
        int sepIndex = indexOfDepPathSuffix(depPath).getPeersIndex();
        return sepIndex != -1 ? depPath.substring(0, sepIndex) : depPath;
    }

    static String tryGetPackageId(String relDepPath) {
        String pkgId = relDepPath;
        SuffixIndex index = indexOfDepPathSuffix(pkgId);
        int sepIndex = index.getPatchHashIndex() == -1 ? index.getPeersIndex() : index.getPatchHashIndex();
        if (sepIndex != -1) {
            pkgId = pkgId.substring(0, sepIndex);
        }
        if (pkgId.contains(":")) {
            String newPkgId = pkgId.substring(pkgId.indexOf('@', 1) + 1);
            if (!newPkgId.startsWith("runtime:")) {
                pkgId = newPkgId;
            }
        }
        return pkgId;
    }

    static String getRegistryByPackageName(Map<String, String> registries, String packageName) {
        if (packageName.isEmpty() || packageName.charAt(0) != '@') {
            return registries.get("default");
        }
        String scope = packageName.substring(0, Math.max(0, packageName.indexOf('/')));
        String registry = registries.get(scope);
        return registry != null && !registry.isEmpty() ? registry : registries.get("default");
    }

    static @Nullable String refToRelative(String reference, String pkgName) {
        if (reference.startsWith("link:")) {
            return null;
        }
        if (!reference.isEmpty() && reference.charAt(0) == '@') {
            return reference;
        }
        int atIndex = reference.indexOf('@');
        if (atIndex == -1) {
            return pkgName + "@" + reference;
        }
        int colonIndex = reference.indexOf(':');
        int bracketIndex = reference.indexOf('(');
        if ((colonIndex == -1 || atIndex < colonIndex) && (bracketIndex == -1 || atIndex < bracketIndex)) {
            return reference;
        }
        return pkgName + "@" + reference;
    }

    static DependencyPath parse(@Nullable Object dependencyPath) {
        if (!(dependencyPath instanceof String)) {
            throw new IllegalArgumentException("TypeError: Expected `dependencyPath` to be of type `string`, got `" +
                                               (dependencyPath == null ? "null" : Js.typeOf(dependencyPath)) + "`");
        }
        String path = (String) dependencyPath;
        int sepIndex = path.indexOf('@', 1);
        if (sepIndex == -1) {
            return new DependencyPath(null, null, null, null, null);
        }
        String name = path.substring(0, sepIndex);
        String version = path.substring(sepIndex + 1);
        if (!version.isEmpty()) {
            String peerDepGraphHash = null;
            String patchHash = null;
            SuffixIndex index = indexOfDepPathSuffix(version);
            int peersIndex = index.getPeersIndex();
            int patchHashIndex = index.getPatchHashIndex();
            if (peersIndex != -1 || patchHashIndex != -1) {
                if (peersIndex == -1) {
                    patchHash = version.substring(patchHashIndex);
                    version = version.substring(0, patchHashIndex);
                } else if (patchHashIndex == -1) {
                    peerDepGraphHash = version.substring(peersIndex);
                    version = version.substring(0, peersIndex);
                } else {
                    patchHash = version.substring(patchHashIndex, peersIndex);
                    peerDepGraphHash = version.substring(peersIndex);
                    version = version.substring(0, patchHashIndex);
                }
            }
            if (NodeSemver.valid(version, NodeSemver.Options.DEFAULT) != null) {
                return new DependencyPath(name, peerDepGraphHash, version, null, patchHash);
            }
            return new DependencyPath(name, peerDepGraphHash, null, version, patchHash);
        }
        return new DependencyPath(null, null, null, null, null);
    }

    static String depPathToFilename(String depPath, int maxLengthWithoutHash) {
        String filename = FILENAME_UNSAFE.matcher(depPathToFilenameUnescaped(depPath)).replaceAll("+");
        if (filename.contains("(")) {
            filename = filename.replaceFirst("\\)\\z", "").replaceAll("\\)\\(|\\(|\\)", "_");
        }
        if (filename.length() > maxLengthWithoutHash ||
            !filename.equals(filename.toLowerCase(Locale.ROOT)) && !filename.startsWith("file+")) {
            return filename.substring(0, Math.max(0, Math.min(filename.length(), maxLengthWithoutHash - 33))) + "_" +
                   createShortHash(filename);
        }
        return filename;
    }

    private static String depPathToFilenameUnescaped(String depPath) {
        if (!depPath.startsWith("file:")) {
            if (!depPath.isEmpty() && depPath.charAt(0) == '/') {
                depPath = depPath.substring(1);
            }
            int index = depPath.indexOf('@', 1);
            if (index == -1) {
                return depPath;
            }
            return depPath.substring(0, index) + "@" + depPath.substring(index + 1);
        }
        return depPath.replaceFirst(":", "+");
    }

    static String createPeerDepGraphHash(List<PeerId> peerIds, int maxLength) {
        List<String> ids = new ArrayList<>();
        for (PeerId peerId : peerIds) {
            if (peerId.getDepPath() == null) {
                ids.add(peerId.getName() + "@" + peerId.getVersion());
            } else if (!peerId.getDepPath().isEmpty() && peerId.getDepPath().charAt(0) == '/') {
                ids.add(peerId.getDepPath().substring(1));
            } else {
                ids.add(peerId.getDepPath());
            }
        }
        Collections.sort(ids);
        String dirName = String.join(")(", ids);
        if (dirName.length() > maxLength) {
            dirName = createShortHash(dirName);
        }
        return "(" + dirName + ")";
    }

    static boolean isRuntimeDepPath(String depPath) {
        return RUNTIME_DEP_PATH_RE.matcher(depPath).find();
    }

    /**
     * @pnpm/crypto.hash {@code createShortHash}: the first 32 hex digits of the sha256 of the UTF-8 text.
     */
    static String createShortHash(String input) {
        return createHexHash(input).substring(0, 32);
    }

    static String createHexHash(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b & 0xFF));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
