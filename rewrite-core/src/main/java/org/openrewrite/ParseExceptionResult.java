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
package org.openrewrite;

import lombok.Value;
import lombok.With;
import org.openrewrite.internal.ExceptionUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;

import java.util.UUID;

import static org.openrewrite.Tree.randomId;

@Value
@With
public class ParseExceptionResult implements Marker {
    UUID id;
    String parserType;
    String exceptionType;
    String message;

    /**
     * The type of tree element that was being parsed when the failure occurred.
     */
    @Nullable
    String treeType;

    public static ParseExceptionResult build(Class<? extends Parser> parserClass,
                                             Throwable t,
                                             @Nullable String message) {
        String simpleName = t.getClass().getSimpleName();
        return new ParseExceptionResult(
                randomId(),
                parserClass.getSimpleName(),
                !StringUtils.isBlank(simpleName) ? simpleName : t.getClass().getName(),
                (message != null ? message : "") + ExceptionUtils.sanitizeStackTrace(t, parserClass),
                null
        );
    }

    public static ParseExceptionResult build(Parser parser, Throwable t, @Nullable String message) {
        return build(parser.getClass(), t, message);
    }

    public static ParseExceptionResult build(Parser parser, Throwable t) {
        return build(parser.getClass(), t, null);
    }
}
