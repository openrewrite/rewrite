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
package org.openrewrite.java;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.cache.JavaTypeCache;
import org.openrewrite.java.cache.DelegatingJavaTypeCache;

public class JavaExecutionContextView extends DelegatingExecutionContext {
    private static final String TYPE_CACHE = "org.openrewrite.java.typeCache";
    private static final String SKIP_SOURCE_SET_MARKER = "org.openrewrite.java.skipSourceSetMarker";

    public JavaExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public JavaExecutionContextView setTypeCache(JavaTypeCache typeCache) {
        putMessage(TYPE_CACHE, typeCache);
        return this;
    }

    public JavaTypeCache getTypeCache() {
        JavaTypeCache jtc = getMessage(TYPE_CACHE);
        if(jtc == null) {
            jtc =  new DelegatingJavaTypeCache();
            setTypeCache(jtc);
        }
        return jtc;
    }

    public JavaExecutionContextView setSkipSourceSetMarker(boolean skip) {
        putMessage(SKIP_SOURCE_SET_MARKER, skip);
        return this;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSkipSourceSetMarker() {
        return getMessage(SKIP_SOURCE_SET_MARKER, false);
    }
}
