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
package org.openrewrite.java;

import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.template.JavaTemplateJavaExtension;
import org.openrewrite.java.internal.template.JavaTemplateParser;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.template.SourceTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class JavaTemplate implements SourceTemplate<J, JavaCoordinates> {

    @Nullable
    private static Path TEMPLATE_CLASSPATH_DIR;

    protected static Path getTemplateClasspathDir() {
        if (TEMPLATE_CLASSPATH_DIR == null) {
            try {
                TEMPLATE_CLASSPATH_DIR = Files.createTempDirectory("java-template");
                Path templateDir = Files.createDirectories(TEMPLATE_CLASSPATH_DIR.resolve("org/openrewrite/java/internal/template"));
                try (InputStream in = JavaTemplateParser.class.getClassLoader().getResourceAsStream("org/openrewrite/java/internal/template/__M__.class")) {
                    assert in != null;
                    Files.copy(in, templateDir.resolve("__M__.class"));
                }
                try (InputStream in = JavaTemplateParser.class.getClassLoader().getResourceAsStream("org/openrewrite/java/internal/template/__P__.class")) {
                    assert in != null;
                    Files.copy(in, templateDir.resolve("__P__.class"));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return TEMPLATE_CLASSPATH_DIR;
    }

    @Getter
    private final String code;

    private final Consumer<String> onAfterVariableSubstitution;
    private final JavaTemplateParser templateParser;

    private JavaTemplate(boolean contextSensitive, JavaParser.Builder<?, ?> parser, String code, Set<String> imports,
                         Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate) {
        this(code, onAfterVariableSubstitution, new JavaTemplateParser(contextSensitive, augmentClasspath(parser), onAfterVariableSubstitution, onBeforeParseTemplate, imports));
    }

    private static JavaParser.Builder<?,?> augmentClasspath(JavaParser.Builder<?,?> parserBuilder) {
        return parserBuilder.addClasspathEntry(getTemplateClasspathDir());
    }

    protected JavaTemplate(String code, Consumer<String> onAfterVariableSubstitution, JavaTemplateParser templateParser) {
        this.code = code;
        this.onAfterVariableSubstitution = onAfterVariableSubstitution;
        this.templateParser = templateParser;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <J2 extends J> J2 apply(Cursor scope, JavaCoordinates coordinates, Object... parameters) {
        if (!(scope.getValue() instanceof J)) {
            throw new IllegalArgumentException("`scope` must point to a J instance.");
        }

        Substitutions substitutions = substitutions(parameters);
        String substitutedTemplate = substitutions.substitute();
        onAfterVariableSubstitution.accept(substitutedTemplate);

        //noinspection ConstantConditions
        return (J2) new JavaTemplateJavaExtension(templateParser, substitutions, substitutedTemplate, coordinates)
                .getMixin()
                .visit(scope.getValue(), 0, scope.getParentOrThrow());
    }

    protected Substitutions substitutions(Object[] parameters) {
        return new Substitutions(code, parameters);
    }

    @Incubating(since = "8.0.0")
    public static boolean matches(String template, Cursor cursor) {
        return JavaTemplate.builder(template).build().matches(cursor);
    }

    @Incubating(since = "7.38.0")
    public boolean matches(Cursor cursor) {
        return matcher(cursor).find();
    }

    @Incubating(since = "7.38.0")
    public Matcher matcher(Cursor cursor) {
        return new Matcher(cursor);
    }

    @Incubating(since = "7.38.0")
    @Value
    public class Matcher {
        Cursor cursor;

        @NonFinal
        JavaTemplateSemanticallyEqual.TemplateMatchResult matchResult;

        Matcher(Cursor cursor) {
            this.cursor = cursor;
        }

        public boolean find() {
            matchResult = JavaTemplateSemanticallyEqual.matchesTemplate(JavaTemplate.this, cursor);
            return matchResult.isMatch();
        }

        public J parameter(int i) {
            return matchResult.getMatchedParameters().get(i);
        }
    }

    public static <J2 extends J> J2 apply(String template, Cursor scope, JavaCoordinates coordinates, Object... parameters) {
        return builder(template).build().apply(scope, coordinates, parameters);
    }

    public static Builder builder(String code) {
        return new Builder(code);
    }

    @SuppressWarnings("unused")
    public static class Builder {

        private final String code;
        private final Set<String> imports = new HashSet<>();

        private boolean contextSensitive;

        private JavaParser.Builder<?, ?> parser = org.openrewrite.java.JavaParser.fromJavaVersion();

        private Consumer<String> onAfterVariableSubstitution = s -> {
        };
        private Consumer<String> onBeforeParseTemplate = s -> {
        };

        protected Builder(String code) {
            this.code = code.trim();
        }

        /**
         * A template snippet is context-sensitive when it refers to the class, variables, methods, or other symbols
         * visible from its insertion scope. When a template is completely self-contained, it is not context-sensitive.
         * Context-free template snippets can be cached, since it does not matter where the resulting LST elements will
         * be inserted. Since the LST elements in a context-sensitive snippet vary depending on where they are inserted
         * the resulting LST elements cannot be reused between different insertion points and are not cached.
         * <p>
         * An example of a context-free snippet might be something like this, to be used as a local variable declaration:
         * <code>int i = 1</code>;
         * <p>
         * An example of a context-sensitive snippet is:
         * <code>int i = a</code>;
         * This cannot be made sense of without the surrounding scope which includes the declaration of "a".
         */
        public Builder contextSensitive() {
            this.contextSensitive = true;
            return this;
        }

        public Builder imports(String... fullyQualifiedTypeNames) {
            for (String typeName : fullyQualifiedTypeNames) {
                validateImport(typeName);
                this.imports.add("import " + typeName + ";\n");
            }
            return this;
        }

        public Builder staticImports(String... fullyQualifiedMemberTypeNames) {
            for (String typeName : fullyQualifiedMemberTypeNames) {
                validateImport(typeName);
                this.imports.add("import static " + typeName + ";\n");
            }
            return this;
        }

        private void validateImport(String typeName) {
            if (StringUtils.isBlank(typeName)) {
                throw new IllegalArgumentException("Imports must not be blank");
            } else if (typeName.startsWith("import ") || typeName.startsWith("static ")) {
                throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" or \"static \" prefix");
            } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
            }
        }

        public Builder javaParser(JavaParser.Builder<?, ?> parser) {
            this.parser = parser;
            return this;
        }

        public Builder doAfterVariableSubstitution(Consumer<String> afterVariableSubstitution) {
            this.onAfterVariableSubstitution = afterVariableSubstitution;
            return this;
        }

        public Builder doBeforeParseTemplate(Consumer<String> beforeParseTemplate) {
            this.onBeforeParseTemplate = beforeParseTemplate;
            return this;
        }

        public JavaTemplate build() {
            return new JavaTemplate(contextSensitive, parser.clone(), code, imports,
                    onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }
}
