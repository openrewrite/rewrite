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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/plugin-patch sources/patchUtils.ts (Yarn 4.10.3): the spec of a {@code patch:} range. The patch
 * files are not read: a builtin patch's text ships inside the release.
 */
final class PatchUtils {

    static final String PROTOCOL = "patch:";

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
        int flagIndex = patchPath.lastIndexOf('!');
        String path = flagIndex != -1 ? patchPath.substring(flagIndex + 1) : patchPath;
        return builtinName(patchPath) == null && !path.startsWith("~/") && !path.startsWith("/");
    }
}
