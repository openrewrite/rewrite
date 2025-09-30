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

import org.openrewrite.*;
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.style.Style;

public class BlankLines extends Recipe {

    @Override
    public String getDisplayName() {
        return "Blank lines";
    }

    @Override
    public String getDescription() {
        return "Add and/or remove blank lines.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new BlankLinesFromCompilationUnitStyle();
    }

    private static class BlankLinesFromCompilationUnitStyle extends HclIsoVisitor<ExecutionContext> {
        @Override
        public Hcl.ConfigFile visitConfigFile(Hcl.ConfigFile configFile, ExecutionContext ctx) {
            BlankLinesStyle style = Style.from(BlankLinesStyle.class, configFile, () -> BlankLinesStyle.DEFAULT);
            return (Hcl.ConfigFile) new BlankLinesVisitor<>(style).visitNonNull(configFile, ctx);
        }
    }

    public static <H extends Hcl> H formatBlankLines(Hcl j, Cursor cursor) {
        BlankLinesStyle style = Style.from(BlankLinesStyle.class, cursor.firstEnclosingOrThrow(SourceFile.class));
        //noinspection unchecked
        return (H) new BlankLinesVisitor<>(style == null ? BlankLinesStyle.DEFAULT : style)
                .visitNonNull(j, 0, cursor);
    }
}
