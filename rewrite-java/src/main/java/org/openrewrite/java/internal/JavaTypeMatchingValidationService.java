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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.TypeMatchingValidationService;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

/**
 * Respells changed Java code without changing what it means, for the test framework's check that recipes match by type.
 */
public class JavaTypeMatchingValidationService implements TypeMatchingValidationService {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern LOCAL_CLASS = Pattern.compile(".*\\$\\d.*");

    /**
     * The most places of a respelling tried one at a time, to tell which of them a recipe misses.
     */
    private static final int MAX_PLACES = 10;

    @Override
    public List<Respelling> respell(SourceFile before, SourceFile after,
                                    Function<SourceFile, @Nullable SourceFile> reparse) {
        // a change only to imports names nothing by type
        if (!(before instanceof J.CompilationUnit) || !(after instanceof J.CompilationUnit) ||
            !changesCode(before, after)) {
            return emptyList();
        }
        J.CompilationUnit cu = (J.CompilationUnit) before;
        Set<UUID> inChange = inChange(Change.of(cu, (J.CompilationUnit) after));
        if (inChange.isEmpty()) {
            return emptyList();
        }
        String afterCode = printedWithoutImports((J.CompilationUnit) after);
        List<Respelling> respellings = new ArrayList<>(3);
        for (Respell respell : new Respell[]{new RespellStaticCalls(inChange, true),
                new RespellStaticCalls(inChange, false), new QualifyTypeNames(inChange)}) {
            Respelling respelling = respelling(cu, respell, afterCode, reparse);
            if (respelling != null) {
                respellings.add(respelling);
            }
        }
        return respellings;
    }

    private static @Nullable Respelling respelling(J.CompilationUnit cu, Respell respell, String afterCode,
                                                   Function<SourceFile, @Nullable SourceFile> reparse) {
        J.CompilationUnit respelled = (J.CompilationUnit) respell.visitNonNull(cu, new InMemoryExecutionContext());
        // nothing to respell, or the recipe's own change is this respelling
        if (respell.description == null || printedWithoutImports(respelled).equals(afterCode)) {
            return null;
        }
        SourceFile reparsed = reparse.apply(respelled);
        if (!(reparsed instanceof J.CompilationUnit) || !sameMeaning(cu, (J.CompilationUnit) reparsed)) {
            return null;
        }
        List<UUID> places = respell.places;
        return new Respelling(reparsed, respell.description +
                                        (places.size() > 1 ? " and " + (places.size() - 1) + " more like it" : ""),
                () -> {
                    List<Respelling> each = new ArrayList<>();
                    for (int i = 0; places.size() > 1 && i < Math.min(places.size(), MAX_PLACES); i++) {
                        Respelling one = respelling(cu, respell.only(places.get(i)), afterCode, reparse);
                        if (one != null) {
                            each.add(one);
                        }
                    }
                    return each;
                });
    }

    @Override
    public boolean changesCode(SourceFile before, @Nullable SourceFile after) {
        if (!(before instanceof J.CompilationUnit) || !(after instanceof J.CompilationUnit)) {
            return after == null || !before.printAll().equals(after.printAll());
        }
        J.CompilationUnit b = (J.CompilationUnit) before;
        J.CompilationUnit a = (J.CompilationUnit) after;
        // a statically imported call or field changes type in its import, not where it is used
        return !printedWithoutImports(b).equals(printedWithoutImports(a)) || !references(b).equals(references(a));
    }

    /**
     * The calls and names in or right around the code the recipe changed.
     */
    private static Set<UUID> inChange(Change change) {
        Set<UUID> inChange = new HashSet<>();
        for (Cursor anchor : change.anchors) {
            new JavaIsoVisitor<Set<UUID>>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<UUID> inChange) {
                    inChange.add(method.getId());
                    return super.visitMethodInvocation(method, inChange);
                }

                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, Set<UUID> inChange) {
                    inChange.add(identifier.getId());
                    return super.visitIdentifier(identifier, inChange);
                }
            }.visit(anchor.getValue(), inChange, anchor.getParentOrThrow());
            J.MethodInvocation enclosing = anchor.getParentOrThrow().firstEnclosing(J.MethodInvocation.class);
            if (enclosing != null) {
                inChange.add(enclosing.getId());
            }
        }
        return inChange;
    }

    /**
     * A respelling means the same when it calls the same methods and is as well attributed.
     */
    private static boolean sameMeaning(J.CompilationUnit original, J.CompilationUnit respelled) {
        return calls(original).equals(calls(respelled)) && unknownTypes(respelled) <= unknownTypes(original) &&
               FindMissingTypes.findMissingTypes(respelled, false).size() <=
               FindMissingTypes.findMissingTypes(original, false).size();
    }

    private static int unknownTypes(J.CompilationUnit cu) {
        AtomicInteger unknown = new AtomicInteger();
        new JavaIsoVisitor<AtomicInteger>() {
            @Override
            public J.Import visitImport(J.Import anImport, AtomicInteger unknown) {
                // statically imported methods have no type of their own
                return anImport;
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, AtomicInteger unknown) {
                // the package of a fully qualified type name has no type, but the name does
                boolean typeName = fieldAccess.getName().getFieldType() == null &&
                                   TypeUtils.asFullyQualified(fieldAccess.getType()) != null;
                return typeName ? fieldAccess : super.visitFieldAccess(fieldAccess, unknown);
            }

            @Override
            public J preVisit(J tree, AtomicInteger unknown) {
                if (tree instanceof TypedTree && ((TypedTree) tree).getType() instanceof JavaType.Unknown) {
                    unknown.incrementAndGet();
                }
                return tree;
            }
        }.visit(cu, unknown);
        return unknown.get();
    }

    private static List<String> calls(J.CompilationUnit cu) {
        List<String> calls = new ArrayList<>();
        new JavaIsoVisitor<List<String>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<String> calls) {
                calls.add(method.getMethodType() == null ? "?" : MethodMatcher.methodPattern(method.getMethodType()));
                return super.visitMethodInvocation(method, calls);
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, List<String> calls) {
                JavaType.Method constructor = newClass.getConstructorType();
                calls.add(constructor == null ? "?" : MethodMatcher.methodPattern(constructor));
                return super.visitNewClass(newClass, calls);
            }
        }.visit(cu, calls);
        Collections.sort(calls);
        return calls;
    }

    /**
     * The methods the code calls and the fields it reads, outside its imports.
     */
    private static List<String> references(J.CompilationUnit cu) {
        List<String> references = calls(cu);
        new JavaIsoVisitor<List<String>>() {
            @Override
            public J.Import visitImport(J.Import anImport, List<String> references) {
                return anImport;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, List<String> references) {
                JavaType.Variable field = identifier.getFieldType();
                if (field != null && field.getOwner() instanceof JavaType.FullyQualified) {
                    references.add(((JavaType.FullyQualified) field.getOwner()).getFullyQualifiedName() + "#" +
                                   field.getName());
                }
                return identifier;
            }
        }.visit(cu, references);
        Collections.sort(references);
        return references;
    }

    /**
     * Without imports, or the comments on the line of the package declaration or last import, which stay behind
     * when imports move.
     */
    private static String printedWithoutImports(J.CompilationUnit cu) {
        J.CompilationUnit code = cu.withImports(emptyList());
        if (cu.getPackageDeclaration() != null || !cu.getImports().isEmpty()) {
            code = code.getClasses().isEmpty() ? code.withEof(withoutTrailingComments(code.getEof())) :
                    code.withClasses(ListUtils.mapFirst(code.getClasses(),
                            c -> c.withPrefix(withoutTrailingComments(c.getPrefix()))));
        }
        return normalized(code.printAll());
    }

    private static Space withoutTrailingComments(Space space) {
        String before = space.getWhitespace();
        int trailing = 0;
        for (Comment comment : space.getComments()) {
            if (before.contains("\n")) {
                break;
            }
            before = comment.getSuffix();
            trailing++;
        }
        return trailing == 0 ? space :
                space.withComments(space.getComments().subList(trailing, space.getComments().size()));
    }

    /**
     * Without whitespace, so that formatting alone doesn't count as a change.
     */
    private static String normalized(String printed) {
        return WHITESPACE.matcher(printed).replaceAll("");
    }

    /**
     * Where a recipe changed a source: the topmost elements it removed and the smallest ones whose printed form it
     * changed.
     */
    private static final class Change {
        final List<Cursor> anchors = new ArrayList<>();

        static Change of(J.CompilationUnit before, J.CompilationUnit after) {
            Map<UUID, Cursor> inAfter = new HashMap<>();
            new JavaVisitor<Integer>() {
                @Override
                public J preVisit(J tree, Integer p) {
                    inAfter.putIfAbsent(tree.getId(), getCursor());
                    return tree;
                }
            }.visit(after, 0);

            Change change = new Change();
            new JavaVisitor<Integer>() {
                boolean changed;

                @Override
                public @Nullable J visit(@Nullable Tree tree, Integer p) {
                    if (!(tree instanceof J)) {
                        return super.visit(tree, p);
                    }
                    Cursor afterCursor = inAfter.get(tree.getId());
                    if (afterCursor != null && afterCursor.getValue() == tree) {
                        return (J) tree;
                    }
                    if (afterCursor == null) {
                        change.anchors.add(new Cursor(getCursor(), tree));
                        changed = true;
                        return (J) tree;
                    }
                    boolean changedBefore = changed;
                    changed = false;
                    super.visit(tree, p);
                    if (!changed && !printed(tree, getCursor())
                            .equals(printed(afterCursor.getValue(), afterCursor.getParentOrThrow()))) {
                        change.anchors.add(new Cursor(getCursor(), tree));
                        changed = true;
                    }
                    changed |= changedBefore;
                    return (J) tree;
                }
            }.visit(before, 0);
            return change;
        }

        private static String printed(Tree tree, Cursor parent) {
            try {
                return normalized(tree instanceof SourceFile ?
                        ((SourceFile) tree).printAll() : tree.printTrimmed(parent));
            } catch (Exception e) {
                return "";
            }
        }
    }

    /**
     * Respells code in the change without changing what it means.
     */
    private abstract static class Respell extends JavaIsoVisitor<ExecutionContext> {
        final Set<UUID> inChange;

        @Nullable
        String description;

        final List<UUID> places = new ArrayList<>();

        Respell(Set<UUID> inChange) {
            this.inChange = inChange;
        }

        /**
         * The same respelling of only the given place.
         */
        abstract Respell only(UUID place);

        void respelled(UUID place, String from, String to) {
            if (description == null) {
                description = "`" + from + "` written `" + to + "`";
            }
            places.add(place);
        }
    }

    /**
     * Writes statically imported calls as qualified calls, or qualified static calls as statically imported ones.
     */
    private static final class RespellStaticCalls extends Respell {
        private final boolean qualify;

        RespellStaticCalls(Set<UUID> inChange, boolean qualify) {
            super(inChange);
            this.qualify = qualify;
        }

        @Override
        Respell only(UUID place) {
            return new RespellStaticCalls(singleton(place), qualify);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            JavaType.Method type = m.getMethodType();
            if (!inChange.contains(m.getId()) || type == null || !type.hasFlags(Flag.Static) ||
                m.getTypeParameters() != null) {
                return m;
            }
            String args = m.getArguments().stream().allMatch(J.Empty.class::isInstance) ? "()" : "(..)";
            if (qualify && m.getSelect() == null) {
                JavaType.FullyQualified importedFrom =
                        staticallyImportedFrom(m.getSimpleName(), type.getDeclaringType());
                if (importedFrom != null) {
                    maybeAddImport(importedFrom.getFullyQualifiedName());
                    respelled(m.getId(), m.getSimpleName() + args,
                            importedFrom.getClassName() + "." + m.getSimpleName() + args);
                    return m.withSelect(new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(),
                            importedFrom.getClassName(), importedFrom, null));
                }
            } else if (!qualify && m.getSelect() != null &&
                       namesDeclaringType(m.getSelect(), type.getDeclaringType())) {
                maybeAddImport(type.getDeclaringType().getFullyQualifiedName(), m.getSimpleName(), false);
                respelled(m.getId(), m.getSelect().printTrimmed(getCursor()) + "." + m.getSimpleName() + args,
                        m.getSimpleName() + args);
                return m.withSelect(null).withName(m.getName().withPrefix(m.getSelect().getPrefix()));
            }
            return m;
        }

        private JavaType.@Nullable FullyQualified staticallyImportedFrom(String name,
                                                                         JavaType.FullyQualified declaringType) {
            J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
            for (J.Import anImport : cu.getImports()) {
                String imported = anImport.getQualid().getSimpleName();
                JavaType.FullyQualified from = TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType());
                if (anImport.isStatic() && (imported.equals(name) || "*".equals(imported)) && from != null &&
                    TypeUtils.isAssignableTo(declaringType.getFullyQualifiedName(), from)) {
                    return from;
                }
            }
            return null;
        }

        /**
         * A select that names the class declaring the method, not a subclass that inherits it or a variable.
         */
        private static boolean namesDeclaringType(Expression select, JavaType.FullyQualified declaringType) {
            boolean typeName = select instanceof J.Identifier && ((J.Identifier) select).getFieldType() == null &&
                               !"this".equals(((J.Identifier) select).getSimpleName()) &&
                               !"super".equals(((J.Identifier) select).getSimpleName()) ||
                               select instanceof J.FieldAccess &&
                               ((J.FieldAccess) select).getName().getFieldType() == null;
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(select.getType());
            return typeName && type != null &&
                   TypeUtils.fullyQualifiedNamesAreEqual(type.getFullyQualifiedName(),
                           declaringType.getFullyQualifiedName());
        }
    }

    /**
     * Writes type names fully qualified, as `java.util.List` for an imported `List`.
     */
    private static final class QualifyTypeNames extends Respell {
        QualifyTypeNames(Set<UUID> inChange) {
            super(inChange);
        }

        @Override
        Respell only(UUID place) {
            return new QualifyTypeNames(singleton(place));
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            J.Identifier i = super.visitIdentifier(identifier, ctx);
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(i.getType());
            if (!inChange.contains(i.getId()) || type == null || i.getFieldType() != null ||
                !i.getAnnotations().isEmpty() ||
                type.getPackageName().isEmpty() || !type.getClassName().endsWith(i.getSimpleName()) ||
                LOCAL_CLASS.matcher(type.getFullyQualifiedName()).matches() || !standsForItsType(getCursor())) {
                return i;
            }
            String qualified = type.getFullyQualifiedName().replace('$', '.');
            respelled(i.getId(), i.getSimpleName(), qualified);
            // printed and parsed again, so the name doesn't need to be a tree of its own
            return i.withSimpleName(qualified);
        }

        /**
         * Whether a type name stands alone, rather than ending a qualified name or naming a declaration.
         */
        private static boolean standsForItsType(Cursor cursor) {
            Object parent = cursor.getParentTreeCursor().getValue();
            Object name = cursor.getValue();
            return !(parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == name) &&
                   !(parent instanceof J.ClassDeclaration && ((J.ClassDeclaration) parent).getName() == name) &&
                   !(parent instanceof J.MethodDeclaration && ((J.MethodDeclaration) parent).getName() == name) &&
                   !(parent instanceof J.VariableDeclarations.NamedVariable) &&
                   !(parent instanceof J.NewClass && ((J.NewClass) parent).getEnclosing() != null) &&
                   cursor.firstEnclosing(J.Import.class) == null && cursor.firstEnclosing(J.Package.class) == null &&
                   cursor.firstEnclosing(Javadoc.class) == null;
        }
    }
}
