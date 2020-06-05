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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.config.Profile;
import org.openrewrite.config.ProfileSource;
import org.openrewrite.text.PlainText;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Formatting.EMPTY;
import static org.openrewrite.Tree.randomId;

public class RefactorTest {
    @Test
    void scanAutoConfigurableRules() {
        ProfileSource fixedProfileSource = () -> singletonList(new Profile()
                .setInclude(Set.of("*"))
                .setConfigure(Map.of("org.openrewrite.text.ChangeText.toText", "Hello Jon!"))
        );

        Environment environment = new Environment().load(fixedProfileSource);

        PlainText fixed = new PlainText(randomId(), "Hello World!", EMPTY)
                .refactor()
                .environment(environment)
                .scan("org.openrewrite")
                .fix().getFixed();

        assertThat(fixed.print()).isEqualTo("Hello Jon!");
    }
}
