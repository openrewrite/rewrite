package org.openrewrite.gradle.style;

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
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(4);
    }
}