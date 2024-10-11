package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

class ChangeGroovyMethodInvocationParameterTest implements RewriteTest {

    @Test
    public void givenGroovyFile_whenMultiParamSet_thenChangeToNewValue() {
        rewriteRun(spec -> spec.recipe(new ChangeGroovyMethodInvocationParameter("jdkVersion", "Eclipse Temurin 17.0.11")),
          //language=groovy
          Assertions.groovy("""
                    releaseJob(
                                    mavenVersion: 'Maven 3.9.5',
                                    jdkVersion: "RedHat OpenJDK 11.0 latest",
                                    gitUrl: 'github.com/openrewrite/rewrite.git',
                                    baseDirectory: '.',
                                    buildsToKeep: '5'
                    )
            """, """
                    releaseJob(
                                    mavenVersion: 'Maven 3.9.5',
                                    jdkVersion: 'Eclipse Temurin 17.0.11',
                                    gitUrl: 'github.com/openrewrite/rewrite.git',
                                    baseDirectory: '.',
                                    buildsToKeep: '5'
                    )
            """));
    }

    @Test
    public void givenGroovyFile_whenSingleParamSet_thenChangeToNewValue() {
        rewriteRun(spec -> spec.recipe(new ChangeGroovyMethodInvocationParameter("mavenVersion", "Maven 3.9.6")),
          //language=groovy
          Assertions.groovy("""
                    releaseJob(
                                    mavenVersion: 'Maven 3.9.5'
                    )
            """, """
                    releaseJob(
                                    mavenVersion: 'Maven 3.9.5'
                    )
            """));
    }

    @Test
    public void givenGroovyFile_whenSingleParamSet_thenChangeToNewValueUsingGStrings() {
        rewriteRun(spec -> spec.recipe(new ChangeGroovyMethodInvocationParameter("mavenVersion", "Maven 3.9.6")),
          //language=groovy
          Assertions.groovy("""
                    releaseJob(
                                    mavenVersion: "Maven 3.9.5"
                    )
            """, """
                    releaseJob(
                                    mavenVersion: "Maven 3.9.6"
                    )
            """));
    }

    @Test
    public void givenGroovyFile_whenSingleParamSet_thenChangeToNewValueUsingTemplating() {
        rewriteRun(spec -> spec.recipe(new ChangeGroovyMethodInvocationParameter("jdkVersion", "Eclipse Temurin 17.0.11")), Assertions.groovy("""
                  job(
                                  mavenVersion: 'Maven 3.9.5'
                  )
                   releaseJob(
                                  mavenVersion: 'Maven 3.9.5'
                  )
          """, """
                  job(
                                  mavenVersion: 'Maven 3.9.5'
                  )
                  releaseJob(
                                  mavenVersion: 'Maven 3.9.6'
                  )
          """));
    }
}