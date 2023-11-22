/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https:www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Calendar.YEAR;
import static java.util.Calendar.getInstance;
import static org.openrewrite.java.Assertions.java;

class AddLicenseHeaderTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddLicenseHeader(
          """
            Copyright ${CURRENT_YEAR} the original author or authors.
            <p>
            Licensed under the Apache License, Version 2.0 (the "License");
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at
            """.trim()
        ));
    }

    @Test
    void addLicenseHeader() {
        rewriteRun(
          java(
            """
              package com.sample;
              class Test {
              }
              """,
            """
              /*
               * Copyright %s the original author or authors.
               * <p>
               * Licensed under the Apache License, Version 2.0 (the "License");
               * you may not use this file except in compliance with the License.
               * You may obtain a copy of the License at
               */
              package com.sample;
              class Test {
              }
              """.formatted(getInstance().get(YEAR))
          )
        );
    }

    @Test
    void dontChangeExistingHeader() {
        rewriteRun(
          java(
            """
              /*
               * My license header
               */
              package com.sample;
              class Test {
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3198")
    void dontChangeJavadoc() {
        rewriteRun(
          java(
            """
              /*
               * My license header
               */
              package com.sample;
              /**
               * Foo {@link int[] values} bar.
               */
              class Test {
              }
              """
          )
        );
    }

    @Test
    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite/issues/3198")
    void dontChangeInvalidJavadoc() {
        rewriteRun(
          java(
            """
              /*
               * My license header
               */
              package com.sample;
              /**
               * {@link Stream<? extends Foo>}
               */              
              class Test {
              }
              """
          )
        );
    }
}
