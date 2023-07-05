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
package org.openrewrite.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openrewrite.Validated;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class DeclarativeNamedStyles extends NamedStyles {
    @JsonIgnore
    private Validated<Object> validation = Validated.none();

    public DeclarativeNamedStyles(UUID id, String name, String displayName, String description, Set<String> tags, Collection<Style> styles) {
        super(id, name, displayName, description, tags, styles);
    }

    void addValidation(Validated<Object> validated) {
        validation = validation.and(validated);
    }

    @Override
    public Validated<Object> validate() {
        return validation;
    }
}
