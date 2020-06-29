package org.openrewrite.properties;

import org.openrewrite.Validated;
import org.openrewrite.properties.tree.Properties;

import static org.openrewrite.Validated.required;

public class ChangePropertyKey extends PropertiesRefactorVisitor {
    private String key;
    private String toKey;

    public void setKey(String key) {
        this.key = key;
    }

    public void setToKey(String toKey) {
        this.toKey = toKey;
    }

    @Override
    public Validated validate() {
        return required("key", key)
                .and(required("toKey", toKey));
    }

    @Override
    public Properties visitEntry(Properties.Entry entry) {
        Properties.Entry e = refactor(entry, super::visitEntry);
        if (e.getKey().equals(key)) {
            e = e.withKey(toKey);
        }
        return e;
    }
}
