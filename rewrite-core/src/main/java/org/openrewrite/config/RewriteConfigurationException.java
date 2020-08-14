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
package org.openrewrite.config;

import java.net.URI;

public class RewriteConfigurationException extends RuntimeException {
    private URI source = null;
    private final String message;
    public RewriteConfigurationException(String message) {
        this.message = message;
    }
    public RewriteConfigurationException(String message, URI source) {
        this.message = message;
        this.source = source;
    }
    public RewriteConfigurationException(RewriteConfigurationException other, URI source) {
        this.message = other.message;
        this.source = source;
        setStackTrace(other.getStackTrace());
    }

    @Override
    public String getMessage() {
        if(source != null) {
            return "Problem parsing rewrite configuration from: " + source + " \n" + message;
        } else {
            return message;
        }
    }

    public URI getSource() {
        return source;
    }

    public void setSource(URI source) {
        this.source = source;
    }
}
