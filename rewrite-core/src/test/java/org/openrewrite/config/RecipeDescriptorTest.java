/*
 * Copyright 2026 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeDescriptorTest {

    /**
     * Polyglot RPC peers (rewrite-javascript-remote, rewrite-csharp-remote,
     * rewrite-python-remote) may omit empty collection-valued fields from the
     * descriptor JSON they return. Java callers iterate {@code getPreconditions()},
     * {@code getRecipeList()}, etc. without null checks, so deserialization must
     * normalize missing fields to empty collections to preserve the "never null"
     * contract that {@link org.openrewrite.Recipe#createRecipeDescriptor()} maintains
     * for locally constructed descriptors.
     */
    @Test
    void deserializeOmittedCollectionsAsEmpty() throws Exception {
        String json = """
                {
                  "name": "org.example.Foo",
                  "displayName": "Foo",
                  "instanceName": "Foo",
                  "description": "A recipe."
                }
                """;

        RecipeDescriptor descriptor = new ObjectMapper().readValue(json, RecipeDescriptor.class);

        assertThat(descriptor.getTags()).isEmpty();
        assertThat(descriptor.getOptions()).isEmpty();
        assertThat(descriptor.getPreconditions()).isEmpty();
        assertThat(descriptor.getRecipeList()).isEmpty();
        assertThat(descriptor.getDataTables()).isEmpty();
        assertThat(descriptor.getMaintainers()).isEmpty();
        assertThat(descriptor.getContributors()).isEmpty();
        assertThat(descriptor.getExamples()).isEmpty();
    }
}
