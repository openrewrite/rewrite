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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class FindRepeatableAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindRepeatableAnnotations());
    }

    @DocumentExample
    @Test
    void findRepeatable() {
        rewriteRun(
          java(
            """
              import org.mapstruct.*;
              class Test {
                  @ValueMappings({
                          @ValueMapping(source = "UNKNOWN", target = MappingConstants.NULL),
                          @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
                  })
                  void test() {
                  }
              }
              """,
            """
              import org.mapstruct.*;
              class Test {
                  @ValueMappings({
                          /*~~>*/@ValueMapping(source = "UNKNOWN", target = MappingConstants.NULL),
                          /*~~>*/@ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
                  })
                  void test() {
                  }
              }
              """
          ),
          java(
            """
              package org.mapstruct;

              import java.lang.annotation.ElementType;
              import java.lang.annotation.Repeatable;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;

              @Repeatable(ValueMappings.class)
              @Retention(RetentionPolicy.CLASS)
              @Target(ElementType.METHOD)
              public @interface ValueMapping {
                  String source();

                  String target();
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package org.mapstruct;
                          
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
                          
              @Target({ElementType.METHOD})
              @Retention(RetentionPolicy.CLASS)
              public @interface ValueMappings {
                  ValueMapping[] value();
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package org.mapstruct;
                          
              public class MappingConstants {
                  public static final String NULL = "null";
              }
              """,
            SourceSpec::skip
          )
        );
    }
}
