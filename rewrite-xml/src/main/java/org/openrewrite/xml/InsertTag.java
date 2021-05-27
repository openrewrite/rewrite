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
package org.openrewrite.xml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

/**
 * Hypothetical + demonstration.
 * "AddTagByPath"
 * "AddTag"
 * "AddContentByPath"
 * etc.
 * <p>
 * todo
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class InsertTag extends Recipe {

    @Option(displayName = "XPath expression",
            description = "XPath expression for selecting the scope of the xml tag content to add.",
            example = "/project/build/plugins/plugin[artifactId=\"rewrite-maven-plugin\"]/configuration/activeRecipes/recipe")
    String xpathExpression;

    @Option(displayName = "Value",
            description = "Content of what to add at this particular path",
            example = "org.openrewrite.java.format.AutoFormat")
    String value;

    @Override
    public String getDisplayName() {
        return "Add content by path";
    }

    @Override
    public String getDescription() {
        return "Add content by xpath matcher path.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new InsertTagVisitor<>();
    }

    private class InsertTagVisitor<P> extends XmlVisitor<P> {
    }
}
