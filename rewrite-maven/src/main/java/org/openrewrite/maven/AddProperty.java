/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Alex Boyko
 */
package org.openrewrite.maven;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

public final class AddProperty extends Recipe {

    private static final XPathMatcher PROPERTIES_MATCHER = new XPathMatcher("/project/properties");

    @Option(
        displayName = "Key",
        description = "The name of the property key.",
        example = "junit.version"
    )
    private final String key;
    @Option(
        displayName = "Value",
        description = "The value of the property",
        example = "4.13"
    )
    private final String newValue;

    public String getDisplayName() {
        return "Change Maven project property value";
    }

    public String getDescription() {
        return "Changes the specified Maven project property value leaving the key intact.";
    }

    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddPropertyVisitor();
    }

    public AddProperty(String key, String newValue) {
        if (key != null) {
            key = key.replace("${", "").replace("}", "");
        }

        this.key = key;
        this.newValue = newValue;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.newValue;
    }

    @NonNull
    public String toString() {
        return "ChangePropertyValue(key=" + this.getKey() + ", newValue=" + this.getValue() + ")";
    }

    public boolean equals(@Nullable final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof AddProperty)) {
            return false;
        } else {
            AddProperty other = (AddProperty)o;
            if (!other.canEqual(this)) {
                return false;
            } else if (!super.equals(o)) {
                return false;
            } else {
                Object this$key = this.getKey();
                Object other$key = other.getKey();
                if (this$key == null) {
                    if (other$key != null) {
                        return false;
                    }
                } else if (!this$key.equals(other$key)) {
                    return false;
                }

                Object this$newValue = this.getValue();
                Object other$newValue = other.getValue();
                if (this$newValue == null) {
                    if (other$newValue != null) {
                        return false;
                    }
                } else if (!this$newValue.equals(other$newValue)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(@Nullable final Object other) {
        return other instanceof AddProperty;
    }

    public int hashCode() {
        int result = super.hashCode();
        Object $key = this.getKey();
        result = result * 59 + ($key == null ? 43 : $key.hashCode());
        Object $newValue = this.getValue();
        result = result * 59 + ($newValue == null ? 43 : $newValue.hashCode());
        return result;
    }

    private class AddPropertyVisitor extends MavenVisitor {

        private boolean done = false;

        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isPropertyTag()) {
                return visitPropertyTag(tag, ctx);
            } else if (PROPERTIES_MATCHER.matches(getCursor())) {
                return visitPropertiesTag(tag, ctx);
            } else {
                return super.visitTag(tag, ctx);
            }
        }

        private Xml visitPropertiesTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag properties = (Xml.Tag) super.visitTag(tag, ctx);
            if (!done) {
                doAfterVisit(new AddToTagVisitor(properties, Xml.Tag.build(generateProperty())));
                done = true;
            }
            return properties;
        }

        private String generateProperty() {
            StringBuilder sb = new StringBuilder();
            sb.append("<");
            sb.append(getKey());
            sb.append(">");
            sb.append(getValue());
            sb.append("</");
            sb.append(getKey());
            sb.append(">");
            return sb.toString();
        }

        private Xml visitPropertyTag(Xml.Tag tag, ExecutionContext ctx) {
            if (AddProperty.this.key.equals(tag.getName())) {
                if (!AddProperty.this.newValue.equals(tag.getValue().orElse(null))) {
                    this.doAfterVisit(new ChangeTagValueVisitor(tag, AddProperty.this.newValue));
                }
                done = true;
            }
            return super.visitTag(tag, ctx);
        }
    }
}
