/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

import java.util.List;

import static java.util.Collections.emptyList;

public class PythonExecutionContextView extends DelegatingExecutionContext {
    private static final String PACKAGE_INDEXES = "org.openrewrite.python.packageIndexes";
    private static final String INDEX_CREDENTIALS = "org.openrewrite.python.indexCredentials";

    public PythonExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static PythonExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof PythonExecutionContextView) {
            return (PythonExecutionContextView) ctx;
        }
        return new PythonExecutionContextView(ctx);
    }

    /**
     * Host-supplied package indexes: the full-override channel. When set, index
     * discovery is skipped entirely and these indexes are used exactly as given,
     * including their credentials.
     */
    public PythonExecutionContextView setPackageIndexes(List<PythonPackageIndex> packageIndexes) {
        putMessage(PACKAGE_INDEXES, packageIndexes);
        return this;
    }

    public List<PythonPackageIndex> getPackageIndexes() {
        return getMessage(PACKAGE_INDEXES, emptyList());
    }

    /**
     * Host-supplied credentials, matched by host against discovered indexes whose URL
     * embeds none. URL-embedded credentials win over these; these win over netrc.
     * Ignored when {@link #setPackageIndexes(List)} supplies the indexes outright.
     */
    public PythonExecutionContextView setIndexCredentials(List<PythonIndexCredentials> credentials) {
        putMessage(INDEX_CREDENTIALS, credentials);
        return this;
    }

    public List<PythonIndexCredentials> getIndexCredentials() {
        return getMessage(INDEX_CREDENTIALS, emptyList());
    }
}
