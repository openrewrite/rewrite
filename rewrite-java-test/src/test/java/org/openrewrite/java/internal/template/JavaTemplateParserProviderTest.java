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
package org.openrewrite.java.internal.template;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.DefaultJavaTypeFactory;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.internal.JavaTypeFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class JavaTemplateParserProviderTest {

    @Test
    void providerReceivesParserResolvedClasspath() {
        AtomicReference<List<Path>> capturedClasspath = new AtomicReference<>();
        JavaTypeFactory.Provider provider = (classpath, jdkHome) -> {
            capturedClasspath.set(new ArrayList<>(classpath));
            return new DefaultJavaTypeFactory(new JavaTypeCache());
        };

        Path sentinel = Paths.get("/nonexistent/sentinel.jar");
        JavaParser.Builder<?, ?> parserBuilder = JavaParser.fromJavaVersion()
                .classpath(Collections.singletonList(sentinel));

        JavaTemplateParser templateParser = new JavaTemplateParser(
                false, parserBuilder, s -> {}, s -> {}, emptySet(), "Type");

        Cursor cursor = new Cursor(null, Cursor.ROOT_VALUE);
        cursor.putMessage(JavaTemplateParser.TYPE_FACTORY_PROVIDER_KEY, provider);

        templateParser.parseTypeParameters(cursor, "T");

        assertThat(capturedClasspath.get())
                .as("Provider should be invoked with the parser's resolved classpath")
                .isNotNull()
                .contains(sentinel);
    }
}
