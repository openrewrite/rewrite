/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.openrewrite.text.ChangeText;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeBasicsTest {

    @Test
    void cloneRecipe() throws JsonMappingException {
        ChangeText ct = new ChangeText("hi");
        ChangeText ct2 = (ChangeText) ct.clone();
        ObjectMapper mapper = new ObjectMapper();
        mapper.updateValue(ct2, new HashMap<String, String>() {{
            put("toText", "hello");
        }});

        assertThat(ct2).isNotSameAs(ct);
        assertThat(ct.getToText()).isEqualTo("hi");
        assertThat(ct2.getToText()).isEqualTo("hello");
    }

    @Test
    void instanceName() {
        ChangeText ct = new ChangeText("hi");
        assertThat(ct.getInstanceName()).isEqualTo("Change text to `hi`");
    }
}
