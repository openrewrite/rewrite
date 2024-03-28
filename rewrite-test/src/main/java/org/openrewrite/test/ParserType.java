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
import org.openrewrite.SourceFile;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

final class ParserTypeUtils {
    private ParserTypeUtils() {
    }

    public static Class<SourceFile> parserType(Parser parser) {
        return parserType(parser.getClass());
    }

    private static Class<SourceFile> parserType(Class<?> parser) {
        for (Type anInterface : parser.getGenericInterfaces()) {
            if (anInterface instanceof Class) {
                return parserType((Class<?>) anInterface);
            }

            if (anInterface instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) anInterface;
                if (pt.getRawType().equals(Parser.class)) {
                    //noinspection unchecked
                    return (Class<SourceFile>) pt.getActualTypeArguments()[0];
                }
            }
        }

        throw new IllegalArgumentException("Could not determine SourceFile type for this parser.");
    }
}
