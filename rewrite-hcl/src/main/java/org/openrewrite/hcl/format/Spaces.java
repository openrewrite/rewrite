/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.hcl.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.style.SpacesStyle;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.style.Style;

public class Spaces extends Recipe {

    @Override
    public String getDisplayName() {
        return "Spaces";
    }

    @Override
    public String getDescription() {
        return "Format whitespace in HCL code.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SpacesFromCompilationUnitStyle();
    }

    private static class SpacesFromCompilationUnitStyle extends HclIsoVisitor<ExecutionContext> {
        @Override
        public Hcl.ConfigFile visitConfigFile(Hcl.ConfigFile cf, ExecutionContext ctx) {
            SpacesStyle style = Style.from(SpacesStyle.class, cf, () -> SpacesStyle.DEFAULT);
            return (Hcl.ConfigFile) new SpacesVisitor<>(style).visitNonNull(cf, ctx);
        }
    }
}
