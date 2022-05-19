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
package org.openrewrite;

import lombok.*;
import org.openrewrite.internal.lang.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Value
public class FileAttributes {

    @Nullable
    List<PosixFilePermission> posixFilePermissions;

    @Nullable
    ZonedDateTime creationTime;

    @Nullable
    ZonedDateTime lastModifiedTime;

    @Nullable
    ZonedDateTime lastAccessTime;

    @Nullable
    Long size;

    @Nullable
    public static FileAttributes fromPath(Path path) {
        if (Files.exists(path)) {
            try {
                List<PosixFilePermission> permissions = new ArrayList<>(Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS));
                BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
                return new FileAttributes(permissions,
                        ZonedDateTime.from(basicFileAttributes.creationTime().toInstant().atZone(ZoneId.systemDefault())),
                        ZonedDateTime.from(basicFileAttributes.lastAccessTime().toInstant().atZone(ZoneId.systemDefault())),
                        ZonedDateTime.from(basicFileAttributes.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault())),
                        basicFileAttributes.size());
            } catch (IOException ignored) {}
        }
        return null;
    }
}
