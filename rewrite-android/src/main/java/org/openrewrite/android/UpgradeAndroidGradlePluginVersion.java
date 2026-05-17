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
package org.openrewrite.android;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.UpgradeDependencyVersion;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Upgrade the Android Gradle Plugin (AGP) version.
 * <p>
 * Two declaration shapes are supported:
 * <ol>
 *   <li><b>buildscript classpath</b> — {@code classpath 'com.android.tools.build:gradle:8.0.0'}
 *       — handled by composing {@link UpgradeDependencyVersion} against
 *       {@code com.android.tools.build:gradle}. Reusing the upstream recipe
 *       gets variable / version-catalog / gradle.properties resolution for free
 *       and avoids reimplementing it.</li>
 *   <li><b>plugins block</b> — {@code plugins { id("com.android.application") version "8.0.0" }}
 *       (and the Groovy DSL equivalent) — handled by a small inline visitor
 *       that mutates the version string literal directly.</li>
 * </ol>
 * The plugin ID prefixes recognized as AGP: {@code com.android.application},
 * {@code com.android.library}, {@code com.android.test}, {@code com.android.dynamic-feature},
 * {@code com.android.asset-pack}, {@code com.android.asset-pack-bundle},
 * {@code com.android.fused-library}, {@code com.android.kmp.library},
 * plus any other {@code com.android.*} identifier.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeAndroidGradlePluginVersion extends Recipe {

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "8.5.0")
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics.",
            example = "8.5.0",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Upgrade Android Gradle Plugin version";
    }

    @Override
    public String getDescription() {
        return "Upgrade the Android Gradle Plugin (AGP) version. Handles both the legacy " +
                "`buildscript { dependencies { classpath 'com.android.tools.build:gradle:...' } }` " +
                "form (delegating to the upstream `UpgradeDependencyVersion` recipe for full DSL coverage) " +
                "and the modern `plugins { id(\"com.android.application\") version \"...\" }` form.";
    }

    @Override
    public String getInstanceNameSuffix() {
        return newVersion;
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    @Override
    public List<Recipe> getRecipeList() {
        // Compose UpgradeDependencyVersion for the buildscript classpath form. This delegates
        // all of the version-variable / version-catalog / gradle.properties resolution to the
        // upstream recipe; no need to reimplement it here.
        return Collections.singletonList(
                new UpgradeDependencyVersion(
                        "com.android.tools.build",
                        "gradle",
                        newVersion,
                        versionPattern)
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator comparator = Semver.validate(newVersion, versionPattern).getValue();
        return new PluginsBlockVisitor(newVersion, comparator);
    }

    /**
     * Mutates {@code plugins { id("com.android.application") version "X" }} (and the Groovy DSL
     * equivalent {@code id 'com.android.application' version '8.0.0'}). For Groovy, the parser
     * models this as a method call chain {@code id('...').version('...')}; for Kotlin it's
     * similar with infix {@code version} or method-form. Both surface as a top-level
     * {@code J.MethodInvocation} whose name is {@code version} and whose select is the
     * {@code id(...)} invocation.
     */
    private static final class PluginsBlockVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final Set<String> AGP_PLUGIN_IDS = new HashSet<>();

        static {
            AGP_PLUGIN_IDS.add("com.android.application");
            AGP_PLUGIN_IDS.add("com.android.library");
            AGP_PLUGIN_IDS.add("com.android.test");
            AGP_PLUGIN_IDS.add("com.android.dynamic-feature");
            AGP_PLUGIN_IDS.add("com.android.asset-pack");
            AGP_PLUGIN_IDS.add("com.android.asset-pack-bundle");
            AGP_PLUGIN_IDS.add("com.android.fused-library");
            AGP_PLUGIN_IDS.add("com.android.kmp.library");
        }

        private final String configuredNewVersion;
        private final @Nullable VersionComparator comparator;
        private boolean inPluginsBlock;

        PluginsBlockVisitor(String configuredNewVersion, @Nullable VersionComparator comparator) {
            this.configuredNewVersion = configuredNewVersion;
            this.comparator = comparator;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            boolean entered = false;
            if (!inPluginsBlock && "plugins".equals(method.getSimpleName()) && hasLambdaArg(method)) {
                inPluginsBlock = true;
                entered = true;
            }
            try {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (inPluginsBlock && "version".equals(m.getSimpleName()) && m.getArguments().size() == 1) {
                    Expression versionArg = m.getArguments().get(0);
                    if (!(versionArg instanceof J.Literal)) {
                        return m;
                    }
                    J.Literal versionLit = (J.Literal) versionArg;
                    if (versionLit.getType() != JavaType.Primitive.String || !(versionLit.getValue() instanceof String)) {
                        return m;
                    }
                    if (!isAgpId(m.getSelect())) {
                        return m;
                    }
                    String currentVersion = (String) versionLit.getValue();
                    // For the plugins-block form there are no Maven repos to query, so we treat the
                    // configured newVersion as the available target. The comparator decides whether
                    // it qualifies as an upgrade (so a `latest.release` selector with no metadata
                    // is a no-op rather than an erroneous mutation).
                    if (comparator == null) {
                        return m;
                    }
                    Optional<String> upgrade = comparator.upgrade(currentVersion, Collections.singletonList(configuredNewVersion));
                    if (!upgrade.isPresent()) {
                        return m;
                    }
                    String selectedVersion = upgrade.get();
                    if (selectedVersion.equals(currentVersion)) {
                        return m;
                    }
                    String src = versionLit.getValueSource();
                    String quote = src == null || src.isEmpty() ? "\"" : src.substring(0, 1);
                    J.Literal newLit = versionLit.withValue(selectedVersion).withValueSource(quote + selectedVersion + quote);
                    return m.withArguments(Collections.singletonList(newLit));
                }
                return m;
            } finally {
                if (entered) {
                    inPluginsBlock = false;
                }
            }
        }

        private static boolean isAgpId(@Nullable Expression select) {
            if (!(select instanceof J.MethodInvocation)) {
                return false;
            }
            J.MethodInvocation idCall = (J.MethodInvocation) select;
            if (!"id".equals(idCall.getSimpleName()) || idCall.getArguments().size() != 1) {
                return false;
            }
            Expression arg = idCall.getArguments().get(0);
            if (!(arg instanceof J.Literal)) {
                return false;
            }
            Object val = ((J.Literal) arg).getValue();
            if (!(val instanceof String)) {
                return false;
            }
            String pluginId = (String) val;
            return AGP_PLUGIN_IDS.contains(pluginId) || pluginId.startsWith("com.android.");
        }

        private static boolean hasLambdaArg(J.MethodInvocation m) {
            if (m.getArguments().isEmpty()) {
                return false;
            }
            Expression last = m.getArguments().get(m.getArguments().size() - 1);
            return last instanceof J.Lambda;
        }
    }
}
