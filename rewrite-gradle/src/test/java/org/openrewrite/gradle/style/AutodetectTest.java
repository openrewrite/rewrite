/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.test.RewriteTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"ConstantConditions"})
class AutodetectTest implements RewriteTest {
    @Test
    void gradleTabsAndIndents() {
        Stream<SourceFile> parse = GroovyParser.builder().build()
          .parse(
            """
              plugins {
                id 'groovy-gradle-plugin'
              }
              repositories {
                gradlePluginPortal() // so that external plugins can be resolved in dependencies section
              }

              dependencies {
                implementation 'io.freefair.gradle:lombok-plugin:8.4'
                implementation 'com.github.spotbugs.snom:spotbugs-gradle-plugin:5.0.13'
              }
              """
          );
        var detector = Autodetect.detector();
        parse.forEach(detector::sample);
        var styles = detector.build();
        assertThat(styles.getName()).isEqualTo("org.openrewrite.gradle.Autodetect");

        var tabsAndIndents = styles.getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(4);
    }
}
