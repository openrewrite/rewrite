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
package org.openrewrite.gradle;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

public class GradleExecutionContextView extends DelegatingExecutionContext {

    private static final String HTTP_SENDER = "httpSender";

    public GradleExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static GradleExecutionContextView view(ExecutionContext ctx) {
        if(ctx instanceof GradleExecutionContextView) {
            return (GradleExecutionContextView) ctx;
        }
        return new GradleExecutionContextView(ctx);
    }

    public void setHttpSender(org.openrewrite.ipc.http.HttpSender httpSender) {
        putMessage(HTTP_SENDER, httpSender);
    }

    public org.openrewrite.ipc.http.HttpSender getHttpSender() {
        return getMessage(HTTP_SENDER, new HttpUrlConnectionSender());
    }
}
