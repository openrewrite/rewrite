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
package org.openrewrite.java;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

public class JavaParserExecutionContextView extends DelegatingExecutionContext {
    private static final String PARSER_CLASSPATH_DOWNLOAD_LOCATION = "org.openrewrite.java.parserClasspathDownloadLocation";

    public JavaParserExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static JavaParserExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof JavaParserExecutionContextView) {
            return (JavaParserExecutionContextView) ctx;
        }
        return new JavaParserExecutionContextView(ctx);
    }

    public JavaParserExecutionContextView setParserClasspathDownloadTarget(File folder) {
        putMessage(PARSER_CLASSPATH_DOWNLOAD_LOCATION, folder);
        return this;
    }

    public File getParserClasspathDownloadTarget() {
        File target = getMessage(PARSER_CLASSPATH_DOWNLOAD_LOCATION);
        if (target == null) {
            File defaultTarget = new File(System.getProperty("user.home") + "/.rewrite/classpath");
            if (!defaultTarget.mkdirs() && !defaultTarget.exists()) {
                throw new UncheckedIOException(new IOException("Failed to create directory " + defaultTarget.getAbsolutePath()));
            }
            return defaultTarget;
        }
        return target;
    }
}

