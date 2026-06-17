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
package org.openrewrite.groovy.internal;

import org.openrewrite.java.internal.JavaIdentifierValidationService;

public class GroovyIdentifierValidationService extends JavaIdentifierValidationService {

    @Override
    protected boolean isValidChar(char c) {
        // Groovy permits the Java identifier characters plus '@' for attribute (field) access, e.g. obj.@field.
        return super.isValidChar(c) || c == '@';
    }

    @Override
    protected boolean isWhitelisted(String name) {
        // Groovy allows quoted identifiers (e.g. foo.'some name' or foo."name"), which may contain any character.
        if (super.isWhitelisted(name)) {
            return true;
        }
        if (name.length() < 2) {
            return false;
        }
        char first = name.charAt(0);
        return (first == '\'' || first == '"') && name.charAt(name.length() - 1) == first;
    }
}
