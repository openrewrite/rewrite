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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/plugin-patch sources/patchUtils.ts and constants.ts (Yarn 4.10.3).
 */
final class PatchUtils {

    static final String PROTOCOL = "patch:";

    private static final String CACHE_VERSION = "3";
    private static final String COMPAT_TAG = "compat/";
    private static final Pattern CRLF = Pattern.compile("\\r\\n?");

    private static final Pattern BUILTIN_REGEXP = Pattern.compile("^builtin<([^>]+)>$");

    private PatchUtils() {
    }

    @Value
    static class Spec<T> {
        @Nullable
        Locator parentLocator;

        T sourceItem;
        List<String> patchPaths;

        @Nullable
        String sourceVersion;
    }

    private static <T> Spec<T> parseSpec(String spec, Function<String, T> sourceParser) {
        StructUtils.Range range = StructUtils.parseRange(spec);
        if (!PROTOCOL.equals(range.getProtocol())) {
            throw new JsError(JsError.ERROR, "Invalid patch range");
        }
        if (range.getSource() == null) {
            throw new JsError(JsError.ERROR, "Patch locators must explicitly define their source");
        }
        List<String> patchPaths = range.getSelector().isEmpty() ? new ArrayList<>() :
                new ArrayList<>(Arrays.asList(range.getSelector().split("&", -1)));
        Map<String, Object> params = range.getParams();
        Object locator = params == null ? null : params.get("locator");
        Object version = params == null ? null : params.get("version");
        return new Spec<>(locator instanceof String ? StructUtils.parseLocator((String) locator) : null,
                sourceParser.apply(range.getSource()), patchPaths, version instanceof String ? (String) version : null);
    }

    static boolean isPatchDescriptor(Descriptor descriptor) {
        return descriptor.range.startsWith(PROTOCOL);
    }

    static boolean isPatchLocator(Locator locator) {
        return locator.reference.startsWith(PROTOCOL);
    }

    static Spec<Descriptor> parseDescriptor(Descriptor descriptor) {
        return parseSpec(descriptor.range, StructUtils::parseDescriptor);
    }

    static Spec<Locator> parseLocator(Locator locator) {
        return parseSpec(locator.reference, StructUtils::parseLocator);
    }

    /**
     * {@code visitPatchPath}'s builtin case: the name in {@code builtin<name>} after the flags, else null.
     */
    static @Nullable String builtinName(String patchPath) {
        int flagIndex = patchPath.lastIndexOf('!');
        Matcher builtin = BUILTIN_REGEXP.matcher(flagIndex != -1 ? patchPath.substring(flagIndex + 1) : patchPath);
        return builtin.find() ? builtin.group(1) : null;
    }

    /**
     * {@code isParentRequired}: a relative path is read through the package that depends on the patch.
     */
    static boolean isParentRequired(String patchPath) {
        return builtinName(patchPath) == null && !withoutFlags(patchPath).startsWith("~/") &&
               !withoutFlags(patchPath).startsWith("/");
    }

    private static String withoutFlags(String patchPath) {
        int flagIndex = patchPath.lastIndexOf('!');
        return flagIndex != -1 ? patchPath.substring(flagIndex + 1) : patchPath;
    }

    /**
     * Whether a patch path is a builtin patch the release ships, whose text the engine does not hold.
     */
    static boolean isShippedBuiltin(String patchPath, YarnRelease release) {
        String name = builtinName(patchPath);
        if (name == null || !name.startsWith(COMPAT_TAG)) {
            return false;
        }
        Ident ident = StructUtils.parseIdent(name.substring(COMPAT_TAG.length()));
        return release.getPatches().contains(StructUtils.stringifyIdent(ident));
    }

    /**
     * {@code loadPatchFiles} where no path is a builtin the release ships: each file's text with LF line endings, null
     * for any other builtin (no hook answers); throws CannotKnow for a file outside the project or a non-workspace's.
     */
    static List<@Nullable String> loadPatchFiles(@Nullable Locator parentLocator, List<String> patchPaths,
                                                 Project project) {
        Workspace parent = null;
        if (parentLocator != null) {
            parent = project.tryWorkspaceByLocator(parentLocator);
            if (parent == null) {
                throw new CannotKnow("the patch files " + String.join(", ", patchPaths) + " inside " +
                                     StructUtils.stringifyLocator(parentLocator) + " (Yarn reads them from its " +
                                             "archive)");
            }
        }
        List<@Nullable String> sources = new ArrayList<>();
        for (String patchPath : patchPaths) {
            String name = builtinName(patchPath);
            String path = withoutFlags(patchPath);
            String source;
            if (name != null) {
                if (name.startsWith(COMPAT_TAG)) {
                    StructUtils.parseIdent(name.substring(COMPAT_TAG.length()));
                }
                source = null;
            } else if (path.startsWith("~/")) {
                source = project.readFile(projectPath(".", path.substring(2), patchPath));
            } else if (path.startsWith("/")) {
                throw new CannotKnow("the patch file " + path + " (a path outside the project)");
            } else {
                if (parent == null) {
                    throw new JsError(JsError.ERROR, "Assertion failed: The parent locator should have been fetched");
                }
                source = project.readFile(projectPath(parent.getRelativeCwd(), path, patchPath));
            }
            sources.add(source == null ? null : CRLF.matcher(source).replaceAll("\n"));
        }
        return sources;
    }

    private static String projectPath(String cwd, String path, String patchPath) {
        String resolved = PosixPath.normalize(".".equals(cwd) ? path : cwd + "/" + path);
        if ("..".equals(resolved) || resolved.startsWith("../")) {
            throw new CannotKnow("the patch file " + patchPath + " (a path outside the project)");
        }
        return resolved;
    }

    /**
     * {@code makePatchHash}: the parsed effects of each file the source version is not excluded from, hashed.
     */
    static String makePatchHash(List<@Nullable String> patchFiles, @Nullable String sourceVersion) {
        List<String> parts = new ArrayList<>();
        parts.add(CACHE_VERSION);
        for (String source : patchFiles) {
            if (source == null) {
                continue;
            }
            for (Map<String, Object> effect : PatchParse.parsePatchFile(source)) {
                Object semverExclusivity = effect.get("semverExclusivity");
                if (semverExclusivity != null && sourceVersion != null &&
                    !YarnpkgSemverUtils.satisfiesWithPrereleases(sourceVersion, (String) semverExclusivity, false)) {
                    continue;
                }
                Map<String, Object> effectWithoutRange = new LinkedHashMap<>(effect);
                effectWithoutRange.remove("semverExclusivity");
                parts.add(Js.jsonStringify(effectWithoutRange));
            }
        }
        return HashUtils.makeHash(parts.toArray(new String[0])).substring(0, 6);
    }

    static Locator makeLocator(Ident ident, @Nullable Locator parentLocator, Package sourcePackage,
                               List<String> patchPaths, String patchHash) {
        Map<String, @Nullable Object> params = new LinkedHashMap<>();
        params.put("version", sourcePackage.version);
        params.put("hash", patchHash);
        if (parentLocator != null) {
            params.put("locator", StructUtils.stringifyLocator(parentLocator));
        }
        return StructUtils.makeLocator(ident,
                StructUtils.makeRange(PROTOCOL, StructUtils.stringifyLocator(sourcePackage),
                String.join("&", patchPaths), params));
    }
}
