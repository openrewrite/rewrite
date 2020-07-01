package org.openrewrite.yaml;

import org.openrewrite.RefactorVisitorSupport;
import org.openrewrite.Tree;
import org.openrewrite.yaml.tree.Yaml;

public class YamlRefactorVisitor extends YamlSourceVisitor<Yaml> implements RefactorVisitorSupport {
    @Override
    public Yaml defaultTo(Tree t) {
        return (Yaml) t;
    }

    @Override
    public Yaml visitDocuments(Yaml.Documents documents) {
        Yaml.Documents d = documents;
        d = d.withDocuments(refactor(d.getDocuments()));
        return d;
    }

    @Override
    public Yaml visitDocument(Yaml.Document document) {
        Yaml.Document d = document;
        d = d.withBlocks(refactor(d.getBlocks()));
        return d;
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping mapping) {
        Yaml.Mapping m = mapping;
        m = m.withEntries(refactor(m.getEntries()));
        return m;
    }

    @Override
    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry) {
        Yaml.Mapping.Entry e = entry;
        e = e.withKey(refactor(e.getKey()));
        e = e.withValue(refactor(e.getValue()));
        return e;
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence sequence) {
        Yaml.Sequence s = sequence;
        s = s.withEntries(refactor(s.getEntries()));
        return s;
    }

    @Override
    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry) {
        Yaml.Sequence.Entry e = entry;
        e = e.withBlock(refactor(e.getBlock()));
        return e;
    }
}
