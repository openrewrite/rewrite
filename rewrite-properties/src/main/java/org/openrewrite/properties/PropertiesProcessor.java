package org.openrewrite.properties;

import org.openrewrite.TreeProcessor;
import org.openrewrite.properties.tree.Properties;

public class PropertiesProcessor<P> extends TreeProcessor<Properties, P> implements PropertiesVisitor<Properties, P> {

    @Override
    public Properties visitFile(Properties.File file, P p) {
        Properties.File f = call(file, p, this::visitEach);
        return f.withContent(call(file.getContent(), p));
    }

    @Override
    public Properties visitEntry(Properties.Entry entry, P p) {
        return call(entry, p, this::visitEach);
    }

    @Override
    public Properties visitComment(Properties.Comment comment, P p) {
        return call(comment, p, this::visitEach);
    }
}
