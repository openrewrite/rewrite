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
package org.openrewrite.java.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class JavaNamingServiceTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
      foo_bar,fooBar
      foo$bar,fooBar
      foo_bar$,fooBar
      foo$bar$,fooBar
      """)
    void changeMethodName(String before, String after) {
        String actual = new JavaNamingService().standardizeMethodName(before);
        assertThat(actual).isEqualTo(after);
    }

}