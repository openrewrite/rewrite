/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.kotlin.internal.template.KotlinBlockStatementTemplateGenerator;
import org.openrewrite.kotlin.internal.template.KotlinSubstitutions;
import org.openrewrite.kotlin.internal.template.KotlinTemplateParser;

import java.util.Set;
import java.util.function.Consumer;

public class KotlinTemplate extends JavaTemplate {
    private KotlinTemplate(boolean contextSensitive,
                           KotlinParser.Builder parser,
                           String code,
                           Set<String> imports,
                           Set<String> genericTypes,
                           String bindType,
                           Consumer<String> onAfterVariableSubstitution,
                           Consumer<String> onBeforeParseTemplate) {
        super(
                code,
                genericTypes,
                onAfterVariableSubstitution,
                new KotlinTemplateParser(
                        contextSensitive,
                        augmentClasspath(parser),
                        onAfterVariableSubstitution,
                        onBeforeParseTemplate,
                        imports,
                        bindType
                )
        );
    }

    private static KotlinParser.Builder augmentClasspath(KotlinParser.Builder parserBuilder) {
        return parserBuilder.addClasspathEntry(getTemplateClasspathDir());
    }

    @Override
    protected Substitutions substitutions(Object[] parameters) {
        return new KotlinSubstitutions(getCode(), getGenericTypes(), parameters);
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

        private KotlinParser.Builder parser = KotlinParser.builder();
        private boolean contextSensitive;
        private String bindType = KotlinBlockStatementTemplateGenerator.DEFAULT_BIND_TYPE;

        Builder(String code) {
            super(code);
        }

        // The inherited fluent methods all return JavaTemplate.Builder, which ends a chain's access to the
        // Kotlin-specific methods below. Each is narrowed so any ordering of calls stays chainable.

        @Override
        public Builder contextSensitive() {
            this.contextSensitive = true;
            return this;
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
        public Builder staticImports(String... fullyQualifiedMemberTypeNames) {
            return imports(fullyQualifiedMemberTypeNames);
        }

        @Override
        public Builder doAfterVariableSubstitution(Consumer<String> afterVariableSubstitution) {
            super.doAfterVariableSubstitution(afterVariableSubstitution);
            return this;
        }

        @Override
        public Builder doBeforeParseTemplate(Consumer<String> beforeParseTemplate) {
            super.doBeforeParseTemplate(beforeParseTemplate);
            return this;
        }

        /**
         * The declared type of the synthetic binding a context-free expression template is assigned to, and so
         * the expected type it is inferred against. Defaults to {@code Any}; set it for expressions that have
         * no type without a target, such as a lambda, a callable reference, or {@code emptyList()}.
         */
        @Override
        public Builder bindType(String bindType) {
            super.bindType(bindType);
            this.bindType = bindType;
            return this;
        }

        @Override
        public Builder genericTypes(String... genericTypes) {
            super.genericTypes(genericTypes);
            return this;
        }

        public Builder parser(KotlinParser.Builder parser) {
            this.parser = parser;
            return this;
        }

        /**
         * A Kotlin template compiles its stub with a {@link KotlinParser}, so a Java parser cannot be honoured.
         * The inherited implementation would be silently discarded by {@link #build()} rather than take effect.
         */
        @Override
        public Builder javaParser(JavaParser.Builder<?, ?> parser) {
            throw new UnsupportedOperationException(
                    "KotlinTemplate compiles its stub with a KotlinParser. Use parser(KotlinParser.Builder) instead.");
        }

        @Override
        public KotlinTemplate build() {
            return new KotlinTemplate(contextSensitive, parser.clone(), code, imports, genericTypes, bindType, onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }
}
