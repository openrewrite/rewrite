package org.openrewrite.properties.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.properties.PropertiesProcessor;
import org.openrewrite.properties.tree.Properties;

public class FindPropertyProcessor extends PropertiesProcessor<ExecutionContext> {

    private final String key;

    public FindPropertyProcessor(String key) {
        this.key = key;
    }

    @Override
    public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
        if (entry.getKey().equals(key)) {
            return entry;
        }
        return super.visitEntry(entry, ctx);
    }
}
