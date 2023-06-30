/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.remote;

import org.openrewrite.internal.lang.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public interface RemoteArtifactCache {

    @Nullable
    Path get(URI uri);

    @Nullable
    Path put(URI uri, InputStream is, Consumer<Throwable> onError);

    @Nullable
    default Path compute(URI uri, Callable<@Nullable InputStream> artifactStream, Consumer<Throwable> onError) {
        Path artifact = get(uri);
        if (artifact == null) {
            try {
                InputStream is = artifactStream.call();
                if (is != null) {
                    artifact = put(uri, is, onError);
                }
            } catch (Exception e) {
                onError.accept(e);
            }
        }
        return artifact;
    }
}
