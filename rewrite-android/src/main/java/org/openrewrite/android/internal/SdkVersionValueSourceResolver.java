/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.android.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the value-source kind of an SDK assignment's right-hand side.
 * See {@link SdkVersionValueSource} for the categories.
 * <p>
 * Reusable across all three SDK upgrade recipes (compileSdk / minSdk /
 * targetSdk), and across both Groovy DSL and Kotlin DSL build scripts.
 */
public final class SdkVersionValueSourceResolver {

    /**
     * The set of LHS identifiers that name an Android SDK assignment slot
     * in either DSL. Recipes use these to detect candidate sites.
     */
    public static final Set<String> COMPILE_SDK_NAMES = unmodifiable("compileSdk", "compileSdkVersion");
    public static final Set<String> MIN_SDK_NAMES = unmodifiable("minSdk", "minSdkVersion");
    public static final Set<String> TARGET_SDK_NAMES = unmodifiable("targetSdk", "targetSdkVersion");

    private static final Pattern ANDROID_NUMBER_PATTERN = Pattern.compile("^(android-)?(\\d+)$");

    private SdkVersionValueSourceResolver() {
    }

    private static Set<String> unmodifiable(String... names) {
        return java.util.Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names)));
    }

    /**
     * Classify the value source of an SDK assignment RHS.
     */
    public static SdkVersionValueSource resolve(Expression value) {
        if (value instanceof J.Literal) {
            J.Literal literal = (J.Literal) value;
            JavaType.Primitive type = literal.getType();
            Object literalValue = literal.getValue();
            if (type == JavaType.Primitive.Int && literalValue instanceof Integer) {
                return SdkVersionValueSource.literalInt((Integer) literalValue);
            }
            if (type == JavaType.Primitive.String && literalValue instanceof String) {
                Matcher m = ANDROID_NUMBER_PATTERN.matcher((String) literalValue);
                if (m.matches()) {
                    String prefix = m.group(1) == null ? "" : m.group(1);
                    return SdkVersionValueSource.literalString(Integer.parseInt(m.group(2)), prefix);
                }
                return SdkVersionValueSource.unresolved("string literal `" + literalValue + "` does not match `android-N` or `N`");
            }
            return SdkVersionValueSource.unresolved("literal of unexpected type " + type);
        }

        if (value instanceof J.Identifier) {
            // `compileSdk = compileSdkVersion` -> extra property reference in same script
            return SdkVersionValueSource.extraProperty(((J.Identifier) value).getSimpleName());
        }

        if (value instanceof J.MethodInvocation) {
            SdkVersionValueSource sub = resolveMethodInvocation((J.MethodInvocation) value);
            if (sub != null) {
                return sub;
            }
        }

        if (value instanceof J.FieldAccess) {
            String access = render((J.FieldAccess) value);
            String catalogKey = matchVersionCatalogFieldAccess(access);
            if (catalogKey != null) {
                return SdkVersionValueSource.versionCatalog(catalogKey);
            }
            return SdkVersionValueSource.unresolved("unrecognized field access `" + access + "`");
        }

        return SdkVersionValueSource.unresolved(value.getClass().getSimpleName() + " not recognized as an SDK value source");
    }

    /**
     * Match call chains of the shape:
     * <ul>
     *   <li>{@code libs.versions.compileSdk.get()} - returns "compileSdk"</li>
     *   <li>{@code libs.versions.compileSdk.get().toInt()} - returns "compileSdk"</li>
     *   <li>{@code providers.gradleProperty("compileSdk").get()} - returns properties source</li>
     *   <li>{@code providers.gradleProperty("compileSdk").get().toInt()} - returns properties source</li>
     *   <li>{@code Integer.parseInt(...)} / {@code .toInt()} wrappers - recurse into receiver</li>
     * </ul>
     */
    private static @Nullable SdkVersionValueSource resolveMethodInvocation(J.MethodInvocation method) {
        String name = method.getSimpleName();
        Expression select = method.getSelect();

        // `Integer.parseInt(...)` wrapper
        if ("parseInt".equals(name) && method.getArguments().size() == 1) {
            Expression argInner = method.getArguments().get(0);
            if (argInner instanceof J.MethodInvocation) {
                return resolveMethodInvocation((J.MethodInvocation) argInner);
            }
        }

        // `.toInt()` / `.toInteger()` / `.get()` are transparent wrappers; recurse into the receiver
        if (("toInt".equals(name) || "toInteger".equals(name) || "get".equals(name)) && noArgs(method)) {
            if (select instanceof J.MethodInvocation) {
                return resolveMethodInvocation((J.MethodInvocation) select);
            }
            if (select instanceof J.FieldAccess) {
                String access = render((J.FieldAccess) select);
                String catalogKey = matchVersionCatalogFieldAccess(access);
                if (catalogKey != null) {
                    return SdkVersionValueSource.versionCatalog(catalogKey);
                }
            }
            // After an unmatched wrapper we cannot say more — fall through to UNRESOLVED below.
            return null;
        }

        // `providers.gradleProperty("foo")`
        if ("gradleProperty".equals(name) && method.getArguments().size() == 1 &&
                method.getArguments().get(0) instanceof J.Literal &&
                ((J.Literal) method.getArguments().get(0)).getValue() instanceof String &&
                select instanceof J.Identifier && "providers".equals(((J.Identifier) select).getSimpleName())) {
            return SdkVersionValueSource.gradleProperty((String) ((J.Literal) method.getArguments().get(0)).getValue());
        }

        // `project.findProperty("foo")` / `project.property("foo")` -> gradle.properties
        if (("findProperty".equals(name) || "property".equals(name)) && method.getArguments().size() == 1 &&
                method.getArguments().get(0) instanceof J.Literal &&
                ((J.Literal) method.getArguments().get(0)).getValue() instanceof String) {
            return SdkVersionValueSource.gradleProperty((String) ((J.Literal) method.getArguments().get(0)).getValue());
        }

        return null;
    }

    /**
     * Match {@code libs.versions.<key>} where {@code <key>} can be a single identifier
     * or a dotted segment (e.g. {@code libs.versions.compileSdk}). Returns the key (the
     * portion after {@code versions.}). If the field access doesn't match, returns null.
     * <p>
     * The catalog key is rendered with hyphens (the canonical TOML form). Gradle
     * normalizes hyphens to camelCase in the generated accessor, so
     * {@code libs.versions.compileSdkVersion} and {@code libs.versions.compile-sdk-version}
     * both resolve here; we return the camelCase form as-typed and let the caller
     * map to TOML by trying both forms.
     */
    private static @Nullable String matchVersionCatalogFieldAccess(String rendered) {
        // Expect "libs.versions.<key...>"
        if (!rendered.startsWith("libs.versions.")) {
            return null;
        }
        String key = rendered.substring("libs.versions.".length());
        if (key.isEmpty()) {
            return null;
        }
        return key;
    }

    private static boolean noArgs(J.MethodInvocation method) {
        List<org.openrewrite.java.tree.Expression> args = method.getArguments();
        return args.isEmpty() || (args.size() == 1 && args.get(0) instanceof J.Empty);
    }

    private static String render(J.FieldAccess fa) {
        StringBuilder sb = new StringBuilder();
        renderInto(fa, sb);
        return sb.toString();
    }

    private static void renderInto(Expression e, StringBuilder sb) {
        if (e instanceof J.FieldAccess) {
            J.FieldAccess fa = (J.FieldAccess) e;
            renderInto(fa.getTarget(), sb);
            sb.append('.').append(fa.getSimpleName());
        } else if (e instanceof J.Identifier) {
            sb.append(((J.Identifier) e).getSimpleName());
        } else {
            sb.append('?');
        }
    }

    /**
     * Try common camelCase/hyphenation/dotted variants of a version-catalog key
     * to match TOML keys. Returns candidates in priority order.
     */
    public static List<String> versionCatalogTomlKeyCandidates(String rawKey) {
        Set<String> out = new java.util.LinkedHashSet<>();
        out.add(rawKey);
        // camelCase to kebab-case
        out.add(camelToKebab(rawKey));
        // dot-separated to kebab-case (libs.versions.compile.sdk -> compile-sdk)
        out.add(rawKey.replace('.', '-'));
        // dot-separated to camelCase
        out.add(dotToCamel(rawKey));
        return new java.util.ArrayList<>(out);
    }

    private static String camelToKebab(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('-').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String dotToCamel(String s) {
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
