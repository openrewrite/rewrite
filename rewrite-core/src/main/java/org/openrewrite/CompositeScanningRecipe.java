/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Simplifies the syntax for invoking a ScanningRecipe from another ScanningRecipe,
 * by implementing the boilerplate code to call the base ScanningRecipe methods
 * and providing new abstract methods for the new recipe's ScanningRecipe lifecycle.
 */
public abstract class CompositeScanningRecipe<T, R extends ScanningRecipe<RT>, RT> extends ScanningRecipe<Map.Entry<T, RT>> {

    protected R baseRecipe;

    protected CompositeScanningRecipe() {
        this.baseRecipe = getBaseRecipe();
    }

    protected abstract R getBaseRecipe();

    @Override
    public final Map.Entry<T, RT> getInitialValue(ExecutionContext ctx) {
        return new AbstractMap.SimpleEntry<>(getInitialValuez(ctx), baseRecipe.getInitialValue(ctx));
    }

    protected abstract T getInitialValuez(ExecutionContext ctx);

    @Override
    public final TreeVisitor<?, ExecutionContext> getScanner(Map.Entry<T, RT> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                baseRecipe.getScanner(acc.getValue()).visit(tree, executionContext);
                getScannerz(acc.getKey()).visit(tree, executionContext);
                return tree;
            }
        };
    }

    protected abstract TreeVisitor<?, ExecutionContext> getScannerz(T acc);

    @Override
    public final Collection<? extends SourceFile> generate(Map.Entry<T, RT> acc, Collection<SourceFile> generatedInThisCycle, ExecutionContext ctx) {
        if (baseRecipeShouldMakeChanges(acc.getKey())) {
            Collection<? extends SourceFile> generatedByBase = baseRecipe.generate(acc.getValue(), generatedInThisCycle, ctx);
            Collection<SourceFile> newGeneratedInThisCycle = new ArrayList<>();
            newGeneratedInThisCycle.addAll(generatedInThisCycle);
            newGeneratedInThisCycle.addAll(generatedByBase);
            Collection<? extends SourceFile> generatedBySubclass = generatez(acc.getKey(), newGeneratedInThisCycle, ctx);
            Collection<SourceFile> result = new ArrayList<>();
            result.addAll(generatedBySubclass);
            result.addAll(generatedByBase);
            return result;
        } else {
            return generatez(acc.getKey(), generatedInThisCycle, ctx);
        }
    }

    protected Collection<? extends SourceFile> generatez(T acc, Collection<SourceFile> generatedInThisCycle, ExecutionContext ctx) {
        return generatez(acc, ctx);
    }

    @Override
    public final Collection<? extends SourceFile> generate(Map.Entry<T, RT> acc, ExecutionContext ctx) {
        if (baseRecipeShouldMakeChanges(acc.getKey())) {
            Collection<? extends SourceFile> generatedByBase = baseRecipe.generate(acc.getValue(), ctx);
            Collection<? extends SourceFile> generatedBySubclass = generatez(acc.getKey(), ctx);
            Collection<SourceFile> result = new ArrayList<>();
            result.addAll(generatedBySubclass);
            result.addAll(generatedByBase);
            return result;
        } else {
            return baseRecipe.generate(acc.getValue(), ctx);
        }
    }

    protected Collection<? extends SourceFile> generatez(T acc, ExecutionContext ctx) {
        return Collections.emptyList();
    }

    @Override
    public final TreeVisitor<?, ExecutionContext> getVisitor(Map.Entry<T, RT> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                Tree t = tree;
                if (baseRecipeShouldMakeChanges(acc.getKey())) {
                    t = baseRecipe.getVisitor(acc.getValue()).visit(t, executionContext);
                }
                t = getScannerz(acc.getKey()).visit(t, executionContext);
                return t;
            }
        };
    }

    protected TreeVisitor<?, ExecutionContext> getVisitorz(T acc) {
        return TreeVisitor.noop();
    }

    protected abstract boolean baseRecipeShouldMakeChanges(T acc);
}
