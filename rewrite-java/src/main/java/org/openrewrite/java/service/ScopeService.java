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
package org.openrewrite.java.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.*;

import javax.lang.model.SourceVersion;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * The name binding rules of one language, which {@link Scope} asks through the source file. This class holds
 * Java's rules, and a language built on {@link J} with rules of its own overrides it.
 */
@Incubating(since = "8.113.0")
public class ScopeService {

    /**
     * Whether the tree at the cursor opens a scope. A class body is not a scope. Its class is, since members are
     * visible throughout the body.
     */
    public boolean isScope(Cursor cursor) {
        Object tree = cursor.getValue();
        if (tree instanceof J.Block) {
            Object parent = parentTree(cursor);
            return !(parent instanceof J.ClassDeclaration || parent instanceof J.NewClass);
        }
        return tree instanceof JavaSourceFile ||
               tree instanceof J.ClassDeclaration ||
               tree instanceof J.NewClass && ((J.NewClass) tree).getBody() != null ||
               tree instanceof J.MethodDeclaration ||
               tree instanceof J.Lambda ||
               tree instanceof J.ForLoop ||
               tree instanceof J.ForEachLoop ||
               tree instanceof J.Try ||
               tree instanceof J.Try.Catch ||
               tree instanceof J.Case;
    }

    /**
     * The names the scope at the cursor binds, wherever in it they become visible. A local declared in a colon-style
     * case belongs to the whole switch block, so the switch block binds it and the case does not. A class, method or
     * local class binds its name in the scope around it. Attribution does not list a supertype's member types, so
     * the inherited ones bound are those the source file uses.
     */
    public List<Scope.Binding> bindings(Cursor scope) {
        J scopeTree = scope.getValue();
        List<Scope.Binding> bindings = new ArrayList<>();
        if (scopeTree instanceof JavaSourceFile) {
            imports(scope, (JavaSourceFile) scopeTree, bindings);
        }
        new JavaIsoVisitor<List<Scope.Binding>>() {
            @Override
            public @Nullable J preVisit(J tree, List<Scope.Binding> found) {
                if (tree != scopeTree && isScope(getCursor())) {
                    if (tree instanceof J.ClassDeclaration) {
                        found.add(declared(new Cursor(getCursor(), ((J.ClassDeclaration) tree).getName()), scope, Scope.Namespace.TYPE));
                    } else if (tree instanceof J.MethodDeclaration && !isConstructor((J.MethodDeclaration) tree)) {
                        found.add(declared(new Cursor(getCursor(), ((J.MethodDeclaration) tree).getName()), scope, Scope.Namespace.METHOD));
                    } else if (tree instanceof J.Case && isColonCase(tree)) {
                        for (Statement statement : ((J.Case) tree).getStatements()) {
                            if (statement instanceof J.VariableDeclarations) {
                                Cursor declarations = new Cursor(getCursor(), statement);
                                for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) statement).getVariables()) {
                                    found.add(declared(new Cursor(new Cursor(declarations, variable), variable.getName()), scope, Scope.Namespace.VARIABLE));
                                }
                            }
                        }
                    }
                    stopAfterPreVisit();
                }
                return tree;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, List<Scope.Binding> found) {
                Cursor parent = getCursor().getParentTreeCursor();
                Object owner = parent.getValue();
                if (owner instanceof J.VariableDeclarations.NamedVariable && ((J.VariableDeclarations.NamedVariable) owner).getName() == identifier) {
                    Object declarations = parentTree(parent);
                    if (!(scopeTree instanceof J.Case && isColonCase(scopeTree) &&
                          ((J.Case) scopeTree).getStatements().contains(declarations))) {
                        found.add(declared(getCursor(), scope, Scope.Namespace.VARIABLE));
                    }
                } else if (owner instanceof J.InstanceOf && ((J.InstanceOf) owner).getPattern() == identifier ||
                           owner instanceof J.EnumValue && ((J.EnumValue) owner).getName() == identifier) {
                    found.add(declared(getCursor(), scope, Scope.Namespace.VARIABLE));
                } else if (owner instanceof J.TypeParameter && ((J.TypeParameter) owner).getName() == identifier) {
                    found.add(declared(getCursor(), scope, Scope.Namespace.TYPE));
                }
                return identifier;
            }
        }.visit(scopeTree, bindings, scope.getParentOrThrow());
        JavaType.FullyQualified inheriting = null;
        if (scopeTree instanceof J.ClassDeclaration) {
            inheriting = ((J.ClassDeclaration) scopeTree).getType();
        } else if (scopeTree instanceof J.NewClass && ((J.NewClass) scopeTree).getClazz() != null) {
            inheriting = TypeUtils.asFullyQualified(((J.NewClass) scopeTree).getClazz().getType());
        }
        if (inheriting != null) {
            // An anonymous class belongs to the file's package, whichever package its supertype is in
            JavaSourceFile sourceFile = scope.firstEnclosing(JavaSourceFile.class);
            String fromPackage = sourceFile == null ? inheriting.getPackageName() :
                    sourceFile.getPackageDeclaration() == null ? "" : sourceFile.getPackageDeclaration().getPackageName();
            inherited(scope, inheriting, fromPackage, scopeTree instanceof J.NewClass, bindings);
            inheritedMemberTypes(scope, inheriting, fromPackage, scopeTree instanceof J.NewClass, bindings);
        }
        return bindings;
    }

    /**
     * Whether the method is a constructor, which binds no name. Attribution tells where it is present, since a
     * language may leave the return type off a method too.
     */
    private static boolean isConstructor(J.MethodDeclaration method) {
        return method.getMethodType() != null ? method.getMethodType().isConstructor() : method.isConstructor();
    }

    /**
     * Whether a binding is in scope at a position under the tree binding it. A local is visible from its declaration
     * to the end of its block, a member throughout its class body, a type parameter throughout its declaration, and
     * a pattern variable where it is definitely matched (JLS 6.3.1).
     */
    public boolean isVisibleAt(Scope.Binding binding, Cursor position) {
        J scopeTree = binding.getScope().getTree();
        List<J> at = pathBelow(scopeTree, position);
        if (at == null) {
            return false;
        }
        if (scopeTree instanceof JavaSourceFile) {
            return true;
        }
        if (!(binding.getTree() instanceof J.Identifier)) {
            return !at.isEmpty() && isBody(scopeTree, at.get(0));
        }
        List<J> declaration = pathBelow(scopeTree, binding.getCursor());
        if (declaration == null || declaration.isEmpty() || at.isEmpty()) {
            return false;
        }
        if (declaration.size() >= 2 && declaration.get(declaration.size() - 2) instanceof J.TypeParameter) {
            return true;
        }
        for (int i = declaration.size() - 1; i >= 0; i--) {
            if (declaration.get(i) instanceof J.InstanceOf) {
                return matchedAt(declaration, i, scopeTree, at);
            }
        }
        if (scopeTree instanceof J.Block) {
            return precedes(statementsOf((J.Block) scopeTree), declaration, at);
        } else if (scopeTree instanceof J.ForLoop) {
            J.ForLoop.Control control = ((J.ForLoop) scopeTree).getControl();
            if (same(at.get(0), control) && at.size() > 1 && control.getInit().contains(at.get(1))) {
                return precedes(new ArrayList<>(control.getInit()), declaration.subList(1, declaration.size()), at.subList(1, at.size()));
            }
            return true;
        } else if (scopeTree instanceof J.Try) {
            J.Try try_ = (J.Try) scopeTree;
            if (same(at.get(0), try_.getBody())) {
                return true;
            }
            List<J.Try.Resource> resources = try_.getResources() == null ? Collections.emptyList() : try_.getResources();
            int declared = resources.indexOf(declaration.get(0));
            int asked = resources.indexOf(at.get(0));
            return declared >= 0 && asked > declared;
        } else if (scopeTree instanceof J.Case) {
            return !((J.Case) scopeTree).getCaseLabels().contains(at.get(0));
        } else if (scopeTree instanceof J.MethodDeclaration && !isBody(scopeTree, at.get(0))) {
            // Where a language gives parameters default values, one can read the parameters before it
            List<Statement> parameters = ((J.MethodDeclaration) scopeTree).getParameters();
            int declared = parameters.indexOf(declaration.get(0));
            int asked = parameters.indexOf(at.get(0));
            return declared >= 0 && asked > declared;
        }
        return isBody(scopeTree, at.get(0));
    }

    /**
     * Whether the tree at the cursor is a static context (JLS 8.1.3), so that code under it runs with no instance
     * of the class around it. An enum constant is one for its arguments and its body.
     */
    public boolean isStaticContext(Cursor cursor) {
        Object tree = cursor.getValue();
        if (tree instanceof J.ClassDeclaration) {
            J.ClassDeclaration classDeclaration = (J.ClassDeclaration) tree;
            return classDeclaration.hasModifier(J.Modifier.Type.Static) ||
                   classDeclaration.getKind() != J.ClassDeclaration.Kind.Type.Class;
        }
        return tree instanceof J.MethodDeclaration && ((J.MethodDeclaration) tree).hasModifier(J.Modifier.Type.Static) ||
               tree instanceof J.VariableDeclarations && ((J.VariableDeclarations) tree).hasModifier(J.Modifier.Type.Static) ||
               tree instanceof J.Block && ((J.Block) tree).isStatic() ||
               tree instanceof J.EnumValue;
    }

    /**
     * The namespace the identifier at the cursor refers into, or null where it declares a name or names a member
     * selected with a dot, an enum constant in a case label, a label or a package. An ambiguous name such as
     * {@code A} in {@code A.b} reads a type only where attribution marks it one and no variable of its name is in
     * scope, as a variable obscures a type of its name (JLS 6.4.2).
     */
    public Scope.@Nullable Namespace namespaceOf(Cursor identifier) {
        J.Identifier ident = identifier.getValue();
        if ("this".equals(ident.getSimpleName()) || "super".equals(ident.getSimpleName()) || isInImportPackageOrJavadoc(identifier)) {
            return null;
        }
        Cursor parentCursor = identifier.getParentTreeCursor();
        Object parent = parentCursor.getValue();
        if (parent instanceof J.VariableDeclarations.NamedVariable && ((J.VariableDeclarations.NamedVariable) parent).getName() == ident ||
            parent instanceof J.MethodDeclaration && ((J.MethodDeclaration) parent).getName() == ident ||
            parent instanceof J.ClassDeclaration && ((J.ClassDeclaration) parent).getName() == ident ||
            parent instanceof J.TypeParameter && ((J.TypeParameter) parent).getName() == ident ||
            parent instanceof J.InstanceOf && ((J.InstanceOf) parent).getPattern() == ident ||
            parent instanceof J.EnumValue || parent instanceof J.Label || parent instanceof J.Break ||
            parent instanceof J.Continue ||
            parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == ident ||
            parent instanceof J.MemberReference && ((J.MemberReference) parent).getReference() == ident ||
            parent instanceof J.Assignment && ((J.Assignment) parent).getVariable() == ident && parentTree(parentCursor) instanceof J.Annotation ||
            parent instanceof J.Case && ("default".equals(ident.getSimpleName()) || isEnumCaseLabel((J.Case) parent, ident))) {
            return null;
        }
        if (parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getName() == ident) {
            return ((J.MethodInvocation) parent).getSelect() == null ? Scope.Namespace.METHOD : null;
        }
        if (isTypePosition(parentCursor, parent, ident) || isTypeName(ident) && !isVariableInScope(identifier)) {
            return Scope.Namespace.TYPE;
        }
        return isPackage(parentCursor, ident) ? null : Scope.Namespace.VARIABLE;
    }

    /**
     * The walk stops at the first block, as none of the three can hold one.
     */
    private static boolean isInImportPackageOrJavadoc(Cursor identifier) {
        for (Cursor c = identifier.getParent(); c != null; c = c.getParent()) {
            Object tree = c.getValue();
            if (tree instanceof J.Import || tree instanceof J.Package || tree instanceof Javadoc) {
                return true;
            } else if (tree instanceof J.Block) {
                return false;
            }
        }
        return false;
    }

    /**
     * Whether the identifier starts the package of a qualified type name, as {@code java} does in
     * {@code java.util.List}. Attribution tells, as the names selected from it lead to a type of exactly that name.
     */
    private static boolean isPackage(Cursor parentCursor, J.Identifier ident) {
        if (ident.getFieldType() != null || ident.getType() != null && !(ident.getType() instanceof JavaType.Unknown)) {
            return false;
        }
        StringBuilder qualified = new StringBuilder(ident.getSimpleName());
        Object selected = ident;
        for (Cursor c = parentCursor; c.getValue() instanceof J.FieldAccess; c = c.getParentTreeCursor()) {
            J.FieldAccess select = c.getValue();
            if (select.getTarget() != selected) {
                return false;
            }
            qualified.append('.').append(select.getSimpleName());
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(select.getType());
            if (type != null && !(type instanceof JavaType.Unknown)) {
                return type.getFullyQualifiedName().contentEquals(qualified);
            }
            selected = select;
        }
        return false;
    }

    /**
     * Whether a local variable may take the name of another local that is in scope where it is declared. Java
     * forbids it.
     */
    public boolean localMayShadowLocal() {
        return false;
    }

    /**
     * Whether a name is a keyword or literal of the language, which no variable can take.
     */
    public boolean isReserved(String name) {
        return SourceVersion.isKeyword(name);
    }

    private static boolean isTypePosition(Cursor parentCursor, Object parent, J.Identifier ident) {
        if (parent instanceof J.VariableDeclarations ||
            parent instanceof J.ClassDeclaration || parent instanceof J.TypeParameter ||
            parent instanceof J.ParameterizedType || parent instanceof J.ArrayType || parent instanceof J.Wildcard ||
            parent instanceof J.DeconstructionPattern ||
            parent instanceof J.AnnotatedType || parent instanceof J.IntersectionType || parent instanceof J.MultiCatch) {
            return true;
        } else if (parent instanceof J.MethodDeclaration) {
            return ((J.MethodDeclaration) parent).getDefaultValue() != ident;
        } else if (parent instanceof J.NewArray) {
            return ((J.NewArray) parent).getTypeExpression() == ident;
        } else if (parent instanceof J.NewClass) {
            return ((J.NewClass) parent).getClazz() == ident;
        } else if (parent instanceof J.InstanceOf) {
            return ((J.InstanceOf) parent).getClazz() == ident;
        } else if (parent instanceof J.Annotation) {
            return ((J.Annotation) parent).getAnnotationType() == ident;
        } else if (parent instanceof J.ControlParentheses) {
            return parentTree(parentCursor) instanceof J.TypeCast;
        }
        return false;
    }

    private Scope.Binding declared(Cursor identifier, Cursor scope, Scope.Namespace namespace) {
        J.Identifier ident = identifier.getValue();
        return new Scope.Binding(identifier, ident.getSimpleName(), namespace, scope,
                namespace == Scope.Namespace.VARIABLE ? ident.getFieldType() : ident.getType(), this);
    }

    private void imports(Cursor scope, JavaSourceFile cu, List<Scope.Binding> bindings) {
        List<Scope.Binding> onDemand = new ArrayList<>();
        for (J.Import anImport : cu.getImports()) {
            Cursor importCursor = new Cursor(scope, anImport);
            String name = anImport.getQualid().getSimpleName();
            if (!anImport.isStatic()) {
                if (!"*".equals(name)) {
                    J.Identifier alias = anImport.getAlias();
                    bindings.add(new Scope.Binding(importCursor, alias != null ? alias.getSimpleName() : name,
                            Scope.Namespace.TYPE, scope, anImport.getQualid().getType(), this));
                }
                continue;
            }
            JavaType.FullyQualified owner = TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType());
            if (owner == null) {
                if (!"*".equals(name)) {
                    for (Scope.Namespace namespace : Scope.Namespace.values()) {
                        bindings.add(new Scope.Binding(importCursor, name, namespace, scope, null, this));
                    }
                }
                continue;
            }
            List<Scope.Binding> imported = "*".equals(name) ? onDemand : bindings;
            boolean found = false;
            for (JavaType.Variable field : membersOf(owner, true, null, true, JavaType.FullyQualified::getMembers,
                    JavaType.Variable::getFlagsBitMap, JavaType.Variable::getName)) {
                if ("*".equals(name) || field.getName().equals(name)) {
                    imported.add(new Scope.Binding(importCursor, field.getName(), Scope.Namespace.VARIABLE, scope, field, this));
                    found = true;
                }
            }
            for (JavaType.Method method : membersOf(owner, true, null, true, JavaType.FullyQualified::getMethods,
                    JavaType.Method::getFlagsBitMap, JavaType.Method::getName)) {
                if (!method.isConstructor() && ("*".equals(name) || method.getName().equals(name))) {
                    imported.add(new Scope.Binding(importCursor, method.getName(), Scope.Namespace.METHOD, scope, method, this));
                    found = true;
                }
            }
            if (!found && !"*".equals(name)) {
                // A static import of a name that is neither a field nor a method imports a member type.
                bindings.add(new Scope.Binding(importCursor, name, Scope.Namespace.TYPE, scope, null, this));
            }
        }
        // An import of one name shadows an on-demand import of it (JLS 6.4.1), so it is found first
        bindings.addAll(onDemand);
    }

    /**
     * Binds the fields and methods the class has without its body declaring them. Those of its supertypes come
     * after the ones the class declares, so a lookup by name finds the declared one and {@code super} still reaches
     * the inherited one it hides. A named class also has members only its attribution lists, as an enum's
     * {@code values()}. An anonymous class is given by the type it extends, whose members it all inherits.
     */
    private void inherited(Cursor scope, JavaType.FullyQualified type, String fromPackage, boolean anonymous,
                                  List<Scope.Binding> bindings) {
        if (!anonymous) {
            Set<String> fields = new HashSet<>();
            Set<String> methods = new HashSet<>();
            for (Scope.Binding binding : bindings) {
                (binding.getNamespace() == Scope.Namespace.METHOD ? methods : fields).add(binding.getName());
            }
            // A class reaches its own members whatever their access, as it does a private field an annotation
            // processor generates
            for (JavaType.Variable field : type.getMembers()) {
                if (fields.add(field.getName())) {
                    bindings.add(new Scope.Binding(scope, field.getName(), Scope.Namespace.VARIABLE, scope, field, this));
                }
            }
            for (JavaType.Method method : type.getMethods()) {
                if (!method.isConstructor() && methods.add(method.getName())) {
                    bindings.add(new Scope.Binding(scope, method.getName(), Scope.Namespace.METHOD, scope, method, this));
                }
            }
        }
        for (JavaType.Variable field : membersOf(type, anonymous, fromPackage, false,
                JavaType.FullyQualified::getMembers, JavaType.Variable::getFlagsBitMap, JavaType.Variable::getName)) {
            bindings.add(new Scope.Binding(scope, field.getName(), Scope.Namespace.VARIABLE, scope, field, this));
        }
        for (JavaType.Method method : membersOf(type, anonymous, fromPackage, false,
                JavaType.FullyQualified::getMethods, JavaType.Method::getFlagsBitMap, JavaType.Method::getName)) {
            if (!method.isConstructor()) {
                bindings.add(new Scope.Binding(scope, method.getName(), Scope.Namespace.METHOD, scope, method, this));
            }
        }
    }

    private static boolean isVisibleFrom(long flags, JavaType.FullyQualified owner, @Nullable String fromPackage) {
        return Flag.hasFlags(flags, Flag.Public) || Flag.hasFlags(flags, Flag.Protected) ||
               !Flag.hasFlags(flags, Flag.Private) && (fromPackage == null || fromPackage.equals(owner.getPackageName()));
    }

    private void inheritedMemberTypes(Cursor scope, JavaType.FullyQualified type, String fromPackage, boolean includeSelf,
                                             List<Scope.Binding> bindings) {
        JavaSourceFile sourceFile = scope.firstEnclosing(JavaSourceFile.class);
        if (sourceFile == null) {
            return;
        }
        Set<String> supertypes = new HashSet<>();
        Deque<JavaType.FullyQualified> pending = new ArrayDeque<>();
        if (includeSelf) {
            pending.add(type);
        } else {
            if (type.getSupertype() != null) {
                pending.add(type.getSupertype());
            }
            pending.addAll(type.getInterfaces());
        }
        while (!pending.isEmpty()) {
            JavaType.FullyQualified next = pending.poll();
            if (!(next instanceof JavaType.Unknown) && supertypes.add(next.getFullyQualifiedName())) {
                if (next.getSupertype() != null) {
                    pending.add(next.getSupertype());
                }
                pending.addAll(next.getInterfaces());
            }
        }
        Set<String> declared = new HashSet<>();
        for (Scope.Binding binding : bindings) {
            if (binding.getNamespace() == Scope.Namespace.TYPE) {
                declared.add(binding.getName());
            }
        }
        for (JavaType used : sourceFile.getTypesInUse().getTypesInUse()) {
            JavaType.FullyQualified member = TypeUtils.asFullyQualified(used);
            JavaType.FullyQualified owner = member == null ? null : member.getOwningClass();
            if (owner != null && supertypes.contains(owner.getFullyQualifiedName()) && !member.hasFlags(Flag.Private) &&
                (member.hasFlags(Flag.Public) || member.hasFlags(Flag.Protected) || owner.getPackageName().equals(fromPackage))) {
                String raw = member.getRawClassName();
                String name = raw.substring(raw.lastIndexOf('$') + 1);
                if (declared.add(name)) {
                    bindings.add(new Scope.Binding(scope, name, Scope.Namespace.TYPE, scope, member, this));
                }
            }
        }
    }

    /**
     * The fields or methods of a type's supertypes, and of the type itself where {@code includeSelf} asks, that are
     * visible from a package, or only the static ones where {@code onlyStatic} asks for those, one per name and
     * nearest declaration first.
     */
    private static <M> List<M> membersOf(JavaType.FullyQualified type, boolean includeSelf, @Nullable String fromPackage,
                                         boolean onlyStatic,
                                         Function<JavaType.FullyQualified, List<M>> members,
                                         ToLongFunction<M> flags, Function<M, String> name) {
        List<M> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Deque<JavaType.FullyQualified> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.add(type);
        while (!pending.isEmpty()) {
            JavaType.FullyQualified next = pending.poll();
            if (next instanceof JavaType.Unknown || !visited.add(next.getFullyQualifiedName())) {
                continue;
            }
            for (M member : includeSelf || next != type ? members.apply(next) : Collections.<M>emptyList()) {
                long bits = flags.applyAsLong(member);
                if (isVisibleFrom(bits, next, fromPackage) && (!onlyStatic || Flag.hasFlags(bits, Flag.Static)) &&
                    seen.add(name.apply(member))) {
                    found.add(member);
                }
            }
            if (next.getSupertype() != null) {
                pending.add(next.getSupertype());
            }
            pending.addAll(next.getInterfaces());
        }
        return found;
    }

    /**
     * Whether the {@code instanceof} at the cursor is known to have been true wherever the position is reached,
     * which is where a pattern variable it declared would be in scope (JLS 6.3.1 and 6.3.2). The value tested may
     * have been assigned since.
     */
    public boolean isMatchedAt(Cursor instanceOf, Cursor position) {
        for (Cursor c = instanceOf.getParent(); c != null; c = c.getParent()) {
            if (c.getValue() instanceof J && isScope(c)) {
                J scopeTree = c.getValue();
                List<J> tested = pathBelow(scopeTree, instanceOf);
                List<J> at = pathBelow(scopeTree, position);
                return tested != null && at != null && !tested.isEmpty() &&
                       matchedAt(tested, tested.size() - 1, scopeTree, at);
            }
        }
        return false;
    }

    /**
     * Whether a pattern variable declared by the {@code instanceof} at {@code declaration.get(instanceOf)} is
     * definitely matched at the position {@code at}, following JLS 6.3.1 and 6.3.2.
     */
    private boolean matchedAt(List<J> declaration, int instanceOf, J scopeTree, List<J> at) {
        boolean whenTrue = true;
        J child = declaration.get(instanceOf);
        for (int i = instanceOf - 1; i >= -1; i--) {
            J parent = i >= 0 ? declaration.get(i) : scopeTree;
            if (parent instanceof J.Parentheses || parent instanceof J.ControlParentheses) {
                child = parent;
                continue;
            }
            if (parent instanceof J.Unary && ((J.Unary) parent).getOperator() == J.Unary.Type.Not) {
                whenTrue = !whenTrue;
            } else if (parent instanceof J.Binary && ((J.Binary) parent).getOperator() == J.Binary.Type.And) {
                J.Binary and = (J.Binary) parent;
                if (whenTrue && and.getLeft() == child && contains(at, and.getRight())) {
                    return true;
                }
                if (!whenTrue) {
                    return false;
                }
            } else if (parent instanceof J.Binary && ((J.Binary) parent).getOperator() == J.Binary.Type.Or) {
                J.Binary or = (J.Binary) parent;
                if (!whenTrue && or.getLeft() == child && contains(at, or.getRight())) {
                    return true;
                }
                if (whenTrue) {
                    return false;
                }
            } else if (parent instanceof J.Ternary) {
                J.Ternary ternary = (J.Ternary) parent;
                return ternary.getCondition() == child &&
                       contains(at, whenTrue ? ternary.getTruePart() : ternary.getFalsePart());
            } else if (parent instanceof J.If) {
                J.If if_ = (J.If) parent;
                if (if_.getIfCondition() != child) {
                    return false;
                }
                if (contains(at, whenTrue ? if_.getThenPart() : if_.getElsePart() == null ? null : if_.getElsePart().getBody())) {
                    return true;
                }
                boolean introduced;
                if (if_.getElsePart() == null) {
                    introduced = !whenTrue && !completesNormally(if_.getThenPart());
                } else {
                    boolean thenCompletes = completesNormally(if_.getThenPart());
                    boolean elseCompletes = completesNormally(if_.getElsePart().getBody());
                    introduced = whenTrue ? thenCompletes && !elseCompletes : !thenCompletes && elseCompletes;
                }
                return introduced && followedBy(declaration, i, scopeTree, at);
            } else if (parent instanceof J.WhileLoop) {
                J.WhileLoop loop = (J.WhileLoop) parent;
                if (loop.getCondition() != child) {
                    return false;
                }
                if (whenTrue) {
                    return contains(at, loop.getBody());
                }
                return !breaksTo(declaration, i, loop.getBody()) && followedBy(declaration, i, scopeTree, at);
            } else if (parent instanceof J.DoWhileLoop) {
                J.DoWhileLoop loop = (J.DoWhileLoop) parent;
                return loop.getWhileCondition() == child && !whenTrue && !breaksTo(declaration, i, loop.getBody()) &&
                       followedBy(declaration, i, scopeTree, at);
            } else if (parent instanceof J.ForLoop.Control) {
                // The loop is its own scope, so a binding of it is never visible after the loop.
                J.ForLoop.Control control = (J.ForLoop.Control) parent;
                J.ForLoop loop = i - 1 >= 0 ? (J.ForLoop) declaration.get(i - 1) : (J.ForLoop) scopeTree;
                return control.getCondition() == child && whenTrue &&
                       (contains(at, loop.getBody()) || control.getUpdate().stream().anyMatch(u -> contains(at, u)));
            } else if (parent instanceof J.Case) {
                J.Case case_ = (J.Case) parent;
                return case_.getGuard() == child && whenTrue &&
                       (contains(at, case_.getBody()) || case_.getStatements().stream().anyMatch(s -> contains(at, s)));
            } else {
                return false;
            }
            child = parent;
        }
        return false;
    }

    /**
     * Whether a {@code break} in the body of the loop at {@code declaration.get(loop)} targets that loop, unlabeled or
     * by a label on it. JLS 6.3.2 counts one that a {@code finally} discards.
     */
    private boolean breaksTo(List<J> declaration, int loop, Statement body) {
        if (jumpsTo(body, null, J.Break.class, true)) {
            return true;
        }
        for (int i = loop - 1; i >= 0 && declaration.get(i) instanceof J.Label; i--) {
            if (jumpsTo(body, ((J.Label) declaration.get(i)).getLabel().getSimpleName(), J.Break.class, true)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the position is in a statement after the one at {@code declaration.get(statement)}, in the block or
     * case holding it.
     */
    private static boolean followedBy(List<J> declaration, int statement, J scopeTree, List<J> at) {
        J stated = declaration.get(statement);
        int i = statement - 1;
        while (i >= 0 && declaration.get(i) instanceof J.Label) {
            stated = declaration.get(i--);
        }
        J holder = i >= 0 ? declaration.get(i) : scopeTree;
        List<Statement> statements = holder instanceof J.Block ? ((J.Block) holder).getStatements() :
                holder instanceof J.Case ? ((J.Case) holder).getStatements() : null;
        if (statements == null) {
            return false;
        }
        int index = statements.indexOf(stated);
        for (int later = index + 1; later < statements.size() && index >= 0; later++) {
            if (contains(at, statements.get(later))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a statement can complete normally (JLS 14.22), read from its shape alone. Only the literal
     * {@code true}, parenthesized or not, counts as a constant loop condition, so a loop on {@code 1 == 1} is taken
     * to complete.
     */
    public boolean completesNormally(@Nullable Statement statement) {
        if (statement == null) {
            return true;
        } else if (statement instanceof J.Return || statement instanceof J.Throw || statement instanceof J.Break ||
                   statement instanceof J.Continue || statement instanceof J.Yield) {
            return false;
        } else if (statement instanceof J.Block) {
            for (Statement s : ((J.Block) statement).getStatements()) {
                if (!completesNormally(s)) {
                    return false;
                }
            }
            return true;
        } else if (statement instanceof J.If) {
            J.If if_ = (J.If) statement;
            return if_.getElsePart() == null || completesNormally(if_.getThenPart()) ||
                   completesNormally(if_.getElsePart().getBody());
        } else if (statement instanceof J.WhileLoop) {
            J.WhileLoop loop = (J.WhileLoop) statement;
            return !isTrue(loop.getCondition().getTree()) || jumpsTo(loop.getBody(), null, J.Break.class, false);
        } else if (statement instanceof J.DoWhileLoop) {
            J.DoWhileLoop loop = (J.DoWhileLoop) statement;
            return jumpsTo(loop.getBody(), null, J.Break.class, false) || !isTrue(loop.getWhileCondition().getTree()) &&
                   (completesNormally(loop.getBody()) || jumpsTo(loop.getBody(), null, J.Continue.class, false));
        } else if (statement instanceof J.ForLoop) {
            J.ForLoop loop = (J.ForLoop) statement;
            Expression condition = loop.getControl().getCondition();
            return !(condition instanceof J.Empty || isTrue(condition)) ||
                   jumpsTo(loop.getBody(), null, J.Break.class, false);
        } else if (statement instanceof J.Label) {
            J.Label label = (J.Label) statement;
            Statement labeled = label.getStatement();
            String name = label.getLabel().getSimpleName();
            if (completesNormally(labeled) || jumpsTo(labeled, name, J.Break.class, false)) {
                return true;
            }
            if (labeled instanceof J.DoWhileLoop) {
                J.DoWhileLoop loop = (J.DoWhileLoop) labeled;
                return !isTrue(loop.getWhileCondition().getTree()) &&
                       jumpsTo(loop.getBody(), name, J.Continue.class, false);
            }
            return false;
        } else if (statement instanceof J.Try) {
            J.Try try_ = (J.Try) statement;
            boolean completes = completesNormally(try_.getBody());
            for (J.Try.Catch catch_ : try_.getCatches()) {
                completes |= completesNormally(catch_.getBody());
            }
            return completes && (try_.getFinally() == null || completesNormally(try_.getFinally()));
        } else if (statement instanceof J.Synchronized) {
            return completesNormally(((J.Synchronized) statement).getBody());
        } else if (statement instanceof J.Switch) {
            return switchCompletesNormally((J.Switch) statement);
        }
        return true;
    }

    private boolean switchCompletesNormally(J.Switch switch_) {
        List<Statement> cases = switch_.getCases().getStatements();
        boolean hasDefault = false;
        for (Statement statement : cases) {
            if (!(statement instanceof J.Case)) {
                return true;
            }
            J.Case case_ = (J.Case) statement;
            for (J label : case_.getCaseLabels()) {
                hasDefault |= label instanceof J.Identifier && "default".equals(((J.Identifier) label).getSimpleName());
            }
            if (case_.getType() == J.Case.Type.Rule) {
                J body = case_.getBody();
                if (!(body instanceof Statement) || completesNormally((Statement) body) ||
                    jumpsTo((Statement) body, null, J.Break.class, false)) {
                    return true;
                }
            } else {
                for (Statement s : case_.getStatements()) {
                    if (jumpsTo(s, null, J.Break.class, false)) {
                        return true;
                    }
                }
            }
        }
        if (!hasDefault || cases.isEmpty()) {
            return true;
        }
        J.Case last = (J.Case) cases.get(cases.size() - 1);
        if (last.getType() == J.Case.Type.Rule) {
            return false;
        }
        List<Statement> statements = last.getStatements();
        return statements.isEmpty() || completesNormally(statements.get(statements.size() - 1));
    }

    /**
     * Whether a {@code break} or {@code continue}, as {@code jump} says, in the body of a statement targets that
     * statement (JLS 14.15 and 14.16). With no label given, that is an unlabeled one that targets no loop nested in
     * the body, nor any switch when it is a {@code break}. With the statement's label, it is one naming that label.
     * A {@code finally} that cannot complete normally discards a jump from its {@code try} block or catches, and such
     * a jump counts only with {@code countDiscarded}.
     */
    private boolean jumpsTo(Statement body, @Nullable String label, Class<? extends Statement> jump,
                            boolean countDiscarded) {
        boolean[] found = {false};
        new JavaIsoVisitor<Integer>() {
            @Override
            public @Nullable J preVisit(J tree, Integer p) {
                boolean takesUnlabeled = tree instanceof Loop || jump == J.Break.class &&
                                         (tree instanceof J.Switch || tree instanceof J.SwitchExpression);
                if (label == null && takesUnlabeled ||
                    tree instanceof J.Lambda || tree instanceof J.ClassDeclaration ||
                    tree instanceof J.NewClass && ((J.NewClass) tree).getBody() != null) {
                    stopAfterPreVisit();
                }
                return tree;
            }

            @Override
            public J.Try visitTry(J.Try try_, Integer p) {
                if (!countDiscarded && try_.getFinally() != null && !completesNormally(try_.getFinally())) {
                    visit(try_.getFinally(), p);
                    return try_;
                }
                return super.visitTry(try_, p);
            }

            @Override
            public J.Break visitBreak(J.Break breakStatement, Integer p) {
                found[0] |= jump == J.Break.class && targets(breakStatement.getLabel());
                return breakStatement;
            }

            @Override
            public J.Continue visitContinue(J.Continue continueStatement, Integer p) {
                found[0] |= jump == J.Continue.class && targets(continueStatement.getLabel());
                return continueStatement;
            }

            private boolean targets(J.@Nullable Identifier target) {
                return target == null ? label == null : target.getSimpleName().equals(label);
            }
        }.visit(body, 0);
        return found[0];
    }

    private static boolean isTrue(@Nullable Expression condition) {
        Expression unwrapped = Expression.unwrap(condition);
        return unwrapped instanceof J.Literal && Boolean.TRUE.equals(((J.Literal) unwrapped).getValue());
    }

    private static boolean isColonCase(J tree) {
        return tree instanceof J.Case && ((J.Case) tree).getType() == J.Case.Type.Statement;
    }

    /**
     * Whether the identifier is a case label naming a constant of the enum switched on, which is looked up in that
     * enum rather than in scope (JLS 14.11.1).
     */
    private static boolean isEnumCaseLabel(J.Case case_, J.Identifier identifier) {
        return identifier.getFieldType() != null && identifier.getFieldType().hasFlags(Flag.Enum) &&
               case_.getCaseLabels().contains(identifier);
    }

    /**
     * Whether a variable of the identifier's name is visible where it stands. An identifier a recipe built may
     * carry a variable's type without its field type, and would otherwise pass for a type name.
     */
    private boolean isVariableInScope(Cursor identifier) {
        for (Cursor c = identifier.getParent(); c != null; c = c.getParent()) {
            if (c.getValue() instanceof J && isScope(c)) {
                return Scope.at(identifier).lookup(identifier.<J.Identifier>getValue().getSimpleName()).isPresent();
            }
        }
        return false;
    }

    private static boolean isTypeName(J.Identifier identifier) {
        return identifier.getFieldType() == null && identifier.getType() instanceof JavaType.FullyQualified &&
               !(identifier.getType() instanceof JavaType.Unknown);
    }

    private static boolean isBody(J scopeTree, J child) {
        if (scopeTree instanceof J.ClassDeclaration) {
            return same(((J.ClassDeclaration) scopeTree).getBody(), child);
        } else if (scopeTree instanceof J.NewClass) {
            return same(((J.NewClass) scopeTree).getBody(), child);
        } else if (scopeTree instanceof J.MethodDeclaration) {
            return same(((J.MethodDeclaration) scopeTree).getBody(), child);
        } else if (scopeTree instanceof J.Lambda) {
            return same(((J.Lambda) scopeTree).getBody(), child);
        } else if (scopeTree instanceof J.ForEachLoop) {
            return same(((J.ForEachLoop) scopeTree).getBody(), child);
        } else if (scopeTree instanceof J.Try.Catch) {
            return same(((J.Try.Catch) scopeTree).getBody(), child);
        }
        return false;
    }

    /**
     * The statements of a block in source order, a switch block's cases each followed by its own statements.
     */
    private static List<J> statementsOf(J.Block block) {
        List<J> statements = new ArrayList<>();
        for (Statement statement : block.getStatements()) {
            statements.add(statement);
            if (isColonCase(statement)) {
                statements.addAll(((J.Case) statement).getStatements());
            }
        }
        return statements;
    }

    /**
     * Whether the declaration comes before the position in a statement list, or is the declarator the position sits
     * in or one to its left, since a local is in scope in its own initializer and those after it.
     */
    private static boolean precedes(List<J> statements, List<J> declaration, List<J> at) {
        int declared = indexIn(statements, declaration);
        int asked = indexIn(statements, at);
        if (declared < 0 || asked < 0) {
            return false;
        }
        if (asked != declared) {
            return asked > declared;
        }
        J statement = statements.get(declared);
        if (statement instanceof J.ClassDeclaration || statement instanceof J.MethodDeclaration) {
            return true;
        }
        if (!(statement instanceof J.VariableDeclarations)) {
            return false;
        }
        List<J.VariableDeclarations.NamedVariable> variables = ((J.VariableDeclarations) statement).getVariables();
        int variable = -1;
        int position = -1;
        for (J j : declaration) {
            variable = Math.max(variable, variables.indexOf(j));
        }
        for (J j : at) {
            position = Math.max(position, variables.indexOf(j));
        }
        return variable >= 0 && position >= variable;
    }

    private static int indexIn(List<J> statements, List<J> path) {
        int index = -1;
        for (J j : path) {
            int i = statements.indexOf(j);
            if (i >= 0) {
                index = i;
            }
        }
        return index;
    }

    /**
     * The trees on the path from just below {@code scopeTree} down to the cursor's own tree, or null where the cursor
     * is not under it. Trees are matched by id, so a cursor holding an edited copy of a tree still finds it.
     */
    private static @Nullable List<J> pathBelow(J scopeTree, Cursor cursor) {
        List<J> path = new ArrayList<>();
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            Object value = c.getValue();
            if (value instanceof J) {
                if (((J) value).getId().equals(scopeTree.getId())) {
                    Collections.reverse(path);
                    return path;
                }
                path.add((J) value);
            }
        }
        return null;
    }

    /**
     * Whether the two are one tree, by id, since a cursor may hold an edited copy of a tree its scope has.
     */
    private static boolean same(@Nullable J a, @Nullable J b) {
        return a != null && b != null && a.getId().equals(b.getId());
    }

    private static boolean contains(List<J> path, @Nullable J tree) {
        if (tree == null) {
            return false;
        }
        for (J j : path) {
            if (j.getId().equals(tree.getId())) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Object parentTree(Cursor cursor) {
        for (Cursor c = cursor.getParent(); c != null; c = c.getParent()) {
            if (c.getValue() instanceof Tree) {
                return c.getValue();
            }
        }
        return null;
    }
}
