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
        return c.getPadding().withStatements(insert(c.getPadding().getStatements()));
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block b = super.visitBlock(block, p);
        return b.getPadding().withStatements(moveStaleSelectors(b.getPadding().getStatements()));
    }

    private List<JRightPadded<Statement>> moveStaleSelectors(List<JRightPadded<Statement>> statements) {
        List<JRightPadded<Statement>> result = null;
        for (int i = 0; i < statements.size(); i++) {
            JRightPadded<Statement> rp = statements.get(i);
            List<Statement> replacement = rp.getElement() instanceof S.Import ?
                    moveStaleSelectors((S.Import) rp.getElement()) : null;
            if (replacement == null) {
                if (result != null) {
                    result.add(rp);
                }
                continue;
            }
            if (result == null) {
                result = new ArrayList<>(statements.subList(0, i));
            }
            result.add(rp.withElement(replacement.get(0)));
            for (Statement s : replacement.subList(1, replacement.size())) {
                result.add(JRightPadded.build(s));
            }
        }
        return result == null ? statements : result;
    }

    private @Nullable List<Statement> moveStaleSelectors(S.Import anImport) {
        String qualifier = path(anImport.getQualifier());
        String fullyQualifiedName = fullyQualifiedName();
        List<JRightPadded<S.ImportSelector>> kept = new ArrayList<>();
        List<S.ImportSelector> stale = new ArrayList<>();
        for (JRightPadded<S.ImportSelector> rp : anImport.getPadding().getSelectors().getPadding().getElements()) {
            J.Identifier selected = rp.getElement().getName();
            if (selected != null && !isHiding(rp.getElement()) &&
                fullyQualifiedName.equals(definitionName(selected.getType())) &&
                !(qualifier + "." + selected.getSimpleName()).equals(fullyQualifiedName)) {
                stale.add(rp.getElement());
            } else {
                kept.add(rp);
            }
        }
        if (stale.isEmpty()) {
            return null;
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
                Object parent = getCursor().getParentTreeCursor().getValue();
                boolean qualified = parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == ident ||
                                    parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getName() == ident &&
                                    ((J.MethodInvocation) parent).getSelect() != null;
                if (!qualified && ident.getSimpleName().equals(bound) && fullyQualifiedName.equals(
                        definitionName(ident.getFieldType() != null ? ident.getFieldType() : ident.getType()))) {
                    found.set(true);
                }
                return ident;
            }
        }.visit(cu, referenced);
        return referenced.get();
    }

    private boolean isVisible(S.CompilationUnit cu) {
        if (alias == null && (DEFAULT_IMPORTS.contains(owner) ||
                              cu.getPackageDeclaration() != null && owner.equals(path(cu.getPackageDeclaration().getExpression())))) {
            return true;
        }
        for (Statement s : cu.getStatements()) {
            if (s instanceof J.Import) {
                J.FieldAccess qualid = ((J.Import) s).getQualid();
                if (owner.equals(path(qualid.getTarget())) && (alias == null && name.equals(qualid.getSimpleName()) ||
                                                              isWildcard(qualid.getSimpleName()))) {
                    return true;
                }
            } else if (s instanceof S.Import && owner.equals(path(((S.Import) s).getQualifier()))) {
                boolean wildcard = false;
                for (S.ImportSelector selector : ((S.Import) s).getSelectors()) {
                    J.Identifier selected = selector.getName();
                    if (selected != null && selected.getSimpleName().equals(name)) {
                        J.Identifier selectorAlias = selector.getAlias();
                        return alias == null ? selectorAlias == null :
                                selectorAlias != null && alias.equals(selectorAlias.getSimpleName());
                    }
                    wildcard |= selector.isWildcard();
                }
                if (wildcard && alias == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Inserts the import among the imports that open the file, after the closest one {@link OrderImports} orders
     * before it, or ahead of the file's first statement when there are none.
     */
    private List<JRightPadded<Statement>> insert(List<JRightPadded<Statement>> statements) {
        int leading = 0;
        while (leading < statements.size() && isImport(statements.get(leading).getElement())) {
            leading++;
        }
        Statement added = alias == null ? newImport(Space.EMPTY, JavaType.ShallowClass.build(fullyQualifiedName())) :
                S.Import.build(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(qualifier(Space.EMPTY)),
                        Space.EMPTY, JContainer.build(singletonList(JRightPadded.build(renamingSelector()))));
        List<JRightPadded<Statement>> result = new ArrayList<>(statements);
        if (statements.isEmpty()) {
            result.add(JRightPadded.build(added));
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
        Comparator<Statement> order = Comparator.<Statement>comparingInt(s -> OrderImports.group(s, boundNames))
                .thenComparing(OrderImports::key);
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
