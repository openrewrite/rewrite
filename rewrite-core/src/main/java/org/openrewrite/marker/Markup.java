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
package org.openrewrite.marker;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.RecipeScheduler;
import org.openrewrite.Tree;
import org.openrewrite.internal.ExceptionUtils;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.lang.Nullable;

import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.31.0")
public interface Markup extends Marker {

    String getMessage();

    @Nullable
    String getDetail();

    @Override
    default String print(Cursor cursor, UnaryOperator<String> commentWrapper, boolean verbose) {
        if (verbose) {
            return commentWrapper.apply("(" + getDetail() + ")");
        }
        return commentWrapper.apply("(" + getMessage() + ")");
    }

    static <T extends Tree> T error(T t, Throwable throwable) {
        if (ExceptionUtils.containsCircularReferences(throwable)) {
            throwable = new Exception(throwable.getMessage());
            throwable.setStackTrace(throwable.getStackTrace());
        }
        return markup(t, new Markup.Error(randomId(), throwable));
    }

    static <T extends Tree> T warn(T t, Throwable throwable) {
        if (ExceptionUtils.containsCircularReferences(throwable)) {
            throwable = new Exception(throwable.getMessage());
            throwable.setStackTrace(throwable.getStackTrace());
        }
        return markup(t, new Markup.Warn(randomId(), throwable));
    }

    static <T extends Tree> T info(T t, String message) {
        return info(t, message, null);
    }

    static <T extends Tree> T info(T t, String message, @Nullable String detail) {
        return markup(t, new Markup.Info(randomId(), message, detail));
    }

    static <T extends Tree> T debug(T t, String message) {
        return debug(t, message, null);
    }

    static <T extends Tree> T debug(T t, String message, @Nullable String detail) {
        return markup(t, new Markup.Debug(randomId(), message, detail));
    }

    static <T extends Tree> T markup(T t, Markup markup) {
        return t.withMarkers(t.getMarkers().compute(markup, (s1, s2) -> s1 == null ? s2 : s1));
    }

    @Value
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @With
    class Error implements Markup {
        UUID id;

        @EqualsAndHashCode.Include
        String message;

        @EqualsAndHashCode.Include
        String detail;

        public Error(UUID id, Throwable exception) {
            this.id = id;
            Throwable cause = exception instanceof RecipeRunException ? exception.getCause() : exception;
            this.message = cause.getMessage();
            this.detail = ExceptionUtils.sanitizeStackTrace(cause, RecipeScheduler.class);
        }

        @JsonCreator
        public Error(UUID id, String message, String detail) {
            this.id = id;
            this.message = message;
            this.detail = detail;
        }
    }

    @Value
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @With
    class Warn implements Markup {
        UUID id;

        @EqualsAndHashCode.Include
        String message;

        @EqualsAndHashCode.Include
        String detail;

        public Warn(UUID id, Throwable exception) {
            this.id = id;
            Throwable cause = exception instanceof RecipeRunException ? exception.getCause() : exception;
            this.message = cause.getMessage();
            this.detail = ExceptionUtils.sanitizeStackTrace(cause, RecipeScheduler.class);
        }

        @JsonCreator
        public Warn(UUID id, String message, String detail) {
            this.id = id;
            this.message = message;
            this.detail = detail;
        }
    }

    @Value
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @With
    class Info implements Markup {
        UUID id;

        @EqualsAndHashCode.Include
        String message;

        @EqualsAndHashCode.Include
        @Nullable
        String detail;

        @Override
        public String getMessage() {
            return message;
        }

        @Nullable
        @Override
        public String getDetail() {
            return detail;
        }
    }

    @Value
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @With
    class Debug implements Markup {
        UUID id;

        @EqualsAndHashCode.Include
        String message;

        @EqualsAndHashCode.Include
        @Nullable
        String detail;

        @Override
        public String getMessage() {
            return message;
        }

        @Nullable
        @Override
        public String getDetail() {
            return detail;
        }
    }
}
