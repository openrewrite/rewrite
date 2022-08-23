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
package org.openrewrite.test;

import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;

import java.util.Objects;

public class DslParserBuilder extends Parser.Builder {
    @Nullable
    private final String dsl;

    private final Parser.Builder delegate;

    public DslParserBuilder(@Nullable String dsl, Parser.Builder delegate) {
        super(delegate.getSourceFileType());
        this.delegate = delegate;
        this.dsl = dsl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DslParserBuilder that = (DslParserBuilder) o;
        return delegate.getSourceFileType().equals(that.getSourceFileType()) &&
                Objects.equals(dsl, that.dsl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate.getSourceFileType(), dsl);
    }

    @Override
    public Parser<?> build() {
        return delegate.build();
    }
}
