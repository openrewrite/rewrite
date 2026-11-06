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
package org.openrewrite.java.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.marker.CompactSourceFile;
import org.openrewrite.java.tree.*;

import java.util.*;

import static java.util.Collections.emptyList;

/**
 * The types and static members code names by a simple name that neither the imports in effect nor its scope declare.
 */
public final class UnimportedReferences {

    private static final String TEMPLATE_PACKAGE = "org.openrewrite.java.internal.template";

    private UnimportedReferences() {
    }

    /**
     * @return Each type and static member {@code cu} names that nothing in it imports or declares, such as
     * "`List` (java.util.List)".
     */
    public static List<String> of(J.CompilationUnit cu) {
        for (J.ClassDeclaration classDecl : cu.getClasses()) {
            if (classDecl.getMarkers().findFirst(CompactSourceFile.class).isPresent()) {
                // a compact source file imports the java.base module implicitly
                return emptyList();
            }
        }
        Imports imports = new Imports();
        for (J.Import anImport : cu.getImports()) {
            imports.add(anImport);
        }
        return find(cu, imports, null);
    }

    /**
     * @param stub            A stub compiled from a template, which also holds the code around where it is applied.
     * @param templated       The code from the template, as the stub holds it.
     * @param templateImports The template's own imports, such as "import java.util.List;".
     * @return Each type and static member the template's code names that its imports don't declare, so that it
     * resolves only through imports the stub copied from the file the template is applied to.
     */
    public static List<String> ofTemplate(J.CompilationUnit stub, Collection<? extends J> templated,
                                          Collection<String> templateImports) {
        Imports imports = new Imports();
        for (String templateImport : templateImports) {
            imports.add(templateImport);
        }
        Set<UUID> inTemplate = new HashSet<>();
        for (J j : templated) {
            new TreeVisitor<Tree, Set<UUID>>() {
                @Override
                public Tree preVisit(Tree tree, Set<UUID> ids) {
                    ids.add(tree.getId());
                    return tree;
                }
            }.visit(j, inTemplate);
        }
        return inTemplate.isEmpty() ? emptyList() : find(stub, imports, inTemplate);
    }

    private static List<String> find(J.CompilationUnit cu, Imports imports, @Nullable Set<UUID> only) {
        String packageName = cu.getPackageDeclaration() == null ? "" : cu.getPackageDeclaration().getPackageName();
        Set<String> declared = new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<String> d) {
                d.add(classDecl.getSimpleName());
                return super.visitClassDeclaration(classDecl, d);
            }

            @Override
            public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, Set<String> d) {
                if (typeParam.getName() instanceof J.Identifier) {
                    d.add(((J.Identifier) typeParam.getName()).getSimpleName());
                }
                return super.visitTypeParameter(typeParam, d);
            }
        }.reduce(cu, new HashSet<>());

        Set<String> found = new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.Import visitImport(J.Import anImport, Set<String> f) {
                return anImport;
            }

            @Override
            public J.Package visitPackage(J.Package pkg, Set<String> f) {
                return pkg;
            }

            @Override
            protected JavadocVisitor<Set<String>> getJavadocVisitor() {
                return new JavadocVisitor<Set<String>>(this) {
                    @Override
                    public @Nullable Javadoc visit(@Nullable Tree tree, Set<String> f) {
                        return (Javadoc) tree;
                    }
                };
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<String> f) {
                if (method.getSelect() instanceof J.Identifier &&
                    ("__P__".equals(((J.Identifier) method.getSelect()).getSimpleName()) ||
                     "__M__".equals(((J.Identifier) method.getSelect()).getSimpleName()))) {
                    // a template's placeholder, whose types the template engine writes
                    return method;
                }
                J.MethodInvocation m = super.visitMethodInvocation(method, f);
                JavaType.Method type = m.getMethodType();
                if (m.getSelect() == null && type != null && type.hasFlags(Flag.Static) &&
                    known(type.getDeclaringType()) && inScope(m) &&
                    !declaresStatic(type.getDeclaringType(), m.getSimpleName(), getCursor())) {
                    f.add("`" + m.getSimpleName() + "()` (" + name(type.getDeclaringType()) + ")");
                }
                return m;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> f) {
                if (identifier.getSimpleName().indexOf('.') >= 0 || !inScope(identifier)) {
                    // some recipes put a qualified name in one identifier
                    return identifier;
                }
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == identifier ||
                    parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getName() == identifier ||
                    parent instanceof J.MemberReference && ((J.MemberReference) parent).getReference() == identifier ||
                    parent instanceof J.VariableDeclarations.NamedVariable &&
                    ((J.VariableDeclarations.NamedVariable) parent).getName() == identifier ||
                    parent instanceof J.ClassDeclaration || parent instanceof J.MethodDeclaration) {
                    return identifier;
                }
                JavaType.Variable fieldType = identifier.getFieldType();
                if (fieldType != null) {
                    if (fieldType.hasFlags(Flag.Static) && fieldType.getOwner() instanceof JavaType.FullyQualified &&
                        known((JavaType.FullyQualified) fieldType.getOwner()) &&
                        getCursor().firstEnclosing(J.Case.class) == null &&
                        !(parent instanceof J.Assignment && ((J.Assignment) parent).getVariable() == identifier &&
                          getCursor().firstEnclosing(J.Annotation.class) != null) &&
                        !declaresStatic((JavaType.FullyQualified) fieldType.getOwner(), identifier.getSimpleName(),
                                getCursor())) {
                        f.add("`" + identifier.getSimpleName() + "` (" +
                              name((JavaType.FullyQualified) fieldType.getOwner()) + ")");
                    }
                    return identifier;
                }
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(identifier.getType());
                // a nested type resolves through enclosing and inherited scopes too
                if (type == null || !known(type) || type.getOwningClass() != null ||
                    !identifier.getSimpleName().equals(type.getClassName())) {
                    return identifier;
                }
                String typePackage = type.getPackageName();
                // a type in the default package can't be imported at all
                if (!typePackage.isEmpty() && !"java.lang".equals(typePackage) && !typePackage.equals(packageName) &&
                    !typePackage.equals(TEMPLATE_PACKAGE) && !declared.contains(identifier.getSimpleName()) &&
                    !imports.declaresType(type.getFullyQualifiedName(), typePackage)) {
                    f.add("`" + identifier.getSimpleName() + "` (" + type.getFullyQualifiedName() + ")");
                }
                return identifier;
            }

            private boolean inScope(J j) {
                return only == null || only.contains(j.getId());
            }

            private boolean declaresStatic(JavaType.FullyQualified declaringType, String member, Cursor cursor) {
                return declaringType.getPackageName().isEmpty() ||
                       name(declaringType).startsWith(TEMPLATE_PACKAGE + ".") ||
                       inLexicalScope(declaringType, cursor) || imports.declaresStatic(declaringType, member);
            }
        }.reduce(cu, new LinkedHashSet<>());
        return new ArrayList<>(found);
    }

    private static boolean known(JavaType.@Nullable FullyQualified type) {
        return type != null && !(type instanceof JavaType.Unknown);
    }

    private static String name(JavaType.FullyQualified type) {
        return type.getFullyQualifiedName().replace('$', '.');
    }

    /**
     * Whether a class around the cursor declares or inherits the static member's declaring type, or it can't be told.
     */
    private static boolean inLexicalScope(JavaType.FullyQualified declaringType, Cursor cursor) {
        String fqn = declaringType.getFullyQualifiedName();
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            Object value = c.getValue();
            JavaType.FullyQualified enclosing;
            if (value instanceof J.ClassDeclaration) {
                enclosing = ((J.ClassDeclaration) value).getType();
            } else if (value instanceof J.NewClass && ((J.NewClass) value).getBody() != null) {
                enclosing = TypeUtils.asFullyQualified(((J.NewClass) value).getType());
            } else {
                continue;
            }
            if (!known(enclosing) || hierarchyIncomplete(enclosing, new HashSet<>())) {
                return true;
            }
            for (JavaType.FullyQualified scope = enclosing; scope != null; scope = scope.getOwningClass()) {
                if (fqn.equals(scope.getFullyQualifiedName()) || TypeUtils.isAssignableTo(fqn, scope)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hierarchyIncomplete(JavaType.FullyQualified type, Set<String> seen) {
        if (type instanceof JavaType.ShallowClass) {
            return true;
        }
        if (!seen.add(type.getFullyQualifiedName())) {
            return false;
        }
        JavaType.FullyQualified supertype = type.getSupertype();
        if (supertype instanceof JavaType.Unknown) {
            return true;
        }
        if (supertype != null && hierarchyIncomplete(supertype, seen)) {
            return true;
        }
        for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
            if (anInterface instanceof JavaType.Unknown || hierarchyIncomplete(anInterface, seen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * What a file's or a template's imports bring into scope.
     */
    private static final class Imports {
        final Set<String> types = new HashSet<>();
        final Set<String> onDemand = new HashSet<>();

        /**
         * The classes each static member is imported from, by the member's name, with null for one that has no type.
         */
        final Map<String, List<JavaType.@Nullable FullyQualified>> staticMembers = new HashMap<>();
        final Map<String, List<String>> staticMemberOwners = new HashMap<>();
        final List<JavaType.@Nullable FullyQualified> staticOnDemand = new ArrayList<>();
        final Set<String> staticOnDemandOwners = new HashSet<>();

        void add(J.Import anImport) {
            J.FieldAccess qualid = anImport.getQualid();
            String member = qualid.getSimpleName();
            if (anImport.isStatic()) {
                JavaType.FullyQualified owner = TypeUtils.asFullyQualified(qualid.getTarget().getType());
                if ("*".equals(member)) {
                    staticOnDemand.add(owner);
                } else {
                    staticMembers.computeIfAbsent(member, k -> new ArrayList<>()).add(owner);
                }
            } else if ("*".equals(member)) {
                onDemand.add(printed(qualid.getTarget()));
            } else {
                types.add(printed(qualid));
            }
        }

        /**
         * Adds an import as a template declares it, such as "import static java.util.Collections.emptyList;".
         */
        void add(String templateImport) {
            String statement = templateImport.trim();
            if (statement.endsWith(";")) {
                statement = statement.substring(0, statement.length() - 1).trim();
            }
            if (statement.startsWith("import static ")) {
                String qualified = statement.substring("import static ".length()).trim();
                int lastDot = qualified.lastIndexOf('.');
                if (lastDot < 0) {
                    return;
                }
                String owner = qualified.substring(0, lastDot);
                String member = qualified.substring(lastDot + 1);
                if ("*".equals(member)) {
                    staticOnDemandOwners.add(owner);
                } else {
                    staticMemberOwners.computeIfAbsent(member, k -> new ArrayList<>()).add(owner);
                }
            } else if (statement.startsWith("import ")) {
                String qualified = statement.substring("import ".length()).trim();
                if (qualified.endsWith(".*")) {
                    onDemand.add(qualified.substring(0, qualified.length() - 2));
                } else {
                    types.add(qualified);
                }
            }
        }

        boolean declaresType(String fqn, String packageName) {
            return types.contains(fqn) || onDemand.contains(packageName);
        }

        boolean declaresStatic(JavaType.FullyQualified declaringType, String member) {
            for (JavaType.FullyQualified owner : staticMembers.getOrDefault(member, emptyList())) {
                if (couldDeclare(owner, declaringType)) {
                    return true;
                }
            }
            for (JavaType.FullyQualified owner : staticOnDemand) {
                if (couldDeclare(owner, declaringType)) {
                    return true;
                }
            }
            // a template's imports have no types, so a subtype that inherits the member can't be told apart
            return staticMemberOwners.containsKey(member) || staticOnDemandOwners.contains(name(declaringType));
        }

        /**
         * Whether a static member imported from {@code owner} can be one {@code declaringType} declares, which it is
         * when the owner is that type or inherits from it.
         */
        private static boolean couldDeclare(JavaType.@Nullable FullyQualified owner,
                                            JavaType.FullyQualified declaringType) {
            return owner == null || owner instanceof JavaType.Unknown || owner instanceof JavaType.ShallowClass ||
                   owner.getFullyQualifiedName().equals(declaringType.getFullyQualifiedName()) ||
                   TypeUtils.isAssignableTo(declaringType.getFullyQualifiedName(), owner);
        }

        private static String printed(Expression name) {
            if (name instanceof J.FieldAccess) {
                return printed(((J.FieldAccess) name).getTarget()) + "." + ((J.FieldAccess) name).getSimpleName();
            } else if (name instanceof J.Identifier) {
                return ((J.Identifier) name).getSimpleName();
            }
            return name.toString();
        }
    }
}
