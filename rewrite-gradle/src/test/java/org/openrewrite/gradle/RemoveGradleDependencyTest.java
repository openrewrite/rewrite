/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class RemoveGradleDependencyTest implements RewriteTest {

    //Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    //In this case, the recipe and the parser are common. See below, on how the defaults can be overriden
    //per test.
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveGradleDependency("implementation", "org.springframework.boot", "spring-boot-starter-web"));
    }

    @Test
    void removeGradleDependencyUsingStringNotation() {
        rewriteRun(
          buildGradle(
            "dependencies {\n" +
              "        implementation \"org.springframework.boot:spring-boot-starter-web:2.7.0\"\n" +
              "        testImplementation \"org.jupiter.vintage:junit-vintage-engine:5.6.2\"\n" +
              "}\n",
            "dependencies {\n" +
              "        \n" +
              "        testImplementation \"org.jupiter.vintage:junit-vintage-engine:5.6.2\"\n" +
              "}\n"
          )
        );
    }

    @Test
    void removeGradleDependencyUsingStringNotationWithExclusion() {
        rewriteRun(
          buildGradle(
            "dependencies {\n" +
              "        implementation(\"org.springframework.boot:spring-boot-starter-web:2.7.0\") {\n" +
              "            exclude group: \"junit\"\n" +
              "        }\n" +
              "        testImplementation \"org.jupiter.vintage:junit-vintage-engine:5.6.2\"\n" +
              "}\n",
            "dependencies {\n" +
              "        \n" +
              "        testImplementation \"org.jupiter.vintage:junit-vintage-engine:5.6.2\"\n" +
              "}\n"
          )
        );
    }

    @Test
    void removeGradleDependencyUsingMapNotation() {
        rewriteRun(
          buildGradle(
            "dependencies {\n" +
              "        implementation group: \"org.springframework.boot\", name: \"spring-boot-starter-web\", version: \"2.7.0\"\n" +
              "        testImplementation \"org.jupiter.vintage:junit-vintage-engine:5.6.2\"\n" +
              "}\n",
            "dependencies {\n" +
              "        \n" +
              "        testImplementation \"org.jupiter.vintage:junit-vintage-engine:5.6.2\"\n" +
              "}\n"
          )
        );
    }

    @Test
    void removeGradleDependencyUsingMapNotationWithExclusion() {
        rewriteRun(
          buildGradle(
            "dependencies {\n" +
              "        implementation(group: \"org.springframework.boot\", name: \"spring-boot-starter-web\", version: \"2.7.0\") {\n" +
              "            exclude group: \"junit\"\n" +
              "        }\n" +
              "        testImplementation \"org.jupiter.vintage:junit-vintage-engine:5.6.2\"\n" +
              "}\n",
            "dependencies {\n" +
              "        \n" +
              "        testImplementation \"org.jupiter.vintage:junit-vintage-engine:5.6.2\"\n" +
              "}\n"
          )
        );
    }
}
