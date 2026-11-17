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

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.scala.marker.CommaContinuation;
import org.openrewrite.scala.marker.OmitImportBraces;
import org.openrewrite.scala.tree.S;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static org.openrewrite.scala.RemoveUnusedImports.*;

public class OrderImports extends Recipe {
    private static final Pattern JAVA_AND_SCALA = Pattern.compile("^(javax?|scala)\\.");
    private static final int RELATIVE = 2;

    @Getter
    final String displayName = "Order Scala imports";

    @Getter
    final String description = "Groups and sorts each run of adjacent imports the way scalafix's `OrganizeImports` " +
            "and IntelliJ IDEA's Scala plugin do by default: other imports first, then `java`, `javax` and `scala` " +
            "imports, with a blank line between the groups and each sorted by ASCII. Imports never move past other " +
            "statements, and are neither merged, split nor folded into wildcards.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ScalaIsoVisitor<ExecutionContext>() {
            @Override
            public S.CompilationUnit visitCompilationUnit(S.CompilationUnit cu, ExecutionContext ctx) {
                S.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
                return c.getPadding().withStatements(order(c.getPadding().getStatements()));
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                return b.getPadding().withStatements(order(b.getPadding().getStatements()));
            }

            @Override
            public S.Import visitSImport(S.Import anImport, ExecutionContext ctx) {
                return sortSelectors((S.Import) super.visitSImport(anImport, ctx));
            }
        };
    }

    private static List<JRightPadded<Statement>> order(List<JRightPadded<Statement>> statements) {
        List<JRightPadded<Statement>> result = new ArrayList<>(statements.size());
        int i = 0;
        while (i < statements.size()) {
            if (!isImport(statements.get(i).getElement())) {
                result.add(statements.get(i++));
                continue;
            }
            List<List<JRightPadded<Statement>>> clauses = new ArrayList<>();
            while (i < statements.size() && isImport(statements.get(i).getElement())) {
                if (clauses.isEmpty() || !statements.get(i).getElement().getMarkers().findFirst(CommaContinuation.class).isPresent()) {
                    clauses.add(new ArrayList<>());
                }
                clauses.get(clauses.size() - 1).add(statements.get(i++));
            }
            result.addAll(orderRun(clauses));
        }
        for (int j = 0; j < result.size(); j++) {
            if (result.get(j) != statements.get(j)) {
                return result;
            }
        }
        return statements;
    }

    /**
     * Sorts a run of adjacent import clauses, each an import and its comma-separated continuations. The prefix of
     * the first clause belongs to the run's position, so it stays first.
     */
    private static List<JRightPadded<Statement>> orderRun(List<List<JRightPadded<Statement>>> clauses) {
        if (clauses.size() < 2) {
            return clauses.isEmpty() ? Collections.emptyList() : clauses.get(0);
        }
        Set<String> boundNames = new HashSet<>();
        for (List<JRightPadded<Statement>> clause : clauses) {
            for (JRightPadded<Statement> rp : clause) {
                boundNames.addAll(boundNames(rp.getElement()));
            }
        }
        List<List<JRightPadded<Statement>>> sorted = new ArrayList<>(clauses);
        sorted.sort(comparing(c -> c.get(0).getElement(), order(boundNames)));

        Statement originalFirst = clauses.get(0).get(0).getElement();
        Space first = originalFirst.getPrefix();
        String indent = indent(first);
        List<JRightPadded<Statement>> result = new ArrayList<>();
        int previousGroup = -1;
        for (List<JRightPadded<Statement>> clause : sorted) {
            Statement head = clause.get(0).getElement();
            int group = group(head, boundNames);
            Space prefix;
            if (result.isEmpty()) {
                prefix = head == originalFirst ? first : inheritPrefix(first, head.getPrefix());
            } else {
                String whitespace = (group == previousGroup ? "\n" : "\n\n") + indent;
                prefix = head == originalFirst ?
                        Space.build(whitespace, Collections.emptyList()) :
                        head.getPrefix().withWhitespace(whitespace);
            }
            boolean samePrefix = prefix.getWhitespace().equals(head.getPrefix().getWhitespace()) &&
                                 prefix.getComments().equals(head.getPrefix().getComments());
            result.add(samePrefix ? clause.get(0) : clause.get(0).withElement(head.withPrefix(prefix)));
            result.addAll(clause.subList(1, clause.size()));
            previousGroup = group;
        }
        return result;
    }

    /**
     * Groups imports the way scalafix's {@code OrganizeImports} does by default, with an import relative to a name
     * another import in the run binds (like {@code mutable.ListBuffer} after {@code scala.collection.mutable}) last,
     * as it only compiles after that import.
     */
    static int group(Statement anImport, Set<String> boundNames) {
        String path = importPath(anImport);
        int dot = path.indexOf('.');
        if (dot > 0 && boundNames.contains(path.substring(0, dot))) {
            return RELATIVE;
        }
        return JAVA_AND_SCALA.matcher(path).find() ? 1 : 0;
    }

    /**
     * Orders imports by group, then by ASCII, except for relative imports, which stay in source order since one may
     * be relative to a name another binds, as {@code atomic.AtomicInteger} after {@code concurrent.atomic}.
     */
    static Comparator<Statement> order(Set<String> boundNames) {
        return Comparator.<Statement>comparingInt(s -> group(s, boundNames))
                .thenComparing((a, b) -> group(a, boundNames) == RELATIVE ? 0 : key(a).compareTo(key(b)));
    }

    private static String key(Statement anImport) {
        if (anImport instanceof J.Import) {
            return importPath(anImport);
        }
        S.Import i = (S.Import) anImport;
        return importPath(i) + ".{" + i.getSelectors().stream().map(OrderImports::selectorName).collect(joining(",")) + "}";
    }

    private static String importPath(Statement anImport) {
        return RemoveUnusedImports.path(anImport instanceof J.Import ?
                ((J.Import) anImport).getQualid() : ((S.Import) anImport).getQualifier());
    }

    static Set<String> boundNames(Statement anImport) {
        Set<String> names = new HashSet<>();
        if (anImport instanceof J.Import) {
            String name = ((J.Import) anImport).getQualid().getSimpleName();
            if (!isWildcard(name)) {
                names.add(name);
            }
        } else if (anImport instanceof S.Import) {
            for (S.ImportSelector selector : ((S.Import) anImport).getSelectors()) {
                J.Identifier bound = selector.getAlias() != null ? selector.getAlias() : selector.getName();
                if (bound != null && !isHiding(selector)) {
                    names.add(bound.getSimpleName());
                }
            }
        }
        return names;
    }

    /**
     * Sorts named selectors by ASCII ahead of wildcard and {@code given} selectors, which Scala requires last. The
     * padding stays with each position.
     */
    private static S.Import sortSelectors(S.Import anImport) {
        if (anImport.getMarkers().findFirst(OmitImportBraces.class).isPresent()) {
            return anImport;
        }
        List<JRightPadded<S.ImportSelector>> padded = anImport.getPadding().getSelectors().getPadding().getElements();
        List<S.ImportSelector> sorted = new ArrayList<>(anImport.getSelectors());
        sorted.sort(comparing((S.ImportSelector s) -> s.getName() == null)
                .thenComparing(s -> s.getName() == null ? "" : s.getName().getSimpleName()));
        if (sorted.equals(anImport.getSelectors())) {
            return anImport;
        }
        List<JRightPadded<S.ImportSelector>> result = new ArrayList<>(padded.size());
        for (int i = 0; i < padded.size(); i++) {
            result.add(padded.get(i).withElement(sorted.get(i).withPrefix(padded.get(i).getElement().getPrefix())));
        }
        return anImport.getPadding().withSelectors(anImport.getPadding().getSelectors().getPadding().withElements(result));
    }

    private static String selectorName(S.ImportSelector selector) {
        if (selector.getName() != null) {
            return selector.getName().getSimpleName();
        }
        return selector.isGiven() ? "given" : selector.isLegacyUnderscore() ? "_" : "*";
    }

    private static String indent(Space prefix) {
        String whitespace = prefix.getComments().isEmpty() ? prefix.getWhitespace() :
                prefix.getComments().get(prefix.getComments().size() - 1).getSuffix();
        return whitespace.substring(whitespace.lastIndexOf('\n') + 1);
    }
}
