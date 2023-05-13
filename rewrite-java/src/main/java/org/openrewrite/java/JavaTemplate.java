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

import lombok.Value;
import lombok.experimental.NonFinal;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.template.JavaTemplateJavaExtension;
import org.openrewrite.java.internal.template.JavaTemplateParser;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.template.SourceTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JavaTemplate implements SourceTemplate<J, JavaCoordinates> {
    @Nullable
    private final Supplier<Cursor> parentScopeGetter;
    private final String code;
    private final int parameterCount;
    private final Consumer<String> onAfterVariableSubstitution;
    private final JavaTemplateParser templateParser;

    private JavaTemplate(@Nullable Supplier<Cursor> parentScopeGetter, JavaParser.Builder<?, ?> javaParser, String code, Set<String> imports,
                         Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate) {
        this.parentScopeGetter = parentScopeGetter;
        this.code = code;
        this.onAfterVariableSubstitution = onAfterVariableSubstitution;
        this.parameterCount = StringUtils.countOccurrences(code, "#{");
        this.templateParser = new JavaTemplateParser(javaParser, onAfterVariableSubstitution, onBeforeParseTemplate, imports, parentScopeGetter != null);
    }

    public String getCode() {
        return code;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <J2 extends J> J2 withTemplate(Tree changing, @Nullable Cursor parentScope, JavaCoordinates coordinates, Object[] parameters) {
        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        Substitutions substitutions = new Substitutions(code, parameters);
        String substitutedTemplate = substitutions.substitute();
        onAfterVariableSubstitution.accept(substitutedTemplate);

        if (parentScopeGetter == null && parentScope == null) {
            //noinspection ConstantConditions
            return (J2) new JavaTemplateJavaExtension(templateParser, substitutions, substitutedTemplate, coordinates)
                    .getMixin()
                    .visit(changing, 0);
        }

        AtomicReference<Cursor> parentCursorRef = new AtomicReference<>();

        // Find the parent cursor of the CHANGING element, which may not be the same as the cursor of
        // the method using the template. For example, using a template on a class declaration body
        // inside visitClassDeclaration. The body is the changing element, but the current cursor
        // is at the class declaration.
        //
        //      J visitClassDeclaration(J.ClassDeclaration c, Integer p) {
        //            c.getBody().withTemplate(template, c.getBody().coordinates.lastStatement());
        //      }
        if (parentScope == null) {
            parentScope = parentScopeGetter.get();
        }
        if (!(parentScope.getValue() instanceof J)) {
            // Handle the provided parent cursor pointing to a JRightPadded or similar
            parentScope = parentScope.getParentTreeCursor();
        }
        new JavaIsoVisitor<Integer>() {
            @Nullable
            @Override
            public J visit(@Nullable Tree tree, Integer integer) {
                if (tree != null && tree.isScope(changing)) {
                    // Currently getCursor still points to the parent, because super.visit() has the logic to update it
                    Cursor cursor = getCursor();
                    if (!(cursor.getValue() instanceof J)) {
                        cursor = cursor.getParentTreeCursor();
                    }
                    parentCursorRef.set(cursor);
                    return (J) tree;
                }
                return super.visit(tree, integer);
            }
        }.visit(parentScope.getValue(), 0, parentScope.getParentOrThrow());

        Cursor parentCursor = parentCursorRef.get();

        //noinspection ConstantConditions
        return (J2) new JavaTemplateJavaExtension(templateParser, substitutions, substitutedTemplate, coordinates)
                .getMixin()
                .visit(changing, 0, parentCursor);
    }

    @Incubating(since = "7.38.0")
    public boolean matches(J tree) {
        return matcher(tree).find();
    }

    @Incubating(since = "7.38.0")
    public Matcher matcher(J tree) {
        return new Matcher(tree);
    }

    @Incubating(since = "7.38.0")
    @Value
    public class Matcher {
        J tree;
        @NonFinal
        JavaTemplateSemanticallyEqual.TemplateMatchResult matchResult;

        Matcher(J tree) {
            this.tree = tree;
        }

        public boolean find() {
            matchResult = JavaTemplateSemanticallyEqual.matchesTemplate(JavaTemplate.this, tree);
            return matchResult.isMatch();
        }

        public J parameter(int i) {
            return matchResult.getMatchedParameters().get(i);
        }
    }

    public static Builder builder(String code) {
        return new Builder(code);
    }

    @Deprecated
    public static Builder builder(Supplier<Cursor> parentScope, String code) {
        return new Builder(code).context(parentScope);
    }

    @SuppressWarnings("unused")
    public static class Builder {

        @Nullable
        private Supplier<Cursor> context;
        private final String code;
        private final Set<String> imports = new HashSet<>();

        private JavaParser.Builder<?, ?> javaParser = JavaParser.fromJavaVersion();

        private Consumer<String> onAfterVariableSubstitution = s -> {
        };
        private Consumer<String> onBeforeParseTemplate = s -> {
        };

        Builder(String code) {
            this.code = code.trim();
        }

        public Builder context(@Nullable Supplier<Cursor> context) {
            this.context = context;
            return this;
        }

        public Builder imports(String... fullyQualifiedTypeNames) {
            for (String typeName : fullyQualifiedTypeNames) {
                if (shouldAddImport(typeName)) {
                    this.imports.add("import " + typeName + ";\n");
                }
            }
            return this;
        }

        public Builder staticImports(String... fullyQualifiedMemberTypeNames) {
            for (String typeName : fullyQualifiedMemberTypeNames) {
                if (shouldAddImport(typeName)) {
                    this.imports.add("import static " + typeName + ";\n");
                }
            }
            return this;
        }

        private boolean shouldAddImport(String typeName) {
            if (StringUtils.isBlank(typeName)) {
                return false;
            } else if (typeName.startsWith("import ") || typeName.startsWith("static ")) {
                throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" or \"static \" prefix");
            } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
            }
            return true;
        }

        public Builder javaParser(JavaParser.Builder<?, ?> javaParser) {
            this.javaParser = javaParser;
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
            return new JavaTemplate(context, javaParser, code, imports,
                    onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P0 p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P1<?> p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P2<?, ?> p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P3<?, ?, ?> p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P4<?, ?, ?, ?> p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P5<?, ?, ?, ?, ?> p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P6<?, ?, ?, ?, ?, ?> p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P7<?, ?, ?, ?, ?, ?, ?> p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P8<?, ?, ?, ?, ?, ?, ?, ?> p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P9<?, ?, ?, ?, ?, ?, ?, ?, ?> p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, P10<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> p) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F0<?> f) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F1<?, ?> f) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F2<?, ?, ?> f) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F3<?, ?, ?, ?> f) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F4<?, ?, ?, ?, ?> f) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F5<?, ?, ?, ?, ?, ?> f) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F6<?, ?, ?, ?, ?, ?, ?> f) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F7<?, ?, ?, ?, ?, ?, ?, ?> f) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F8<?, ?, ?, ?, ?, ?, ?, ?, ?> f) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F9<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> f) {
        return new PatternBuilder(name).build(owner);
    }

    public static JavaTemplate.Builder compile(JavaVisitor<?> owner, String name, F10<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> f) {
        return new PatternBuilder(name).build(owner);
    }

    @Value
    @SuppressWarnings("unused")
    static class PatternBuilder {
        String name;

        public JavaTemplate.Builder build(JavaVisitor<?> owner) {
            try {
                Class<?> templateClass = Class.forName(owner.getClass().getName() + "_" + name, true,
                        owner.getClass().getClassLoader());
                Method getTemplate = templateClass.getDeclaredMethod("getTemplate", JavaVisitor.class);
                return (JavaTemplate.Builder) getTemplate.invoke(null, owner);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public interface P0 {
        void accept() throws Exception;
    }

    public interface P1<P1> {
        void accept(P1 p1) throws Exception;
    }

    public interface P2<P1, P2> {
        void accept(P1 p1, P2 p2) throws Exception;
    }

    public interface P3<P1, P2, P3> {
        void accept(P1 p1, P2 p2, P3 p3) throws Exception;
    }

    public interface P4<P1, P2, P3, P4> {
        void accept(P1 p1, P2 p2, P3 p3, P4 p4) throws Exception;
    }

    public interface P5<P1, P2, P3, P4, P5> {
        void accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) throws Exception;
    }

    public interface P6<P1, P2, P3, P4, P5, P6> {
        void accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6) throws Exception;
    }

    public interface P7<P1, P2, P3, P4, P5, P6, P7> {
        void accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7) throws Exception;
    }

    public interface P8<P1, P2, P3, P4, P5, P6, P7, P8> {
        void accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8) throws Exception;
    }

    public interface P9<P1, P2, P3, P4, P5, P6, P7, P8, P9> {
        void accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9) throws Exception;
    }

    public interface P10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> {
        void accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10) throws Exception;
    }

    public interface F0<R> {
        R accept() throws Exception;
    }

    public interface F1<R, P1> {
        R accept(P1 p1) throws Exception;
    }

    public interface F2<R, P1, P2> {
        R accept(P1 p1, P2 p2) throws Exception;
    }

    public interface F3<R, P1, P2, P3> {
        R accept(P1 p1, P2 p2, P3 p3) throws Exception;
    }

    public interface F4<R, P1, P2, P3, P4> {
        R accept(P1 p1, P2 p2, P3 p3, P4 p4) throws Exception;
    }

    public interface F5<R, P1, P2, P3, P4, P5> {
        R accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) throws Exception;
    }

    public interface F6<R, P1, P2, P3, P4, P5, P6> {
        R accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6) throws Exception;
    }

    public interface F7<R, P1, P2, P3, P4, P5, P6, P7> {
        R accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7) throws Exception;
    }

    public interface F8<R, P1, P2, P3, P4, P5, P6, P7, P8> {
        R accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8) throws Exception;
    }

    public interface F9<R, P1, P2, P3, P4, P5, P6, P7, P8, P9> {
        R accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9) throws Exception;
    }

    public interface F10<R, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> {
        R accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10) throws Exception;
    }
}
