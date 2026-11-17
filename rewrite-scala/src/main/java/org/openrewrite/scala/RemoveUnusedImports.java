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
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.scala.marker.CommaContinuation;
import org.openrewrite.scala.marker.ImplicitArguments;
import org.openrewrite.scala.marker.ImplicitConversion;
import org.openrewrite.scala.marker.OmitImportBraces;
import org.openrewrite.scala.tree.S;

import java.util.*;

import static java.util.Collections.emptyList;

public class RemoveUnusedImports extends Recipe {

    @Getter
    final String displayName = "Remove unused Scala imports";

    @Getter
    final String description = "Removes imports, and selectors of brace imports, that nothing after them in their scope uses. " +
            "Uses through implicit arguments and implicit conversions count, and an import whose use can't be ruled out " +
            "for lack of type information is kept.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Remover<>(null);
    }

    /**
     * Removes the imports of a type or member, of its members, and the wildcard import of its package or owner,
     * where nothing in their scope uses them.
     */
    public static <P> ScalaVisitor<P> removing(String fullyQualifiedName) {
        return new Remover<>(fullyQualifiedName);
    }

    @EqualsAndHashCode(callSuper = false)
    private static class Remover<P> extends ScalaIsoVisitor<P> {
        private final @Nullable String fullyQualifiedName;

        Remover(@Nullable String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName == null ? null : normalize(fullyQualifiedName);
        }

        @Override
        public S.CompilationUnit visitCompilationUnit(S.CompilationUnit cu, P p) {
            S.CompilationUnit c = super.visitCompilationUnit(cu, p);
            return c.getPadding().withStatements(removeUnused(c.getPadding().getStatements()));
        }

        @Override
        public J.Block visitBlock(J.Block block, P p) {
            J.Block b = super.visitBlock(block, p);
            return b.getPadding().withStatements(removeUnused(b.getPadding().getStatements()));
        }

        /**
         * An import is in scope from its position to the end of its statement list, so the list is walked
         * backwards, accumulating what the statements after each import use.
         */
        private List<JRightPadded<Statement>> removeUnused(List<JRightPadded<Statement>> statements) {
            if (statements.stream().noneMatch(rp -> isImport(rp.getElement()))) {
                return statements;
            }
            Usages usages = new Usages();
            Statement[] decided = new Statement[statements.size()];
            boolean changed = false;
            for (int i = statements.size() - 1; i >= 0; i--) {
                Statement s = statements.get(i).getElement();
                Statement kept = s instanceof J.Import ? keep((J.Import) s, usages) :
                        s instanceof S.Import ? keep((S.Import) s, usages) : s;
                decided[i] = kept;
                changed |= kept != s;
                if (kept != null) {
                    usages.collect(kept);
                }
            }
            if (!changed) {
                return statements;
            }

            List<JRightPadded<Statement>> result = new ArrayList<>(statements.size());
            Space carried = null;
            boolean clauseHeadRemoved = false;
            for (int i = 0; i < statements.size(); i++) {
                JRightPadded<Statement> rp = statements.get(i);
                boolean continuation = rp.getElement().getMarkers().findFirst(CommaContinuation.class).isPresent();
                Statement s = decided[i];
                if (s == null) {
                    if (!continuation) {
                        if (carried == null) {
                            carried = rp.getElement().getPrefix();
                        }
                        clauseHeadRemoved = true;
                    }
                    continue;
                }
                if (continuation && clauseHeadRemoved) {
                    s = startClause(s, carried == null ? s.getPrefix() : carried);
                    carried = null;
                    clauseHeadRemoved = false;
                } else if (carried != null) {
                    if (isImport(s) || result.isEmpty()) {
                        s = s.withPrefix(inheritPrefix(carried, s.getPrefix()));
                    }
                    carried = null;
                }
                if (!continuation) {
                    clauseHeadRemoved = false;
                }
                result.add(rp.withElement(s));
            }
            return result;
        }

        private J.@Nullable Import keep(J.Import anImport, Usages usages) {
            J.FieldAccess qualid = anImport.getQualid();
            String name = qualid.getSimpleName();
            String qualifier = path(qualid.getTarget());
            boolean wildcard = isWildcard(name);
            if (isLanguageFeature(qualifier) || !targets(qualifier, wildcard ? null : name)) {
                return anImport;
            }
            boolean used;
            if (wildcard) {
                used = usages.usesMemberOf(qualifier, qualid.getTarget().getType());
            } else if ("given".equals(name)) {
                used = usages.usesImplicitMemberOf(qualifier, qualid.getTarget().getType());
            } else {
                used = usages.uses(name, qualid.getType());
            }
            return used ? anImport : null;
        }

        private @Nullable Statement keep(S.Import anImport, Usages usages) {
            String qualifier = path(anImport.getQualifier());
            if (isLanguageFeature(qualifier)) {
                return anImport;
            }
            JavaType qualifierType = anImport.getQualifier().getType();
            List<JRightPadded<S.ImportSelector>> selectors = anImport.getPadding().getSelectors().getPadding().getElements();
            List<JRightPadded<S.ImportSelector>> kept = new ArrayList<>(selectors.size());
            boolean importsAnything = false;
            for (JRightPadded<S.ImportSelector> rp : selectors) {
                S.ImportSelector selector = rp.getElement();
                if (isHiding(selector)) {
                    kept.add(rp);
                    continue;
                }
                boolean used;
                if (selector.isGiven()) {
                    used = !targets(qualifier, null) || usages.usesImplicitMemberOf(qualifier, qualifierType);
                } else if (selector.isWildcard()) {
                    used = !targets(qualifier, null) || usages.usesMemberOf(qualifier, qualifierType);
                } else {
                    J.Identifier name = Objects.requireNonNull(selector.getName());
                    J.Identifier alias = selector.getAlias();
                    used = !targets(qualifier, name.getSimpleName()) ||
                           usages.uses(alias == null ? name.getSimpleName() : alias.getSimpleName(), name.getType());
                }
                if (used) {
                    kept.add(rp);
                    importsAnything = true;
                }
            }
            if (!importsAnything) {
                return null;
            }
            return kept.size() == selectors.size() ? anImport : withSelectors(anImport, kept);
        }

        /**
         * Whether this visitor may remove an import of {@code name} (a wildcard when null) from {@code qualifier}.
         */
        private boolean targets(String qualifier, @Nullable String name) {
            if (fullyQualifiedName == null) {
                return true;
            }
            if (name == null) {
                return qualifier.equals(fullyQualifiedName) ||
                       fullyQualifiedName.startsWith(qualifier + ".") &&
                       fullyQualifiedName.indexOf('.', qualifier.length() + 1) < 0;
            }
            String imported = qualifier + "." + name;
            return imported.equals(fullyQualifiedName) || imported.startsWith(fullyQualifiedName + ".");
        }

        private static Statement startClause(Statement s, Space prefix) {
            Markers markers = s.getMarkers().removeByType(CommaContinuation.class);
            if (s instanceof J.Import) {
                J.Import i = (J.Import) s;
                return i.withPrefix(prefix).withMarkers(markers).withQualid((J.FieldAccess) withoutLeadingSpace(i.getQualid()));
            }
            S.Import i = (S.Import) s;
            return i.withPrefix(prefix).withMarkers(markers).withQualifier(withoutLeadingSpace(i.getQualifier()));
        }

        private static Expression withoutLeadingSpace(Expression e) {
            if (e instanceof J.FieldAccess) {
                J.FieldAccess fa = (J.FieldAccess) e;
                return fa.withPrefix(Space.EMPTY).withTarget(withoutLeadingSpace(fa.getTarget()));
            }
            return e.withPrefix(Space.EMPTY);
        }
    }

    /**
     * Keeps the whitespace and comments of the removed statement's prefix, which belong to its position, such as
     * a file header, followed by the comments of the statement that takes the position.
     */
    static Space inheritPrefix(Space removed, Space own) {
        if (removed.getComments().isEmpty()) {
            return own.withWhitespace(removed.getWhitespace());
        }
        List<Comment> comments = new ArrayList<>(removed.getComments());
        int last = comments.size() - 1;
        comments.set(last, comments.get(last).withSuffix(own.getWhitespace()));
        comments.addAll(own.getComments());
        return removed.withComments(comments);
    }

    /**
     * An import with the given selectors, written without braces when a single plain name or wildcard remains.
     */
    static Statement withSelectors(S.Import anImport, List<JRightPadded<S.ImportSelector>> kept) {
        List<JRightPadded<S.ImportSelector>> all = anImport.getPadding().getSelectors().getPadding().getElements();
        List<JRightPadded<S.ImportSelector>> selectors = new ArrayList<>(kept);
        if (selectors.get(0) != all.get(0)) {
            selectors.set(0, selectors.get(0).map(sel -> sel.withPrefix(all.get(0).getElement().getPrefix())));
        }
        int last = selectors.size() - 1;
        if (selectors.get(last) != all.get(all.size() - 1)) {
            selectors.set(last, selectors.get(last).withAfter(all.get(all.size() - 1).getAfter()));
        }
        S.ImportSelector only = selectors.get(0).getElement();
        if (selectors.size() == 1 && !anImport.getMarkers().findFirst(OmitImportBraces.class).isPresent() &&
            only.getAlias() == null && !only.isGiven()) {
            J.Identifier name = only.isWildcard() ?
                    new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(),
                            only.isLegacyUnderscore() ? "_" : "*", null, null) :
                    Objects.requireNonNull(only.getName()).withPrefix(Space.EMPTY);
            Expression qualifier = anImport.getQualifier();
            J.FieldAccess qualid = new J.FieldAccess(Tree.randomId(), qualifier.getPrefix(), Markers.EMPTY,
                    qualifier.withPrefix(Space.EMPTY), JLeftPadded.build(name), name.getType());
            return new J.Import(anImport.getId(), anImport.getPrefix(), anImport.getMarkers(),
                    JLeftPadded.build(false), qualid, null);
        }
        return anImport.getPadding().withSelectors(anImport.getPadding().getSelectors().getPadding().withElements(selectors));
    }

    static boolean isImport(Statement s) {
        return s instanceof J.Import || s instanceof S.Import;
    }

    static boolean isWildcard(String name) {
        return "_".equals(name) || "*".equals(name);
    }

    static boolean isHiding(S.ImportSelector selector) {
        J.Identifier alias = selector.getAlias();
        return alias != null && "_".equals(alias.getSimpleName());
    }

    /**
     * The dotted path of an import qualifier, without a leading {@code _root_}.
     */
    /**
     * Whether imports from the qualifier enable compiler features, which no reference in the source shows a use of.
     */
    private static boolean isLanguageFeature(String qualifier) {
        String path = qualifier.startsWith("scala.") ? qualifier.substring("scala.".length()) : qualifier;
        return path.equals("language") || path.startsWith("language.");
    }

    static String path(Expression e) {
        String path;
        if (e instanceof J.FieldAccess) {
            J.FieldAccess fa = (J.FieldAccess) e;
            path = path(fa.getTarget()) + "." + fa.getSimpleName();
        } else if (e instanceof J.Identifier) {
            path = ((J.Identifier) e).getSimpleName();
        } else {
            path = e.printTrimmed();
        }
        return path.startsWith("_root_.") ? path.substring("_root_.".length()) : path;
    }

    /**
     * A fully qualified name as written in Scala source, where nested classes and objects are separated by dots.
     */
    static String normalize(String fullyQualifiedName) {
        String n = fullyQualifiedName.replace('$', '.');
        while (n.endsWith(".")) {
            n = n.substring(0, n.length() - 1);
        }
        return n;
    }

    /**
     * The name a definition is imported by, such as {@code scala.math.max} for a method of the {@code scala.math}
     * package object, or null if the type does not name a definition.
     */
    static @Nullable String definitionName(@Nullable JavaType type) {
        if (type instanceof JavaType.Method) {
            JavaType.Method m = (JavaType.Method) type;
            return owner(m.getDeclaringType()) + "." + m.getName();
        }
        if (type instanceof JavaType.Variable) {
            JavaType.Variable v = (JavaType.Variable) type;
            JavaType.FullyQualified owner = TypeUtils.asFullyQualified(v.getOwner());
            return owner == null ? null : owner(owner) + "." + v.getName();
        }
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        return fq == null || fq instanceof JavaType.Unknown ? null : normalize(fq.getFullyQualifiedName());
    }

    /**
     * The path members of the type are imported from, where the members of a package object are those of its
     * package.
     */
    private static String owner(JavaType.FullyQualified type) {
        String owner = normalize(type.getFullyQualifiedName());
        return owner.endsWith(".package") ? owner.substring(0, owner.length() - ".package".length()) : owner;
    }

    /**
     * Whether the identifier is selected from something else, like {@code b} in {@code a.b} or a name in an import
     * selector, and so is not a reference an import could provide.
     */
    static boolean isSelected(J.Identifier ident, Object parent) {
        if (parent instanceof J.FieldAccess) {
            return ((J.FieldAccess) parent).getName() == ident;
        }
        if (parent instanceof J.MethodInvocation) {
            return ((J.MethodInvocation) parent).getName() == ident && ((J.MethodInvocation) parent).getSelect() != null;
        }
        if (parent instanceof S.ImportSelector) {
            return ((S.ImportSelector) parent).getName() == ident || ((S.ImportSelector) parent).getAlias() == ident;
        }
        return false;
    }

    static @Nullable JavaType referencedType(J.Identifier ident) {
        return ident.getFieldType() != null ? ident.getFieldType() : ident.getType();
    }

    /**
     * Whether the identifier names a type by another name than the type's own, as a type alias does, so that the
     * definition it refers to is not known.
     */
    private static boolean isAlias(J.Identifier ident) {
        if (ident.getFieldType() != null) {
            return false;
        }
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(ident.getType());
        if (fq == null || fq instanceof JavaType.Unknown) {
            return false;
        }
        String className = normalize(fq.getClassName());
        return !className.substring(className.lastIndexOf('.') + 1).equals(ident.getSimpleName());
    }

    /**
     * The references a range of statements makes to definitions an import could provide.
     */
    private static class Usages {
        private final Map<String, List<J.Identifier>> identifiersByName = new HashMap<>();
        private final List<JavaType> implicitReferences = new ArrayList<>();
        private final Set<String> owners = new HashSet<>();
        private final List<JavaType.FullyQualified> memberOwners = new ArrayList<>();
        private boolean unresolved;

        void collect(Statement statement) {
            new ScalaIsoVisitor<Usages>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier ident, Usages u) {
                    if (!isSelected(ident, getCursor().getParentTreeCursor().getValue())) {
                        u.add(ident);
                    }
                    return ident;
                }

                @Override
                public @Nullable J preVisit(J tree, Usages u) {
                    tree.getMarkers().findFirst(ImplicitArguments.class)
                            .ifPresent(args -> args.getArguments().forEach(u::addImplicit));
                    tree.getMarkers().findFirst(ImplicitConversion.class)
                            .ifPresent(conversion -> u.addImplicit(conversion.getConversion()));
                    return tree;
                }
            }.visit(statement, this);
        }

        private void add(J.Identifier ident) {
            identifiersByName.computeIfAbsent(ident.getSimpleName(), n -> new ArrayList<>()).add(ident);
            JavaType type = referencedType(ident);
            if (type instanceof JavaType.Unknown || isAlias(ident)) {
                unresolved = true;
            } else if (type instanceof JavaType.Method || type instanceof JavaType.Variable) {
                addMember(type);
            } else {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
                if (fq != null) {
                    owners.add(normalize(fq.getPackageName()));
                    if (fq.getOwningClass() != null) {
                        memberOwners.add(fq.getOwningClass());
                    }
                }
            }
        }

        private void addImplicit(@Nullable JavaType type) {
            if (type != null) {
                implicitReferences.add(type);
                addMember(type);
            }
        }

        private void addMember(JavaType type) {
            JavaType.FullyQualified owner = type instanceof JavaType.Method ?
                    ((JavaType.Method) type).getDeclaringType() :
                    TypeUtils.asFullyQualified(((JavaType.Variable) type).getOwner());
            if (owner != null) {
                memberOwners.add(owner);
            }
        }

        /**
         * Whether a reference by {@code name} may be to {@code definition}, which it is unless both are typed
         * and the types disagree.
         */
        boolean uses(String name, @Nullable JavaType definition) {
            String definitionName = definitionName(definition);
            for (JavaType implicit : implicitReferences) {
                if (definitionName != null && definitionName.equals(definitionName(implicit))) {
                    return true;
                }
            }
            for (J.Identifier ident : identifiersByName.getOrDefault(name, emptyList())) {
                String referenced = isAlias(ident) ? null : definitionName(referencedType(ident));
                if (definitionName == null || referenced == null || referenced.equals(definitionName)) {
                    return true;
                }
            }
            return false;
        }

        boolean usesMemberOf(String qualifier, @Nullable JavaType qualifierType) {
            return unresolved || qualifierType instanceof JavaType.Unknown || owners.contains(qualifier) ||
                   memberOwners.stream().anyMatch(owner -> isMemberOf(owner, qualifier, qualifierType));
        }

        boolean usesImplicitMemberOf(String qualifier, @Nullable JavaType qualifierType) {
            for (JavaType implicit : implicitReferences) {
                JavaType.FullyQualified owner = implicit instanceof JavaType.Method ?
                        ((JavaType.Method) implicit).getDeclaringType() :
                        implicit instanceof JavaType.Variable ? TypeUtils.asFullyQualified(((JavaType.Variable) implicit).getOwner()) : null;
                if (owner != null && isMemberOf(owner, qualifier, qualifierType)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Whether a member declared on {@code owner} is reachable through {@code qualifier}, which holds when the
         * qualifier is the owner or inherits from it.
         */
        private static boolean isMemberOf(JavaType.FullyQualified owner, String qualifier, @Nullable JavaType qualifierType) {
            return owner(owner).equals(qualifier) ||
                   qualifierType instanceof JavaType.FullyQualified &&
                   TypeUtils.isAssignableTo(owner.getFullyQualifiedName(), qualifierType);
        }
    }
}
