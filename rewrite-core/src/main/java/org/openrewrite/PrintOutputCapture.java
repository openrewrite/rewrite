/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.internal.lang.Nullable;

public class PrintOutputCapture<P> {
    private final P p;
    public final StringBuilder out = new StringBuilder();

    public PrintOutputCapture(P p) {
        this.p = p;
    }

    public P getContext() {
        return p;
    }

    public String getOut() {
        return out.toString();
    }

    public PrintOutputCapture<P> append(@Nullable String text) {
        if (text == null) {
            return this;
        }
        out.append(text);
        return this;
    }

    public PrintOutputCapture<P> append(char c) {
        out.append(c);
        return this;
    }
}
