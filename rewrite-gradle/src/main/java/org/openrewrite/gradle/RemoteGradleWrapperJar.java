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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.openrewrite.Checksum;
import org.openrewrite.FileAttributes;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
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
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class RemoteGradleWrapperJar implements Remote {
    @EqualsAndHashCode.Include
    UUID id;

    Path sourcePath;
    Markers markers;
    URI uri;

    String gradleVersion;

    @Nullable
    Checksum checksum;

    @Override
    public Charset getCharset() {
        return null;
    }

    @Override
    public <T extends SourceFile> T withCharset(Charset charset) {
        //noinspection unchecked
        return (T) this;
    }

    @Override
    public boolean isCharsetBomMarked() {
        return false;
    }

    @Override
    public <T extends SourceFile> T withCharsetBomMarked(boolean marked) {
        //noinspection unchecked
        return (T) this;
    }

    static final FileAttributes WRAPPER_JAR_FILE_ATTRIBUTES = new FileAttributes(null, null, null, true, true, false, 0);
    public FileAttributes getFileAttributes() {
        return WRAPPER_JAR_FILE_ATTRIBUTES;
    }

    @Override
    public <T extends SourceFile> T withFileAttributes(@Nullable FileAttributes fileAttributes) {
        //noinspection unchecked
        return (T) this;
    }

    @Language("markdown")
    public String getDescription() {
        return "Gradle wrappers include a jar file named gradle-wrapper.jar.";
    }

    public <T extends Remote> T withDescription(String description) {
        //noinspection unchecked
        return (T) this;
    }

    @Override
    public InputStream getInputStream(HttpSender httpSender) {
        // The versioned wrapper jar lives inside the gradle distribution
        // The wrapper which gets placed in gradle/wrappers/gradle-wrapper.jar lives inside the versioned wrapper jar
        Path versionedWrapperJarPath = Paths.get("gradle-" + gradleVersion + "/lib/gradle-wrapper-" + gradleVersion + ".jar");

        RemoteArchive remoteArchive = new RemoteArchive(id, sourcePath, markers, uri, checksum, getCharset(),
                isCharsetBomMarked(), getFileAttributes(), getDescription(), versionedWrapperJarPath);
        ZipInputStream zis = new ZipInputStream(remoteArchive.getInputStream(httpSender));
        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("gradle-wrapper.jar".equals(entry.getName())) {
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
            throw new IllegalArgumentException("Unable to locate gradle-wrapper.jar within " + uri, e);
        }
        throw new IllegalArgumentException("Unable to locate gradle-wrapper.jar within " + uri);
    }

    public static RemoteGradleWrapperJar build(URI uri) {
        return build(uri, false);
    }

    public static RemoteGradleWrapperJar build(URI uri, boolean skipChecksum) {
        String uriStr = uri.toString();
        Matcher m = GradleProperties.VERSION_EXTRACTING_PATTERN.matcher(uriStr);
        if(!m.find()) {
            throw new IllegalArgumentException("Unable to determine Gradle version from URI " + uri);
        }
        String gradleVersion = m.group(1);
        Checksum checksum = null;
        if(!skipChecksum) {
            String shaUriStr = uriStr.substring(0, uriStr.lastIndexOf('/') + 1);
            URI shaUri = URI.create(shaUriStr + "gradle-" + gradleVersion + "-wrapper.jar.sha256");
            checksum = Checksum.fromUri(new HttpUrlConnectionSender(), shaUri);
        }

        return build(uri, gradleVersion, checksum);
    }

    public static RemoteGradleWrapperJar build(URI uri, String gradleVersion, @Nullable Checksum checksum) {
        return new RemoteGradleWrapperJar(Tree.randomId(), GradleProperties.WRAPPER_JAR_LOCATION, Markers.EMPTY, uri, gradleVersion, checksum);
    }
}
