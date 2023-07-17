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

import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

public class HttpSenderExecutionContextView extends DelegatingExecutionContext {
    private static final String HTTP_SENDER = "org.openrewrite.httpSender";
    private static final String LARGE_FILE_HTTP_SENDER = "org.openrewrite.largeFileHttpSender";

    public HttpSenderExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static HttpSenderExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof HttpSenderExecutionContextView) {
            return (HttpSenderExecutionContextView) ctx;
        }
        return new HttpSenderExecutionContextView(ctx);
    }

    public HttpSenderExecutionContextView setHttpSender(HttpSender httpSender) {
        putMessage(HTTP_SENDER, httpSender);
        return this;
    }

    public HttpSender getHttpSender() {
        return getMessage(HTTP_SENDER, new HttpUrlConnectionSender());
    }

    public HttpSenderExecutionContextView setLargeFileHttpSender(HttpSender httpSender) {
        putMessage(LARGE_FILE_HTTP_SENDER, httpSender);
        return this;
    }

    public HttpSender getLargeFileHttpSender() {
        return getMessage(LARGE_FILE_HTTP_SENDER, new HttpUrlConnectionSender());
    }
}
