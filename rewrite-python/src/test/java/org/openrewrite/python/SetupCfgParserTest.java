/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class SetupCfgParserTest {

    @Test
    void acceptsSetupCfg() {
        SetupCfgParser parser = new SetupCfgParser();
        assertThat(parser.accept(Paths.get("setup.cfg"))).isTrue();
        assertThat(parser.accept(Paths.get("pyproject.toml"))).isFalse();
        assertThat(parser.accept(Paths.get("requirements.txt"))).isFalse();
        assertThat(parser.accept(Paths.get("other.cfg"))).isFalse();
    }

    @Test
    void builderCreatesDslName() {
        SetupCfgParser.Builder builder = SetupCfgParser.builder();
        assertThat(builder.getDslName()).isEqualTo("setup.cfg");
        assertThat(builder.build()).isInstanceOf(SetupCfgParser.class);
    }
}
