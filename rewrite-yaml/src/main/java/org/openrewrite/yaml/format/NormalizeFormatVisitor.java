package org.openrewrite.yaml.format;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

@RequiredArgsConstructor
public class NormalizeFormatVisitor<P> extends YamlIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);
        if (!e.getKey().getPrefix().isEmpty()) {
            return e
                    .withPrefix(e.getPrefix() + e.getKey().getPrefix())
                    .withKey(e.getKey().withPrefix(""));
        }
        return e;
    }

    @Nullable
    @Override
    public Yaml postVisit(Yaml tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Yaml.Documents.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Nullable
    @Override
    public Yaml visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Yaml) tree;
        }
        return super.visit(tree, p);
    }
}
