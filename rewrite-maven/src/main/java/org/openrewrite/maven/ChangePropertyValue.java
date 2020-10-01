/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven;

import org.openrewrite.Validated;
import org.openrewrite.maven.tree.Maven;

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
