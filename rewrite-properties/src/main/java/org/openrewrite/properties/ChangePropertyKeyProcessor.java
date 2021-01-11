package org.openrewrite.properties;

import org.openrewrite.properties.tree.Properties;

public class ChangePropertyKeyProcessor<P> extends PropertiesProcessor<P> {

    private final String property;
    private final String toProperty;

    public ChangePropertyKeyProcessor(String property, String toProperty) {
        this.property = property;
        this.toProperty = toProperty;
    }

    @Override
    public Properties visitEntry(Properties.Entry entry, P p) {
        if (entry.getKey().equals(property)) {
            entry = entry.withKey(toProperty);
        }
        return super.visitEntry(entry, p);
    }
}
