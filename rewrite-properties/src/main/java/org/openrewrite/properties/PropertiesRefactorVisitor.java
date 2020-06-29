package org.openrewrite.properties;

import org.openrewrite.RefactorVisitorSupport;
import org.openrewrite.Tree;
import org.openrewrite.properties.tree.Properties;

public class PropertiesRefactorVisitor extends PropertiesSourceVisitor<Properties> implements RefactorVisitorSupport {
    @Override
    public Properties defaultTo(Tree t) {
        return (Properties) t;
    }

    @Override
    public Properties visitFile(Properties.File file) {
        Properties.File f = refactor(file, super::visitFile);
        f = f.withContent(refactor(f.getContent()));
        return f;
    }
}
