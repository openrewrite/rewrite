/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.test;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Value
@EqualsAndHashCode(callSuper = false)
public class AdHocRecipe extends Recipe {
    @With
    @Nullable
    @Language("markdown")
    String displayName;

    @With
    @Nullable
    String name;

    @With
    @Nullable
    Boolean causesAnotherCycle;

    @With
    Supplier<TreeVisitor<?, ExecutionContext>> getVisitor;

    @Nullable
    @With
    BiFunction<List<SourceFile>, ExecutionContext, List<SourceFile>> visit;

    public String getDisplayName() {
        return StringUtils.isBlank(displayName) ? "Ad hoc recipe" : displayName;
    }

    public String getName() {
        return StringUtils.isBlank(name) ? super.getName() : name;
    }

    @Override
    public boolean causesAnotherCycle() {
        return causesAnotherCycle == null ? super.causesAnotherCycle() : causesAnotherCycle;
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        return visit == null ? before : visit.apply(before, ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return getVisitor.get();
    }
}
