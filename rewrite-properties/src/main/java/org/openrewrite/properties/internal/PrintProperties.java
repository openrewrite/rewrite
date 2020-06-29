package org.openrewrite.properties.internal;

import org.openrewrite.Tree;
import org.openrewrite.properties.PropertiesSourceVisitor;
import org.openrewrite.properties.tree.Properties;

public class PrintProperties extends PropertiesSourceVisitor<String> {
    @Override
    public String defaultTo(Tree t) {
        return "";
    }

    @Override
    public String visitFile(Properties.File file) {
        return file.getFormatting().getPrefix() + visit(file.getContent()) + file.getFormatting().getSuffix();
    }

    @Override
    public String visitEntry(Properties.Entry entry) {
        return entry.getFormatting().getPrefix() + entry.getKey() +
                entry.getEqualsFormatting().getPrefix() + "=" + entry.getEqualsFormatting().getSuffix() +
                entry.getValue() +
                entry.getFormatting().getSuffix();
    }
}
