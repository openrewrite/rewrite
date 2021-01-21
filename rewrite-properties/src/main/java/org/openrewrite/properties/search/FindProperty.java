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

import lombok.Data;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeProcessor;
import org.openrewrite.Validated;
import org.openrewrite.properties.PropertiesProcessor;
import org.openrewrite.properties.tree.Properties;

import static org.openrewrite.Validated.required;

@Data
public class FindProperty extends Recipe {

    private final String key;

    @Override
    public Validated validate() {
        return required("key", key);
    }

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new FindPropertyProcessor();
    }

    public class FindPropertyProcessor extends PropertiesProcessor<ExecutionContext> {

        @Override
        public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
            if (entry.getKey().equals(key)) {
                return entry;
            }
            return super.visitEntry(entry, ctx);
        }
    }

}

