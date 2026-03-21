/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.json.format;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.style.Autodetect;
import org.openrewrite.json.style.TabsAndIndentsStyle;
import org.openrewrite.json.style.WrappingAndBracesStyle;
import org.openrewrite.json.tree.Json;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;

public class Indents extends Recipe {
    @Getter
    final String displayName = "JSON indent";

    @Getter
    final String description = "Format tabs and indents in JSON.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TabsAndIndentsFromCompilationUnitStyle();
    }

    private static class TabsAndIndentsFromCompilationUnitStyle extends JsonIsoVisitor<ExecutionContext> {
        @Override
        public Json. Document visitDocument(Json.Document docs, ExecutionContext ctx) {
            Autodetect autodetected = Autodetect.detector().sample(docs).build();
            TabsAndIndentsStyle tabsAndIndentsStyle = Style.from(TabsAndIndentsStyle.class, docs, () -> autodetected.getStyle(TabsAndIndentsStyle.class));
            WrappingAndBracesStyle wrappingAndBracesStyle = Style.from(WrappingAndBracesStyle.class, docs, () -> autodetected.getStyle(WrappingAndBracesStyle.class));
            GeneralFormatStyle generalFormatStyle = Style.from(GeneralFormatStyle.class, docs, () -> autodetected.getStyle(GeneralFormatStyle.class));
            doAfterVisit(new TabsAndIndentsVisitor<>(wrappingAndBracesStyle, tabsAndIndentsStyle, generalFormatStyle,null));
            return docs;
        }
    }
}
