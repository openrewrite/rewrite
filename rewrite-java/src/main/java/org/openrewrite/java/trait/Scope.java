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
package org.openrewrite.java.trait;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.ScopeService;
import org.openrewrite.java.tree.*;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * One lexical scope of the tree, asked about the names visible from a position inside it. The binding rules
 * come from the source file's {@link ScopeService}, so the same questions work for every language built on {@link J}
 * that supplies one.
 */
@Incubating(since = "8.111.0")
@Value
public class Scope implements Trait<J> {
    private static final String SERVICE = ScopeService.class.getName();
    private static final String BINDINGS = Scope.class.getName() + ".bindings";

    /**
     * The tree that opens the scope.
     */
    Cursor cursor;

    /**
     * Where the questions are asked from, at or below the scope's tree.
     */
    Cursor position;

    /**
     * The innermost scope holding the position.
     */
    public static Scope at(Cursor position) {
        ScopeService service = service(position);
        Cursor scope = enclosingScope(service, position);
        if (scope == null) {
            throw new IllegalArgumentException("No scope encloses the cursor");
        }
        return new Scope(scope, position);
    }

    /**
     * The scope around this one, asked from the same position.
     */
    public Optional<Scope> enclosing() {
        Cursor scope = cursor.getParent() == null ? null : enclosingScope(service(cursor), cursor.getParent());
        return scope == null ? Optional.empty() : Optional.of(new Scope(scope, position));
    }

    /**
     * Every name this scope binds itself, in every namespace, wherever in it each one becomes visible.
     */
    public List<Binding> getBindings() {
        return cursor.computeMessageIfAbsent(BINDINGS, k -> service(cursor).bindings(cursor));
    }

    /**
     * Every binding visible at the position, innermost first, leaving out any a nearer binding of its name and
     * namespace shadows.
     */
    public Stream<Binding> visible() {
        ScopeService service = service(position);
        Set<String> seen = new HashSet<>();
        List<Binding> visible = new ArrayList<>();
        for (Scope scope = this; scope != null; scope = scope.enclosing().orElse(null)) {
            for (Binding binding : scope.getBindings()) {
                if (service.isVisibleAt(binding, position) && seen.add(binding.getNamespace() + " " + binding.getName())) {
                    visible.add(binding);
                }
            }
        }
        return visible.stream();
    }

    /**
     * The innermost variable of a simple name that is visible at the position.
     */
    public Optional<Binding> lookup(String name) {
        return lookup(name, Namespace.VARIABLE);
    }

    /**
     * The innermost binding of a simple name in a namespace that is visible at the position.
     */
    public Optional<Binding> lookup(String name, Namespace namespace) {
        ScopeService service = service(position);
        for (Scope scope = this; scope != null; scope = scope.enclosing().orElse(null)) {
            for (Binding binding : scope.getBindings()) {
                if (binding.getNamespace() == namespace && binding.getName().equals(name) &&
                    service.isVisibleAt(binding, position)) {
                    return Optional.of(binding);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Whether a binding is in scope at the position.
     */
    public boolean isVisible(Binding binding) {
        return service(position).isVisibleAt(binding, position);
    }

    /**
     * A name for a new variable declared at the position, which is {@code preferred} or that name with a number
     * counted up. Any name this scope spells counts as taken, except a member's name after a dot. That keeps clear of
     * later declarations in any language. A name visible at the position, a reserved word and a name
     * {@code alsoTaken} accepts are taken too.
     */
    public String freeName(String preferred, Predicate<String> alsoTaken) {
        ScopeService service = service(position);
        Set<String> spelled = namesSpelledWithin(cursor);
        Predicate<String> taken = name -> alsoTaken.test(name) || service.isReserved(name) ||
                                          spelled.contains(name) || lookup(name).isPresent();
        if (!taken.test(preferred)) {
            return preferred;
        }
        int digits = preferred.length();
        while (digits > 0 && Character.isDigit(preferred.charAt(digits - 1))) {
            digits--;
        }
        String base = preferred;
        long count = 0;
        if (digits < preferred.length() && preferred.length() - digits < 18) {
            base = preferred.substring(0, digits);
            count = Long.parseLong(preferred.substring(digits));
        }
        String candidate;
        do {
            candidate = base + ++count;
        } while (taken.test(candidate));
        return candidate;
    }

    /**
     * Whether the names this scope binds itself would be in the way were they in scope across the given regions too,
     * as when a block's statements move into the block around it. A region is in the way where it declares one of
     * the names, or reads one from outside itself, which the moved declaration would then capture.
     */
    public boolean collidesAcross(Collection<Cursor> regions) {
        Set<String> own = new HashSet<>();
        for (Binding binding : getBindings()) {
            own.add(binding.getNamespace() + " " + binding.getName());
        }
        if (own.isEmpty()) {
            return false;
        }
        for (Cursor region : regions) {
            if (Stream.concat(declaredWithin(region), readWithin(region))
                    .anyMatch(binding -> own.contains(binding.getNamespace() + " " + binding.getName()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The bindings declared anywhere under the region's tree, in source order.
     */
    public static Stream<Binding> declaredWithin(Cursor region) {
        // Read from the region's own tree, which may be an edited copy its ancestors do not hold yet.
        J regionTree = region.getValue();
        ScopeService service = service(region);
        Map<UUID, Binding> byIdentifier = new HashMap<>();
        List<Binding> onStatements = new ArrayList<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public @Nullable J preVisit(J tree, Integer p) {
                if (tree == regionTree || service.isScope(getCursor())) {
                    for (Binding binding : service.bindings(getCursor())) {
                        J anchor = binding.getTree();
                        if (anchor instanceof J.Identifier) {
                            byIdentifier.putIfAbsent(anchor.getId(), binding);
                        } else if (anchor != tree && !(anchor instanceof J.Import)) {
                            // A language may anchor a declared name on its statement. An anchor on the scope node itself marks an inherited or implied name.
                            onStatements.add(binding);
                        }
                    }
                }
                return tree;
            }
        }.visit(regionTree, 0, region.getParentOrThrow());
        List<Binding> ordered = new ArrayList<>();
        // A class or method that is the region declares its own name, which the scope around it binds
        J.Identifier ownName = regionTree instanceof J.ClassDeclaration ? ((J.ClassDeclaration) regionTree).getName() :
                regionTree instanceof J.MethodDeclaration ? ((J.MethodDeclaration) regionTree).getName() : null;
        if (ownName != null) {
            new Binding.Matcher().get(new Cursor(region, ownName)).ifPresent(ordered::add);
        }
        new JavaIsoVisitor<List<Binding>>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, List<Binding> found) {
                Binding binding = byIdentifier.remove(identifier.getId());
                if (binding != null) {
                    found.add(binding);
                }
                return identifier;
            }
        }.visit(regionTree, ordered, region.getParentOrThrow());
        ordered.addAll(onStatements);
        return ordered.stream();
    }

    /**
     * The bindings that references under the region's tree read from outside it, in the order first read. These
     * are what code moved elsewhere still needs to reach. A name read but bound nowhere has no binding to report.
     */
    public static Stream<Binding> readWithin(Cursor region) {
        J regionTree = region.getValue();
        ScopeService service = service(region);
        Set<String> seen = new HashSet<>();
        List<Binding> read = new ArrayList<>();
        new JavaIsoVisitor<List<Binding>>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, List<Binding> found) {
                Namespace namespace = service.namespaceOf(getCursor());
                if (namespace != null) {
                    at(getCursor()).lookup(identifier.getSimpleName(), namespace)
                            .filter(binding -> !isUnder(binding.getCursor(), regionTree))
                            .filter(binding -> seen.add(binding.getNamespace() + " " + binding.getName() + " " + binding.getTree().getId()))
                            .ifPresent(found::add);
                }
                return identifier;
            }
        }.visit(regionTree, read, region.getParentOrThrow());
        return read.stream();
    }

    /**
     * Every identifier under the region's tree, except the names of members selected with a dot or {@code ::}.
     */
    private static Set<String> namesSpelledWithin(Cursor region) {
        Set<String> names = new HashSet<>();
        new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> found) {
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (!(parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getName() == identifier ||
                      parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == identifier ||
                      parent instanceof J.MemberReference && ((J.MemberReference) parent).getReference() == identifier)) {
                    found.add(identifier.getSimpleName());
                }
                return identifier;
            }
        }.visit(region.getValue(), names, region.getParentOrThrow());
        return names;
    }

    private static @Nullable Cursor enclosingScope(ScopeService service, Cursor from) {
        for (Cursor c = from; c != null; c = c.getParent()) {
            if (c.getValue() instanceof J && service.isScope(c)) {
                return c;
            }
        }
        return null;
    }

    private static boolean isUnder(Cursor cursor, J tree) {
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof J && ((J) c.getValue()).getId().equals(tree.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The source file's scope service, looked up once per traversal of that file.
     */
    static ScopeService service(Cursor cursor) {
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof JavaSourceFile) {
                JavaSourceFile sourceFile = c.getValue();
                return c.computeMessageIfAbsent(SERVICE, k -> sourceFile.service(ScopeService.class));
            }
        }
        return new ScopeService();
    }

    /**
     * Every reference under the region's tree, resolved in one walk. Ask it about many bindings where asking each
     * binding would walk its scope again.
     */
    public static References referencesWithin(Cursor region) {
        ScopeService service = service(region);
        Map<String, List<References.Reference>> byName = new HashMap<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                Namespace namespace = service.namespaceOf(getCursor());
                Binding binding = null;
                if (namespace != null) {
                    binding = at(getCursor()).lookup(identifier.getSimpleName(), namespace).orElse(null);
                    if (binding == null) {
                        return identifier;
                    }
                } else {
                    Object parent = getCursor().getParentTreeCursor().getValue();
                    if (!(parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == identifier ||
                          parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getName() == identifier)) {
                        return identifier;
                    }
                }
                byName.computeIfAbsent(identifier.getSimpleName(), n -> new ArrayList<>())
                        .add(new References.Reference(getCursor(), binding));
                return identifier;
            }
        }.visit(region.getValue(), 0, region.getParentOrThrow());
        return new References(byName);
    }

    /**
     * The references found under one region, grouped so that each binding's are a lookup away.
     */
    @Value
    public static class References {
        @Getter(AccessLevel.NONE)
        Map<String, List<Reference>> byName;

        /**
         * The identifiers in the region that refer to the binding, in source order.
         */
        public Stream<Cursor> of(Binding binding) {
            return byName.getOrDefault(binding.getName(), Collections.emptyList()).stream()
                    .filter(reference -> reference.binding != null ? reference.binding.equals(binding) : binding.selects(reference.cursor))
                    .map(reference -> reference.cursor);
        }

        /**
         * As {@link Binding#isEffectivelyFinal()}, counting the assignments in the region.
         */
        public boolean isEffectivelyFinal(Binding binding) {
            return binding.isEffectivelyFinal(of(binding));
        }

        /**
         * One identifier, with the binding its name resolves to, or none where it names a member after a dot.
         */
        @Value
        private static class Reference {
            Cursor cursor;

            @Nullable
            Binding binding;
        }
    }

    /**
     * What a name denotes. Java looks variables, methods and types up separately, so one name can be bound in each.
     */
    public enum Namespace {
        VARIABLE,
        METHOD,
        TYPE
    }

    /**
     * One name a scope binds. It is anchored on its declaring identifier, or on the static import or inheriting
     * class when the field is declared elsewhere.
     */
    @Value
    public static class Binding implements Trait<J> {
        Cursor cursor;
        String name;
        Namespace namespace;

        /**
         * The cursor of the tree whose scope binds the name.
         */
        Cursor scope;

        /**
         * The attributed variable, method or type, where attribution knows it.
         */
        @Nullable
        JavaType type;

        public Scope getScope() {
            return new Scope(scope, scope);
        }

        /**
         * Whether the name belongs to a class or comes in through an import, rather than being local to code.
         */
        public boolean isMember() {
            Object scopeTree = scope.getValue();
            if (scopeTree instanceof JavaSourceFile) {
                return true;
            }
            if (!(scopeTree instanceof J.ClassDeclaration || scopeTree instanceof J.NewClass)) {
                return false;
            }
            // A pattern variable in a field initializer is bound by the class but local to that initializer.
            return !(getTree() instanceof J.Identifier) || cursor.firstEnclosing(J.InstanceOf.class) == null;
        }

        /**
         * The identifiers in the binding's scope that refer to it, by its simple name, as {@code this.name} or
         * {@code super.name} in its own class, or for a field as {@code other.name} where attribution says so.
         */
        public Stream<Cursor> references() {
            List<Cursor> references = new ArrayList<>();
            Scope scope = getScope();
            ScopeService service = service(scope.getCursor());
            new JavaIsoVisitor<List<Cursor>>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, List<Cursor> found) {
                    if (!identifier.getSimpleName().equals(name)) {
                        return identifier;
                    }
                    if (namespace == service.namespaceOf(getCursor())) {
                        if (at(getCursor()).lookup(name, namespace).filter(Binding.this::equals).isPresent()) {
                            found.add(getCursor());
                        }
                    } else if (selects(getCursor())) {
                        found.add(getCursor());
                    }
                    return identifier;
                }
            }.visit(scope.getTree(), references, scope.getCursor().getParentOrThrow());
            return references.stream();
        }

        /**
         * Whether the identifier names this member after a dot: from {@code this} or {@code super} in its own class,
         * or for a field from another instance.
         */
        private boolean selects(Cursor identifier) {
            return selectedFromOwnClass(identifier) || isSameField(identifier);
        }

        /**
         * Whether the identifier selects this field from some other instance, as attribution tells.
         */
        private boolean isSameField(Cursor identifier) {
            Object parent = identifier.getParentTreeCursor().getValue();
            J.Identifier selected = identifier.getValue();
            return namespace == Namespace.VARIABLE && type instanceof JavaType.Variable &&
                   parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == selected &&
                   selected.getFieldType() != null && TypeUtils.isOfType(selected.getFieldType(), type);
        }

        /**
         * Whether the identifier is this member's name selected from {@code this} or {@code super} of the class
         * binding it, which is the innermost class around the identifier.
         */
        private boolean selectedFromOwnClass(Cursor identifier) {
            J scopeTree = scope.getValue();
            if (!(scopeTree instanceof J.ClassDeclaration || scopeTree instanceof J.NewClass)) {
                return false;
            }
            Object parent = identifier.getParentTreeCursor().getValue();
            Expression select = parent instanceof J.FieldAccess && namespace == Namespace.VARIABLE &&
                                ((J.FieldAccess) parent).getName() == identifier.getValue() ? ((J.FieldAccess) parent).getTarget() :
                    parent instanceof J.MethodInvocation && namespace == Namespace.METHOD &&
                    ((J.MethodInvocation) parent).getName() == identifier.getValue() ? ((J.MethodInvocation) parent).getSelect() : null;
            if (!(select instanceof J.Identifier) ||
                !"this".equals(((J.Identifier) select).getSimpleName()) && !"super".equals(((J.Identifier) select).getSimpleName())) {
                return false;
            }
            for (Cursor c = identifier.getParent(); c != null; c = c.getParent()) {
                if (c.getValue() instanceof J.ClassDeclaration || c.getValue() instanceof J.NewClass && ((J.NewClass) c.getValue()).getBody() != null) {
                    return ((J) c.getValue()).getId().equals(scopeTree.getId());
                }
            }
            return false;
        }

        /**
         * Whether the variable is final or never assigned after its declaration (JLS 4.12.4). A field not declared
         * final counts as assigned, since code outside its scope may assign it. A method or type is never assigned.
         */
        public boolean isEffectivelyFinal() {
            return isEffectivelyFinal(references());
        }

        private boolean isEffectivelyFinal(Stream<Cursor> references) {
            if (namespace != Namespace.VARIABLE) {
                return true;
            }
            J anchor = getTree();
            if (!(anchor instanceof J.Identifier)) {
                return type instanceof JavaType.Variable && ((JavaType.Variable) type).hasFlags(Flag.Final);
            }
            Cursor declaring = cursor.getParentTreeCursor();
            if (declaring.getValue() instanceof J.EnumValue) {
                return true;
            }
            if (declaring.getValue() instanceof J.VariableDeclarations.NamedVariable) {
                Cursor declarations = declaring.getParentTreeCursor();
                if (((J.VariableDeclarations) declarations.getValue()).hasModifier(J.Modifier.Type.Final)) {
                    return true;
                }
                Cursor owner = declarations.getParentTreeCursor();
                if (owner.getValue() instanceof J.ClassDeclaration) {
                    // A record component, which is final.
                    return true;
                }
                if (owner.getValue() instanceof J.Block) {
                    Object type = owner.getParentTreeCursor().getValue();
                    if (type instanceof J.ClassDeclaration || type instanceof J.NewClass) {
                        return false;
                    }
                }
            }
            boolean blank = declaring.getValue() instanceof J.VariableDeclarations.NamedVariable &&
                            ((J.VariableDeclarations.NamedVariable) declaring.getValue()).getInitializer() == null &&
                            declaring.getParentTreeCursor().getParentTreeCursor().getValue() instanceof J.Block;
            int assignments = 0;
            for (Cursor reference : (Iterable<Cursor>) references::iterator) {
                if (isAssigned(reference)) {
                    // A local declared without an initializer may be assigned once (JLS 4.12.4).
                    if (!blank || !isSimpleAssignment(reference) || ++assignments > 1 || isInLoopBelow(reference, scope)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Two bindings are the same where they bind one name in one namespace from one tree, whichever cursors hold
         * it, so a binding is a stable key for facts computed about it.
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Binding)) {
                return false;
            }
            Binding other = (Binding) o;
            return namespace == other.namespace && name.equals(other.name) && getTree().getId().equals(other.getTree().getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, name, getTree().getId());
        }

        private static boolean isSimpleAssignment(Cursor identifier) {
            Object child = identifier.getValue();
            for (Cursor c = identifier.getParentTreeCursor(); ; c = c.getParentTreeCursor()) {
                if (c.getValue() instanceof J.Parentheses) {
                    child = c.getValue();
                    continue;
                }
                return c.getValue() instanceof J.Assignment && ((J.Assignment) c.getValue()).getVariable() == child;
            }
        }

        /**
         * Whether a loop between the identifier and the scope declaring it repeats the identifier.
         */
        private static boolean isInLoopBelow(Cursor identifier, Cursor scope) {
            J scopeTree = scope.getValue();
            for (Cursor c = identifier.getParent(); c != null; c = c.getParent()) {
                if (c.getValue() instanceof J && ((J) c.getValue()).getId().equals(scopeTree.getId())) {
                    return false;
                }
                if (c.getValue() instanceof Loop) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isAssigned(Cursor identifier) {
            Object child = identifier.getValue();
            for (Cursor c = identifier.getParentTreeCursor(); ; c = c.getParentTreeCursor()) {
                Object parent = c.getValue();
                if (parent instanceof J.Parentheses) {
                    child = parent;
                    continue;
                }
                return parent instanceof J.Assignment && ((J.Assignment) parent).getVariable() == child ||
                       parent instanceof J.AssignmentOperation && ((J.AssignmentOperation) parent).getVariable() == child ||
                       parent instanceof J.Unary && ((J.Unary) parent).getOperator().isModifying() &&
                       ((J.Unary) parent).getExpression() == child;
            }
        }

        /**
         * Matches an identifier that declares a name, giving the binding it declares.
         */
        public static class Matcher extends SimpleTraitMatcher<Binding> {
            @Override
            protected @Nullable Binding test(Cursor cursor) {
                if (!(cursor.getValue() instanceof J.Identifier)) {
                    return null;
                }
                J.Identifier identifier = cursor.getValue();
                ScopeService service = service(cursor);
                for (Cursor scope = cursor.getParent() == null ? null : enclosingScope(service, cursor.getParent());
                     scope != null;
                     scope = scope.getParent() == null ? null : enclosingScope(service, scope.getParent())) {
                    for (Binding binding : new Scope(scope, scope).getBindings()) {
                        if (binding.getTree() instanceof J.Identifier && binding.getTree().getId().equals(identifier.getId())) {
                            return binding;
                        }
                    }
                }
                return null;
            }
        }
    }

    /**
     * Matches the trees that open a scope, optionally only those binding a given name.
     */
    public static class Matcher extends SimpleTraitMatcher<Scope> {
        private @Nullable String declaring;

        public Matcher declaring(String name) {
            this.declaring = name;
            return this;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<Scope, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public @Nullable J preVisit(J tree, P p) {
                    Scope scope = test(getCursor());
                    return scope != null ? (J) visitor.visit(scope, p) : tree;
                }
            };
        }

        @Override
        protected @Nullable Scope test(Cursor cursor) {
            if (!(cursor.getValue() instanceof J) || !service(cursor).isScope(cursor)) {
                return null;
            }
            Scope scope = new Scope(cursor, cursor);
            if (declaring != null && scope.getBindings().stream().noneMatch(b -> b.getName().equals(declaring))) {
                return null;
            }
            return scope;
        }
    }
}
