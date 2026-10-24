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
package org.openrewrite.gradle.toolingapi;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class OpenRewriteModelBuilderTest {

    @Test
    void theBundledInitScriptResolvesThePluginThroughTheMirror() throws Exception {
        String script = OpenRewriteModelBuilder.throughMirror(bundledInitScript(),
          "https://mirror.example.com/maven/", "ci-user", "ci-'token");

        assertThat(script)
          .doesNotContain("mavenCentral()", "central.sonatype.com")
          .contains(
            "mavenLocal()",
            "url = uri('https://mirror.example.com/maven/')",
            "username = 'ci-user'",
            "password = 'ci-\\'token'",
            "classpath 'org.openrewrite.gradle.tooling:plugin:latest.integration'");
    }

    @Test
    void withoutAWholeMirrorTheBundledInitScriptIsUnchanged() throws Exception {
        String script = bundledInitScript();

        assertThat(OpenRewriteModelBuilder.throughMirror(script, null, null, null)).isEqualTo(script);
        assertThat(OpenRewriteModelBuilder.throughMirror(script, "https://mirror.example.com/maven/", "ci-user", ""))
          .isEqualTo(script);
    }

    private static String bundledInitScript() throws Exception {
        URL initScript = OpenRewriteModel.class.getResource("/init.gradle");
        assertThat(initScript).isNotNull();
        return new String(Files.readAllBytes(Paths.get(initScript.toURI())), StandardCharsets.UTF_8);
    }
}
