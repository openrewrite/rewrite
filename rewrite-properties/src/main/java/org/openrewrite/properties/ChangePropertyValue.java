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
        if (e.getKey().equals(key) && !e.getValue().getText().equals(toValue)) {
            e = e.withValue(e.getValue().withText(toValue));
        }
        return e;
    }
}
