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
import org.openrewrite.Checksum;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.marker.Markers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class RemoteArchive implements Remote {
    @EqualsAndHashCode.Include
    UUID id;

    Path sourcePath;
    Markers markers;
    URI uri;
    @Nullable
    Checksum checksum;

    /**
     * Any text describing what this remote URI represents. This will only
     * be used to present results to an end user in a way that is more human
     * readable then just a raw URI.
     */
    @Language("markdown")
    String description;

    Path path;

    @Override
    public InputStream getInputStream(HttpSender httpSender) {
        //noinspection resource
        HttpSender.Response response = httpSender.send(httpSender.get(uri.toString()).build());
        InputStream body = response.getBody();
        ZipInputStream zis = new ZipInputStream(body);

        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(path.toString())) {
                    return new InputStream() {
                        @Override
                        public int read() throws IOException {
                            return zis.read();
                        }

                        @Override
                        public void close() throws IOException {
                            zis.closeEntry();
                            zis.close();
                        }
                    };
                }
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load path " + path + " in zip file " + uri, e);
        }
        throw new IllegalArgumentException("Unable to find path " + path + " in zip file " + uri);
    }
}
