package org.openrewrite.gradle.plugins;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.settingsGradle;

class AddSettingPluginTest implements RewriteTest {
    @Test
    void addPluginToNewBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingPlugin("com.gradle.enterprise", "3.11.x", null)),
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """,
            spec -> spec.after(actual -> {
                assertThat(actual).isNotNull();
                Matcher version = Pattern.compile("3.11(.\\d+)?").matcher(actual);
                assertThat(version.find()).isTrue();
                return """
                  plugins {
                      id 'com.gradle.enterprise' version '%s'
                  }

                  rootProject.name = 'my-project'
                  """.formatted(version.group(0));
            })
          )
        );
    }

    @Test
    void addPluginToExistingBlock() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'org.openrewrite' version '1'
              }
                            
              rootProject.name = 'my-project'
              """,
            spec -> spec.after(actual -> {
                assertThat(actual).isNotNull();
                Matcher version = Pattern.compile("3.11.\\d+").matcher(actual);
                assertThat(version.find()).isTrue();
                return """
                  plugins {
                      id 'org.openrewrite' version '1'
                      id 'com.gradle.enterprise' version '%s'
                  }
                  
                  rootProject.name = 'my-project'
                  """.formatted(version.group(0));
            })
          )
        );
    }
}
