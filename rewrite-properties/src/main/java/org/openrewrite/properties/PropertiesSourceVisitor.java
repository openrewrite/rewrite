package org.openrewrite.properties;

import org.openrewrite.SourceVisitor;
import org.openrewrite.properties.tree.Properties;

public abstract class PropertiesSourceVisitor<R> extends SourceVisitor<R> {
    public R visitFile(Properties.File file) {
        return reduce(
                defaultTo(file),
                visit(file.getContent())
        );
    }

    public R visitEntry(Properties.Entry entry) {
        return defaultTo(entry);
    }

    public R visitComment(Properties.Comment comment) {
        return defaultTo(comment);
    }
}
