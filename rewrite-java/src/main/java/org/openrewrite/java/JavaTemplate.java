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

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.format.AutoFormatVisitor;
import org.openrewrite.java.internal.template.ExtractTrees;
import org.openrewrite.java.internal.template.InsertAtCoordinates;
import org.openrewrite.java.internal.template.JavaTemplatePrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaCoordinates;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Build ASTs from the text of Java source code without knowing how to build the AST
 * elements that make up that text.
 */
@Incubating(since = "7.0.0")
public class JavaTemplate {
    private final Supplier<Cursor> parentScopeGetter;
    private final JavaParser parser;
    private final String code;
    private final int parameterCount;
    private final Set<String> imports;
    private final String parameterMarker;
    private final Consumer<String> onAfterVariableSubstitution;
    private final Consumer<String> onBeforeParseTemplate;

    private JavaTemplate(Supplier<Cursor> parentScopeGetter, JavaParser parser, String code, Set<String> imports,
                         String parameterMarker, Consumer<String> onAfterVariableSubstitution,
                         Consumer<String> onBeforeParseTemplate) {
        this.parentScopeGetter = parentScopeGetter;
        this.parser = parser;
        this.code = code;
        this.imports = imports;
        this.parameterMarker = parameterMarker;
        this.onAfterVariableSubstitution = onAfterVariableSubstitution;
        this.onBeforeParseTemplate = onBeforeParseTemplate;
        this.parameterCount = StringUtils.countOccurrences(code, parameterMarker);
    }

    public static Builder builder(Supplier<Cursor> parentScope, String code) {
        return new Builder(parentScope, code);
    }

    /**
     * @param changing    The tree that will be returned modified where one of its subtrees will have
     *                    been added or replaced by an AST formed from the template.
     * @param coordinates The point where the template will either insert or replace code.
     * @param parameters  Parameters substituted into the template.
     * @param <J2>        The type of the changing tree.
     * @return A modified form of the changing tree.
     */
    public <J2 extends J> J2 withTemplate(Tree changing, JavaCoordinates coordinates, Object... parameters) {
        Cursor parentScope = parentScopeGetter.get();

        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        //Substitute parameter markers with the string representation of each parameter.
        String substitutedTemplate = substituteParameters(parameters);
        onAfterVariableSubstitution.accept(substitutedTemplate);

        J.CompilationUnit cu = parentScope.firstEnclosingOrThrow(J.CompilationUnit.class);
        //The tree printer uses the cursor path from the compilation unit down to the tree element within the coordinates
        //to generate the synthetic compilation unit. It is possible that the coordinates exist only in the "changed"
        //tree. To accommodate this, the cursor path is extended from the parent into the changed/mutated tree until the
        //coordinates are found.
        Cursor insertionScope = JavaTemplatePrinter.findCoordinateCursor(parentScope, changing, coordinates);

        String generatedSource = new JavaTemplatePrinter(substitutedTemplate, changing, coordinates, imports)
                .print(cu, insertionScope);
        onBeforeParseTemplate.accept(generatedSource);

        parser.reset();
        J.CompilationUnit synthetic = parser.parse(generatedSource).iterator().next();

        List<J> generatedElements = ExtractTrees.extract(synthetic);
        for (int i = 0; i < generatedElements.size(); i++) {
            J snippet = generatedElements.get(i);
            generatedElements.set(i, new AutoFormatVisitor<Integer>().visit(snippet, 0, parentScope));
        }

        //noinspection unchecked,ConstantConditions
        return (J2) new InsertAtCoordinates(coordinates).visit(changing, generatedElements, parentScope);
    }

    /**
     * Replace the parameter markers in the template with the parameters passed into the generate method.
     * Parameters that are Java Tree's will be correctly printed into the string. The parameters are not named and
     * rely purely on ordinal position.
     *
     * @param parameters A list of parameters
     * @return The final snippet to be generated.
     */
    private String substituteParameters(Object... parameters) {
        String codeInstance = code;
        for (Object parameter : parameters) {
            codeInstance = StringUtils.replaceFirst(codeInstance, parameterMarker,
                    substituteParameter(parameter));
        }
        return codeInstance;
    }

    private String substituteParameter(Object parameter) {
        if (parameter instanceof Tree) {
            return ((Tree) parameter).printTrimmed();
        } else if (parameter instanceof JRightPadded) {
            return substituteParameter(((JRightPadded<?>) parameter).getElement());
        } else if (parameter instanceof JLeftPadded) {
            return substituteParameter(((JLeftPadded<?>) parameter).getElement());
        }
        return parameter.toString();
    }

    public static class Builder {
        private final Supplier<Cursor> parentScope;
        private final String code;
        private final Set<String> imports = new HashSet<>();

        private JavaParser javaParser = JavaParser.fromJavaVersion().build();

        private String parameterMarker = "#{}";

        private Consumer<String> onAfterVariableSubstitution = s -> {
        };
        private Consumer<String> onBeforeParseTemplate = s -> {
        };

        Builder(Supplier<Cursor> parentScope, String code) {
            this.parentScope = parentScope;
            this.code = code.trim();
        }

        /**
         * A list of fully-qualified types that will be added when generating/compiling snippets
         *
         * <PRE>
         * Examples:
         * <p>
         * java.util.Collections
         * java.util.Date
         * </PRE>
         */
        public Builder imports(String... fullyQualifiedTypeNames) {
            for (String typeName : fullyQualifiedTypeNames) {
                if (typeName.startsWith("import ") || typeName.startsWith("static ")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" or \"static \" prefix");
                } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
                }
                this.imports.add("import " + typeName + ";\n");
            }
            return this;
        }

        /**
         * A list of fully-qualified member type names that will be statically imported when generating/compiling snippets.
         *
         * <PRE>
         * Examples:
         * <p>
         * java.util.Collections.emptyList
         * java.util.Collections.*
         * </PRE>
         */
        public Builder staticImports(String... fullyQualifiedMemberTypeNames) {
            for (String typeName : fullyQualifiedMemberTypeNames) {
                if (typeName.startsWith("import ") || typeName.startsWith("static ")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" or \"static \" prefix");
                } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
                }
                this.imports.add("import static " + typeName + ";\n");
            }
            return this;
        }

        public Builder javaParser(JavaParser javaParser) {
            this.javaParser = javaParser;
            return this;
        }

        /**
         * Define an alternate marker to denote where a parameter should be inserted into the template. If not specified, the
         * default format for parameter marker is "#{}"
         */
        public Builder parameterMarker(String parameterMarker) {
            this.parameterMarker = parameterMarker;
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
            return new JavaTemplate(parentScope, javaParser, code, imports, parameterMarker,
                    onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }
}
