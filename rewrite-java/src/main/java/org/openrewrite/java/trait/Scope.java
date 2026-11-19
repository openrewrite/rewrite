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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.ScopeService;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.trait.Trait;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * One lexical scope of the tree, asked about the names visible from a position inside it. The binding rules
 * come from the source file's {@link ScopeService}, so the same questions work for every language built on {@link J}
 * that supplies one.
 * <p>
 * Which question to ask:
 * <ul>
 * <li>A name for a variable the recipe declares: {@link #freeName(String, Predicate)}.</li>
 * <li>The declaration an identifier reads: {@link VariableAccess#getBinding()}, or {@link #lookup(String)} from
 * its position.</li>
 * <li>The binding of a declaration at hand, and from it the scope declaring it: {@link #bindingOf(Cursor, J)}.</li>
 * <li>Whether a rename keeps every name reading what it reads now: {@link Binding#canRenameTo(String)}.</li>
 * <li>Whether a block's statements can move out of it: {@link #canMoveBefore(Cursor)} and
 * {@link #canMoveAfter(Cursor)}.</li>
 * <li>Whether code can move into a lambda: {@link #lambdaCanCapture(Collection)}. That a local
 * {@link Binding#isEffectivelyFinal() is effectively final} does not tell, since a lambda cannot assign a local it
 * captures.</li>
 * <li>Whether a variable is ever written: a {@link VariableAccess#isWriteAccess() write} among its
 * {@link Binding#references()}.</li>
 * </ul>
 * A visible name is not always one the code may use. An instance field is visible in a static method, which cannot
 * read it.
 * <p>
 * Every question takes a cursor that holds each tree up to the source file. For a tree the visitor has not reached,
 * {@link #cursorTo(Cursor, J)} gives one.
 * <p>
 * {@link Binding#references()} walks the binding's whole scope, which for a field is its class, and
 * {@link #referencedWithin(Cursor)} resolves every name in its region. A recipe that can often decide from the tree
 * at hand asks them only where it cannot.
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
     * Whether this scope's statements could stand just before the statement, with every name meaning what it means
     * now. The rules are those of {@link #canMoveAfter(Cursor)}, and the statement itself counts among those the
     * moved names come into scope for, apart from the part of it that is this scope.
     */
    public boolean canMoveBefore(Cursor statement) {
        return canMove(statement, false);
    }

    /**
     * Whether this scope's statements could stand just after the statement, which is a statement of a block, with
     * every name meaning what it means now. Any of these prevents it:
     * <ul>
     * <li>A name this scope binds meets a local there that the language forbids declaring again. So does a local
     * declared deeper in the moved code, where that code leaves a class body.</li>
     * <li>A statement from there to the end of the block declares one of those names, or reads one from outside
     * itself.</li>
     * <li>The moved code reads a name that is bound differently there, as a member of an anonymous class it leaves
     * is, or names {@code this} or {@code super} of a class it leaves.</li>
     * </ul>
     * Only names are looked at, so a {@code return} or {@code break} among the statements is the caller's to check.
     * <p>
     * To ask about some of a block's statements, ask the scope of a copy of the block that holds only those.
     */
    public boolean canMoveAfter(Cursor statement) {
        return canMove(statement, true);
    }

    private boolean canMove(Cursor statement, boolean after) {
        Cursor holder = statement.getParentTreeCursor();
        if (!(holder.getValue() instanceof J.Block)) {
            return false;
        }
        J.Block block = holder.getValue();
        List<Statement> statements = block.getStatements();
        int index = 0;
        while (index < statements.size() && !statements.get(index).getId().equals(statement.<J>getValue().getId())) {
            index++;
        }
        if (index == statements.size()) {
            return false;
        }
        List<Cursor> inScope = new ArrayList<>();
        if (!after) {
            inScope.add(statement);
        }
        for (Statement later : statements.subList(index + 1, statements.size())) {
            inScope.add(new Cursor(holder, later));
        }
        Cursor position;
        if (inScope.isEmpty()) {
            // Nothing follows the statement, so a statement added to a copy of the block stands for the place after it
            J.Empty end = new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY);
            position = new Cursor(new Cursor(holder.getParentOrThrow(), block.withStatements(ListUtils.concat(statements, end))), end);
        } else {
            position = inScope.get(0);
        }
        ScopeService service = service(position);
        Cursor around = position.getParentTreeCursor();
        Scope destination = at(around).withPosition(position);
        Cursor into = enclosingScope(service, holder);
        if (into == null) {
            return false;
        }
        J moving = getTree();
        Set<String> own = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Binding binding : getBindings()) {
            own.add(binding.namespace + " " + binding.name);
            names.add(binding.name);
            Binding met = destination.lookup(binding.name, binding.namespace).orElse(null);
            // The class body is looked for from the block, since the statement at the position may itself be a class
            if (met != null && (met.scope.<J>getValue().getId().equals(into.<J>getValue().getId()) ||
                                met.isLocal() && !service.localMayShadowLocal() && !crossesClassBody(around, met.scope))) {
                return false;
            }
        }
        if (!service.localMayShadowLocal() && crossesClassBody(cursor, into)) {
            // Inside the class body a local at any depth could shadow one outside it, which it cannot once moved.
            // One declared in a class body deeper still is refused too, though it could stay.
            for (Binding nested : (Iterable<Binding>) declaredWithin(cursor)::iterator) {
                if (nested.isLocal() && destination.lookup(nested.name, nested.namespace)
                        .filter(met -> met.isLocal() && !crossesClassBody(around, met.scope)).isPresent()) {
                    return false;
                }
            }
        }
        if (!own.isEmpty()) {
            for (Cursor region : inScope) {
                if (declaredWithin(region).anyMatch(binding -> own.contains(binding.namespace + " " + binding.name) &&
                                                               !isUnder(binding.cursor, moving)) ||
                    referenced(region, names).anyMatch(binding -> own.contains(binding.namespace + " " + binding.name))) {
                    return false;
                }
            }
        }
        for (Binding read : (Iterable<Binding>) referencedWithin(cursor)::iterator) {
            // What the statement declares is not in scope before it, though a local class is visible at its own
            // declaration
            if (!after && isUnder(read.cursor, statement.getValue()) ||
                !destination.lookup(read.name, read.namespace).filter(read::equals).isPresent()) {
                return false;
            }
        }
        return !(crossesClassBody(cursor, into) && namesThisOrSuper(cursor));
    }

    /**
     * Whether the regions could be the body of a lambda written where they stand, going by the locals they read and
     * assign from outside themselves. Each such local must be effectively final, and the regions must not assign it.
     * A local declared without an initializer and assigned once is effectively final, yet a lambda cannot assign it.
     * A local declared in one of the regions is inside for all of them. The body of a local or anonymous class is
     * held to the same rule. Java's rule is applied in every language, so the answer may be no where a language's
     * lambdas capture more.
     */
    public static boolean lambdaCanCapture(Collection<Cursor> regions) {
        List<References> within = new ArrayList<>();
        for (Cursor region : regions) {
            within.add(referencesWithin(region));
        }
        for (Cursor region : regions) {
            for (Binding binding : (Iterable<Binding>) referencedWithin(region)::iterator) {
                if (binding.namespace != Namespace.VARIABLE || !binding.isLocal() ||
                    regions.stream().anyMatch(other -> isUnder(binding.cursor, other.getValue()))) {
                    continue;
                }
                if (within.stream().anyMatch(references -> references.writesOf(binding).findAny().isPresent()) ||
                    !binding.isEffectivelyFinal()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The cursor of a tree under the one the given cursor holds, with every tree between the two. A cursor built by
     * hand that leaves one out, as {@code new Cursor(methodBody, lambdaBody)} leaves out the lambda, hides what that
     * tree binds. The tree is found by its id, so it may be an edited copy, which the cursor returned then holds.
     *
     * @throws IllegalArgumentException where the tree is not under the cursor's tree
     */
    public static Cursor cursorTo(Cursor from, J tree) {
        Cursor found = find(from, tree);
        if (found == null) {
            throw new IllegalArgumentException("The tree is not under the cursor's tree");
        }
        return found.getValue() == tree ? found : new Cursor(found.getParent(), tree);
    }

    /**
     * The cursor of the tree with the given tree's id, at or under the cursor's tree.
     */
    private static @Nullable Cursor find(Cursor from, J tree) {
        if (!(from.getValue() instanceof Tree)) {
            return null;
        }
        if (from.<Tree>getValue().getId().equals(tree.getId())) {
            return from;
        }
        Cursor[] found = {null};
        new JavaIsoVisitor<Integer>() {
            @Override
            public @Nullable J visit(@Nullable Tree visited, Integer p) {
                return found[0] == null ? super.visit(visited, p) : (J) visited;
            }

            @Override
            public @Nullable J preVisit(J visited, Integer p) {
                if (visited.getId().equals(tree.getId())) {
                    found[0] = getCursor();
                    stopAfterPreVisit();
                }
                return visited;
            }
        }.visit(from.getValue(), 0, from.getParentOrThrow());
        if (found[0] == null) {
            return null;
        }
        // The visitor makes a cursor of its own for the given cursor's tree. The path is rebuilt on the given one,
        // so that its messages are reached from the cursor returned.
        Deque<Object> below = new ArrayDeque<>();
        for (Cursor c = found[0]; c.getValue() != from.getValue(); c = c.getParentOrThrow()) {
            below.push(c.getValue());
        }
        Cursor cursor = from;
        for (Object value : below) {
            cursor = new Cursor(cursor, value);
        }
        return cursor;
    }

    /**
     * The binding a declaration introduces, found from a cursor on the declaration, above it, or anywhere in the
     * scope that binds it. The declaration is a named variable, a method or a class, or the identifier that names one.
     */
    public static Optional<Binding> bindingOf(Cursor cursor, J declaration) {
        J.Identifier name = declaration instanceof J.Identifier ? (J.Identifier) declaration :
                declaration instanceof J.VariableDeclarations.NamedVariable ? ((J.VariableDeclarations.NamedVariable) declaration).getName() :
                declaration instanceof J.MethodDeclaration ? ((J.MethodDeclaration) declaration).getName() :
                declaration instanceof J.ClassDeclaration ? ((J.ClassDeclaration) declaration).getName() : null;
        if (name == null) {
            return Optional.empty();
        }
        Cursor found = find(cursor, name);
        return Optional.ofNullable(found != null ? Binding.declaredBy(found) : Binding.declaredBy(cursor, name));
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
        return referenced(region, null);
    }

    /**
     * Those of the bindings {@link #referencedWithin(Cursor)} the region that have one of the names, where names
     * are given, so that no other name is resolved.
     */
    private static Stream<Binding> referenced(Cursor region, @Nullable Set<String> names) {
        J regionTree = region.getValue();
        ScopeService service = service(region);
        Set<String> seen = new HashSet<>();
        List<Binding> read = new ArrayList<>();
        new JavaIsoVisitor<List<Binding>>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, List<Binding> found) {
                Namespace namespace = names == null || names.contains(identifier.getSimpleName()) ?
                        service.namespaceOf(getCursor()) : null;
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

    /**
     * Whether the region's code names {@code this} or {@code super} of the class around it, which a class body
     * inside the region does not.
     */
    private static boolean namesThisOrSuper(Cursor region) {
        boolean[] named = {false};
        new JavaIsoVisitor<Integer>() {
            @Override
            public @Nullable J preVisit(J tree, Integer p) {
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (tree instanceof J.Block && tree != region.getValue() &&
                    (parent instanceof J.ClassDeclaration || parent instanceof J.NewClass)) {
                    stopAfterPreVisit();
                }
                return tree;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (("this".equals(identifier.getSimpleName()) || "super".equals(identifier.getSimpleName())) &&
                    !(parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == identifier)) {
                    named[0] = true;
                }
                return identifier;
            }
        }.visit(region.getValue(), 0, region.getParentOrThrow());
        return named[0];
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
                return c.computeMessageIfAbsent(SERVICE, k -> serviceOf(sourceFile));
            }
        }
        return new ScopeService();
    }

    /**
     * The source file's own service, or Java's rules where the source file's class comes from a release that does
     * not know the service and refuses it.
     */
    private static ScopeService serviceOf(JavaSourceFile sourceFile) {
        try {
            return sourceFile.service(ScopeService.class);
        } catch (RuntimeException e) {
            if (e instanceof UnsupportedOperationException || e.getCause() instanceof UnsupportedOperationException) {
                return new ScopeService();
            }
            throw e;
        }
    }

    /**
     * Every identifier under the region's tree, grouped by name in one walk.
     */
    private static References referencesWithin(Cursor region) {
        Map<String, List<Cursor>> byName = new HashMap<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                byName.computeIfAbsent(identifier.getSimpleName(), n -> new ArrayList<>()).add(getCursor());
                return identifier;
            }
        }.visit(region.getValue(), 0, region.getParentOrThrow());
        return new References(service(region), byName);
    }

    /**
     * The identifiers under one region, grouped by name. A name's identifiers are resolved the first time a binding
     * of that name asks, since a caller usually asks about few of the names a region spells.
     */
    @Value
    private static class References {
        ScopeService service;

        @Getter(AccessLevel.NONE)
        Map<String, List<Cursor>> identifiers;

        @Getter(AccessLevel.NONE)
        Map<String, List<Reference>> resolved = new HashMap<>();

        /**
         * The identifiers in the region that refer to the binding, in source order.
         */
        Stream<Cursor> of(Binding binding) {
            List<Reference> named = resolved.get(binding.getName());
            if (named == null) {
                named = resolve(binding.getName());
                resolved.put(binding.getName(), named);
            }
            return named.stream().filter(reference -> refersTo(reference, binding)).map(reference -> reference.cursor);
        }

        /**
         * The identifiers in the region that assign the binding, in source order. An identifier's parent tells
         * whether it is assigned, so of a name not resolved yet only the assigned identifiers are resolved.
         */
        Stream<Cursor> writesOf(Binding binding) {
            String name = binding.getName();
            List<Reference> named = resolved.get(name);
            Stream<Reference> assigned = named != null ?
                    named.stream().filter(reference -> Binding.isAssigned(reference.cursor)) :
                    identifiers.getOrDefault(name, Collections.emptyList()).stream()
                            .filter(Binding::isAssigned)
                            .map(identifier -> resolve(name, identifier))
                            .filter(Objects::nonNull);
            return assigned.filter(reference -> refersTo(reference, binding)).map(reference -> reference.cursor);
        }

        private static boolean refersTo(Reference reference, Binding binding) {
            return reference.binding != null ? reference.binding.equals(binding) : binding.selects(reference.cursor);
        }

        private List<Reference> resolve(String name) {
            List<Reference> references = new ArrayList<>();
            List<Cursor> named = identifiers.remove(name);
            for (Cursor identifier : named == null ? Collections.<Cursor>emptyList() : named) {
                Reference reference = resolve(name, identifier);
                if (reference != null) {
                    references.add(reference);
                }
            }
            return references;
        }

        private @Nullable Reference resolve(String name, Cursor identifier) {
            Namespace namespace = service.namespaceOf(identifier);
            Binding binding = null;
            if (namespace != null) {
                binding = at(identifier).lookup(name, namespace).orElse(null);
                if (binding == null) {
                    return null;
                }
            } else {
                Object parent = identifier.getParentTreeCursor().getValue();
                J.Identifier ident = identifier.getValue();
                if (!(parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == ident ||
                      parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getName() == ident ||
                      parent instanceof J.Case && ((J.Case) parent).getCaseLabels().contains(ident))) {
                    return null;
                }
            }
            return new Reference(identifier, binding);
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
         * the class binds too. They are read from the scope's tree as it was when the binding was found, so a binding
         * found before an edit does not see it. Finding the binding again from a cursor on the edited tree does.
         */
        public Stream<Cursor> references() {
            return referencesInScope().of(this);
        }

        /**
         * The identifiers under the region's tree that refer to the binding, as {@link #references()} tells them.
         * They are read from the region's own tree, which may be an edited copy the binding's scope does not hold.
         */
        public Stream<Cursor> references(Cursor region) {
            return referencesWithin(region).of(this);
        }

        /**
         * The identifiers of the binding's scope, which one walk gathers for every binding in it.
         */
        private References referencesInScope() {
            Map<Object, References> references = perSourceFile(scope).computeMessageIfAbsent(REFERENCES, k -> new IdentityHashMap<>());
            References inScope = references.get(scope.getValue());
            if (inScope == null) {
                inScope = referencesWithin(scope);
                references.put(scope.getValue(), inScope);
            }
            return inScope;
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
            // The checks above read the declaration alone, and the scope is walked where they do not decide
            for (Cursor write : (Iterable<Cursor>) referencesInScope().writesOf(this)::iterator) {
                if (!blank || !isSimpleAssignment(write) || isInLoopBelow(write, scope)) {
                    return false;
                }
                assignments.add(((J) write.getValue()).getId());
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
            return cursor.getValue() instanceof J.Identifier && cursor.getParent() != null ?
                    declaredBy(cursor.getParent(), cursor.getValue()) : null;
        }

        /**
         * The binding an identifier declares, looked for in each scope around the cursor.
         */
        private static @Nullable Binding declaredBy(Cursor from, J.Identifier identifier) {
            ScopeService service = service(from);
            for (Cursor scope = enclosingScope(service, from);
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
