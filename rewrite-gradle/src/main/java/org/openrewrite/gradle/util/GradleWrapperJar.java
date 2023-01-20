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
package org.openrewrite.gradle.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.openrewrite.Checksum;
import org.openrewrite.FileAttributes;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.marker.Markers;
import org.openrewrite.remote.Remote;
import org.openrewrite.remote.RemoteArchive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_JAR_FILE_ATTRIBUTES;

@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class GradleWrapperJar implements Remote {

    @EqualsAndHashCode.Include
    UUID id;
    Path sourcePath;
    Markers markers;
    URI uri;
    boolean charsetBomMarked;
    @Nullable
    FileAttributes fileAttributes;
    @Nullable
    Charset charset;
    @Language("markdown")
    String description;
    String version;
    Checksum checksum;

    Path path;

    @lombok.experimental.Tolerate
    public GradleWrapperJar(URI uri, String version, Checksum checksum) {
        this.uri = uri;
        this.version = version;
        this.checksum = checksum;
        this.description = "gradle-wrapper.jar is part of the gradle wrapper";
        this.sourcePath = GradleWrapper.WRAPPER_JAR_LOCATION;
        this.charset = null;
        this.charsetBomMarked = false;
        this.fileAttributes = WRAPPER_JAR_FILE_ATTRIBUTES;
        this.markers = Markers.EMPTY;
        this.id = Tree.randomId();
        this.path = Paths.get("gradle-" + version + "/**/" + "gradle-wrapper-*.jar!gradle-wrapper.jar");
    }

    public RemoteArchive asRemoteArchive() {
        return new RemoteArchive(id, sourcePath, markers, uri, charset, charsetBomMarked, fileAttributes,
                description, path);
    }

    @Override
    public InputStream getInputStream(HttpSender httpSender) {
        //noinspection resource
        HttpSender.Response response = httpSender.send(httpSender.get(uri.toString()).build());
        InputStream body = response.getBody();
        return readIntoArchive(body, path.toString());
    }


    private InputStream readIntoArchive(InputStream body, String pathPattern) {
        String pathBeforeBang;
        String pathAfterBang = null;
        int bangIndex = pathPattern.indexOf('!');
        if(bangIndex == -1) {
            pathBeforeBang = pathPattern;
        } else {
            pathBeforeBang = pathPattern.substring(0, bangIndex);
            pathAfterBang = pathPattern.substring(bangIndex + 1);
        }

        ZipInputStream zis = new ZipInputStream(body);

        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Starting from Gradle 7.5 a gradle-wrapper-shared.jar is introduced but it does not contain the gradle-wrapper.jar
                if (StringUtils.matchesGlob(entry.getName(), pathBeforeBang) && !entry.getName().contains("gradle-wrapper-shared")) {
                    if(pathAfterBang == null) {
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
                    } else {
                        return readIntoArchive(zis, pathAfterBang);
                    }
                }
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load path " + pathPattern + " in zip file " + uri, e);
        }
        throw new IllegalArgumentException("Unable to find path " + pathPattern + " in zip file " + uri);
    }
}
