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
package org.openrewrite.groovy.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"ConstantConditions"})
class AutodetectTest implements RewriteTest {
    @Test
    void groovyTabsAndIndents() {
        Stream<SourceFile> parse = GroovyParser.builder().build()
          .parse(
            """
              class Example {
                  static void main(String[] args) {
                     String name = "Joe"
                     int ID = 1
                     println(name + ID )
                  }
               }
              """
          );
        var detector = Autodetect.detector();
        parse.forEach(detector::sample);
        var styles = detector.build();
        assertThat(styles.getName()).isEqualTo("org.openrewrite.groovy.Autodetect");

        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }
}
