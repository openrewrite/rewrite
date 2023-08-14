/*
 * Copyright 2023 the original author or authors.
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

import org.openrewrite.Parser;
import org.openrewrite.SourceFile;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public interface JvmParser extends Parser {
    abstract class Builder<P extends JvmParser, B extends JvmParser.Builder<P, B>> extends Parser.Builder {
        protected Collection<Path> classpath = Collections.emptyList();

        Builder(Class<? extends SourceFile> sourceFileType) {
            super(sourceFileType);
        }

        @SuppressWarnings("unchecked")
        public B classpath(Collection<Path> classpath) {
            this.classpath = classpath;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B classpath(String... classpath) {
            this.classpath = JavaParser.dependenciesFromClasspath(classpath);
            return (B) this;
        }

        @Override
        public JvmParser.Builder<?, ?> clone() {
            return (JvmParser.Builder<?, ?>) super.clone();
        }
    }
}
