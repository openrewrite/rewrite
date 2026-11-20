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
package org.openrewrite.kotlin.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.service.ScopeService;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.marker.KObject;
import org.openrewrite.kotlin.marker.PrimaryConstructor;
import org.openrewrite.kotlin.tree.K;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kotlin's binding rules where they part from Java's. A {@code when} subject is visible only in its branches.
 * A lambda without parameters binds {@code it}. A {@code val} or {@code var} primary constructor parameter is a
 * property of its class. An {@code init} block is an instance initializer.
 */
@Incubating(since = "8.113.0")
public class KotlinScopeService extends ScopeService {
    private static final Set<String> HARD_KEYWORDS = new HashSet<>(Arrays.asList(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is",
            "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
            "val", "var", "when", "while"));

    @Override
    public boolean isScope(Cursor cursor) {
        return cursor.getValue() instanceof K.When || super.isScope(cursor);
    }

    @Override
    public List<Scope.Binding> bindings(Cursor scope) {
        List<Scope.Binding> bindings = new ArrayList<>(super.bindings(scope));
        Object tree = scope.getValue();
        if (tree instanceof J.Lambda && declaresNoParameter((J.Lambda) tree)) {
            bindings.add(new Scope.Binding(scope, "it", Scope.Namespace.VARIABLE, scope, null, this));
        } else if (tree instanceof J.ClassDeclaration) {
            properties(scope, (J.ClassDeclaration) tree, bindings);
            companionMembers(scope, (J.ClassDeclaration) tree, bindings);
        } else if (tree instanceof J.MethodDeclaration &&
                   ((J.MethodDeclaration) tree).getMarkers().findFirst(PrimaryConstructor.class).isPresent()) {
            // The class binds a property parameter, where the whole body sees it
            bindings.removeIf(binding -> {
                Object declarations = binding.getCursor().getParentTreeCursor().getParentTreeCursor().getValue();
                return declarations instanceof J.VariableDeclarations && isProperty((J.VariableDeclarations) declarations) &&
                       ((J.MethodDeclaration) tree).getParameters().contains(declarations);
            });
        }
        return bindings;
    }

    @Override
    public boolean isVisibleAt(Scope.Binding binding, Cursor position) {
        J scopeTree = binding.getScope().getTree();
        if (scopeTree instanceof K.When) {
            J branches = ((K.When) scopeTree).getBranches();
            for (Cursor c = position; c != null; c = c.getParent()) {
                if (c.getValue() instanceof J && ((J) c.getValue()).getId().equals(branches.getId())) {
                    return true;
                }
            }
            return false;
        }
        return super.isVisibleAt(binding, position);
    }

    @Override
    public Scope.@Nullable Namespace namespaceOf(Cursor identifier) {
        Cursor parent = identifier.getParentTreeCursor();
        if (parent.getValue() instanceof J.Assignment &&
            ((J.Assignment) parent.getValue()).getVariable() == identifier.getValue()) {
            Object call = parent.getParentTreeCursor().getValue();
            if (call instanceof J.MethodInvocation || call instanceof J.NewClass) {
                return null;
            }
        }
        return super.namespaceOf(identifier);
    }

    @Override
    public boolean isStaticContext(Cursor cursor) {
        if (cursor.getValue() instanceof J.Block) {
            // The parser flags an `init` block static so that the printer writes the keyword
            return false;
        } else if (cursor.getValue() instanceof J.ClassDeclaration
                && cursor.getParentTreeCursor().getValue() instanceof J.Block) {
            Object outer = cursor.getParentTreeCursor().getParentTreeCursor().getValue();
            if (outer instanceof J.ClassDeclaration) {
                // A class or object nested in a class has an instance of the outer class only when declared `inner`.
                // Nested in an object, it always has the one instance there is.
                return !hasModifier(cursor.<J.ClassDeclaration>getValue().getModifiers(), "inner") &&
                       !((J.ClassDeclaration) outer).getMarkers().findFirst(KObject.class).isPresent();
            }
        }
        return super.isStaticContext(cursor);
    }

    /**
     * Kotlin initializes the properties of a class in the order it declares them, its {@code init} blocks among
     * them, and an enum's entries before its companion object. A {@code lateinit} or abstract property waits on no
     * initializer, and neither does one without a backing field. An assignment uses a property as a read does.
     */
    @Override
    public boolean isUsedBeforeInitialized(Scope.Binding member, Cursor position) {
        return isUsedBeforeItsDeclaration(member, position) || isUsedWhileAnEntryIsConstructed(member, position);
    }

    /**
     * Whether the position is in an {@code init} block or a property initializer of the class body that declares
     * the property, at or before the property's declaration.
     */
    private boolean isUsedBeforeItsDeclaration(Scope.Binding property, Cursor position) {
        if (!(property.getTree() instanceof J.Identifier)) {
            return false;
        }
        Cursor declaration = property.getCursor().getParentTreeCursor().getParentTreeCursor();
        if (!(declaration.getValue() instanceof J.VariableDeclarations)) {
            return false;
        }
        J.VariableDeclarations declared = declaration.getValue();
        if (declared.hasModifier(J.Modifier.Type.Abstract) || hasModifier(declared.getModifiers(), "lateinit")) {
            return false;
        }
        if (hasNoBackingField(statementOf(declaration).getValue(),
                property.getCursor().getParentTreeCursor().getValue())) {
            return false;
        }
        Cursor initializer = classBodyMemberAt(position);
        if (initializer == null ||
            !(initializer.getValue() instanceof J.Block || initializer.getValue() instanceof J.VariableDeclarations)) {
            return false;
        }
        Cursor body = statementOf(declaration).getParentTreeCursor();
        if (!isTree(statementOf(initializer).getParentTreeCursor(), body)) {
            return false;
        }
        List<Statement> members = body.<J.Block>getValue().getStatements();
        return indexOf(members, statementOf(initializer).getValue()) <=
               indexOf(members, statementOf(declaration).getValue());
    }

    /**
     * Whether the property has accessors, no initializer and no accessor naming {@code field}, so that it has no
     * backing field to initialize.
     */
    private static boolean hasNoBackingField(Object statement, Object variable) {
        if (!(statement instanceof K.Property) || ((K.Property) statement).getBackingField() != null ||
            !(variable instanceof J.VariableDeclarations.NamedVariable) ||
            ((J.VariableDeclarations.NamedVariable) variable).getInitializer() != null) {
            return false;
        }
        for (J.MethodDeclaration accessor : ((K.Property) statement).getAccessors().getElements()) {
            boolean namesField = new KotlinIsoVisitor<AtomicBoolean>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                    found.set(found.get() || "field".equals(identifier.getSimpleName()));
                    return identifier;
                }
            }.reduce(accessor, new AtomicBoolean()).get();
            if (namesField) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the position uses an entry of the enum or a member of its companion object while an entry is
     * constructed. That is in an {@code init} block or a property initializer of the enum or of an entry's body,
     * for an entry also in a secondary constructor, and for a companion member also in an entry's arguments.
     */
    private boolean isUsedWhileAnEntryIsConstructed(Scope.Binding member, Cursor position) {
        Cursor scope = member.getScope().getCursor();
        if (!(scope.getValue() instanceof J.ClassDeclaration) ||
            scope.<J.ClassDeclaration>getValue().getKind() != J.ClassDeclaration.Kind.Type.Enum) {
            return false;
        }
        boolean entry = member.getTree() instanceof J.Identifier &&
                        member.getCursor().getParentTreeCursor().getValue() instanceof J.EnumValue;
        if (!entry && !isCompanionMember(member, scope)) {
            return false;
        }
        Cursor at = classBodyMemberAt(position);
        if (at == null) {
            return false;
        }
        Cursor constructed = statementOf(at).getParentTreeCursor().getParentTreeCursor();
        if (constructed.getValue() instanceof J.NewClass &&
            constructed.getParentTreeCursor().getValue() instanceof J.EnumValue) {
            constructed = constructed.dropParentUntil(J.ClassDeclaration.class::isInstance);
        }
        if (!isTree(constructed, scope)) {
            return false;
        }
        Object tree = at.getValue();
        return tree instanceof J.Block || tree instanceof J.VariableDeclarations ||
               entry && (tree instanceof K.Constructor || tree instanceof J.MethodDeclaration &&
                         ((J.MethodDeclaration) tree).getMethodType() != null &&
                         ((J.MethodDeclaration) tree).getMethodType().isConstructor()) ||
               !entry && tree instanceof J.EnumValueSet;
    }

    private static boolean isCompanionMember(Scope.Binding member, Cursor scope) {
        if (member.getNamespace() == Scope.Namespace.TYPE) {
            return false;
        }
        for (Cursor c = member.getCursor().getParent(); c != null && !isTree(c, scope); c = c.getParent()) {
            if (c.getValue() instanceof J.ClassDeclaration &&
                hasModifier(c.<J.ClassDeclaration>getValue().getModifiers(), "companion")) {
                return true;
            }
        }
        return false;
    }

    /**
     * The member of the nearest class body around the position, or null outside one. For a property with
     * accessors it is the declaration or the accessor that holds the position.
     */
    private static @Nullable Cursor classBodyMemberAt(Cursor position) {
        Cursor member = null;
        Cursor below = null;
        for (Cursor c = position; c != null; c = c.getParent()) {
            Object tree = c.getValue();
            if (tree instanceof J.Block && (c.getParentTreeCursor().getValue() instanceof J.ClassDeclaration ||
                                            c.getParentTreeCursor().getValue() instanceof J.NewClass)) {
                return member != null && member.getValue() instanceof K.Property ? below : member;
            } else if (tree instanceof J) {
                below = member;
                member = c;
            }
        }
        return null;
    }

    /**
     * The statement of the class body that holds the member, which is the property for a declaration or an
     * accessor of a property with accessors.
     */
    private static Cursor statementOf(Cursor member) {
        Cursor parent = member.getParentTreeCursor();
        return parent.getValue() instanceof K.Property ? parent : member;
    }

    private static boolean isTree(Cursor cursor, Cursor other) {
        return cursor.getValue() instanceof J && cursor.<J>getValue().getId().equals(other.<J>getValue().getId());
    }

    private static int indexOf(List<? extends J> trees, J tree) {
        for (int i = 0; i < trees.size(); i++) {
            if (trees.get(i).getId().equals(tree.getId())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean localMayShadowLocal() {
        return true;
    }

    @Override
    public boolean isReserved(String name) {
        return HARD_KEYWORDS.contains(name);
    }

    private static boolean declaresNoParameter(J.Lambda lambda) {
        for (J parameter : lambda.getParameters().getParameters()) {
            if (!(parameter instanceof J.Empty)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Binds the properties and functions of the class's companion object, which the whole class body sees. The
     * class's own members come first, as one of them hides a companion member of its name.
     */
    private void companionMembers(Cursor scope, J.ClassDeclaration classDeclaration, List<Scope.Binding> bindings) {
        Cursor body = new Cursor(scope, classDeclaration.getBody());
        for (Statement statement : classDeclaration.getBody().getStatements()) {
            if (statement instanceof J.ClassDeclaration &&
                hasModifier(((J.ClassDeclaration) statement).getModifiers(), "companion")) {
                for (Scope.Binding member : bindings(new Cursor(body, statement))) {
                    if (member.getTree() instanceof J.Identifier && member.getNamespace() != Scope.Namespace.TYPE) {
                        bindings.add(new Scope.Binding(member.getCursor(), member.getName(), member.getNamespace(),
                                scope, member.getType(), this));
                    }
                }
            }
        }
    }

    private static boolean hasModifier(List<J.Modifier> modifiers, String keyword) {
        for (J.Modifier modifier : modifiers) {
            if (keyword.equals(modifier.getKeyword())) {
                return true;
            }
        }
        return false;
    }

    private void properties(Cursor scope, J.ClassDeclaration classDeclaration, List<Scope.Binding> bindings) {
        Cursor body = new Cursor(scope, classDeclaration.getBody());
        for (Statement statement : classDeclaration.getBody().getStatements()) {
            if (!(statement instanceof J.MethodDeclaration) ||
                !statement.getMarkers().findFirst(PrimaryConstructor.class).isPresent()) {
                continue;
            }
            Cursor constructor = new Cursor(body, statement);
            for (Statement parameter : ((J.MethodDeclaration) statement).getParameters()) {
                if (!(parameter instanceof J.VariableDeclarations) || !isProperty((J.VariableDeclarations) parameter)) {
                    continue;
                }
                Cursor declarations = new Cursor(constructor, parameter);
                for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) parameter).getVariables()) {
                    Cursor name = new Cursor(new Cursor(declarations, variable), variable.getName());
                    bindings.add(new Scope.Binding(name, variable.getSimpleName(), Scope.Namespace.VARIABLE, scope,
                            variable.getName().getFieldType(), this));
                }
            }
        }
    }

    private static boolean isProperty(J.VariableDeclarations parameter) {
        for (J.Modifier modifier : parameter.getModifiers()) {
            if (modifier.getType() == J.Modifier.Type.Final || "var".equals(modifier.getKeyword())) {
                return true;
            }
        }
        return false;
    }
}
