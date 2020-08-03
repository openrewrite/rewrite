package org.openrewrite.yaml;

import org.openrewrite.AbstractSourceVisitor;
import org.openrewrite.yaml.tree.Yaml;

public abstract class AbstractYamlSourceVisitor<R> extends AbstractSourceVisitor<R>
    implements YamlSourceVisitor<R> {

    public R visitDocuments(Yaml.Documents documents) {
        return reduce(
                defaultTo(documents),
                visit(documents.getDocuments())
        );
    }

    public R visitDocument(Yaml.Document document) {
        return reduce(
                defaultTo(document),
                visit(document.getBlocks())
        );
    }

    public R visitSequence(Yaml.Sequence sequence) {
        return reduce(
                defaultTo(sequence),
                visit(sequence.getEntries())
        );
    }

    public R visitSequenceEntry(Yaml.Sequence.Entry entry) {
        return reduce(
                defaultTo(entry),
                visit(entry.getBlock())
        );
    }

    public R visitMapping(Yaml.Mapping mapping) {
        return reduce(
                defaultTo(mapping),
                visit(mapping.getEntries())
        );
    }

    public R visitMappingEntry(Yaml.Mapping.Entry entry) {
        return reduce(
                defaultTo(entry),
                reduce(
                        visit(entry.getKey()),
                        visit(entry.getValue())
                )
        );
    }

    public R visitScalar(Yaml.Scalar scalar) {
        return defaultTo(scalar);
    }
}
