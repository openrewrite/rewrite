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
package org.openrewrite.maven.internal;

/**
 * Represents problems that can arise during parsing Maven poms into their Rewrite AST.
 */
public class MavenParsingException extends RuntimeException {

    private final Severity severity;

    public MavenParsingException(String message) {
        this(message, Severity.Error);
    }

    public MavenParsingException(String message, Object ... args) {
        this(String.format(message, args), Severity.Error);
    }

    public MavenParsingException(String message, Throwable cause, Object ... args) {
        this(String.format(message, args), Severity.Error, cause);
    }

    public MavenParsingException(String message, Severity severity) {
        super(message);
        this.severity = severity;
    }

    public MavenParsingException(String message, Severity severity, Object ... args) {
        super(String.format(message, args));
        this.severity = severity;
    }

    public MavenParsingException(String message, Severity severity, Throwable cause, Object ... args) {
        super(String.format(message, args), cause);
        this.severity = severity;
    }

    public Severity getSeverity() {
        return severity;
    }

    public enum Severity {
        Error,
        Info
    }
}
