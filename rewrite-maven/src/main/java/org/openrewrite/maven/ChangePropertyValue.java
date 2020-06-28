package org.openrewrite.maven;

import org.openrewrite.Validated;

import static org.openrewrite.Validated.required;

public class ChangePropertyValue extends MavenRefactorVisitor {
    private String key;
    private String toValue;

    public void setKey(String key) {
        this.key = key;
    }

    public void setToValue(String toValue) {
        this.toValue = toValue;
    }

    @Override
    public Maven visitProperty(Maven.Property property) {
        if(property.getKey().equals(key)) {
            andThen(new Scoped(property, toValue));
        }
        return super.visitProperty(property);
    }

    @Override
    public Validated validate() {
        return required("key", key)
                .and(required("toValue", toValue));
    }

    public static class Scoped extends MavenRefactorVisitor {
        private final Maven.Property property;
        private final String toValue;

        public Scoped(Maven.Property property, String toValue) {
            this.property = property;
            this.toValue = toValue;
        }

        @Override
        public Maven visitProperty(Maven.Property property) {
            Maven.Property p = refactor(property, super::visitProperty);
            if(this.property.isScope(p)) {
                p = p.withValue(toValue);
            }
            return p;
        }
    }
}
