/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala;

import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.scala.marker.CommaContinuation;
import org.openrewrite.scala.tree.S;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.scala.RemoveUnusedImports.*;

/**
 * Adds an import to the file's top-level imports unless the name is already visible there.
 * <p>
 * A brace selector that a type change left typed as the imported type but under another qualifier, such as
 * {@code CopyOnWriteArrayList} in {@code import java.util.{CopyOnWriteArrayList, Map}}, moves to an import of its
 * own next to it, so that it keeps its scope and rename.
 */
@EqualsAndHashCode(callSuper = false)
public class AddImport<P> extends ScalaIsoVisitor<P> {
    private static final Set<String> DEFAULT_IMPORTS = new HashSet<>(Arrays.asList("java.lang", "scala", "scala.Predef"));

    /**
     * The aliases the {@code scala} package and {@code Predef} declare for types that live elsewhere, by the path
     * they are imported by. Only aliases naming the same type in Scala 2.12, 2.13 and 3 are listed, which leaves
     * out {@code Seq} and {@code IndexedSeq}.
     */
    private static final Map<String, String> SCALA_ALIASES = new HashMap<>();

    static {
        for (String n : Arrays.asList("List", "Vector", "Range")) {
            SCALA_ALIASES.put("scala." + n, "scala.collection.immutable." + n);
        }
        for (String n : Arrays.asList("Iterable", "Iterator", "BufferedIterator")) {
            SCALA_ALIASES.put("scala." + n, "scala.collection." + n);
        }
        SCALA_ALIASES.put("scala.StringBuilder", "scala.collection.mutable.StringBuilder");
        for (String n : Arrays.asList("Either", "Left", "Right")) {
            SCALA_ALIASES.put("scala." + n, "scala.util." + n);
        }
        for (String n : Arrays.asList("BigInt", "BigDecimal", "Equiv", "Fractional", "Integral", "Numeric", "Ordered", "Ordering")) {
            SCALA_ALIASES.put("scala." + n, "scala.math." + n);
        }
        for (String n : Arrays.asList("Map", "Set")) {
            SCALA_ALIASES.put("scala.Predef." + n, "scala.collection.immutable." + n);
        }
    }

    private final String owner;
    private final String name;
    private final @Nullable String alias;
    private final boolean onlyIfReferenced;

    public AddImport(@Nullable String packageName, String typeName, @Nullable String member,
                     @Nullable String alias, boolean onlyIfReferenced) {
        String type = normalize(packageName == null ? typeName : packageName + "." + typeName);
        int dot = type.lastIndexOf('.');
        this.owner = member == null ? (dot < 0 ? "" : type.substring(0, dot)) : type;
        this.name = member == null ? type.substring(dot + 1) : member;
        this.alias = alias;
        this.onlyIfReferenced = onlyIfReferenced;
    }

    private String fullyQualifiedName() {
        return owner.isEmpty() ? name : owner + "." + name;
    }

    @Override
    public S.CompilationUnit visitCompilationUnit(S.CompilationUnit cu, P p) {
        S.CompilationUnit c = super.visitCompilationUnit(cu, p);
        c = c.getPadding().withStatements(moveStaleSelectors(c.getPadding().getStatements()));
        if (owner.isEmpty() || onlyIfReferenced && !isReferenced(c) || isVisible(c)) {
            return c;
        }
        return c.getPadding().withStatements(insert(c.getPadding().getStatements(), c.getPackageDeclaration() != null));
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block b = super.visitBlock(block, p);
        return b.getPadding().withStatements(moveStaleSelectors(b.getPadding().getStatements()));
    }

    private List<JRightPadded<Statement>> moveStaleSelectors(List<JRightPadded<Statement>> statements) {
        List<JRightPadded<Statement>> result = new ArrayList<>(statements.size());
        boolean changed = false;
        Space carried = null;
        for (JRightPadded<Statement> rp : statements) {
            List<Statement> replacement = rp.getElement() instanceof S.Import ?
                    moveStaleSelectors((S.Import) rp.getElement()) : null;
            if (replacement == null) {
                replacement = singletonList(rp.getElement());
            } else if (replacement.isEmpty()) {
                carried = carried == null ? rp.getElement().getPrefix() : carried;
                changed = true;
                continue;
            } else {
                changed = true;
            }
            Statement first = replacement.get(0);
            if (carried != null && (isImport(first) || result.isEmpty())) {
                first = first.withPrefix(inheritPrefix(carried, first.getPrefix()));
                changed = true;
            }
            carried = null;
            result.add(rp.withElement(first));
            for (Statement s : replacement.subList(1, replacement.size())) {
                result.add(JRightPadded.build(s));
            }
        }
        return changed ? result : statements;
    }

    private @Nullable List<Statement> moveStaleSelectors(S.Import anImport) {
        String qualifier = path(anImport.getQualifier());
        String fullyQualifiedName = fullyQualifiedName();
        List<JRightPadded<S.ImportSelector>> kept = new ArrayList<>();
        List<S.ImportSelector> stale = new ArrayList<>();
        for (JRightPadded<S.ImportSelector> rp : anImport.getPadding().getSelectors().getPadding().getElements()) {
            J.Identifier selected = rp.getElement().getName();
            // A selector of another name, such as a type alias, is not one a type change left behind
            if (selected != null && !isHiding(rp.getElement()) && selected.getSimpleName().equals(name) &&
                fullyQualifiedName.equals(definitionName(selected.getType())) &&
                !importsThis(qualifier, selected.getSimpleName())) {
                // Scala imports the type under this name by default, so the selector is dropped
                if (rp.getElement().getAlias() == null && isImportedByDefault()) {
                    continue;
                }
                stale.add(rp.getElement());
            } else {
                kept.add(rp);
            }
        }
        if (kept.size() == anImport.getSelectors().size()) {
            return null;
        }
        if (kept.isEmpty() && stale.isEmpty()) {
            return emptyList();
        }
        String indent = anImport.getPrefix().getWhitespace().substring(anImport.getPrefix().getWhitespace().lastIndexOf('\n') + 1);
        List<Statement> result = new ArrayList<>();
        if (!kept.isEmpty()) {
            result.add(withSelectors(anImport, kept));
        }
        for (S.ImportSelector selector : stale) {
            Statement moved = selector.getAlias() == null ?
                    newImport(Space.EMPTY, selector.getName().getType()) :
                    anImport.withId(Tree.randomId())
                            .withQualifier(qualifier(anImport.getQualifier().getPrefix()))
                            .withSelectors(singletonList(selector.withPrefix(Space.EMPTY)));
            result.add(result.isEmpty() ? moved.withPrefix(anImport.getPrefix()).withMarkers(anImport.getMarkers()) :
                    moved.withPrefix(Space.format("\n" + indent)));
        }
        return result;
    }

    private boolean isReferenced(S.CompilationUnit cu) {
        String bound = alias == null ? name : alias;
        String fullyQualifiedName = fullyQualifiedName();
        AtomicBoolean referenced = new AtomicBoolean();
        new ScalaIsoVisitor<AtomicBoolean>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, AtomicBoolean found) {
                return found.get() || tree instanceof J.Import || tree instanceof S.Import ? (J) tree : super.visit(tree, found);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier ident, AtomicBoolean found) {
                if (ident.getSimpleName().equals(bound) && !isSelected(ident, getCursor().getParentTreeCursor().getValue()) &&
                    fullyQualifiedName.equals(definitionName(referencedType(ident)))) {
                    found.set(true);
                }
                return ident;
            }
        }.visit(cu, referenced);
        return referenced.get();
    }

    private boolean isVisible(S.CompilationUnit cu) {
        if (alias == null && cu.getPackageDeclaration() != null &&
            owner.equals(path(cu.getPackageDeclaration().getExpression()))) {
            return true;
        }
        for (Statement s : cu.getStatements()) {
            if (s instanceof J.Import) {
                J.FieldAccess qualid = ((J.Import) s).getQualid();
                String qualifier = path(qualid.getTarget());
                if (alias == null && (importsThis(qualifier, qualid.getSimpleName()) ||
                                      isWildcard(qualid.getSimpleName()) && owner.equals(qualifier))) {
                    return true;
                }
            } else if (s instanceof S.Import) {
                String qualifier = path(((S.Import) s).getQualifier());
                boolean wildcard = false;
                // A selector that renames or hides the name also keeps the import's wildcard from binding it.
                boolean renamed = false;
                for (S.ImportSelector selector : ((S.Import) s).getSelectors()) {
                    J.Identifier selected = selector.getName();
                    if (selected != null && selected.getSimpleName().equals(name) && importsThis(qualifier, name)) {
                        J.Identifier selectorAlias = selector.getAlias();
                        if (alias == null ? selectorAlias == null :
                                selectorAlias != null && alias.equals(selectorAlias.getSimpleName())) {
                            return true;
                        }
                        renamed = true;
                    }
                    wildcard |= selector.isWildcard();
                }
                if (wildcard && alias == null && !renamed && owner.equals(qualifier)) {
                    return true;
                }
            }
        }
        return alias == null && isImportedByDefault() && !mayShadowDefaultImport(cu);
    }

    /**
     * Whether {@code qualifier.selected} imports the type or member this visitor adds, directly or through an alias.
     */
    private boolean importsThis(String qualifier, String selected) {
        String imported = qualifier + "." + selected;
        return imported.equals(fullyQualifiedName()) || fullyQualifiedName().equals(SCALA_ALIASES.get(imported));
    }

    private boolean isImportedByDefault() {
        return DEFAULT_IMPORTS.contains(owner) ||
               fullyQualifiedName().equals(SCALA_ALIASES.get("scala." + name)) ||
               fullyQualifiedName().equals(SCALA_ALIASES.get("scala.Predef." + name));
    }

    /**
     * Whether a top-level import binds the name, or is a wildcard that may, and so takes precedence over what Scala
     * imports by default.
     */
    private boolean mayShadowDefaultImport(S.CompilationUnit cu) {
        for (Statement s : cu.getStatements()) {
            if (s instanceof J.Import) {
                String imported = ((J.Import) s).getQualid().getSimpleName();
                if (imported.equals(name) || isWildcard(imported)) {
                    return true;
                }
            } else if (s instanceof S.Import) {
                for (S.ImportSelector selector : ((S.Import) s).getSelectors()) {
                    J.Identifier bound = selector.getAlias() != null ? selector.getAlias() : selector.getName();
                    if (selector.isWildcard() || bound != null && bound.getSimpleName().equals(name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Inserts the import among the imports that open the file, after the closest one {@link OrderImports} orders
     * before it, or ahead of the file's first statement when there are none.
     */
    private List<JRightPadded<Statement>> insert(List<JRightPadded<Statement>> statements, boolean afterPackage) {
        int leading = 0;
        while (leading < statements.size() && isImport(statements.get(leading).getElement())) {
            leading++;
        }
        Statement added = alias == null ? newImport(Space.EMPTY, JavaType.ShallowClass.build(fullyQualifiedName())) :
                S.Import.build(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(qualifier(Space.EMPTY)),
                        Space.EMPTY, JContainer.build(singletonList(JRightPadded.build(renamingSelector()))));
        List<JRightPadded<Statement>> result = new ArrayList<>(statements);
        if (statements.isEmpty()) {
            result.add(JRightPadded.build(afterPackage ? added.withPrefix(Space.format("\n\n")) : added));
            return result;
        }
        if (leading == 0) {
            Statement first = statements.get(0).getElement();
            result.add(0, JRightPadded.build(added.withPrefix(Space.format(first.getPrefix().getWhitespace()))));
            result.set(1, statements.get(0).withElement(first.withPrefix(first.getPrefix().withWhitespace("\n\n"))));
            return result;
        }

        Set<String> boundNames = new HashSet<>(OrderImports.boundNames(added));
        for (int i = 0; i < leading; i++) {
            boundNames.addAll(OrderImports.boundNames(statements.get(i).getElement()));
        }
        Comparator<Statement> order = OrderImports.order(boundNames);
        int previous = -1;
        for (int i = 0; i < leading; i++) {
            Statement s = statements.get(i).getElement();
            if (!isContinuation(s) && order.compare(s, added) <= 0 &&
                (previous < 0 || order.compare(s, statements.get(previous).getElement()) >= 0)) {
                previous = i;
            }
        }
        int at = previous + 1;
        while (at < leading && isContinuation(statements.get(at).getElement())) {
            at++;
        }
        int group = OrderImports.group(added, boundNames);
        if (previous < 0) {
            Statement next = statements.get(0).getElement();
            result.add(0, JRightPadded.build(added.withPrefix(next.getPrefix())));
            result.set(1, statements.get(0).withElement(next.withPrefix(
                    Space.format(OrderImports.group(next, boundNames) == group ? "\n" : "\n\n"))));
            return result;
        }
        result.add(at, JRightPadded.build(added.withPrefix(Space.format(
                OrderImports.group(statements.get(previous).getElement(), boundNames) == group ? "\n" : "\n\n"))));
        if (at < leading) {
            Statement next = statements.get(at).getElement();
            result.set(at + 1, statements.get(at).withElement(next.withPrefix(next.getPrefix().withWhitespace(
                    OrderImports.group(next, boundNames) == group ? "\n" : "\n\n"))));
        }
        return result;
    }

    private static boolean isContinuation(Statement s) {
        return s.getMarkers().findFirst(CommaContinuation.class).isPresent();
    }

    private J.Import newImport(Space prefix, @Nullable JavaType type) {
        J.Identifier id = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, type, null);
        J.FieldAccess qualid = new J.FieldAccess(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                qualifier(Space.EMPTY), JLeftPadded.build(id), type);
        return new J.Import(Tree.randomId(), prefix, Markers.EMPTY, JLeftPadded.build(false), qualid, null);
    }

    private Expression qualifier(Space prefix) {
        Expression qualifier = null;
        for (String segment : owner.split("\\.")) {
            J.Identifier id = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), segment, null, null);
            qualifier = qualifier == null ? id :
                    new J.FieldAccess(Tree.randomId(), Space.EMPTY, Markers.EMPTY, qualifier, JLeftPadded.build(id), null);
        }
        return Objects.requireNonNull(qualifier).withPrefix(prefix);
    }

    private S.ImportSelector renamingSelector() {
        JavaType type = JavaType.ShallowClass.build(fullyQualifiedName());
        return S.ImportSelector.build(Tree.randomId(), Space.EMPTY, Markers.EMPTY, false, null, false, false,
                new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, type, null),
                new JLeftPadded<>(Space.SINGLE_SPACE,
                        new J.Identifier(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, emptyList(), Objects.requireNonNull(alias), type, null),
                        Markers.EMPTY),
                false);
    }
}
