/*
 * Copyright 2020 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.net.URI;

import static java.util.stream.Collectors.joining;

/**
 * Turns a set of {@link Validated.Invalid} into a throwable exception, which is used to throw an unchecked exception
 * at runtime when one or more properties are invalid.
 */
public class ValidationException extends RuntimeException {
    public URI getSource() {
        return source;
    }

    public void setSource(URI source) {
        this.source = source;
    }

    private URI source;

    public ValidationException(Validated<?> validation) {
        this(validation, null);
    }

    public ValidationException(Validated<?> validation, @Nullable URI source) {
        super(validation.failures().stream()
                .map(invalid -> invalid.getProperty() + " was '" +
                        (invalid.getInvalidValue() == null ? "null" : invalid.getInvalidValue()) +
                        "' but it " + invalid.getMessage())
                .collect(joining(
                        "\n",
                        validation.failures().size() > 1 ? "Multiple validation failures:\n" : "",
                        ""
                )));
        this.source = source;
    }

    @Override
    public String getMessage() {
        if (source != null) {
            return "Problem parsing rewrite configuration from: " + source + " \n" + super.getMessage();
        } else {
            return super.getMessage();
        }
    }
}
