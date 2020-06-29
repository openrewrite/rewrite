package org.openrewrite.properties;

import org.openrewrite.Validated;
import org.openrewrite.properties.tree.Properties;

import static org.openrewrite.Validated.required;

public class ChangePropertyValue extends PropertiesRefactorVisitor {
    private String key;
    private String toValue;

    public void setKey(String key) {
        this.key = key;
    }

    public void setToValue(String toValue) {
        this.toValue = toValue;
    }

    @Override
    public Validated validate() {
        return required("key", key)
                .and(required("toValue", toValue));
    }

    @Override
    public Properties visitEntry(Properties.Entry entry) {
        Properties.Entry e = refactor(entry, super::visitEntry);
        if (e.getKey().equals(key) && !e.getValue().equals(toValue)) {
            e = e.withValue(toValue);
        }
        return e;
    }
}
