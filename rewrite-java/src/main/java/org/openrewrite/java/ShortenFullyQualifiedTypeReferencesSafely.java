/*
 * Copyright 2026 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.PathUtils;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Generated;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.tree.TypeUtils.asFullyQualified;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

@EqualsAndHashCode(callSuper = false)
public class ShortenFullyQualifiedTypeReferencesSafely extends ScanningRecipe<Map<JavaType.FullyQualified, ShortenFullyQualifiedTypeReferencesSafely.DeclaredMembers>> {
    private static final String NON_SOURCES = "**/src/*/resources/**;**/docs/evidence/**;**/generated/**;" +
            "**/target/generated-sources/**";

    @Option(displayName = "Exclude files",
            description = "Additional source paths to leave unchanged. Glob patterns are separated by semicolons. " +
                          "Resource fixtures and generated or evidence paths are always excluded.",
            example = "**/src/main/java/org/openrewrite/php/tree/Php.java",
            required = false)
    @Getter
    final @Nullable String exclude;

    public ShortenFullyQualifiedTypeReferencesSafely() {
        this(null);
    }

    @JsonCreator
    public ShortenFullyQualifiedTypeReferencesSafely(@JsonProperty("exclude") @Nullable String exclude) {
        this.exclude = exclude;
    }

    @Getter
    final String displayName = "Safely add imports for fully qualified Java type references";

    @Getter
    final String description = "Shorten fully qualified Java type references and add imports when their simple names remain unambiguous. " +
            "Checks inherited member type names against available source declarations. When that information is unavailable, " +
            "keeps a type qualified unless an existing simple use in the class body proves its binding. " +
            "Only changes Java compilation units, skips resource fixtures and generated or evidence sources, " +
            "and preserves qualification-sensitive Lombok builder annotations and template syntax.";

    @Getter
    final Duration estimatedEffortPerOccurrence = Duration.ofMinutes(2);

    @Override
    public Map<JavaType.FullyQualified, DeclaredMembers> getInitialValue(ExecutionContext ctx) {
        return Collections.synchronizedMap(new IdentityHashMap<>());
    }

    @Override
    public JavaVisitor<ExecutionContext> getScanner(Map<JavaType.FullyQualified, DeclaredMembers> sources) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return isSource(sourceFile);
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration declaration, ExecutionContext ctx) {
                if (declaration.getType() instanceof JavaType.Class && !(declaration.getType() instanceof JavaType.ShallowClass) &&
                    hasCompleteMemberDeclarations(declaration)) {
                    DeclaredMembers members = DeclaredMembers.of(declaration);
                    synchronized (sources) {
                        DeclaredMembers known = sources.computeIfAbsent(declaration.getType(), key -> new DeclaredMembers());
                        known.types.addAll(members.types);
                        known.variables.addAll(members.variables);
                    }
                }
                return super.visitClassDeclaration(declaration, ctx);
            }
        };
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor(Map<JavaType.FullyQualified, DeclaredMembers> sources) {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return isSource(sourceFile);
            }

            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if (JavaSourceSet.isDirty(ctx, cu)) {
                    return cu;
                }
                Set<String> keepQualified = new HashSet<>(Arrays.asList("lombok.Builder", "lombok.experimental.SuperBuilder"));
                new JavaIsoVisitor<Set<String>>() {
                    @Override
                    public J.FieldAccess visitFieldAccess(J.FieldAccess access, Set<String> keep) {
                        JavaType type = access.getType();
                        if (access.getName().getFieldType() == null && type instanceof JavaType.Class &&
                            ((JavaType.Class) type).getOwningClass() == null &&
                            access.getSimpleName().equals(((JavaType.Class) type).getClassName()) &&
                            (hasSensitiveTypeSyntax(access) || "java.lang".equals(((JavaType.Class) type).getPackageName()) ||
                             isTemplateSyntax(getCursor()) || !inheritedNameIsProven(getCursor(), access.getSimpleName(),
                                    ((JavaType.Class) type).getFullyQualifiedName(), sources))) {
                            keep.add(((JavaType.Class) type).getFullyQualifiedName());
                        }
                        return super.visitFieldAccess(access, keep);
                    }

                    @Override
                    public Space visitSpace(Space space, Space.Location location, Set<String> keep) {
                        return space;
                    }
                }.visit(cu, keepQualified);
                Set<String> requestedImports = new TreeSet<>();
                J.CompilationUnit shortened = (J.CompilationUnit) requireNonNull(
                        ShortenFullyQualifiedTypeReferences.getVisitor(cu, keepQualified::contains, requestedImports).visit(cu, ctx));
                // Explicit imports take precedence over wildcard imports and same-package types. Keep existing
                // imports intact: folding them can change the binding of names elsewhere in this compilation unit.
                for (String type : requestedImports) {
                    shortened = (J.CompilationUnit) requireNonNull(new AddImport<ExecutionContext>(type, null, true, true).visit(shortened, ctx));
                }
                return shortened;
            }
        };
    }

    private static boolean hasSensitiveTypeSyntax(J.FieldAccess access) {
        AtomicBoolean sensitive = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, AtomicBoolean found) {
                found.set(true);
                return annotation;
            }

            @Override
            public Space visitSpace(Space space, Space.Location location, AtomicBoolean found) {
                if (!space.getComments().isEmpty()) {
                    found.set(true);
                }
                return space;
            }
        }.visit(access, sensitive);
        return sensitive.get();
    }

    private boolean isSource(SourceFile sourceFile) {
        if (!(sourceFile instanceof J.CompilationUnit) || sourceFile.getMarkers().findFirst(Generated.class).isPresent()) {
            return false;
        }
        String patterns = NON_SOURCES + (exclude == null ? "" : ";" + exclude);
        for (String pattern : patterns.split(";")) {
            if (!pattern.trim().isEmpty() && PathUtils.matchesGlob(sourceFile.getSourcePath(), pattern.trim())) {
                return false;
            }
        }
        J.CompilationUnit cu = (J.CompilationUnit) sourceFile;
        AtomicBoolean generatedDeclaration = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, AtomicBoolean generated) {
                if ("Generated".equals(annotation.getSimpleName())) {
                    generated.set(true);
                }
                return annotation;
            }
        }.visit(cu, generatedDeclaration);
        if (generatedDeclaration.get()) {
            return false;
        }
        List<Comment> headers = new ArrayList<>(cu.getComments());
        if (cu.getPackageDeclaration() != null) {
            headers.addAll(cu.getPackageDeclaration().getComments());
        }
        if (!cu.getImports().isEmpty()) {
            headers.addAll(cu.getImports().get(0).getComments());
        }
        if (!cu.getClasses().isEmpty()) {
            headers.addAll(cu.getClasses().get(0).getComments());
        }
        for (Comment comment : headers) {
            String text = comment.printComment(new Cursor(null, cu)).trim().toLowerCase(Locale.ROOT);
            if (text.contains("@generated") || text.contains("generated") &&
                (text.contains("do not") || text.contains("generated by") || text.contains("generated from"))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasCompleteMemberDeclarations(J.ClassDeclaration declaration) {
        AtomicBoolean augmented = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J preVisit(J tree, AtomicBoolean found) {
                if (found.get()) {
                    stopAfterPreVisit();
                }
                return tree;
            }

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, AtomicBoolean found) {
                JavaType.FullyQualified type = asFullyQualified(annotation.getType());
                // Compiler extensions such as Lombok can add member types absent from the source tree.
                if (type == null || !type.getFullyQualifiedName().startsWith("java.lang.")) {
                    found.set(true);
                }
                return annotation;
            }
        }.visit(declaration, augmented);
        return !augmented.get();
    }

    private static boolean isTemplateSyntax(Cursor cursor) {
        for (Cursor parent = cursor.getParentTreeCursor(); parent.getValue() instanceof J; parent = parent.getParentTreeCursor()) {
            Object tree = parent.getValue();
            if (tree instanceof J.MethodInvocation) {
                JavaType.Method method = ((J.MethodInvocation) tree).getMethodType();
                JavaType.FullyQualified owner = method == null ? null : method.getDeclaringType();
                if (owner == null || owner instanceof JavaType.Unknown) {
                    return true;
                }
                String name = owner.getFullyQualifiedName();
                if ("org.openrewrite.java.template.Semantics".equals(name) || name.startsWith("org.openrewrite.java.template.Semantics$")) {
                    return true;
                }
            } else if (tree instanceof J.MethodDeclaration) {
                for (J.Annotation annotation : ((J.MethodDeclaration) tree).getLeadingAnnotations()) {
                    JavaType.FullyQualified type = asFullyQualified(annotation.getType());
                    if (isOfClassType(type, "com.google.errorprone.refaster.annotation.BeforeTemplate") ||
                        isOfClassType(type, "com.google.errorprone.refaster.annotation.AfterTemplate") ||
                        (type == null || type instanceof JavaType.Unknown) &&
                        ("BeforeTemplate".equals(annotation.getSimpleName()) || "AfterTemplate".equals(annotation.getSimpleName()))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Whether {@code simpleName} at the cursor provably binds to {@code fullyQualifiedName} rather than to a member
     * type, or in an expression to a field, that an enclosing class inherits. {@code sources} holds the member names
     * of the classes whose declarations are known. Any other enclosing class or supertype leaves the inherited names
     * unknown, and then only a simple use of the type in the class body, attributed to it, proves the binding.
     */
    static boolean inheritedNameIsProven(Cursor cursor, String simpleName, String fullyQualifiedName,
                                         Map<JavaType.FullyQualified, DeclaredMembers> sources) {
        J.Block body = null;
        boolean unknown = false;
        Deque<JavaType.FullyQualified> ancestors = new ArrayDeque<>();
        Object child = cursor.getValue();
        for (Cursor parent = cursor.getParentTreeCursor(); parent.getValue() instanceof J; parent = parent.getParentTreeCursor()) {
            Object tree = parent.getValue();
            if (tree instanceof J.ClassDeclaration && ((J.ClassDeclaration) tree).getBody() == child) {
                J.ClassDeclaration declaration = (J.ClassDeclaration) tree;
                if (body == null) {
                    body = declaration.getBody();
                }
                JavaType.FullyQualified type = declaration.getType();
                if (type == null) {
                    unknown = true;
                } else {
                    unknown |= !sources.containsKey(type);
                    if (type.getSupertype() != null) {
                        ancestors.add(type.getSupertype());
                    }
                    ancestors.addAll(type.getInterfaces());
                }
            } else if (tree instanceof J.NewClass && ((J.NewClass) tree).getBody() == child) {
                J.NewClass newClass = (J.NewClass) tree;
                if (body == null) {
                    body = newClass.getBody();
                }
                JavaType.FullyQualified type = newClass.getClazz() == null ? null : asFullyQualified(newClass.getClazz().getType());
                if (type == null) {
                    unknown = true;
                } else {
                    ancestors.add(type);
                }
            }
            child = tree;
        }
        Set<String> visited = new HashSet<>();
        while (!ancestors.isEmpty()) {
            JavaType.FullyQualified ancestor = ancestors.removeFirst();
            String name = ancestor.getFullyQualifiedName();
            if (isOfClassType(ancestor, "java.lang.Object") || !visited.add(name)) {
                continue;
            }
            JavaType.FullyQualified raw = ancestor instanceof JavaType.Parameterized ? ((JavaType.Parameterized) ancestor).getType() : ancestor;
            DeclaredMembers members = sources.get(raw);
            if (members == null) {
                unknown = true;
            } else if (members.types.contains(simpleName) ||
                       ShortenFullyQualifiedTypeReferences.isTypeQualifierInExpression(cursor) && members.variables.contains(simpleName)) {
                // Also keep private or inaccessible member names when the index has no access information.
                return false;
            }
            if (ancestor instanceof JavaType.ShallowClass || ancestor instanceof JavaType.Unknown) {
                unknown = true;
                continue;
            }
            if (ancestor.getSupertype() != null) {
                ancestors.add(ancestor.getSupertype());
            }
            ancestors.addAll(ancestor.getInterfaces());
        }
        if (!unknown || body == null) {
            return true;
        }
        // An import alone cannot prove that its name wins: an inherited member type takes precedence over it.
        // An existing simple use in this class body, attributed to the intended type, does prove its binding.
        Cursor source = cursor.dropParentUntil(value -> value instanceof SourceFile);
        Map<J.Block, SimpleTypes> simpleTypes = source.computeMessageIfAbsent(
                ShortenFullyQualifiedTypeReferencesSafely.class.getName() + ".simpleTypes", key -> new IdentityHashMap<>());
        SimpleTypes witnesses = simpleTypes.computeIfAbsent(body, ShortenFullyQualifiedTypeReferencesSafely::simpleTypesInBody);
        return (ShortenFullyQualifiedTypeReferences.isTypeQualifierInExpression(cursor) ? witnesses.expressions : witnesses.types)
                .contains(fullyQualifiedName);
    }

    static final class DeclaredMembers {
        final Set<String> types = new HashSet<>();
        final Set<String> variables = new HashSet<>();

        static DeclaredMembers of(J.ClassDeclaration declaration) {
            DeclaredMembers members = new DeclaredMembers();
            for (Statement statement : declaration.getBody().getStatements()) {
                if (statement instanceof J.ClassDeclaration) {
                    members.types.add(((J.ClassDeclaration) statement).getSimpleName());
                } else if (statement instanceof J.VariableDeclarations) {
                    ((J.VariableDeclarations) statement).getVariables().forEach(variable -> members.variables.add(variable.getSimpleName()));
                } else if (statement instanceof J.EnumValueSet) {
                    ((J.EnumValueSet) statement).getEnums().forEach(value -> members.variables.add(value.getName().getSimpleName()));
                }
            }
            return members;
        }
    }

    private static class SimpleTypes {
        final Set<String> types = new HashSet<>();
        final Set<String> expressions = new HashSet<>();
    }

    private static SimpleTypes simpleTypesInBody(J.Block body) {
        SimpleTypes types = new SimpleTypes();
        new JavaIsoVisitor<SimpleTypes>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration declaration, SimpleTypes names) {
                return declaration;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, SimpleTypes names) {
                // The constructor and its arguments are in this class, but an anonymous body has its own inheritance.
                visit(newClass.getEnclosing(), names);
                visit(newClass.getClazz(), names);
                newClass.getArguments().forEach(argument -> visit(argument, names));
                return newClass;
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess access, SimpleTypes names) {
                visit(access.getTarget(), names);
                return access;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, SimpleTypes names) {
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (parent instanceof J.VariableDeclarations.NamedVariable && ((J.VariableDeclarations.NamedVariable) parent).getName() == identifier ||
                    parent instanceof J.MethodDeclaration && ((J.MethodDeclaration) parent).getName() == identifier ||
                    parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getName() == identifier ||
                    parent instanceof J.MemberReference && ((J.MemberReference) parent).getReference() == identifier ||
                    parent instanceof J.TypeParameter && ((J.TypeParameter) parent).getName() == identifier ||
                    parent instanceof J.InstanceOf && ((J.InstanceOf) parent).getPattern() == identifier ||
                    parent instanceof J.EnumValue && ((J.EnumValue) parent).getName() == identifier) {
                    return identifier;
                }
                if (identifier.getFieldType() == null && identifier.getType() instanceof JavaType.FullyQualified) {
                    JavaType.FullyQualified type = (JavaType.FullyQualified) identifier.getType();
                    String raw = type.getRawClassName();
                    if (identifier.getSimpleName().equals(raw.substring(raw.lastIndexOf('$') + 1))) {
                        names.types.add(type.getFullyQualifiedName());
                        if (ShortenFullyQualifiedTypeReferences.isTypeQualifierInExpression(getCursor())) {
                            names.expressions.add(type.getFullyQualifiedName());
                        }
                    }
                }
                return identifier;
            }

            @Override
            public Space visitSpace(Space space, Space.Location location, SimpleTypes names) {
                return space;
            }
        }.visit(body, types);
        return types;
    }

}
