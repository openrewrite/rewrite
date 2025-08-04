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
package org.openrewrite.groovy;

import org.openrewrite.Cursor;
import org.openrewrite.groovy.internal.template.GroovySubstitutions;
import org.openrewrite.groovy.internal.template.GroovyTemplateParser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;

public class GroovyTemplate extends JavaTemplate {
    private GroovyTemplate(boolean contextSensitive, GroovyParser.Builder parser, String code, Set<String> imports, Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate) {
        super(
                code,
                emptySet(),
                onAfterVariableSubstitution,
                new GroovyTemplateParser(
                        contextSensitive,
                        augmentClasspath(parser),
                        onAfterVariableSubstitution,
                        onBeforeParseTemplate,
                        imports
                )
        );
    }

    private static GroovyParser.Builder augmentClasspath(GroovyParser.Builder parserBuilder) {
        return parserBuilder.addClasspathEntry(getTemplateClasspathDir());
    }

    @Override
    protected Substitutions substitutions(Object[] parameters) {
        return new GroovySubstitutions(getCode(), parameters);
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

        private final String code;
        private final Set<String> imports = new HashSet<>();

        private GroovyParser.Builder parser = GroovyParser.builder();

        private Consumer<String> onAfterVariableSubstitution = s -> {
        };
        private Consumer<String> onBeforeParseTemplate = s -> {
        };

        Builder(String code) {
            super(code);
            this.code = code;
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

        private void validateImport(String typeName) {
            if (StringUtils.isBlank(typeName)) {
                throw new IllegalArgumentException("Imports must not be blank");
            } else if (typeName.startsWith("import ")) {
                throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" prefix");
            } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
            }
        }

        Builder parser(GroovyParser.Builder parser) {
            this.parser = parser;
            return this;
        }

        @Override
        public Builder doAfterVariableSubstitution(Consumer<String> afterVariableSubstitution) {
            this.onAfterVariableSubstitution = afterVariableSubstitution;
            return this;
        }

        @Override
        public Builder doBeforeParseTemplate(Consumer<String> beforeParseTemplate) {
            this.onBeforeParseTemplate = beforeParseTemplate;
            return this;
        }

        @Override
        public GroovyTemplate build() {
            return new GroovyTemplate(false, parser.clone(), code, imports,
                    onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }
}
