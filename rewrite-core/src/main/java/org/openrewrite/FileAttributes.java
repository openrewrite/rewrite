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

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Value
@With
public class FileAttributes implements RpcCodec<FileAttributes> {
    @Nullable
    ZonedDateTime creationTime;

    @Nullable
    ZonedDateTime lastModifiedTime;

    @Nullable
    ZonedDateTime lastAccessTime;

    boolean isReadable;

    boolean isWritable;

    boolean isExecutable;

    long size;

    public static @Nullable FileAttributes fromPath(Path path) {
        if (Files.exists(path)) {
            try {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
                return new FileAttributes(ZonedDateTime.from(basicFileAttributes.creationTime().toInstant().atZone(ZoneId.systemDefault())),
                        ZonedDateTime.from(basicFileAttributes.lastAccessTime().toInstant().atZone(ZoneId.systemDefault())),
                        ZonedDateTime.from(basicFileAttributes.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault())),
                        Files.isReadable(path),
                        Files.isWritable(path),
                        Files.isExecutable(path),
                        basicFileAttributes.size());
            } catch (IOException ignored) {}
        }
        return null;
    }

    @Override
    public void rpcSend(FileAttributes after, RpcSendQueue q) {
        q.getAndSend(after, FileAttributes::getCreationTime);
        q.getAndSend(after, FileAttributes::getLastModifiedTime);
        q.getAndSend(after, FileAttributes::getLastAccessTime);
        q.getAndSend(after, FileAttributes::isReadable);
        q.getAndSend(after, FileAttributes::isWritable);
        q.getAndSend(after, FileAttributes::isExecutable);
        q.getAndSend(after, FileAttributes::getSize);
    }

    @Override
    public FileAttributes rpcReceive(FileAttributes before, RpcReceiveQueue q) {
        return before
                .withCreationTime(q.receive(before.getCreationTime()))
                .withLastModifiedTime(q.receive(before.getLastModifiedTime()))
                .withLastAccessTime(q.receive(before.getLastAccessTime()))
                .withReadable(q.receive(before.isReadable()))
                .withWritable(q.receive(before.isWritable()))
                .withExecutable(q.receive(before.isExecutable()))
                .withSize(q.receive(before.getSize()));
    }
}
