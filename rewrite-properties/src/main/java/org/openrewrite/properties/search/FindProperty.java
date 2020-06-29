package org.openrewrite.properties.search;

import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.properties.PropertiesSourceVisitor;
import org.openrewrite.properties.tree.Properties;

import static org.openrewrite.Validated.required;

public class FindProperty extends PropertiesSourceVisitor<Properties.Entry> {
    private String key;

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public Validated validate() {
        return required("key", key);
    }

    @Override
    public Properties.Entry defaultTo(Tree t) {
        return null;
    }

    @Override
    public Properties.Entry visitEntry(Properties.Entry entry) {
        if (entry.getKey().equals(key)) {
            return entry;
        }
        return super.visitEntry(entry);
    }
}
