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
package org.openrewrite.golang;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.golang.marker.GoProject;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.text.PlainText;

import java.util.function.Consumer;

public final class Assertions {
    private Assertions() {
    }

    public static SourceSpecs go(@Nullable String before) {
        return go(before, s -> {
        });
    }

    public static SourceSpecs go(@Nullable String before, Consumer<SourceSpec<Go.CompilationUnit>> spec) {
        SourceSpec<Go.CompilationUnit> go = new SourceSpec<>(Go.CompilationUnit.class, null, GolangParser.builder(), before, null);
        spec.accept(go);
        return go;
    }

    public static SourceSpecs go(@Nullable String before, String after) {
        return go(before, after, s -> {
        });
    }

    public static SourceSpecs go(@Nullable String before, String after,
                                       Consumer<SourceSpec<Go.CompilationUnit>> spec) {
        SourceSpec<Go.CompilationUnit> go = new SourceSpec<>(Go.CompilationUnit.class, null, GolangParser.builder(), before, s -> after);
        spec.accept(go);
        return go;
    }

    public static SourceSpecs goMod(@Nullable String before) {
        return goMod(before, s -> {
        });
    }

    public static SourceSpecs goMod(@Nullable String before, Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> goMod = new SourceSpec<>(PlainText.class, null, GoModParser.builder(), before, null);
        goMod.path("go.mod");
        spec.accept(goMod);
        return goMod;
    }

    public static SourceSpecs goMod(@Nullable String before, String after) {
        return goMod(before, after, s -> {
        });
    }

    public static SourceSpecs goMod(@Nullable String before, String after,
                                    Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> goMod = new SourceSpec<>(PlainText.class, null, GoModParser.builder(), before, s -> after);
        goMod.path("go.mod");
        spec.accept(goMod);
        return goMod;
    }

    /**
     * Wrap go.sum content as a sibling SourceSpec. When placed inside a
     * {@link #goProject(String, SourceSpecs...)} alongside a
     * {@link #goMod(String)}, the parser reads the sibling go.sum off disk
     * during parse (via {@link GoModParser#parseSumContent}) and populates
     * {@code GoResolutionResult.resolvedDependencies}.
     * <p>
     * Today there is no dedicated parser for go.sum — content round-trips as
     * a {@link PlainText}. The marker side-effect happens during go.mod
     * parsing.
     */
    public static SourceSpecs goSum(@Nullable String before) {
        return goSum(before, s -> {
        });
    }

    public static SourceSpecs goSum(@Nullable String before, Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> goSum = new SourceSpec<>(PlainText.class, null,
                org.openrewrite.text.PlainTextParser.builder(), before, null);
        goSum.path("go.sum");
        spec.accept(goSum);
        return goSum;
    }

    public static SourceSpecs goSum(@Nullable String before, String after) {
        return goSum(before, after, s -> {
        });
    }

    public static SourceSpecs goSum(@Nullable String before, String after,
                                    Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> goSum = new SourceSpec<>(PlainText.class, null,
                org.openrewrite.text.PlainTextParser.builder(), before, s -> after);
        goSum.path("go.sum");
        spec.accept(goSum);
        return goSum;
    }

    /**
     * Wrap sibling sources in a Go project directory and tag each with a
     * {@link GoProject} marker. Mirrors {@code mavenProject(name, sources...)}:
     * the {@code go.mod} (via {@link #goMod(String)}) and {@code .go} files
     * (via {@link #go(String)}) are SIBLINGS inside the project directory,
     * not nested inside one another. Recipes that need module-level
     * dependency information look up the sibling go.mod's
     * {@link org.openrewrite.golang.marker.GoResolutionResult}.
     */
    public static SourceSpecs goProject(String project, SourceSpecs... sources) {
        return goProject(project, spec -> project(spec, project), sources);
    }

    public static SourceSpecs goProject(String project, Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... sources) {
        return SourceSpecs.dir(project, spec, sources);
    }

    /**
     * Tag a single SourceSpec with a {@link GoProject} marker. Used by the
     * {@code goProject(name, ...)} consumer to apply the marker to every
     * child source.
     */
    public static SourceSpec<?> project(SourceSpec<?> sourceSpec, String projectName) {
        return sourceSpec.markers(new GoProject(Tree.randomId(), projectName));
    }

    /**
     * Walk {@code root} and assert that the first {@link J.Identifier} whose
     * {@code simpleName} equals {@code name} carries a non-null
     * {@link JavaType.FullyQualified} type whose fully-qualified name equals
     * {@code expectedFqn}. Use this for class/struct/parameterized types; for
     * primitives use {@link #expectPrimitiveType(Tree, String, String)}.
     *
     * @throws AssertionError when no such identifier exists, its type is
     *                       null, or the type is not fully qualified.
     */
    public static void expectType(Tree root, String name, String expectedFqn) {
        IdentifierTypeFinder finder = new IdentifierTypeFinder(name);
        finder.visit(root, 0);
        if (!finder.found) {
            throw new AssertionError("expectType(\"" + name + "\"): no identifier with that name in tree");
        }
        if (finder.type == null) {
            throw new AssertionError("expectType(\"" + name + "\"): identifier has null type");
        }
        if (!(finder.type instanceof JavaType.FullyQualified)) {
            throw new AssertionError("expectType(\"" + name + "\"): identifier type is " +
                    finder.type.getClass().getSimpleName() + ", want FullyQualified");
        }
        String got = ((JavaType.FullyQualified) finder.type).getFullyQualifiedName();
        if (!got.equals(expectedFqn)) {
            throw new AssertionError("expectType(\"" + name + "\"): FQN = \"" + got + "\", want \"" + expectedFqn + "\"");
        }
    }

    /**
     * Walk {@code root} and assert that the first {@link J.Identifier} whose
     * {@code simpleName} equals {@code name} carries a {@link JavaType.Primitive}
     * whose keyword equals {@code expectedKeyword} (e.g. {@code "int"},
     * {@code "String"}, {@code "boolean"}).
     */
    public static void expectPrimitiveType(Tree root, String name, String expectedKeyword) {
        IdentifierTypeFinder finder = new IdentifierTypeFinder(name);
        finder.visit(root, 0);
        if (!finder.found) {
            throw new AssertionError("expectPrimitiveType(\"" + name + "\"): no identifier with that name in tree");
        }
        if (finder.type == null) {
            throw new AssertionError("expectPrimitiveType(\"" + name + "\"): identifier has null type");
        }
        if (!(finder.type instanceof JavaType.Primitive)) {
            throw new AssertionError("expectPrimitiveType(\"" + name + "\"): identifier type is " +
                    finder.type.getClass().getSimpleName() + ", want Primitive");
        }
        String got = ((JavaType.Primitive) finder.type).getKeyword();
        if (!got.equals(expectedKeyword)) {
            throw new AssertionError("expectPrimitiveType(\"" + name + "\"): keyword = \"" + got + "\", want \"" + expectedKeyword + "\"");
        }
    }

    /**
     * Walk {@code root} and assert that the first
     * {@link J.MethodInvocation} or {@link J.MethodDeclaration} whose name
     * equals {@code name} carries a non-null {@link JavaType.Method} whose
     * {@code declaringType} fully-qualified name equals
     * {@code expectedDeclaringFqn}.
     *
     * <p>For invocations across packages, {@code expectedDeclaringFqn} is the
     * import path of the owning package (e.g. {@code "fmt"} for
     * {@code fmt.Println}). For methods declared in the file under test it is
     * the package's full path (e.g. {@code "main.Point"}).
     */
    public static void expectMethodType(Tree root, String name, String expectedDeclaringFqn) {
        MethodTypeFinder finder = new MethodTypeFinder(name);
        finder.visit(root, 0);
        if (!finder.found) {
            throw new AssertionError("expectMethodType(\"" + name + "\"): no method with that name in tree");
        }
        if (finder.methodType == null) {
            throw new AssertionError("expectMethodType(\"" + name + "\"): method has null methodType");
        }
        if (finder.methodType.getDeclaringType() == null) {
            throw new AssertionError("expectMethodType(\"" + name + "\"): method has null declaringType");
        }
        String got = finder.methodType.getDeclaringType().getFullyQualifiedName();
        if (!got.equals(expectedDeclaringFqn)) {
            throw new AssertionError("expectMethodType(\"" + name + "\"): declaring FQN = \"" + got +
                    "\", want \"" + expectedDeclaringFqn + "\"");
        }
    }

    private static final class IdentifierTypeFinder extends JavaIsoVisitor<Integer> {
        private final String name;
        boolean found;
        @Nullable JavaType type;

        IdentifierTypeFinder(String name) {
            this.name = name;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
            if (!found && name.equals(identifier.getSimpleName())) {
                found = true;
                type = identifier.getType();
            }
            return identifier;
        }
    }

    private static final class MethodTypeFinder extends JavaIsoVisitor<Integer> {
        private final String name;
        boolean found;
        JavaType.@Nullable Method methodType;

        MethodTypeFinder(String name) {
            this.name = name;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
            if (!found && name.equals(method.getSimpleName())) {
                found = true;
                methodType = method.getMethodType();
            }
            return super.visitMethodInvocation(method, p);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
            if (!found && name.equals(method.getSimpleName())) {
                found = true;
                methodType = method.getMethodType();
            }
            return super.visitMethodDeclaration(method, p);
        }
    }
}
