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
package org.openrewrite.marker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.openrewrite.internal.ObjectMappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class MarkupSerializationTest {
    private static final ObjectMapper mapper = ObjectMappers.propertyBasedMapper(null);

    @Test
    void recipeNameSurvivesRoundTrip() throws Exception {
        for (Markup markup : List.<Markup>of(
          new Markup.Error(randomId(), "m", "d", "org.example.A"),
          new Markup.Warn(randomId(), "m", "d", "org.example.A"),
          new Markup.Info(randomId(), "m", "d", "org.example.A"),
          new Markup.Debug(randomId(), "m", "d", "org.example.A"))) {
            Markup read = mapper.readValue(mapper.writeValueAsString(markup), markup.getClass());
            assertThat(read.getRecipeName()).isEqualTo("org.example.A");
        }
    }

    @Test
    void markupSerializedBeforeRecipeNameExistedIsUnattributed() throws Exception {
        ObjectNode json = mapper.valueToTree(new Markup.Warn(randomId(), "m", "d", "org.example.A"));
        json.remove("recipeName");
        assertThat(mapper.treeToValue(json, Markup.Warn.class).getRecipeName()).isNull();
    }

    @Test
    void recipeNameDoesNotAffectEquality() {
        var id = randomId();
        assertThat(new Markup.Warn(id, "m", "d", "org.example.A"))
          .isEqualTo(new Markup.Warn(id, "m", "d", "org.example.B"));
    }
}
