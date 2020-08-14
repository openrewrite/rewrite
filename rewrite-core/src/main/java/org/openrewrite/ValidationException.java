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

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Turns a set of {@link Validated.Invalid} into a throwable exception, which is used to throw an unchecked exception
 * at runtime when one or more properties are invalid.
 */
@Incubating(since = "2.0.0")
public class ValidationException extends RuntimeException {
    private final Validated validation;

    public URI getSource() {
        return source;
    }

    public void setSource(URI source) {
        this.source = source;
    }

    private URI source = null;

    public ValidationException(Validated validation) {
        this(validation, null);
    }

    public ValidationException(ValidationException other, URI source) {
        this(other.validation, source);
        this.setStackTrace(other.getStackTrace());
    }

    public ValidationException(Validated validation, URI source) {
        super(validation.failures().stream()
                .map(invalid -> invalid.getProperty() + " was '" +
                        (invalid.getValue() == null ? "null" : invalid.getValue()) +
                        "' but it " + invalid.getMessage())
                .collect(Collectors.joining(
                        "\n",
                        validation.failures().size() > 1 ? "Multiple validation failures:\n" : "",
                        ""
                )));
        this.validation = validation;
        this.source = source;
    }

    public Validated getValidation() {
        return validation;
    }

    @Override
    public String getMessage() {
        if(source != null) {
            return "Problem parsing rewrite configuration from: " + source + " \n" + super.getMessage();
        } else {
            return super.getMessage();
        }
    }
}
