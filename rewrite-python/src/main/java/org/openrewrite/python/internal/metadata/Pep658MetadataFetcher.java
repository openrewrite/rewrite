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
package org.openrewrite.python.internal.metadata;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;

/**
 * Fetches the PEP 658/714 {@code .metadata} sidecar published next to a distribution file.
 */
public class Pep658MetadataFetcher {

    private Pep658MetadataFetcher() {
    }

    public static @Nullable CoreMetadata fetch(HttpSender http, String fileUrl) {
        try (HttpSender.Response response = http.send(http.get(metadataUrl(fileUrl)).build())) {
            if (!response.isSuccessful()) {
                return null;
            }
            return MetadataParser.parse(response.getBodyAsBytes());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The {@code .metadata} suffix attaches to the file path, before any query string.
     */
    private static String metadataUrl(String fileUrl) {
        int query = fileUrl.indexOf('?');
        return query < 0 ?
                fileUrl + ".metadata" :
                fileUrl.substring(0, query) + ".metadata" + fileUrl.substring(query);
    }
}
