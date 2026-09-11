/*
 * Copyright 2025 the original author or authors.
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

import lombok.Value;
import org.openrewrite.java.tree.*;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;

/**
 * The language-specific source snippets that {@link JavaTemplateParser} wraps around a template in order to
 * compile it, plus the logic for locating the templated element in the resulting tree.
 * <p>
 * The stub and the extractor are paired deliberately. A language is free to wrap the template in whatever
 * shape its grammar requires, and that choice dictates where the result is read back from — Java hangs a
 * method off a synthetic class, while Kotlin can declare at file scope. Keeping the two halves in one object
 * prevents them from drifting apart.
 * <p>
 * Note this is the <em>snippet</em> language, which is chosen by the {@code JavaTemplate} subclass a recipe
 * author instantiates. It is independent of the language of the source file being modified; that is decided
 * separately by {@link JavaTemplateLanguageExtension}.
 */
public interface TemplateStubs {

    Stub<Statement> parameters();

    Stub<J.Lambda.Parameters> lambdaParameters();

    Stub<TypeTree> anExtends();

    Stub<TypeTree> anImplements();

    Stub<NameTree> checkedExceptions();

    Stub<J.TypeParameter> typeParameters();

    Stub<Expression> packageDeclaration();

    /**
     * Which imports to prepend to the stub before compiling it.
     */
    enum Imports {
        /** None. The stub is compiled exactly as written. */
        NONE,
        /** The template's own imports. */
        TEMPLATE,
        /** The template's own imports, plus those of the enclosing source file when context-sensitive. */
        TEMPLATE_AND_ENCLOSING
    }

    @Value
    class Stub<T extends J> {
        /** The wrapper source, containing a single {@code #{}} placeholder for the template text. */
        String code;

        Imports imports;

        /** Locates the templated elements within the compiled stub. */
        Function<JavaSourceFile, List<T>> extract;

        public static <T extends J> Stub<T> of(String code, Imports imports, Function<JavaSourceFile, List<T>> extract) {
            return new Stub<>(code, imports, extract);
        }

        /**
         * For coordinates that yield exactly one element. The parser still caches a list, so single-valued
         * extractors are wrapped rather than given a separate code path.
         */
        public static <T extends J> Stub<T> ofOne(String code, Imports imports, Function<JavaSourceFile, T> extract) {
            return new Stub<>(code, imports, cu -> singletonList(extract.apply(cu)));
        }
    }
}
