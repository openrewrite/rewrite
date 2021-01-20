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

import org.openrewrite.Recipe;
import org.openrewrite.Validated;

import static org.openrewrite.Validated.required;

public class ChangePropertyKey extends Recipe {

    private String property;
    private String toProperty;

    public ChangePropertyKey() {
        this.processor = () -> new ChangePropertyKeyProcessor<>(property, toProperty);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setToProperty(String toProperty) {
        this.toProperty = toProperty;
    }

    @Override
    public Validated validate() {
        return required("property", property)
                .and(required("toProperty", toProperty));
    }
}
