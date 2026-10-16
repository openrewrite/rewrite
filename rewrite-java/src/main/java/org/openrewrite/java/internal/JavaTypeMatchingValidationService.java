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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.TypeMatchingValidationService;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * Removes type attribution from Java sources, names the types a change refers to, and respells changed code without
 * changing what it means, for the test framework's check that recipes match by type.
 */
public class JavaTypeMatchingValidationService implements TypeMatchingValidationService {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern LOCAL_CLASS = Pattern.compile(".*\\$\\d.*");
    private static final int SNIPPET_LENGTH = 60;

    /**
     * The withers that clear each element's type attribution. Literals and primitives keep their types, which are syntax.
     */
    private static final ClassValue<List<Method>> TYPE_WITHERS = new ClassValue<List<Method>>() {
        @Override
        protected List<Method> computeValue(Class<?> type) {
            List<Method> withers = new ArrayList<>();
            if (J.Literal.class.isAssignableFrom(type) || J.Primitive.class.isAssignableFrom(type)) {
                return withers;
            }
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || !JavaType.class.isAssignableFrom(field.getType())) {
                        continue;
                    }
                    String name = "with" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
                    for (Method method : type.getMethods()) {
                        if (method.getName().equals(name) && method.getParameterCount() == 1 &&
                            method.getParameterTypes()[0].isAssignableFrom(field.getType())) {
                            withers.add(method);
                            break;
                        }
                    }
                }
            }
            return withers;
        }
    };

    @Override
    public @Nullable SourceFile withoutTypes(SourceFile sourceFile) {
        if (!(sourceFile instanceof J.CompilationUnit)) {
            return null;
        }
        return (SourceFile) new JavaVisitor<Integer>() {
            @Override
            public J postVisit(J tree, Integer p) {
                J j = tree;
                for (Method wither : TYPE_WITHERS.get(j.getClass())) {
                    try {
                        j = (J) wither.invoke(j, (Object) null);
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // an element that won't give up a type keeps it
                    }
                }
                if (j instanceof J.MethodInvocation) {
                    // visiting the call gave its name the method type back
                    J.MethodInvocation m = (J.MethodInvocation) j;
                    j = m.withName(m.getName().withType(null));
                }
                return j;
            }
        }.visitNonNull(sourceFile, 0);
    }

    @Override
    public List<String> referencesToOtherTypes(SourceFile before, SourceFile after, Collection<? extends SourceFile> sources) {
        if (!(before instanceof J.CompilationUnit) || !(after instanceof J.CompilationUnit)) {
            return emptyList();
        }
        Set<String> declared = new HashSet<>();
        for (SourceFile source : sources) {
            if (source instanceof J.CompilationUnit) {
                new JavaIsoVisitor<Set<String>>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<String> declared) {
                        if (classDecl.getType() != null) {
                            declared.add(classDecl.getType().getFullyQualifiedName());
                        }
                        return super.visitClassDeclaration(classDecl, declared);
                    }
                }.visit(source, declared);
            }
        }
        Change change = Change.of((J.CompilationUnit) before, (J.CompilationUnit) after);
        List<Cursor> changed = new ArrayList<>(change.anchors);
        Set<Object> anchors = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Cursor anchor : change.anchors) {
            anchors.add(anchor.getValue());
        }
        for (Cursor removed : change.removed) {
            // what a removed declaration was annotated with is often why it was removed, unlike the code inside it
            if (removed.getValue() instanceof J.Annotation && anchors.contains(removed.getParentTreeCursor().getValue())) {
                changed.add(removed);
            }
        }
        Map<String, String> references = new LinkedHashMap<>();
        for (Cursor cursor : changed) {
            if (cursor.firstEnclosing(J.Import.class) != null || cursor.firstEnclosing(J.Package.class) != null) {
                continue;
            }
            Reference reference = reference(cursor.getValue(), cursor);
            if (reference != null && !declared.contains(reference.getType()) &&
                !LOCAL_CLASS.matcher(reference.getType()).matches()) {
                references.putIfAbsent(reference.getType(), "`" + snippet(cursor) + "` (" + reference.getName() + ")");
            }
        }
        return new ArrayList<>(references.values());
    }

    @Override
    public List<Respelling> respell(SourceFile before, SourceFile after, Function<SourceFile, @Nullable SourceFile> reparse) {
        if (!(before instanceof J.CompilationUnit) || !(after instanceof J.CompilationUnit)) {
            return emptyList();
        }
        J.CompilationUnit cu = (J.CompilationUnit) before;
        Set<UUID> inChange = inChange(Change.of(cu, (J.CompilationUnit) after));
        if (inChange.isEmpty()) {
            return emptyList();
        }
        List<Respelling> respellings = new ArrayList<>(3);
        for (Respell respell : new Respell[]{new RespellStaticCalls(inChange, true),
                new RespellStaticCalls(inChange, false), new QualifyTypeNames(inChange)}) {
            J.CompilationUnit respelled = (J.CompilationUnit) respell.visitNonNull(cu, new InMemoryExecutionContext());
            // nothing to respell, or the recipe's own change is this respelling
            if (respell.description == null || printedWithoutImports(respelled).equals(printedWithoutImports((J.CompilationUnit) after))) {
                continue;
            }
            SourceFile reparsed = reparse.apply(respelled);
            if (reparsed instanceof J.CompilationUnit && sameMeaning(cu, (J.CompilationUnit) reparsed)) {
                respellings.add(new Respelling(reparsed, respell.description +
                        (respell.count > 1 ? " and " + (respell.count - 1) + " more like it" : "")));
            }
        }
        return respellings;
    }

    @Override
    public boolean changesCode(SourceFile before, @Nullable SourceFile after) {
        if (!(before instanceof J.CompilationUnit) || !(after instanceof J.CompilationUnit)) {
            return after == null || !before.printAll().equals(after.printAll());
        }
        return !printedWithoutImports((J.CompilationUnit) before).equals(printedWithoutImports((J.CompilationUnit) after));
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
               FindMissingTypes.findMissingTypes(respelled, false).size() <= FindMissingTypes.findMissingTypes(original, false).size();
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
                return fieldAccess.getName().getFieldType() == null && TypeUtils.asFullyQualified(fieldAccess.getType()) != null ?
                        fieldAccess : super.visitFieldAccess(fieldAccess, unknown);
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
                calls.add(newClass.getConstructorType() == null ? "?" : MethodMatcher.methodPattern(newClass.getConstructorType()));
                return super.visitNewClass(newClass, calls);
            }
        }.visit(cu, calls);
        Collections.sort(calls);
        return calls;
    }

    private static String printedWithoutImports(J.CompilationUnit cu) {
        return normalized(cu.withImports(emptyList()).printAll());
    }

    /**
     * Without whitespace, so that formatting alone doesn't count as a change.
     */
    private static String normalized(String printed) {
        return WHITESPACE.matcher(printed).replaceAll("");
    }

    private static @Nullable Reference reference(Object tree, Cursor cursor) {
        if (tree instanceof J.MethodInvocation) {
            return reference(((J.MethodInvocation) tree).getMethodType());
        } else if (tree instanceof J.MemberReference) {
            return reference(((J.MemberReference) tree).getMethodType());
        } else if (tree instanceof J.NewClass) {
            J.NewClass newClass = (J.NewClass) tree;
            JavaType.Method constructor = newClass.getConstructorType();
            return constructor == null ? reference(newClass.getType()) :
                    new Reference(constructor.getDeclaringType().getFullyQualifiedName(), MethodMatcher.methodPattern(constructor));
        } else if (tree instanceof J.Annotation) {
            return reference(((J.Annotation) tree).getType());
        } else if (tree instanceof J.ParameterizedType) {
            return reference(((J.ParameterizedType) tree).getType());
        } else if (tree instanceof J.Identifier) {
            J.Identifier identifier = (J.Identifier) tree;
            Cursor parent = cursor.getParentTreeCursor();
            if (parent.getValue() instanceof J.MethodInvocation && ((J.MethodInvocation) parent.getValue()).getName() == identifier ||
                parent.getValue() instanceof J.MemberReference && ((J.MemberReference) parent.getValue()).getReference() == identifier) {
                return reference(parent.getValue(), parent);
            }
            // a type name, not a variable
            if (identifier.getFieldType() == null && identifier.getType() instanceof JavaType.FullyQualified &&
                ((JavaType.FullyQualified) identifier.getType()).getClassName().endsWith(identifier.getSimpleName())) {
                return reference(identifier.getType());
            }
        } else if (tree instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) tree;
            JavaType.Variable field = fieldAccess.getName().getFieldType();
            if ("class".equals(fieldAccess.getSimpleName())) {
                return reference(fieldAccess.getTarget().getType());
            }
            return field == null ? reference(fieldAccess.getType()) : reference(field.getOwner());
        }
        return null;
    }

    private static @Nullable Reference reference(JavaType.@Nullable Method method) {
        // super() and this() name no type of their own
        return method == null || method.isConstructor() ? null :
                new Reference(method.getDeclaringType().getFullyQualifiedName(), MethodMatcher.methodPattern(method));
    }

    private static @Nullable Reference reference(@Nullable JavaType type) {
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        return fq == null ? null : new Reference(fq.getFullyQualifiedName(), fq.getFullyQualifiedName());
    }

    private static String snippet(Cursor cursor) {
        try {
            String printed = cursor.<Tree>getValue().printTrimmed(cursor.getParentOrThrow()).trim();
            int newline = printed.indexOf('\n');
            boolean truncated = newline >= 0 || printed.length() > SNIPPET_LENGTH;
            String firstLine = newline >= 0 ? printed.substring(0, newline).trim() : printed;
            if (firstLine.length() > SNIPPET_LENGTH) {
                firstLine = firstLine.substring(0, SNIPPET_LENGTH);
            }
            return truncated ? firstLine + "..." : firstLine;
        } catch (Exception e) {
            return cursor.getValue().getClass().getSimpleName();
        }
    }

    @Value
    private static class Reference {
        String type;
        String name;
    }

    /**
     * Where a recipe changed a source: the topmost elements it removed and the smallest ones whose printed form it
     * changed, and every element it removed.
     */
    private static final class Change {
        final List<Cursor> anchors = new ArrayList<>();
        final List<Cursor> removed = new ArrayList<>();

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
                        new JavaVisitor<Integer>() {
                            @Override
                            public @Nullable J visit(@Nullable Tree removed, Integer p) {
                                if (removed instanceof J && inAfter.containsKey(removed.getId())) {
                                    // moved elsewhere, not removed
                                    return (J) removed;
                                }
                                return super.visit(removed, p);
                            }

                            @Override
                            public J preVisit(J removed, Integer p) {
                                change.removed.add(getCursor());
                                return removed;
                            }
                        }.visit(tree, p, getCursor());
                        changed = true;
                        return (J) tree;
                    }
                    boolean changedBefore = changed;
                    changed = false;
                    super.visit(tree, p);
                    if (!changed && !printed(tree, getCursor()).equals(printed(afterCursor.getValue(), afterCursor.getParentOrThrow()))) {
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
                return normalized(tree instanceof SourceFile ? ((SourceFile) tree).printAll() : tree.printTrimmed(parent));
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

        int count;

        Respell(Set<UUID> inChange) {
            this.inChange = inChange;
        }

        void respelled(String from, String to) {
            if (description == null) {
                description = "`" + from + "` written `" + to + "`";
            }
            count++;
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
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            JavaType.Method type = m.getMethodType();
            if (!inChange.contains(m.getId()) || type == null || !type.hasFlags(Flag.Static) || m.getTypeParameters() != null) {
                return m;
            }
            String args = m.getArguments().stream().allMatch(J.Empty.class::isInstance) ? "()" : "(..)";
            if (qualify && m.getSelect() == null) {
                JavaType.FullyQualified importedFrom = staticallyImportedFrom(m.getSimpleName(), type.getDeclaringType());
                if (importedFrom != null) {
                    maybeAddImport(importedFrom.getFullyQualifiedName());
                    respelled(m.getSimpleName() + args, importedFrom.getClassName() + "." + m.getSimpleName() + args);
                    return m.withSelect(new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(),
                            importedFrom.getClassName(), importedFrom, null));
                }
            } else if (!qualify && m.getSelect() != null && namesDeclaringType(m.getSelect(), type.getDeclaringType())) {
                maybeAddImport(type.getDeclaringType().getFullyQualifiedName(), m.getSimpleName(), false);
                respelled(m.getSelect().printTrimmed(getCursor()) + "." + m.getSimpleName() + args, m.getSimpleName() + args);
                return m.withSelect(null).withName(m.getName().withPrefix(m.getSelect().getPrefix()));
            }
            return m;
        }

        private JavaType.@Nullable FullyQualified staticallyImportedFrom(String name, JavaType.FullyQualified declaringType) {
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
                               select instanceof J.FieldAccess && ((J.FieldAccess) select).getName().getFieldType() == null;
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(select.getType());
            return typeName && type != null &&
                   TypeUtils.fullyQualifiedNamesAreEqual(type.getFullyQualifiedName(), declaringType.getFullyQualifiedName());
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
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            J.Identifier i = super.visitIdentifier(identifier, ctx);
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(i.getType());
            if (!inChange.contains(i.getId()) || type == null || i.getFieldType() != null || !i.getAnnotations().isEmpty() ||
                type.getPackageName().isEmpty() || !type.getClassName().endsWith(i.getSimpleName()) ||
                LOCAL_CLASS.matcher(type.getFullyQualifiedName()).matches() || !standsForItsType(getCursor())) {
                return i;
            }
            String qualified = type.getFullyQualifiedName().replace('$', '.');
            respelled(i.getSimpleName(), qualified);
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
