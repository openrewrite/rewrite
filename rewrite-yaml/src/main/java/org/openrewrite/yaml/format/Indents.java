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
package org.openrewrite.yaml.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.style.Autodetect;
import org.openrewrite.yaml.style.IndentsStyle;
import org.openrewrite.yaml.style.YamlDefaultStyles;
import org.openrewrite.yaml.tree.Yaml;

public class Indents extends Recipe {
    @Override
    public String getDisplayName() {
        return "YAML indent";
    }

    @Override
    public String getDescription() {
        return "Format tabs and indents in YAML.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TabsAndIndentsFromCompilationUnitStyle();
    }

    private static class TabsAndIndentsFromCompilationUnitStyle extends YamlIsoVisitor<ExecutionContext> {
        @Override
        public Yaml.Documents visitDocuments(Yaml.Documents docs, ExecutionContext ctx) {
            IndentsStyle style = docs.getStyle(IndentsStyle.class);
            if (style == null) {
                style = Autodetect.tabsAndIndents(docs, YamlDefaultStyles.indents());
            }
            doAfterVisit(new IndentsVisitor<>(style, null));
            return docs;
        }
    }
}
