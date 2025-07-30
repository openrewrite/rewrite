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
package org.openrewrite.maven;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.test.AdHocRecipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class LombokVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().classpath("lombok"))
          .recipe(new AdHocRecipe("Test", "Test", false, () -> new JavaIsoVisitor<>() {

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return super.visitMethodInvocation(method, ctx);
              }
          }, null, null));
    }

    @Test
    void addAnnotationProcessor() {
        rewriteRun(
          mavenProject("root",
            pomXml(
              """
                <project>
                    <groupId>org.example</groupId>
                    <artifactId>testproject</artifactId>
                    <version>0.0.1</version>
                
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.30</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            ),
            srcMainJava(
              java(
                """
                import lombok.Getter;
                
                class TypeSupportTest {
                    void accessor(DataObject dao) {
                        dao.getTestProp();
                    }
                }
                
                class DataObject {
                    @Getter
                    private String testProp;
                }
                """)
            )
          )
        );
    }
}
