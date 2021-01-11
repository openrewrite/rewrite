package org.openrewrite.properties;

import org.openrewrite.properties.tree.Properties;

public class ChangePropertyValueProcessor<P> extends PropertiesProcessor<P> {

    private final String key;
    private final String toValue;

    public ChangePropertyValueProcessor(String key, String toValue) {
        this.key = key;
        this.toValue = toValue;
    }

    @Override
    public Properties visitEntry(Properties.Entry entry, P p) {
        if (entry.getKey().equals(key) && !entry.getValue().getText().equals(toValue)) {
            entry = entry.withValue(entry.getValue().withText(toValue));
        }
        return super.visitEntry(entry, p);
    }
}
