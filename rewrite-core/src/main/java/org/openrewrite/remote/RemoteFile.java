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
package org.openrewrite.remote;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marker.Markers;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class RemoteFile implements Remote {
    @EqualsAndHashCode.Include
    UUID id;

    Path sourcePath;
    Markers markers;
    URI uri;

    @Nullable
    Charset charset;

    boolean charsetBomMarked;

    @Nullable
    FileAttributes fileAttributes;

    @Language("markdown")
    String description;

    @Override
    public <P> byte[] printAllAsBytes(P p) {
        //noinspection resource
        return new HttpUrlConnectionSender().get(uri.toString()).send().getBodyAsBytes();
    }

    @Override
    public InputStream getInputStream(ExecutionContext ctx) {
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
        //noinspection resource
        return httpSender.get(uri.toString()).send().getBody();
    }
}
