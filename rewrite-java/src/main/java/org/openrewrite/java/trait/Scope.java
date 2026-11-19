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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.ScopeService;
import org.openrewrite.java.tree.*;
import org.openrewrite.trait.Trait;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * One lexical scope of the tree, asked about the names visible from a position inside it. The binding rules
 * come from the source file's {@link ScopeService}, so the same questions work for every language built on {@link J}
 * that supplies one.
 * <p>
 * Answers are kept in the messages of the source file's cursor. A caller that keeps its own answer there computes it
 * before storing it, since asking from inside {@link Cursor#computeMessageIfAbsent} on that cursor modifies the map
 * being computed into.
 */
@Incubating(since = "8.113.0")
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Scope implements Trait<J> {
    private static final String SERVICE = ScopeService.class.getName();
    private static final String BINDINGS = Scope.class.getName() + ".bindings";
    private static final String REFERENCES = Scope.class.getName() + ".references";
    private static final String SPELLED = Scope.class.getName() + ".spelled";

    /**
     * The tree that opens the scope.
     */
    Cursor cursor;

    /**
     * Where the questions are asked from, at or below the scope's tree. {@link #at(Cursor)} takes it from its
     * cursor. Another one asks this scope from a place that opens a scope of its own.
     */
    @With
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
        // Kept per source file and by tree identity, so a cursor built by hand finds them and an edited copy does not
        Map<Object, List<Binding>> bindings = perSourceFile(cursor).computeMessageIfAbsent(BINDINGS, k -> new IdentityHashMap<>());
        List<Binding> own = bindings.get(cursor.getValue());
        if (own == null) {
            own = service(cursor).bindings(cursor);
            bindings.put(cursor.getValue(), own);
        }
        return own;
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
     * The innermost binding of a simple name in a namespace that is visible at the position. A type of the same
     * package, a type an on-demand import brings in, and an inherited member type the file never names are not
     * bound, so no type binding does not mean the name is free.
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
     * Whether a new variable declared at the position can take the name. It cannot take a reserved word, a name
     * visible at the position, or any name this scope spells other than a member's name after a dot. The last keeps
     * clear of later declarations in any language.
     */
    public boolean isFree(String name) {
        Map<Object, Set<String>> spelled = perSourceFile(cursor).computeMessageIfAbsent(SPELLED, k -> new IdentityHashMap<>());
        Set<String> inScope = spelled.get(cursor.getValue());
        if (inScope == null) {
            inScope = namesSpelledWithin(cursor);
            spelled.put(cursor.getValue(), inScope);
        }
        return !service(position).isReserved(name) && !inScope.contains(name) && !lookup(name).isPresent();
    }

    /**
     * A free name for a new variable declared at the position, which is {@code preferred} or that name with a
     * number counted up. A name {@code alsoTaken} accepts is passed over, as one handed out earlier in the same
     * visit is. Another way of making names can try its own candidates on {@link #isFree(String)}.
     */
    public String freeName(String preferred, Predicate<String> alsoTaken) {
        Predicate<String> taken = name -> alsoTaken.test(name) || !isFree(name);
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
            if (Stream.concat(declaredWithin(region), referencedWithin(region))
                    .anyMatch(binding -> own.contains(binding.getNamespace() + " " + binding.getName()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The binding a declaration introduces, found from a cursor on the declaration or above it. The declaration is
     * a named variable, a method or a class, or the identifier that names one.
     */
    public static Optional<Binding> bindingOf(Cursor cursor, J declaration) {
        J.Identifier name = declaration instanceof J.Identifier ? (J.Identifier) declaration :
                declaration instanceof J.VariableDeclarations.NamedVariable ? ((J.VariableDeclarations.NamedVariable) declaration).getName() :
                declaration instanceof J.MethodDeclaration ? ((J.MethodDeclaration) declaration).getName() :
                declaration instanceof J.ClassDeclaration ? ((J.ClassDeclaration) declaration).getName() : null;
        if (name == null) {
            return Optional.empty();
        }
        Cursor[] found = {null};
        new JavaIsoVisitor<Integer>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, Integer p) {
                return found[0] == null ? super.visit(tree, p) : (J) tree;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                if (identifier.isScope(name)) {
                    found[0] = getCursor();
                }
                return identifier;
            }
        }.visit(cursor.getValue(), 0, cursor.getParentOrThrow());
        return found[0] == null ? Optional.empty() : Optional.ofNullable(Binding.declaredBy(found[0]));
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
                boolean scope = service.isScope(getCursor());
                if (tree == regionTree || scope) {
                    List<Binding> bindings = scope ? new Scope(getCursor(), getCursor()).getBindings() : service.bindings(getCursor());
                    for (Binding binding : bindings) {
                        J anchor = binding.getTree();
                        if (anchor instanceof J.Identifier) {
                            // A region that opens no scope declares its names in the scope around it, which binds them
                            // unless the region is an edited copy that scope does not hold.
                            Binding bound = scope ? null : Binding.declaredBy(binding.getCursor());
                            byIdentifier.putIfAbsent(anchor.getId(), bound != null ? bound : binding);
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
            Optional.ofNullable(Binding.declaredBy(new Cursor(region, ownName))).ifPresent(ordered::add);
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
     * The bindings that the region's tree reads or assigns but does not declare, in the order first referenced.
     * These are what code moved elsewhere still needs to reach. A name bound nowhere has no binding to report.
     */
    public static Stream<Binding> referencedWithin(Cursor region) {
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

    /**
     * Where facts about the trees of one source file are kept for a traversal: its cursor, or the given one where
     * no source file is above it.
     */
    private static Cursor perSourceFile(Cursor cursor) {
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof JavaSourceFile) {
                return c;
            }
        }
        return cursor;
    }

    private static @Nullable Cursor enclosingScope(ScopeService service, Cursor from) {
        for (Cursor c = from; c != null; c = c.getParent()) {
            if (c.getValue() instanceof J && service.isScope(c)) {
                return c;
            }
        }
        return null;
    }

    private static boolean crossesClassBody(Cursor inner, Cursor outer) {
        for (Cursor c = inner; c != null && c.getValue() != outer.getValue(); c = c.getParent()) {
            if (c.getValue() instanceof J.ClassDeclaration ||
                c.getValue() instanceof J.Block && c.getParentTreeCursor().getValue() instanceof J.NewClass) {
                return true;
            }
        }
        return false;
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
     * Every reference under the region's tree, resolved in one walk.
     */
    private static References referencesWithin(Cursor region) {
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
                          parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getName() == identifier ||
                          parent instanceof J.Case && ((J.Case) parent).getCaseLabels().contains(identifier))) {
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
    private static class References {
        @Getter(AccessLevel.NONE)
        Map<String, List<Reference>> byName;

        /**
         * The identifiers in the region that refer to the binding, in source order.
         */
        Stream<Cursor> of(Binding binding) {
            return byName.getOrDefault(binding.getName(), Collections.emptyList()).stream()
                    .filter(reference -> reference.binding != null ? reference.binding.equals(binding) : binding.selects(reference.cursor))
                    .map(reference -> reference.cursor);
        }

        /**
         * One identifier, with the binding its name resolves to, or none where it names a member after a dot or
         * an enum constant in a case label.
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
    @Incubating(since = "8.113.0")
    public enum Namespace {
        VARIABLE,
        METHOD,
        TYPE
    }

    /**
     * Where a bound name comes from.
     */
    @Incubating(since = "8.113.0")
    public enum Kind {
        /**
         * Declared in code: a local variable, a catch or loop variable, a pattern variable, a local class.
         */
        LOCAL,
        /**
         * A parameter of a method, constructor or lambda.
         */
        PARAMETER,
        /**
         * Declared in the body of the class that binds it, or at the top of the source file.
         */
        MEMBER,
        /**
         * A member the class that binds it has without declaring it in its body. It is taken from a supertype, or
         * only attribution lists it, as an enum's {@code values()}.
         */
        INHERITED,
        /**
         * Brought in by an import.
         */
        IMPORTED
    }

    /**
     * One name a scope binds. It is anchored on its declaring identifier, or on the static import or inheriting
     * class when the field is declared elsewhere.
     */
    @Incubating(since = "8.113.0")
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

        public Kind getKind() {
            J tree = getTree();
            Object scopeTree = scope.getValue();
            if (tree instanceof J.Import) {
                return Kind.IMPORTED;
            }
            if (scopeTree instanceof JavaSourceFile) {
                return Kind.MEMBER;
            }
            if (scopeTree instanceof J.ClassDeclaration || scopeTree instanceof J.NewClass) {
                if (tree == scopeTree) {
                    return Kind.INHERITED;
                }
                // A pattern variable in a field initializer is bound by the class but local to that initializer.
                if (!(tree instanceof J.Identifier) || cursor.firstEnclosing(J.InstanceOf.class) == null) {
                    return Kind.MEMBER;
                }
            }
            if (tree == scopeTree && tree instanceof J.Lambda) {
                // A parameter the lambda takes without naming it, such as Kotlin's or Groovy's `it`
                return Kind.PARAMETER;
            }
            if (tree instanceof J.Identifier) {
                Cursor declarations = cursor.getParentTreeCursor().getParentTreeCursor();
                Object declaredIn = declarations.getParentTreeCursor().getValue();
                if (declarations.getValue() instanceof J.VariableDeclarations &&
                    (declaredIn instanceof J.MethodDeclaration || declaredIn instanceof J.Lambda.Parameters ||
                     declaredIn instanceof J.Lambda)) {
                    return Kind.PARAMETER;
                }
            }
            return Kind.LOCAL;
        }

        /**
         * Whether the name is local to code, as a local or a parameter is, rather than belonging to a class or
         * coming in through an import.
         */
        public boolean isLocal() {
            Kind kind = getKind();
            return kind == Kind.LOCAL || kind == Kind.PARAMETER;
        }

        /**
         * The identifiers in the binding's scope that refer to it, by its simple name, as {@code this.name} or
         * {@code super.name} in its own class, or for a field as {@code other.name} where attribution says so.
         * Where a class redeclares a member of its superclass, {@code super.name} refers to the inherited one, which
         * the class binds too.
         */
        public Stream<Cursor> references() {
            // One walk of the scope answers for every binding in it
            Map<Object, References> references = perSourceFile(scope).computeMessageIfAbsent(REFERENCES, k -> new IdentityHashMap<>());
            References inScope = references.get(scope.getValue());
            if (inScope == null) {
                inScope = referencesWithin(scope);
                references.put(scope.getValue(), inScope);
            }
            return inScope.of(this);
        }

        /**
         * Those of the {@link #references()} that are under the region's tree.
         */
        public Stream<Cursor> references(Cursor region) {
            J regionTree = region.getValue();
            return references().filter(reference -> isUnder(reference, regionTree));
        }

        /**
         * Whether renaming this binding to {@code newName} keeps every name in the file reading what it reads now.
         * It does not where its own scope already binds the name, where a nearer binding of the name would take one
         * of its references, where it would take a reference that now reads a binding further out or none, or where
         * a local would come to share its name with another local in reach, with no class body between them, and the
         * language forbids that.
         * The answer is for the tree the cursor holds, so a visitor renaming several bindings keeps track of the
         * names it has handed out.
         */
        public boolean canRenameTo(String newName) {
            Scope declaring = getScope();
            ScopeService service = service(scope);
            for (Binding sibling : declaring.getBindings()) {
                if (sibling.namespace == namespace && sibling.name.equals(newName)) {
                    return false;
                }
            }
            for (Cursor reference : (Iterable<Cursor>) references()::iterator) {
                if (service.namespaceOf(reference) != null &&
                    at(reference).lookup(newName, namespace).filter(nearer -> isUnder(nearer.scope, scope.getValue())).isPresent()) {
                    return false;
                }
            }
            boolean[] safe = {true};
            new JavaIsoVisitor<Integer>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                    if (safe[0] && identifier.getSimpleName().equals(newName) && service.namespaceOf(getCursor()) == namespace &&
                        service.isVisibleAt(Binding.this, getCursor()) &&
                        !at(getCursor()).lookup(newName, namespace).filter(read -> isUnder(read.scope, scope.getValue())).isPresent()) {
                        safe[0] = false;
                    }
                    return identifier;
                }
            }.visit(declaring.getTree(), 0, scope.getParentOrThrow());
            if (!safe[0]) {
                return false;
            }
            if (namespace == Namespace.VARIABLE && isLocal() && !service.localMayShadowLocal()) {
                // A local may not share its name with a local declared inside its scope or visible where it is
                // declared, unless a class body lies between the two
                return !at(cursor).lookup(newName).filter(outer -> outer.isLocal() && !crossesClassBody(cursor, outer.scope)).isPresent() &&
                       declaredWithin(scope).noneMatch(inner -> inner.namespace == namespace && inner.name.equals(newName) &&
                                                                !crossesClassBody(inner.cursor, scope));
            }
            return true;
        }

        /**
         * Whether the identifier names this member after a dot or as a case label. After a dot it is selected from
         * {@code this} or {@code super} in its own class, or for a field from another instance.
         */
        private boolean selects(Cursor identifier) {
            return selectedFromOwnClass(identifier) || isSameField(identifier) || isCaseLabelOfThisConstant(identifier);
        }

        /**
         * Whether the identifier is a case label naming this enum constant, as the label's attribution tells.
         */
        private boolean isCaseLabelOfThisConstant(Cursor identifier) {
            J.Identifier label = identifier.getValue();
            Object enumClass = scope.getValue();
            return identifier.getParentTreeCursor().getValue() instanceof J.Case &&
                   cursor.getParentTreeCursor().getValue() instanceof J.EnumValue &&
                   enumClass instanceof J.ClassDeclaration && label.getFieldType() != null &&
                   TypeUtils.isOfType(label.getFieldType().getOwner(), ((J.ClassDeclaration) enumClass).getType());
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
         * Whether a binding of the same name and namespace comes before this one in its scope, as the member a class
         * declares does before the inherited one it hides.
         */
        private boolean isHidden() {
            for (Binding sibling : getScope().getBindings()) {
                if (sibling.namespace == namespace && sibling.name.equals(name)) {
                    return !sibling.equals(this);
                }
            }
            return false;
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
            if (!(select instanceof J.Identifier)) {
                return false;
            }
            String qualifier = ((J.Identifier) select).getSimpleName();
            // `super` reaches past a member the class declares itself to the one it inherits, and `this` does not
            if (!("this".equals(qualifier) && !isHidden()) && !("super".equals(qualifier) && getKind() == Kind.INHERITED)) {
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
                if (declarations.getValue() instanceof J.VariableDeclarations &&
                    ((J.VariableDeclarations) declarations.getValue()).hasModifier(J.Modifier.Type.Final)) {
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
            Set<UUID> assignments = new HashSet<>();
            for (Cursor reference : (Iterable<Cursor>) references::iterator) {
                if (isAssigned(reference)) {
                    if (!blank || !isSimpleAssignment(reference) || isInLoopBelow(reference, scope)) {
                        return false;
                    }
                    assignments.add(((J) reference.getValue()).getId());
                }
            }
            // A local declared without an initializer may be assigned once on each path (JLS 4.12.4).
            return assignments.size() <= 1 || mostOnOnePath(scope.getValue(), assignments) <= 1;
        }

        /**
         * The most of the given identifiers that one run through the tree can reach, where only one branch of an
         * {@code if}, a conditional expression or an arrow {@code switch} runs.
         */
        private static int mostOnOnePath(J tree, Set<UUID> identifiers) {
            int[] count = {0};
            new JavaIsoVisitor<Integer>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                    if (identifiers.contains(identifier.getId())) {
                        count[0]++;
                    }
                    return identifier;
                }

                @Override
                public J.If visitIf(J.If iff, Integer p) {
                    visit(iff.getIfCondition(), p);
                    oneOf(Arrays.asList(iff.getThenPart(), iff.getElsePart()), p);
                    return iff;
                }

                @Override
                public J.Ternary visitTernary(J.Ternary ternary, Integer p) {
                    visit(ternary.getCondition(), p);
                    oneOf(Arrays.asList(ternary.getTruePart(), ternary.getFalsePart()), p);
                    return ternary;
                }

                @Override
                public J.Switch visitSwitch(J.Switch switch_, Integer p) {
                    for (Statement case_ : switch_.getCases().getStatements()) {
                        if (!(case_ instanceof J.Case) || ((J.Case) case_).getType() != J.Case.Type.Rule) {
                            return super.visitSwitch(switch_, p);
                        }
                    }
                    visit(switch_.getSelector(), p);
                    oneOf(switch_.getCases().getStatements(), p);
                    return switch_;
                }

                private void oneOf(List<? extends @Nullable J> branches, Integer p) {
                    int before = count[0];
                    int most = before;
                    for (J branch : branches) {
                        count[0] = before;
                        visit(branch, p);
                        most = Math.max(most, count[0]);
                    }
                    count[0] = most;
                }
            }.visit(tree, 0);
            return count[0];
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
            return new VariableAccess(identifier).isWriteAccess();
        }

        /**
         * The binding an identifier declares, looked for in each scope around it.
         */
        private static @Nullable Binding declaredBy(Cursor cursor) {
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
