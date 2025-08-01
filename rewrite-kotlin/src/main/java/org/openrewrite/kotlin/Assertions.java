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
package org.openrewrite.kotlin;


import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ThrowingConsumer;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.marker.Extension;
import org.openrewrite.kotlin.marker.IndexedAccess;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.kotlin.tree.KSpace;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.test.UncheckedConsumer;
import org.opentest4j.AssertionFailedError;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openrewrite.java.Assertions.sourceSet;
import static org.openrewrite.java.tree.TypeUtils.isWellFormedType;
import static org.openrewrite.test.SourceSpecs.dir;

@SuppressWarnings({"unused", "unchecked", "OptionalGetWithoutIsPresent", "DataFlowIssue"})
public final class Assertions {

    private Assertions() {
    }

    public static SourceFile validateTypes(SourceFile source, TypeValidation typeValidation) {
        if (source instanceof JavaSourceFile) {
            assertValidTypes(typeValidation, (JavaSourceFile) source);
        }
        return source;
    }

    static void customizeExecutionContext(ExecutionContext ctx) {
        if (ctx.getMessage(KotlinParser.SKIP_SOURCE_SET_TYPE_GENERATION) == null) {
            ctx.putMessage(KotlinParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);
        }
    }

    // A helper method to adjust white spaces in the input kotlin source to help us detect parse-to-print idempotent issues
    // The idea is to differentiate adjacent spaces by whitespace count so that if any adjacent spaces are swapped or unhandled well by the parser, it can be detected.
    // Just change from `before` to `adjustSpaces(before)` below in the `kotlin()` method to test locally
    @SuppressWarnings("IfStatementWithIdenticalBranches")
    private static @Nullable String adjustSpaces(@Nullable String input) {
        if (input == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int count = 0;
        int limit = 1;
        char pre = 0;
        for (char c : input.toCharArray()) {
            if (c == ' ') {
                if (pre == ' ') {
                    count++;
                    if (count <= limit) {
                        out.append(c);
                    }
                } else {
                    count++;
                    out.append(c);
                }
            } else {
                if (pre == ' ') {
                    for (int i = count; i < limit; i++) {
                        out.append(' ');
                    }
                    count = 0;
                    limit++;
                    if (limit > 5) {
                        limit = 1;
                    }
                }
                out.append(c);
            }
            pre = c;
        }

        return out.toString();
    }

    private static KotlinParser.Builder kotlinParser = KotlinParser.builder()
            .classpath(JavaParser.runtimeClasspath())
            .logCompilationWarningsAndErrors(true);

    private static KotlinParser.Builder ktsParser = KotlinParser.builder(kotlinParser)
            .isKotlinScript(true);

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before) {
        // Change `before` to `adjustSpaces(before)` to test spaces locally here
        return kotlin(before, s -> {
        });
    }

    public static SourceSpecs kotlinScript(@Language("kts") @Nullable String before) {
        return kotlinScript(before, s -> {
        });
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before, Consumer<SourceSpec<K.CompilationUnit>> spec) {
        SourceSpec<K.CompilationUnit> kotlin = new SourceSpec<>(
                K.CompilationUnit.class, null, kotlinParser, before,
                Assertions::validateTypes,
                Assertions::customizeExecutionContext
        );
        acceptSpec(spec, kotlin);
        return kotlin;
    }

    public static SourceSpecs kotlinScript(@Language("kts") @Nullable String before, Consumer<SourceSpec<K.CompilationUnit>> spec) {
        SourceSpec<K.CompilationUnit> kotlinScript = new SourceSpec<>(
                K.CompilationUnit.class, null, ktsParser, before,
                Assertions::validateTypes,
                Assertions::customizeExecutionContext
        );
        acceptSpec(spec, kotlinScript);
        return kotlinScript;
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before, @Language("kotlin") String after) {
        return kotlin(before, after, s -> {
        });
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before, @Language("kotlin") String after,
                                     Consumer<SourceSpec<K.CompilationUnit>> spec) {
        SourceSpec<K.CompilationUnit> kotlin = new SourceSpec<>(K.CompilationUnit.class, null, kotlinParser, before,
                Assertions::validateTypes,
                Assertions::customizeExecutionContext).after(s -> after);
        acceptSpec(spec, kotlin);
        return kotlin;
    }

    public static SourceSpecs srcMainKotlin(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... kotlinSources) {
        return dir("src/main/kotlin", spec, kotlinSources);
    }

    public static SourceSpecs srcMainKotlin(SourceSpecs... kotlinSources) {
        return srcMainKotlin(spec -> sourceSet(spec, "main"), kotlinSources);
    }

    public static SourceSpecs srcTestKotlin(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... kotlinSources) {
        return dir("src/test/kotlin", spec, kotlinSources);
    }

    public static SourceSpecs srcTestKotlin(SourceSpecs... kotlinSources) {
        return srcTestKotlin(spec -> sourceSet(spec, "test"), kotlinSources);
    }

    private static void acceptSpec(Consumer<SourceSpec<K.CompilationUnit>> spec, SourceSpec<K.CompilationUnit> kotlin) {
//        Consumer<K.CompilationUnit> consumer = kotlin.getAfterRecipe().andThen(isFullyParsed()).andThen(spaceConscious(kotlin));
        Consumer<K.CompilationUnit> consumer = kotlin.getAfterRecipe().andThen(isFullyParsed());
        kotlin.afterRecipe(consumer::accept);
        spec.accept(kotlin);
    }

    public static ThrowingConsumer<K.CompilationUnit> isFullyParsed() {
        return cu -> new KotlinIsoVisitor<Integer>() {
            @Override
            public J visitUnknownSource(J.Unknown.Source source, Integer integer) {
                Optional<ParseExceptionResult> result = source.getMarkers().findFirst(ParseExceptionResult.class);
                if (result.isPresent()) {
                    System.out.println(result.get().getMessage());
                    throw new AssertionFailedError("Parsing error, J.Unknown detected");
                } else {
                    throw new UnsupportedOperationException("A J.Unknown should always have a parse exception result.");
                }
            }

            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer integer) {
                if (!space.getWhitespace().trim().isEmpty()) {
                    throw new AssertionFailedError("Parsing error detected, whitespace contains non-whitespace characters: " + space.getWhitespace());
                }
                return super.visitSpace(space, loc, integer);
            }
        }.visit(cu, 0);
    }

    public static UncheckedConsumer<SourceSpec<?>> spaceConscious() {
        return source -> {
            if (source.getSourceFileType() == K.CompilationUnit.class) {
                SourceSpec<K.CompilationUnit> kotlinSourceSpec = (SourceSpec<K.CompilationUnit>) source;
                kotlinSourceSpec.afterRecipe(spaceConscious(kotlinSourceSpec));
            }
        };
    }

    private static void assertValidTypes(TypeValidation typeValidation, J sf) {
        if (typeValidation.identifiers() || typeValidation.methodInvocations() || typeValidation.methodDeclarations() || typeValidation.classDeclarations() ||
            typeValidation.constructorInvocations()) {
            List<FindMissingTypes.MissingTypeResult> missingTypeResults = findMissingTypes(sf);
            missingTypeResults = missingTypeResults.stream()
                    .filter(missingType -> {
                        if (missingType.getJ() instanceof J.Identifier) {
                            return typeValidation.identifiers();
                        } else if (missingType.getJ() instanceof J.ClassDeclaration) {
                            return typeValidation.classDeclarations();
                        } else if (missingType.getJ() instanceof J.MethodInvocation || missingType.getJ() instanceof J.MemberReference) {
                            return typeValidation.methodInvocations();
                        } else if (missingType.getJ() instanceof J.NewClass) {
                            return typeValidation.constructorInvocations();
                        } else if (missingType.getJ() instanceof J.MethodDeclaration) {
                            return typeValidation.methodDeclarations();
                        } else if (missingType.getJ() instanceof J.VariableDeclarations.NamedVariable) {
                            return typeValidation.variableDeclarations();
                        } else {
                            return true;
                        }
                    })
                    .collect(toList());
            if (!missingTypeResults.isEmpty()) {
                throw new IllegalStateException("LST contains missing or invalid type information\n" + missingTypeResults.stream().map(v -> v.getPath() + "\n" + v.getPrintedTree())
                        .collect(joining("\n\n")));
            }
        }
    }

    public static ThrowingConsumer<K.CompilationUnit> spaceConscious(SourceSpec<K.CompilationUnit> spec) {
        return cu -> {
            K.CompilationUnit visited = (K.CompilationUnit) new KotlinIsoVisitor<Integer>() {
                int id = 0;

                @Override
                public Space visitSpace(Space space, KSpace.Location loc, Integer integer) {
                    return next(space);
                }

                private Space next(Space space) {
                    if (!space.getComments().isEmpty()) {
                        return space;
                    }
                    return space.withComments(singletonList(new TextComment(true, Integer.toString(id++), "", Markers.EMPTY)));
                }

                @Override
                public Space visitSpace(Space space, Space.Location loc, Integer integer) {
                    Cursor parentCursor = getCursor().getParentOrThrow();
                    if (loc == Space.Location.IDENTIFIER_PREFIX && parentCursor.getValue() instanceof J.Annotation) {
                        return space;
                    } else if (loc == Space.Location.IDENTIFIER_PREFIX && parentCursor.getValue() instanceof J.Break &&
                            ((J.Break) parentCursor.getValue()).getLabel() == getCursor().getValue()) {
                        return space;
                    } else if (loc == Space.Location.IDENTIFIER_PREFIX && parentCursor.getValue() instanceof K.Return &&
                               ((K.Return) parentCursor.getValue()).getLabel() == getCursor().getValue()) {
                        return space;
                    } else if (loc == Space.Location.LABEL_SUFFIX) {
                        return space;
                    } else if (getCursor().firstEnclosing(J.Import.class) != null) {
                        return space;
                    } else if (getCursor().firstEnclosing(J.Package.class) != null) {
                        return space;
                    }
                    return next(space);
                }
            }.visit(cu, 0);
            try {
                String s = visited.printAll();
                InMemoryExecutionContext ctx = new InMemoryExecutionContext();
                ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
                SourceFile cu2 = spec.getParser().build().parse(ctx, s).findFirst().get();
                String s1 = cu2.printAll();
                assertEquals(s, s1, "Parser is not whitespace print idempotent");
            } catch (Exception e) {
                fail(e);
            }
        };
    }

    public static List<FindMissingTypes.MissingTypeResult> findMissingTypes(J j) {
        J j1 = new FindMissingTypesVisitor().visit(j, new InMemoryExecutionContext());
        List<FindMissingTypes.MissingTypeResult> results = new ArrayList<>();
        if (j1 != j) {
            new KotlinIsoVisitor<List<FindMissingTypes.MissingTypeResult>>() {
                @Override
                public <M extends Marker> M visitMarker(Marker marker, List<FindMissingTypes.MissingTypeResult> missingTypeResults) {
                    if (marker instanceof SearchResult) {
                        String message = ((SearchResult) marker).getDescription();
                        String path = getCursor()
                                .getPathAsStream(j -> j instanceof J || j instanceof Javadoc)
                                .map(t -> t.getClass().getSimpleName())
                                .collect(joining("->"));
                        J j = getCursor().firstEnclosing(J.class);
                        String printedTree;
                        if (getCursor().firstEnclosing(JavaSourceFile.class) != null) {
                            printedTree = j != null ? j.printTrimmed(new InMemoryExecutionContext(), getCursor().getParentOrThrow()) : "";
                        } else {
                            printedTree = String.valueOf(j);
                        }
                        missingTypeResults.add(new FindMissingTypes.MissingTypeResult(message, path, printedTree, j));
                    }
                    return super.visitMarker(marker, missingTypeResults);
                }
            }.visit(j1, results);
        }
        return results;
    }

    static class FindMissingTypesVisitor extends KotlinIsoVisitor<ExecutionContext> {

        private final Set<JavaType> seenTypes = new HashSet<>();

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            // The non-nullability of J.Identifier.getType() in our AST is a white lie
            // J.Identifier.getType() is allowed to be null in places where the containing AST element fully specifies the type
            if (!isWellFormedType(identifier.getType(), seenTypes) && !isAllowedToHaveNullType(identifier)) {
                if (isValidated(identifier)) {
                    identifier = SearchResult.found(identifier, "Identifier type is missing or malformed");
                }
            }
            if (identifier.getFieldType() != null && !identifier.getSimpleName().equals(identifier.getFieldType().getName()) && isNotDestructType(identifier.getFieldType())) {
                identifier = SearchResult.found(identifier, "type information has a different variable name '" + identifier.getFieldType().getName() + "'");
            }
            return identifier;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
            if (v == variable) {
                JavaType.Variable variableType = v.getVariableType();
                if (!isWellFormedType(variableType, seenTypes) && !isAllowedToHaveUnknownType()) {
                    if (isValidated(variable)) {
                        v = SearchResult.found(v, "Variable type is missing or malformed");
                    }
                } else if (variableType != null && !variableType.getName().equals(v.getSimpleName()) && isNotDestructType(variableType)) {
                    v = SearchResult.found(v, "type information has a different variable name '" + variableType.getName() + "'");
                }
            }
            return v;
        }

        private boolean isAllowedToHaveUnknownType() {
            Cursor parent = getCursor().getParent();
            while (parent != null && parent.getParent() != null && !(parent.getParentTreeCursor().getValue() instanceof J.ClassDeclaration)) {
                parent = parent.getParentTreeCursor();
            }
            // If the variable is declared in a class initializer, then it's allowed to have unknown type
            return parent != null && parent.getValue() instanceof J.Block;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            // If one of the method's arguments or type parameters is missing type, then the invocation very likely will too
            // Avoid over-reporting the same problem by checking the invocation only when its elements are well-formed
            if (mi == method) {
                JavaType.Method type = mi.getMethodType();
                if (!isWellFormedType(type, seenTypes)) {
                    mi = SearchResult.found(mi, "MethodInvocation type is missing or malformed");
                } else if (!type.getName().equals(mi.getSimpleName()) && !type.isConstructor() && isValidated(mi)) {
                    mi = SearchResult.found(mi, "type information has a different method name '" + type.getName() + "'");
                }
                if (mi.getName().getType() != null && type != mi.getName().getType()) {
                    mi = SearchResult.found(mi, "MethodInvocation#name type is not the MethodType of MethodInvocation.");
                }
            }
            return mi;
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            J.MemberReference mr = super.visitMemberReference(memberRef, ctx);
            JavaType.Method type = mr.getMethodType();
            if (type != null) {
                if (!isWellFormedType(type, seenTypes)) {
                    mr = SearchResult.found(mr, "MemberReference type is missing or malformed");
                } else if (!type.getName().equals(mr.getReference().getSimpleName()) && !type.isConstructor()) {
                    mr = SearchResult.found(mr, "type information has a different method name '" + type.getName() + "'");
                }
            } else {
                JavaType.Variable variableType = mr.getVariableType();
                if (!isWellFormedType(variableType, seenTypes)) {
                    if (!"class".equals(mr.getReference().getSimpleName())) {
                        mr = SearchResult.found(mr, "MemberReference type is missing or malformed");
                    }
                } else if (!variableType.getName().equals(mr.getReference().getSimpleName())) {
                    mr = SearchResult.found(mr, "type information has a different variable name '" + variableType.getName() + "'");
                }
            }
            return mr;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
            JavaType.Method type = md.getMethodType();
            if (!isWellFormedType(type, seenTypes)) {
                md = SearchResult.found(md, "MethodDeclaration type is missing or malformed");
            } else if (!md.getSimpleName().equals(type.getName()) && !type.isConstructor() && !"anonymous".equals(type.getName())) {
                md = SearchResult.found(md, "type information has a different method name '" + type.getName() + "'");
            }
            if (md.getName().getType() != null && type != md.getName().getType()) {
                md = SearchResult.found(md, "MethodDeclaration#name type is not the MethodType of MethodDeclaration.");
            }
            return md;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
            JavaType.FullyQualified t = cd.getType();
            if (!isWellFormedType(t, seenTypes)) {
                return SearchResult.found(cd, "ClassDeclaration type is missing or malformed");
            }
            if (!cd.getKind().name().equals(t.getKind().name())) {
                cd = SearchResult.found(cd,
                        " J.ClassDeclaration kind " + cd.getKind() + " does not match the kind in its type information " + t.getKind());
            }
            J.CompilationUnit jc = getCursor().firstEnclosing(J.CompilationUnit.class);
            if (jc != null) {
                J.Package pkg = jc.getPackageDeclaration();
                if (pkg != null && t.getPackageName().equals(pkg.printTrimmed(getCursor()))) {
                    cd = SearchResult.found(cd,
                            " J.ClassDeclaration package " + pkg + " does not match the package in its type information " + pkg.printTrimmed(getCursor()));
                }
            }
            return cd;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = super.visitNewClass(newClass, ctx);
            if (n == newClass && !isWellFormedType(n.getType(), seenTypes)) {
                n = SearchResult.found(n, "NewClass type is missing or malformed");
            }
            if (n.getClazz() instanceof J.Identifier && n.getClazz().getType() != null &&
                !(n.getClazz().getType() instanceof JavaType.Class || n.getClazz().getType() instanceof JavaType.Unknown)) {
                n = SearchResult.found(n, "NewClass#clazz is J.Identifier and the type is is not JavaType$Class.");
            }
            return n;
        }

        @Override
        public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, ExecutionContext ctx) {
            J.ParameterizedType p = super.visitParameterizedType(type, ctx);
            if (p.getClazz() instanceof J.Identifier && p.getClazz().getType() != null &&
                !(p.getClazz().getType() instanceof JavaType.Class || p.getClazz().getType() instanceof JavaType.Unknown)) {
                p = SearchResult.found(p, "ParameterizedType#clazz is J.Identifier and the type is is not JavaType$Class.");
            }
            return p;
        }

        private boolean isAllowedToHaveNullType(J.Identifier ident) {
            return inPackageDeclaration() || inImport() || isClassName() ||
                   isMethodName() || isMethodInvocationName() || isFieldAccess(ident) || isBeingDeclared(ident) || isParameterizedType(ident) ||
                   isNewClass(ident) || isTypeParameter() || isMemberReference(ident) || isCaseLabel() || isLabel() || isAnnotationField(ident) ||
                   isInJavaDoc(ident) || isWhenLabel() || isUseSite();
        }

        private boolean inPackageDeclaration() {
            return getCursor().firstEnclosing(J.Package.class) != null;
        }

        private boolean inImport() {
            return getCursor().firstEnclosing(J.Import.class) != null;
        }

        private boolean isClassName() {
            return getCursor().getParentTreeCursor().getValue() instanceof J.ClassDeclaration;
        }

        private boolean isMethodName() {
            return getCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration;
        }

        private boolean isMethodInvocationName() {
            return getCursor().getParentTreeCursor().getValue() instanceof J.MethodInvocation;
        }

        private boolean isFieldAccess(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof J.FieldAccess &&
                   (ident == ((J.FieldAccess) value).getName() ||
                       ident == ((J.FieldAccess) value).getTarget() && !"class".equals(((J.FieldAccess) value).getSimpleName()));
        }

        private boolean isBeingDeclared(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof J.VariableDeclarations.NamedVariable && ident == ((J.VariableDeclarations.NamedVariable) value).getName();
        }

        private boolean isParameterizedType(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof J.ParameterizedType && ident == ((J.ParameterizedType) value).getClazz();
        }

        private boolean isNewClass(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof J.NewClass && ident == ((J.NewClass) value).getClazz();
        }

        private boolean isTypeParameter() {
            return getCursor().getParentTreeCursor().getValue() instanceof J.TypeParameter;
        }

        private boolean isMemberReference(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof J.MemberReference &&
                   ident == ((J.MemberReference) value).getReference();
        }

        private boolean isInJavaDoc(J.Identifier ident) {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof Javadoc.Reference &&
                   ident == ((Javadoc.Reference) value).getTree();
        }

        private boolean isCaseLabel() {
            return getCursor().getParentTreeCursor().getValue() instanceof J.Case;
        }


        private boolean isWhenLabel() {
            return getCursor().getParentTreeCursor().getValue() instanceof K.WhenBranch;
        }

        private boolean isUseSite() {
            Tree value = getCursor().getParentTreeCursor().getValue();
            return value instanceof K.AnnotationType || value instanceof K.MultiAnnotationType;
        }

        private boolean isLabel() {
            return getCursor().firstEnclosing(J.Label.class) != null;
        }

        private boolean isAnnotationField(J.Identifier ident) {
            Cursor parent = getCursor().getParentTreeCursor();
            return parent.getValue() instanceof J.Assignment &&
                   (ident == ((J.Assignment) parent.getValue()).getVariable() && getCursor().firstEnclosing(J.Annotation.class) != null);
        }

        private boolean isValidated(J.Identifier i) {
            J j = getCursor().dropParentUntil(it -> it instanceof J).getValue();
            // TODO: replace with AnnotationUseSite tree.
            return !(j instanceof K.Return);
        }

        private boolean isValidated(J.MethodInvocation mi) {
            return !mi.getMarkers().findFirst(IndexedAccess.class).isPresent();
        }

        private boolean isValidated(J.VariableDeclarations.NamedVariable v) {
            J.VariableDeclarations j = getCursor().firstEnclosing(J.VariableDeclarations.class);
            return j.getModifiers().stream().noneMatch(it -> "typealias".equals(it.getKeyword())) && !j.getMarkers().findFirst(Extension.class).isPresent();
        }

        private boolean isNotDestructType(JavaType.Variable variable) {
            return !"<destruct>".equals(variable.getName());
        }
    }
}
