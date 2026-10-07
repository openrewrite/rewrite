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
import org.openrewrite.javascript.internal.lock.npm.UriComponent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/core sources/structUtils.ts (Yarn 4.10.3): making, parsing and printing idents,
 * descriptors, locators and ranges.
 */
final class StructUtils {

    private static final String VIRTUAL_PROTOCOL = "virtual:";
    // JavaScript's `.`, which unlike Java's also matches U+0085
    private static final String DOT = "[^\\n\\r\\u2028\\u2029]";
    private static final Pattern IDENT_REGEXP = Pattern.compile("^(?:@([^/]+?)/)?([^@/]+)\\z");
    private static final Pattern DESCRIPTOR_REGEX_STRICT =
            Pattern.compile("^(?:@([^/]+?)/)?([^@/]+?)(?:@(" + DOT + "+))\\z");
    private static final Pattern DESCRIPTOR_REGEX_LOOSE =
            Pattern.compile("^(?:@([^/]+?)/)?([^@/]+?)(?:@(" + DOT + "+))?\\z");
    private static final Pattern RANGE_REGEX = Pattern.compile("^([^#:]*:)?((?:(?!::)[^#])*)(?:#((?:(?!::)" + DOT +
                                                               ")*))?(?:::(" + DOT + "*))?\\z");
    private static final Pattern VIRTUAL_PREFIX_REGEXP = Pattern.compile("^[^#]*#");

    private StructUtils() {
    }

    static Ident makeIdent(@Nullable String scope, String name) {
        if (scope != null && scope.startsWith("@")) {
            throw new IllegalArgumentException("Invalid scope: don't prefix it with '@'");
        }
        return new Ident(HashUtils.makeHash(scope, name), scope, name);
    }

    static Descriptor makeDescriptor(Ident ident, String range) {
        return new Descriptor(ident.identHash, ident.scope, ident.name, HashUtils.makeHash(ident.identHash, range),
                range);
    }

    static Locator makeLocator(Ident ident, String reference) {
        return new Locator(ident.identHash, ident.scope, ident.name, HashUtils.makeHash(ident.identHash, reference),
                reference);
    }

    static Ident convertToIdent(Ident source) {
        return new Ident(source.identHash, source.scope, source.name);
    }

    static Locator convertDescriptorToLocator(Descriptor descriptor) {
        return new Locator(descriptor.identHash, descriptor.scope, descriptor.name, descriptor.descriptorHash,
                descriptor.range);
    }

    static Descriptor convertLocatorToDescriptor(Locator locator) {
        return new Descriptor(locator.identHash, locator.scope, locator.name, locator.locatorHash,
                locator.reference);
    }

    static Locator convertPackageToLocator(Package pkg) {
        return new Locator(pkg.identHash, pkg.scope, pkg.name, pkg.locatorHash, pkg.reference);
    }

    static Package renamePackage(Package pkg, Locator locator) {
        return new Package(locator, pkg.version, pkg.languageName, pkg.linkType, pkg.conditions,
                new LinkedHashMap<>(pkg.dependencies), new LinkedHashMap<>(pkg.peerDependencies),
                new LinkedHashMap<>(pkg.dependenciesMeta), new LinkedHashMap<>(pkg.peerDependenciesMeta),
                new LinkedHashMap<>(pkg.bin));
    }

    static Package copyPackage(Package pkg) {
        return renamePackage(pkg, pkg);
    }

    static Descriptor virtualizeDescriptor(Descriptor descriptor, String entropy) {
        if (entropy.contains("#")) {
            throw new IllegalArgumentException("Invalid entropy");
        }
        return makeDescriptor(descriptor, "virtual:" + entropy + "#" + descriptor.range);
    }

    static Package virtualizePackage(Package pkg, String entropy) {
        if (entropy.contains("#")) {
            throw new IllegalArgumentException("Invalid entropy");
        }
        return renamePackage(pkg, makeLocator(pkg, "virtual:" + entropy + "#" + pkg.reference));
    }

    static boolean isVirtualDescriptor(Descriptor descriptor) {
        return descriptor.range.startsWith(VIRTUAL_PROTOCOL);
    }

    static boolean isVirtualLocator(Locator locator) {
        return locator.reference.startsWith(VIRTUAL_PROTOCOL);
    }

    static Descriptor devirtualizeDescriptor(Descriptor descriptor) {
        if (!isVirtualDescriptor(descriptor)) {
            throw new IllegalArgumentException("Not a virtual descriptor");
        }
        return makeDescriptor(descriptor, VIRTUAL_PREFIX_REGEXP.matcher(descriptor.range).replaceFirst(""));
    }

    static Locator devirtualizeLocator(Locator locator) {
        if (!isVirtualLocator(locator)) {
            throw new IllegalArgumentException("Not a virtual descriptor");
        }
        return makeLocator(locator, VIRTUAL_PREFIX_REGEXP.matcher(locator.reference).replaceFirst(""));
    }

    static Descriptor bindDescriptor(Descriptor descriptor, Map<String, String> params) {
        if (descriptor.range.contains("::")) {
            return descriptor;
        }
        return makeDescriptor(descriptor, descriptor.range + "::" + QueryString.stringify(params));
    }

    static Locator bindLocator(Locator locator, Map<String, String> params) {
        if (locator.reference.contains("::")) {
            return locator;
        }
        return makeLocator(locator, locator.reference + "::" + QueryString.stringify(params));
    }

    static boolean areIdentsEqual(Ident a, Ident b) {
        return a.identHash.equals(b.identHash);
    }

    static boolean areDescriptorsEqual(Descriptor a, Descriptor b) {
        return a.descriptorHash.equals(b.descriptorHash);
    }

    static boolean areLocatorsEqual(Locator a, Locator b) {
        return a.locatorHash.equals(b.locatorHash);
    }

    static Ident parseIdent(String string) {
        Ident ident = tryParseIdent(string);
        if (ident == null) {
            throw new IllegalArgumentException("Invalid ident (" + string + ")");
        }
        return ident;
    }

    static @Nullable Ident tryParseIdent(String string) {
        Matcher match = IDENT_REGEXP.matcher(string);
        if (!match.find()) {
            return null;
        }
        return makeIdent(match.group(1), match.group(2));
    }

    static Descriptor parseDescriptor(String string) {
        return parseDescriptor(string, false);
    }

    static Descriptor parseDescriptor(String string, boolean strict) {
        Descriptor descriptor = tryParseDescriptor(string, strict);
        if (descriptor == null) {
            throw new IllegalArgumentException("Invalid descriptor (" + string + ")");
        }
        return descriptor;
    }

    static @Nullable Descriptor tryParseDescriptor(String string) {
        return tryParseDescriptor(string, false);
    }

    static @Nullable Descriptor tryParseDescriptor(String string, boolean strict) {
        Matcher match = (strict ? DESCRIPTOR_REGEX_STRICT : DESCRIPTOR_REGEX_LOOSE).matcher(string);
        if (!match.find()) {
            return null;
        }
        String range = match.group(3);
        if ("unknown".equals(range)) {
            throw new IllegalArgumentException("Invalid range (" + string + ")");
        }
        return makeDescriptor(makeIdent(match.group(1), match.group(2)), range != null ? range : "unknown");
    }

    static Locator parseLocator(String string) {
        return parseLocator(string, false);
    }

    static Locator parseLocator(String string, boolean strict) {
        Locator locator = tryParseLocator(string, strict);
        if (locator == null) {
            throw new IllegalArgumentException("Invalid locator (" + string + ")");
        }
        return locator;
    }

    static @Nullable Locator tryParseLocator(String string, boolean strict) {
        Matcher match = (strict ? DESCRIPTOR_REGEX_STRICT : DESCRIPTOR_REGEX_LOOSE).matcher(string);
        if (!match.find()) {
            return null;
        }
        String reference = match.group(3);
        if ("unknown".equals(reference)) {
            throw new IllegalArgumentException("Invalid reference (" + string + ")");
        }
        return makeLocator(makeIdent(match.group(1), match.group(2)), reference != null ? reference : "unknown");
    }

    /**
     * What {@code parseRange} returns; a {@code selector} parsed as a query string under {@code parseSelector}
     * is in {@link #selectorParams}.
     */
    @Value
    static class Range {
        @Nullable
        String protocol;

        @Nullable
        String source;

        String selector;

        @Nullable
        Map<String, Object> selectorParams;

        @Nullable
        Map<String, Object> params;
    }

    /**
     * {@code parseRange}'s options; a {@code requireProtocol} of {@code ""} means any protocol.
     */
    @Value
    static class RangeOptions {
        static final RangeOptions NONE = new RangeOptions(false, null, false, false);

        boolean requireBindings;

        @Nullable
        String requireProtocol;

        boolean requireSource;
        boolean parseSelector;
    }

    static Range parseRange(String range) {
        return parseRange(range, RangeOptions.NONE);
    }

    /**
     * @throws IllegalArgumentException where Yarn throws, a malformed percent escape included (URIError).
     */
    static Range parseRange(String range, RangeOptions opts) {
        Matcher match = RANGE_REGEX.matcher(range);
        if (!match.find()) {
            throw new IllegalArgumentException("Invalid range (" + range + ")");
        }
        String protocol = match.group(1);
        if (opts.requireProtocol != null && !opts.requireProtocol.isEmpty() &&
            !opts.requireProtocol.equals(protocol)) {
            throw new IllegalArgumentException("Invalid protocol (" + protocol + ")");
        } else if (opts.requireProtocol != null && protocol == null) {
            throw new IllegalArgumentException("Missing protocol (" + protocol + ")");
        }
        String source = match.group(3) != null ? UriComponent.decodeURIComponent(match.group(2)) : null;
        if (opts.requireSource && source == null) {
            throw new IllegalArgumentException("Missing source (" + range + ")");
        }
        String rawSelector = match.group(3) != null ? UriComponent.decodeURIComponent(match.group(3)) :
                UriComponent.decodeURIComponent(match.group(2));
        Map<String, Object> selectorParams = opts.parseSelector ? QueryString.parse(rawSelector) : null;
        Map<String, Object> params = match.group(4) != null ? QueryString.parse(match.group(4)) : null;
        if (opts.requireBindings && params == null) {
            throw new IllegalArgumentException("Missing bindings (" + range + ")");
        }
        return new Range(protocol, source, rawSelector, selectorParams, params);
    }

    static @Nullable Range tryParseRange(String range, RangeOptions opts) {
        try {
            return parseRange(range, opts);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String encodeUnsafeCharacters(String str) {
        return str.replace("%", "%25").replace(":", "%3A").replace("#", "%23");
    }

    static String makeRange(@Nullable String protocol, @Nullable String source, String selector,
                            @Nullable Map<String, ?> params) {
        StringBuilder range = new StringBuilder();
        if (protocol != null) {
            range.append(protocol);
        }
        if (source != null) {
            range.append(encodeUnsafeCharacters(source)).append('#');
        }
        range.append(encodeUnsafeCharacters(selector));
        if (params != null && !params.isEmpty()) {
            range.append("::").append(QueryString.stringify(params));
        }
        return range.toString();
    }

    static String convertToManifestRange(String range) {
        Range parsed = parseRange(range);
        Map<String, Object> params = parsed.params;
        if (params != null) {
            params.keySet().removeIf(name -> name.startsWith("__"));
        }
        return makeRange(parsed.protocol, parsed.source, parsed.selector, params);
    }

    static String stringifyIdent(Ident ident) {
        return ident.scope != null && !ident.scope.isEmpty() ? "@" + ident.scope + "/" + ident.name : ident.name;
    }

    static String stringifyDescriptor(Descriptor descriptor) {
        return stringifyIdent(descriptor) + "@" + descriptor.range;
    }

    static String stringifyLocator(Locator locator) {
        return stringifyIdent(locator) + "@" + locator.reference;
    }

    static String getIdentVendorPath(Ident ident) {
        return "node_modules/" + stringifyIdent(ident);
    }

    /**
     * {@code sortDescriptors}: by ident, then by range.
     */
    static List<Descriptor> sortDescriptors(Iterable<Descriptor> descriptors) {
        return MiscUtils.sortMap(descriptors, StructUtils::stringifyIdent, d -> d.range);
    }
}
