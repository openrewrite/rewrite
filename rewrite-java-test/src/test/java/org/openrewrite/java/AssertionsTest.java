/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.internal.DefaultJavaTypeFactory;
import org.openrewrite.java.internal.HashMapJavaTypeCache;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class AssertionsTest implements RewriteTest {

    @SuppressWarnings("deprecation")
    @Test
    void aParserGetsAHashMapTypeCacheUnlessTheTestGaveItACacheOrFactory() {
        JavaParser.Builder<?, ?> own = JavaParser.fromJavaVersion();
        rewriteRun(spec -> spec.parser(own), java("class A {}"));
        assertThat(own.javaTypeCache).isInstanceOf(HashMapJavaTypeCache.class);

        JavaTypeCache cache = new JavaTypeCache();
        JavaParser.Builder<?, ?> givenCache = JavaParser.fromJavaVersion().typeCache(cache);
        rewriteRun(spec -> spec.parser(givenCache), java("class A {}"));
        assertThat(givenCache.javaTypeCache).isSameAs(cache);

        JavaParser.Builder<?, ?> givenFactory = JavaParser.fromJavaVersion()
                .typeFactory(new DefaultJavaTypeFactory(new JavaTypeCache()));
        rewriteRun(spec -> spec.parser(givenFactory), java("class A {}"));
        assertThat(givenFactory.javaTypeCache).isNotInstanceOf(HashMapJavaTypeCache.class);
    }
}
