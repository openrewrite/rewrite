/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class PackageTest implements RewriteTest {
    @Test
    void packageNameOfUnknownExpression() {
        // given
        J.Unknown expression = new J.Unknown(randomId(), Space.EMPTY, Markers.EMPTY,
          new J.Unknown.Source(randomId(), Space.EMPTY, Markers.EMPTY, "org.company.service.web"));
        J.Package pkg = new J.Package(randomId(), Space.EMPTY, Markers.EMPTY, expression, emptyList());

        // when
        String packageName = pkg.getPackageName();

        // then
        assertThat(packageName).isEqualTo("org.company.service.web");
    }

    @Test
    void packageFormatting() {
        rewriteRun(
          java(
            """
              /*comment*/package a ;
              class A {}
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getPackageDeclaration().getPackageName()).isEqualTo("a"))
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "org.company.service.web",
      "org . company\t. service . web",
      "org.company /* legacy */ .service.web",
      "org /* before dot */ . /* after dot */ company. // line comment\nservice.web"
    })
    void packageName(String packageExpression) {
        rewriteRun(
          java(
            String.format("package %s; class A {}", packageExpression),
            spec -> spec.afterRecipe(cu -> assertThat(cu.getPackageDeclaration().getPackageName())
              .isEqualTo("org.company.service.web"))
          )
        );
    }
}
