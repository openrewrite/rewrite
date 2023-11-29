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
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Value
@EqualsAndHashCode(callSuper = false)
public class AdHocScanningRecipe extends ScanningRecipe<Void> {
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
    Supplier<Collection<SourceFile>> generator;

    @With
    @Nullable
    List<Maintainer> maintainers;

    @With
    @Nullable
    Integer maxCycles;

    public String getDisplayName() {
        return StringUtils.isBlank(displayName) ? "Ad hoc recipe" : displayName;
    }

    @Override
    public String getDescription() {
        return "An ad hoc recipe used in RewriteTest.";
    }

    public String getName() {
        return StringUtils.isBlank(name) ? super.getName() : name;
    }

    @Override
    public boolean causesAnotherCycle() {
        return causesAnotherCycle == null ? super.causesAnotherCycle() : causesAnotherCycle;
    }

    @Override
    public int maxCycles() {
        return maxCycles == null ? super.maxCycles() : maxCycles;
    }

    public List<Maintainer> getMaintainers() {
        return maintainers == null ? Collections.emptyList() : maintainers;
    }

    @Override
    public Void getInitialValue(ExecutionContext ctx) {
        return null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Void acc) {
        return TreeVisitor.noop();
    }

    @Override
    public Collection<? extends SourceFile> generate(Void acc, ExecutionContext ctx) {
        return generator == null ? Collections.emptyList() : generator.get();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Void acc) {
        return getVisitor.get();
    }
}
