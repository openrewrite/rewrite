/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Trait;

import java.util.Optional;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class GradleDependenciesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .beforeRecipe(withToolingApi())
          .recipe(RewriteTest.toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                  Optional<GradleDependencies> meybeDependenciesBlock = new GradleDependencies.Matcher().get(mi, getCursor().getParent());

                  if (meybeDependenciesBlock.isPresent()) {
                      return meybeDependenciesBlock
                        .map(deps -> deps.removeDependency("javax.validation", "validation-api"))
                        .map(Trait::getTree)
                        .orElse(null);
                  }
                  return mi;
              }
          }));
    }

    @DocumentExample
    @Test
    void gradleDependenciesWithStatements() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  //First comment remains
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  implementation 'org.apache.commons:commons-lang3:3.17.0' //Comment at the end of a dependency remains
                  implementation 'org.apache.commons:commons-lang3:3.17.0'         //Even if they are indented with more than a single space
                  implementation 'javax.validation:validation-api:2.0.1.Final'//Comment at end of removed ones not put at end of previous one.
                  //Comment in between 2 removed ones also remains
                  implementation 'javax.validation:validation-api:2.0.1.Final' //Comment at end of removed ones removed
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  //Comment before removed ones also remain
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  implementation 'org.apache.commons:commons-lang3:3.17.0' /*
                      Multiline comments belong to the line where they where added
                  */
                  implementation 'javax.validation:validation-api:2.0.1.Final' /*
                      So this one gets removed
                  */
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  /*
                      But this one does not
                  */
              
                  //Section 1
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  //Section 2
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
              
                  //Section3
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  //comment will also be at end
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  //comment at end
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  //First comment remains
                  implementation 'org.apache.commons:commons-lang3:3.17.0' //Comment at the end of a dependency remains
                  implementation 'org.apache.commons:commons-lang3:3.17.0'         //Even if they are indented with more than a single space
                  //Comment in between 2 removed ones also remains
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  //Comment before removed ones also remain
                  implementation 'org.apache.commons:commons-lang3:3.17.0' /*
                      Multiline comments belong to the line where they where added
                  */
                  /*
                      But this one does not
                  */
              
                  //Section 1
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  //Section 2
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
              
                  //Section3
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  //comment will also be at end
                  //comment at end
              }
              """
          )
        );
    }

    @Test
    void emptyDependenciesBlockIsRemoved() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              dependencies {
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  implementation 'javax.validation:validation-api:2.0.2.Final'
              }
              
              repositories {
                  mavenCentral()
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void emptyDependenciesBlockWithCommentsIsPreserved() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              dependencies {
                  // This comment should preserve the block
                  implementation 'javax.validation:validation-api:2.0.1.Final'
              }
              
              repositories {
                  mavenCentral()
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              dependencies {
                  // This comment should preserve the block
              }
              
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void constraintBlocksArePreserved() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              dependencies {
                  implementation 'javax.validation:validation-api:2.0.1.Final'
              
                  constraints {
                      implementation 'org.apache.commons:commons-lang3:3.17.0'
                  } // end of constraints comment
                  // after constraints comment
              
                  implementation 'javax.validation:validation-api:2.0.2.Final'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              dependencies {
              
                  constraints {
                      implementation 'org.apache.commons:commons-lang3:3.17.0'
                  } // end of constraints comment
                  // after constraints comment
              }
              """
          )
        );
    }

    @Test
    void differentCommentLocations() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0' // end of line comment
                  implementation /* inline block */ 'javax.validation:validation-api:2.0.2.Final'
                  /*
                   * Multiline block comment
                   * before a statement
                   */
                  implementation 'javax.validation:validation-api:2.0.3.Final'
                  implementation 'org.apache.commons:commons-lang3:3.18.0'
                  // End of line comment after last kept statement
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0' // end of line comment
                  /*
                   * Multiline block comment
                   * before a statement
                   */
                  implementation 'org.apache.commons:commons-lang3:3.18.0'
                  // End of line comment after last kept statement
              }
              """
          )
        );
    }

    @Test
    void removeFirstStatement() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment
                  implementation 'javax.validation:validation-api:2.0.1.Final'
              
                  // Second kept statement
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment
              
                  // Second kept statement
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """
          )
        );
    }

    @Test
    void removeMiddleStatement() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  // Middle statement removed
                  implementation 'javax.validation:validation-api:2.0.1.Final'
              
                  // Another kept statement
                  implementation 'org.apache.commons:commons-lang3:3.18.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  // Middle statement removed
              
                  // Another kept statement
                  implementation 'org.apache.commons:commons-lang3:3.18.0'
              }
              """
          )
        );
    }

    @Test
    void removeLastStatement() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment kept
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  // Second statement removed
                  implementation 'javax.validation:validation-api:2.0.1.Final'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment kept
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  // Second statement removed
              }
              """
          )
        );
    }

    @Test
    void removeFirstStatementWithEOLComment() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment
                  implementation 'javax.validation:validation-api:2.0.1.Final' // End of line comment
              
                  // Second kept statement
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment
              
                  // Second kept statement
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """
          )
        );
    }

    @Test
    void removeMiddleStatementWithEOLComment() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  // Middle statement removed
                  implementation 'javax.validation:validation-api:2.0.1.Final' // End of line comment
              
                  // Another kept statement
                  implementation 'org.apache.commons:commons-lang3:3.18.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  // Middle statement removed
              
                  // Another kept statement
                  implementation 'org.apache.commons:commons-lang3:3.18.0'
              }
              """
          )
        );
    }

    @Test
    void removeLastStatementWithEOLComment() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment kept
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  // Second statement removed
                  implementation 'javax.validation:validation-api:2.0.1.Final' // End of line comment
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // First statement comment kept
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  // Second statement removed
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedFirstStatement() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedMiddleStatement() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  implementation 'org.apache.commons:commons-lang3:3.18.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  implementation 'org.apache.commons:commons-lang3:3.18.0'
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedLastStatement() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  implementation 'javax.validation:validation-api:2.0.1.Final'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedFirstStatementWithEOLComment() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'javax.validation:validation-api:2.0.1.Final' // End of line comment
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedMiddleStatementWithEOLComment() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  implementation 'javax.validation:validation-api:2.0.1.Final' // End of line comment
                  implementation 'org.apache.commons:commons-lang3:3.18.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  implementation 'org.apache.commons:commons-lang3:3.18.0'
              }
              """
          )
        );
    }

    @Test
    void removeUnseparatedLastStatementWithEOLComment() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  implementation 'javax.validation:validation-api:2.0.1.Final' // End of line comment
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """
          )
        );
    }

    @Test
    void removeAllButOneStatement() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  implementation 'javax.validation:validation-api:2.0.2.Final'
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  implementation 'javax.validation:validation-api:2.0.3.Final'
                  implementation 'javax.validation:validation-api:2.0.4.Final'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """
          )
        );
    }

    @Test
    void commentedOutDependencies() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  // implementation 'some:commented:1.0'
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  /* implementation 'another:commented:2.0' */
                  implementation 'javax.validation:validation-api:2.0.2.Final'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  // implementation 'some:commented:1.0'
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  /* implementation 'another:commented:2.0' */
              }
              """
          )
        );
    }

    @Test
    void commentInBetweenRemovedOnesRemains() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  implementation 'javax.validation:validation-api:2.0.1.Final'//Comment at end of removed ones not put at end of previous one.
                  //Comment in between 2 removed ones remains
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  implementation 'org.apache.commons:commons-lang3:3.17.0' //Also if the kept item has a end of line
                  implementation 'javax.validation:validation-api:2.0.1.Final'//Comment at end of removed ones not put at end of previous one.
                  //Comment in between 2 removed ones remains
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  //Comment in between 2 removed ones remains
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              
                  implementation 'org.apache.commons:commons-lang3:3.17.0' //Also if the kept item has a end of line
                  //Comment in between 2 removed ones remains
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
              }
              """
          )
        );
    }

    @Test
    void multilineComments() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  /* First comment remains */
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  /* before line comment remains */ implementation 'org.apache.commons:commons-lang3:3.17.0'
                  implementation 'org.apache.commons:commons-lang3:3.17.0' /* Comment at the end of a dependency remains */
                  implementation 'org.apache.commons:commons-lang3:3.17.0'         /* Even if they are indented with more than a single space */
                  implementation 'org.apache.commons:commons-lang3:3.17.0' /* 
                  Comment at end over multiple lines
                  */
                  implementation 'org.apache.commons:commons-lang3:3.17.0' 
                  /* 
                  Comment at new line
                   */
                   /* First comment remains */
                  implementation 'javax.validation:validation-api:2.0.1.Final'
                  /* before line comment gets removed also */ implementation 'javax.validation:validation-api:2.0.1.Final' /* end of line gets removed */
                  /* even gets removed if preceded by another comment not on a new line */ implementation 'javax.validation:validation-api:2.0.1.Final'
                  implementation 'javax.validation:validation-api:2.0.1.Final' /* Comment at the end of a dependency is removed */
                  implementation 'javax.validation:validation-api:2.0.1.Final'         /* Even removed if they are indented with more than a single space */
                  implementation 'javax.validation:validation-api:2.0.1.Final' /* 
                  Comment at end over multiple lines is removed
                  */
                  implementation 'javax.validation:validation-api:2.0.1.Final' 
                  /* 
                  Comment at new line
                   */
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  /* First comment remains */
                  implementation 'org.apache.commons:commons-lang3:3.17.0'
                  /* before line comment remains */ implementation 'org.apache.commons:commons-lang3:3.17.0'
                  implementation 'org.apache.commons:commons-lang3:3.17.0' /* Comment at the end of a dependency remains */
                  implementation 'org.apache.commons:commons-lang3:3.17.0'         /* Even if they are indented with more than a single space */
                  implementation 'org.apache.commons:commons-lang3:3.17.0' /* 
                  Comment at end over multiple lines
                  */
                  implementation 'org.apache.commons:commons-lang3:3.17.0' 
                  /* 
                  Comment at new line
                   */
                   /* First comment remains */
                  /* 
                  Comment at new line
                   */
              }
              """
          )
        );
    }
}
