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
package org.openrewrite.properties.search;

import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.properties.AbstractPropertiesSourceVisitor;
import org.openrewrite.properties.PropertiesSourceVisitor;
import org.openrewrite.properties.tree.Properties;

import static org.openrewrite.Validated.required;

public class FindProperty extends AbstractPropertiesSourceVisitor<Properties.Entry> {
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
