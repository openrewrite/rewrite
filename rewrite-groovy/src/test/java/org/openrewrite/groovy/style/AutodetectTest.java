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
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }
}