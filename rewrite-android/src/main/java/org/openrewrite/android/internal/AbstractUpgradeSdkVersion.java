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
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.toml.tree.Toml;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Base class shared by the three SDK upgrade recipes. Concrete subclasses
 * supply the new value, the optional floor predicate, and the LHS identifier
 * set (e.g. {@code "compileSdk"}, {@code "compileSdkVersion"}).
 * <p>
 * The recipe is a {@link ScanningRecipe} so it can collect extra-property,
 * version-catalog, and gradle-property keys discovered in build files during
 * the scan phase, then use them to drive the corresponding sibling-file
 * visitors during the visit phase.
 */
public abstract class AbstractUpgradeSdkVersion extends ScanningRecipe<AbstractUpgradeSdkVersion.Accumulator> {

    public static class Accumulator {
        public final Set<String> extraPropertyNames = new HashSet<>();
        public final Set<String> versionCatalogKeys = new HashSet<>();
        public final Set<String> gradlePropertyKeys = new HashSet<>();
    }

    /** Get the LHS identifier names that mark an SDK assignment for this recipe. */
    protected abstract Set<String> sdkAssignmentNames();

    /** Target SDK integer value. */
    protected abstract int newValue();

    /**
     * Decide whether the current value should trigger an upgrade. The default
     * implementation refuses downgrades: {@code current < newValue}. The
     * {@code targetSdk} recipe may override to add a {@code minSdkFloor} check.
     */
    protected IntPredicate currentValueAcceptable() {
        return current -> current < newValue();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile) || !UpgradeSdkVersionVisitor.isBuildGradle((SourceFile) tree)) {
                    return tree;
                }
                UpgradeSdkVersionVisitor scan = new UpgradeSdkVersionVisitor(
                        sdkAssignmentNames(),
                        newValue(),
                        currentValueAcceptable(),
                        false);
                scan.visit(tree, ctx);
                for (UpgradeSdkVersionVisitor.Finding f : scan.getFindings()) {
                    switch (f.source.getKind()) {
                        case EXTRA_PROPERTY:
                            if (f.source.getDetail() != null) {
                                acc.extraPropertyNames.add(f.source.getDetail());
                            }
                            break;
                        case VERSION_CATALOG:
                            if (f.source.getDetail() != null) {
                                acc.versionCatalogKeys.add(f.source.getDetail());
                            }
                            break;
                        case GRADLE_PROPERTIES:
                            if (f.source.getDetail() != null) {
                                acc.gradlePropertyKeys.add(f.source.getDetail());
                            }
                            break;
                        default:
                            break;
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sf, ExecutionContext ctx) {
                if (UpgradeSdkVersionVisitor.isBuildGradle(sf)) {
                    return true;
                }
                if (sf instanceof Properties.File) {
                    return sf.getSourcePath().toString().endsWith("gradle.properties");
                }
                if (sf instanceof Toml.Document) {
                    return sf.getSourcePath().toString().endsWith("libs.versions.toml");
                }
                return false;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sf = (SourceFile) tree;
                if (UpgradeSdkVersionVisitor.isBuildGradle(sf)) {
                    UpgradeSdkVersionVisitor primary = new UpgradeSdkVersionVisitor(
                            sdkAssignmentNames(),
                            newValue(),
                            currentValueAcceptable(),
                            true);
                    Tree after = primary.visit(tree, ctx);
                    if (!acc.extraPropertyNames.isEmpty()) {
                        UpgradeSdkExtraPropertyVisitor extra = new UpgradeSdkExtraPropertyVisitor(
                                acc.extraPropertyNames, newValue());
                        after = extra.visit(after, ctx);
                    }
                    return after;
                }
                if (sf instanceof Properties.File && !acc.gradlePropertyKeys.isEmpty()) {
                    return new UpgradeSdkGradlePropertiesVisitor(acc.gradlePropertyKeys, newValue()).visit(sf, ctx);
                }
                if (sf instanceof Toml.Document && !acc.versionCatalogKeys.isEmpty()) {
                    return new UpgradeSdkVersionCatalogVisitor(acc.versionCatalogKeys, newValue()).visit(sf, ctx);
                }
                return tree;
            }
        };
    }
}
