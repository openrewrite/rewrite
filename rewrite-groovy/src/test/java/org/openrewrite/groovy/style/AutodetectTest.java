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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

        var tabsAndIndents = styles.getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }
    @Test
    void mergedAndSerializedDetectorsMatchOneDetectorThatSampledEverything() throws Exception {
        List<SourceFile> sources = GroovyParser.builder().build().parse(
          """
            class A {
            	def a() {
            		if(true) {
            			println "a"
            		}
            	}
            }
            """,
          """
            class B {
                def b() {
                    if (true) {
                        println "b"
                    }
                }
            }
            """
        ).toList();

        Autodetect.Detector all = Autodetect.detector();
        sources.forEach(all::sample);
        Autodetect.Detector first = Autodetect.detector();
        first.sample(sources.get(0));
        Autodetect.Detector second = Autodetect.detector();
        second.sample(sources.get(1));

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        Autodetect.Detector merged = mapper.readValue(mapper.writeValueAsBytes(second), Autodetect.Detector.class)
          .merge(mapper.readValue(mapper.writeValueAsBytes(first), Autodetect.Detector.class));

        assertThat(merged.build().getStyles())
          .usingRecursiveComparison()
          .withEqualsForType((a, b) -> a.pattern().equals(b.pattern()), Pattern.class)
          .isEqualTo(all.build().getStyles());
    }
}
