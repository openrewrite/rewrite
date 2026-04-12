/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.scala.internal.template.ScalaSubstitutions;
import org.openrewrite.scala.internal.template.ScalaTemplateParser;

import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;

public class ScalaTemplate extends JavaTemplate {
    private ScalaTemplate(boolean contextSensitive,
                          ScalaParser.Builder parser,
                          String code,
                          Set<String> imports,
                          Consumer<String> onAfterVariableSubstitution,
                          Consumer<String> onBeforeParseTemplate) {
        super(
                code,
                emptySet(),
                onAfterVariableSubstitution,
                new ScalaTemplateParser(
                        contextSensitive,
                        augmentClasspath(parser),
                        onAfterVariableSubstitution,
                        onBeforeParseTemplate,
                        imports
                )
        );
    }

    private static ScalaParser.Builder augmentClasspath(ScalaParser.Builder parserBuilder) {
        return parserBuilder.addClasspathEntry(getTemplateClasspathDir());
    }

    @Override
    protected Substitutions substitutions(Object[] parameters) {
        return new ScalaSubstitutions(getCode(), parameters);
    }

    public static <J2 extends J> J2 apply(String template, Cursor scope, JavaCoordinates coordinates, Object... parameters) {
        return builder(template).build().apply(scope, coordinates, parameters);
    }

    public static Builder builder(String code) {
        return new Builder(code);
    }

    public static boolean matches(String template, Cursor cursor) {
        return builder(template).build().matches(cursor);
    }

    @SuppressWarnings("unused")
    public static class Builder extends JavaTemplate.Builder {

        private ScalaParser.Builder parser = ScalaParser.builder();

        Builder(String code) {
            super(code);
        }

        @Override
        public JavaTemplate.Builder contextSensitive() {
            throw new UnsupportedOperationException("Only context-free templates are supported");
        }

        @Override
        public Builder imports(String... fullyQualifiedTypeNames) {
            for (String typeName : fullyQualifiedTypeNames) {
                validateImport(typeName);
                this.imports.add("import " + typeName + "\n");
            }
            return this;
        }

        @Override
        public JavaTemplate.Builder staticImports(String... fullyQualifiedMemberTypeNames) {
            return imports(fullyQualifiedMemberTypeNames);
        }

        public Builder parser(ScalaParser.Builder parser) {
            this.parser = parser;
            return this;
        }

        @Override
        public ScalaTemplate build() {
            return new ScalaTemplate(false, parser.clone(), code, imports, onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }
}
