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
                return new FileAttributes(basicFileAttributes.creationTime().toInstant().atZone(ZoneId.systemDefault()),
                        basicFileAttributes.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()),
                        basicFileAttributes.lastAccessTime().toInstant().atZone(ZoneId.systemDefault()),
                        Files.isReadable(path),
                        Files.isWritable(path),
                        Files.isExecutable(path),
                        basicFileAttributes.size());
            } catch (IOException ignored) {}
        }
        return null;
    }

    /**
     * Timestamps travel as ISO-8601 strings; the wire mapper writes a raw {@link ZonedDateTime}
     * as an epoch number, dropping the zone and nanos. Field order is the protocol; peers mirror it.
     */
    @Override
    public void rpcSend(FileAttributes after, RpcSendQueue q) {
        q.getAndSend(after, a -> iso(a.getCreationTime()));
        q.getAndSend(after, a -> iso(a.getLastModifiedTime()));
        q.getAndSend(after, a -> iso(a.getLastAccessTime()));
        q.getAndSend(after, FileAttributes::isReadable);
        q.getAndSend(after, FileAttributes::isWritable);
        q.getAndSend(after, FileAttributes::isExecutable);
        q.getAndSend(after, FileAttributes::getSize);
    }

    private static @Nullable String iso(@Nullable ZonedDateTime time) {
        return time == null ? null : time.toString();
    }

    @Override
    public FileAttributes rpcReceive(FileAttributes before, RpcReceiveQueue q) {
        return before
                .withCreationTime(q.receiveAndGet(before.getCreationTime(), (String iso) -> ZonedDateTime.parse(iso)))
                .withLastModifiedTime(q.receiveAndGet(before.getLastModifiedTime(), (String iso) -> ZonedDateTime.parse(iso)))
                .withLastAccessTime(q.receiveAndGet(before.getLastAccessTime(), (String iso) -> ZonedDateTime.parse(iso)))
                .withReadable(q.receive(before.isReadable()))
                .withWritable(q.receive(before.isWritable()))
                .withExecutable(q.receive(before.isExecutable()))
                .withSize(q.receive(before.getSize()));
    }
}
