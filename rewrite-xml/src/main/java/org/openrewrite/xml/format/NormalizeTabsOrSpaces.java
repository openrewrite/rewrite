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
package org.openrewrite.xml.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.style.TabsAndIndentsStyle;
import org.openrewrite.xml.tree.Xml;

public class NormalizeTabsOrSpaces extends Recipe {

    @Override
    public String getDisplayName() {
        return "Normalize to tabs or spaces";
    }

    @Override
    public String getDescription() {
        return "Consistently use either tabs or spaces in indentation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TabsAndIndentsFromCompilationUnitStyle();
    }

    private static class TabsAndIndentsFromCompilationUnitStyle extends XmlIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
            TabsAndIndentsStyle style = document.getStyle(TabsAndIndentsStyle.class);
            if (style == null) {
                style = TabsAndIndentsStyle.DEFAULT;
            }
            return new NormalizeTabsOrSpacesVisitor<>(style).visit(cu, ctx);
            return document;
        }
    }
}
