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

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;
import org.openrewrite.text.ChangeText;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileTest {
    @Test
    void configureSourceVisitor() {
        Profile profile = new Profile().setConfigure(
                Map.of("org.openrewrite.text.ChangeText",
                        Map.of("toText", "Hello Jon!")
                )
        );

        ChangeText changeText = new ChangeText();
        assertThat(profile.configure(changeText).getToText()).isEqualTo("Hello Jon!");
    }

    @Test
    void defineDeclarativeVisitor() {
        Profile profile = new Profile().setDefine(
                Map.of("org.openrewrite.text.ChangeTextTwice",
                        List.of(
                                Map.of("org.openrewrite.text.ChangeText",
                                        Map.of(
                                                "toText", "Hello Jon"
                                        )
                                ),
                                Map.of("text.ChangeText",
                                        Map.of(
                                                "toText", "Hello Jonathan!"
                                        )
                                )
                        )
                )
        );

        assertThat(profile.getDefinitions()).hasSize(1);

        CompositeSourceVisitor<?> declarative = profile.getDefinitions().iterator().next();
        assertThat(declarative.getTags()).contains(Tag.of("name", "org.openrewrite.text.ChangeTextTwice"));
    }

    @Test
    void propertyNameCombinedWithVisitorName() {
        Profile profile = new Profile().setConfigure(Map.of("org.openrewrite.text.ChangeText.toText", "Hello Jon!"));
        ChangeText changeText = new ChangeText();
        assertThat(profile.configure(changeText).getToText()).isEqualTo("Hello Jon!");
    }

    @Test
    void propertyNameCombinedWithWildcardVisitor() {
        Profile profile = new Profile().setConfigure(Map.of("org.openrewrite.text.*.toText", "Hello Jon!"));
        ChangeText changeText = new ChangeText();
        assertThat(profile.configure(changeText).getToText()).isEqualTo("Hello Jon!");
    }

    @Test
    void splitPackageWildcard() {
        Profile profile = new Profile().setConfigure(
                Map.of("org.openrewrite",
                        Map.of("text.*",
                                Map.of("TOTEXT", "Hello Jon!")
                        )
                )
        );

        ChangeText changeText = new ChangeText();
        assertThat(profile.configure(changeText).getToText()).isEqualTo("Hello Jon!");
    }

    @Test
    void splitPackage() {
        Profile profile = new Profile().setConfigure(
                Map.of("org.openrewrite",
                        Map.of("text.ChangeText",
                                Map.of("TOTEXT", "Hello Jon!")
                        )
                )
        );

        ChangeText changeText = new ChangeText();
        assertThat(profile.configure(changeText).getToText()).isEqualTo("Hello Jon!");
    }
}
