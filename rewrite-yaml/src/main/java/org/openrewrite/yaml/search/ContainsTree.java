package org.openrewrite.yaml.search;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.openrewrite.internal.ListUtils.map;
import static org.openrewrite.yaml.MappingUtils.keyMatches;

public class ContainsTree<Y extends Yaml> extends YamlIsoVisitor<Set<YamlSearchResult>> {

    public static YamlSearchResult MATCHES = new YamlSearchResult(Tree.randomId(), null, null);
    public static YamlSearchResult DOES_NOT_MATCH = new YamlSearchResult(Tree.randomId(), null, null);

    private final Y scope;
    private final Y searchTree;
    private final XPathMatcher matcher;

    public ContainsTree(Y scope, Y searchTree, @Nullable XPathMatcher matcher) {
        this.scope = scope;
        this.searchTree = searchTree;
        this.matcher = matcher;
    }

    public ContainsTree(Y scope, Y searchTree, @NonNull String matcherPath) {
        this(scope, searchTree, "/".equals(matcherPath) ? null : new XPathMatcher(matcherPath));
    }

    public ContainsTree(Y scope, Y searchTree) {
        this(scope, searchTree, "/");
    }

    @Override
    public Yaml.Documents visitDocuments(Yaml.Documents documents, Set<YamlSearchResult> results) {
        if (!scope.isScope(documents)) {
            return super.visitDocuments(documents, results);
        }

        documents.getDocuments().forEach(doc ->
                new ContainsTree<>(doc, this.<Yaml.Documents>getSearchTree().getDocuments().get(0), matcher)
                        .visit(doc, results, getCursor()));

        return documents;
    }

    @Override
    public Yaml.Document visitDocument(Yaml.Document document, Set<YamlSearchResult> results) {
        if (!scope.isScope(document)) {
            return super.visitDocument(document, results);
        } else if (doesNotMatch(results)) {
            return document;
        }
        return document.withBlock((Yaml.Block) new ContainsTree<>(document.getBlock(), this.<Yaml.Document>getSearchTree().getBlock(), matcher)
                .visit(document.getBlock(), results, getCursor()));
    }

    @Override
    public Yaml.Scalar visitScalar(Yaml.Scalar scalar, Set<YamlSearchResult> results) {
        if (!scope.isScope(scalar)) {
            return super.visitScalar(scalar, results);
        } else if (doesNotMatch(results)) {
            return scalar;
        }

        String scalarValue = scalar.getValue();
        if (!scalarValue.equals(this.<Yaml.Scalar>getSearchTree().getValue())) {
            if (!"_".equals(scalarValue)) {
                results.add(ContainsTree.DOES_NOT_MATCH);
            }
        }

        return super.visitScalar(scalar, results);
    }

    @Override
    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, Set<YamlSearchResult> results) {
        if (!scope.isScope(mapping)) {
            return super.visitMapping(mapping, results);
        } else if (doesNotMatch(results)) {
            return mapping;
        }

        List<Yaml.Mapping.Entry> filterEntries = this.<Yaml.Mapping>getSearchTree().getEntries();
        List<Yaml.Mapping.Entry> childEntries = map(mapping.getEntries(), childEntry ->
                filterEntries.stream()
                        .filter(fe -> keyMatches(fe, childEntry))
                        .findFirst()
                        .map(fe -> {
                            return fe.withValue((Yaml.Block) new ContainsTree<>(childEntry.getValue(), fe.getValue(), matcher)
                                    .visit(childEntry.getValue(), results, getCursor()));
                        })
                        .orElse(childEntry));

        return mapping.withEntries(childEntries);
    }

    @Override
    public Yaml.Sequence visitSequence(Yaml.Sequence sequence, Set<YamlSearchResult> results) {
        if (!scope.isScope(sequence)) {
            return super.visitSequence(sequence, results);
        } else if (doesNotMatch(results)) {
            return sequence;
        }

        AtomicInteger idx = new AtomicInteger(0);
        List<Yaml.Sequence.Entry> filterEntries = this.<Yaml.Sequence>getSearchTree().getEntries();
        List<Yaml.Sequence.Entry> childEntries = map(sequence.getEntries(), childEntry -> {
            Yaml.Sequence.Entry filterEntry = filterEntries.get(idx.getAndIncrement());
            return childEntry.withBlock((Yaml.Block) new ContainsTree<>(childEntry.getBlock(), filterEntry.getBlock(), matcher)
                    .visit(childEntry.getBlock(), results, getCursor()));
        });

        return sequence.withEntries(childEntries);
    }

    @SuppressWarnings("unchecked")
    private <V extends Tree> V getSearchTree() {
        return (V) searchTree;
    }

    public static boolean doesNotMatch(Set<YamlSearchResult> results) {
        return results.stream().anyMatch(r -> r == ContainsTree.DOES_NOT_MATCH);
    }

}
