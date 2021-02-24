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
package org.openrewrite.properties;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.properties.tree.Properties;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangePropertyValue extends Recipe {
    String propertyKey;
    String newValue;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangePropertyValueVisitor<>();
    }

    public class ChangePropertyValueVisitor<P> extends PropertiesVisitor<P> {

        public ChangePropertyValueVisitor() {
        }

        @Override
        public Properties visitEntry(Properties.Entry entry, P p) {
            if (entry.getKey().equals(propertyKey) && !entry.getValue().getText().equals(newValue)) {
                entry = entry.withValue(entry.getValue().withText(newValue));
            }
            return super.visitEntry(entry, p);
        }
    }

}
