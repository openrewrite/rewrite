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
package org.openrewrite.javascript.internal.lock.berry;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * The archive @yarnpkg/libzip's ZipFS writes under {@code compressionLevel: 0} (Yarn 4.10.3).
 */
final class ZipArchive {

    // SAFE_TIME, 1984-06-22 21:50:00, as DOS time and date
    private static final int DOS_TIME = 44608;
    private static final int DOS_DATE = 2262;
    private static final int VERSION_MADE_BY = 0x033f;
    private static final int S_IFDIR = 0040000;
    private static final int S_IFREG = 0100000;
    private static final int S_IFLNK = 0120000;

    private static final class Entry {
        int mode;
        byte[] data;

        Entry(int mode, byte[] data) {
            this.mode = mode;
            this.data = data;
        }
    }

    /**
     * Node's EEXIST from a ZipFS operation.
     */
    static final class AlreadyExists extends RuntimeException {
        AlreadyExists(String message) {
            super("EEXIST: file already exists, " + message);
        }
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    /**
     * {@code mkdirpSync(dir, {chmod: 0o755})}: each missing directory from the top.
     */
    void mkdirp(String dir) {
        if (".".equals(dir) || dir.isEmpty()) {
            return;
        }
        String[] parts = dir.split("/");
        StringBuilder path = new StringBuilder();
        for (String part : parts) {
            path.append(part).append('/');
            entries.computeIfAbsent(path.toString(), k -> new Entry(S_IFDIR | 0755, new byte[0]));
        }
    }

    void mkdir(String path, int mode) {
        String key = path + "/";
        if (entries.containsKey(key) || entries.containsKey(path)) {
            throw new AlreadyExists("mkdir '/" + path + "'");
        }
        entries.put(key, new Entry(S_IFDIR | (mode & 0755), new byte[0]));
    }

    void writeFile(String path, byte[] content, int mode) {
        Entry existing = entries.get(path);
        if (existing != null) {
            existing.data = content;
            existing.mode = existing.mode & ~0777 | (mode & 0755);
        } else {
            entries.put(path, new Entry(S_IFREG | (mode & 0755), content));
        }
    }

    void symlink(String target, String path) {
        if (entries.containsKey(path) || entries.containsKey(path + "/")) {
            throw new AlreadyExists("symlink '" + target + "' -> '/" + path + "'");
        }
        entries.put(path, new Entry(S_IFLNK | 0777, target.getBytes(StandardCharsets.UTF_8)));
    }

    byte[] toByteArray() {
        ByteArrayOutputStream local = new ByteArrayOutputStream();
        ByteArrayOutputStream central = new ByteArrayOutputStream();
        CRC32 crc = new CRC32();
        long offset = 0;
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            byte[] nameBytes = e.getKey().getBytes(StandardCharsets.UTF_8);
            boolean directory = (e.getValue().mode & S_IFDIR) == S_IFDIR && (e.getValue().mode & S_IFREG) == 0;
            byte[] data = e.getValue().data;
            crc.reset();
            crc.update(data, 0, data.length);
            long checksum = directory ? 0 : crc.getValue();
            int versionNeeded = directory ? 20 : 10;

            u32(local, 0x04034b50);
            u16(local, versionNeeded);
            u16(local, 0);
            u16(local, 0);
            u16(local, DOS_TIME);
            u16(local, DOS_DATE);
            u32(local, checksum);
            u32(local, data.length);
            u32(local, data.length);
            u16(local, nameBytes.length);
            u16(local, 0);
            local.write(nameBytes, 0, nameBytes.length);
            local.write(data, 0, data.length);

            u32(central, 0x02014b50);
            u16(central, VERSION_MADE_BY);
            u16(central, versionNeeded);
            u16(central, 0);
            u16(central, 0);
            u16(central, DOS_TIME);
            u16(central, DOS_DATE);
            u32(central, checksum);
            u32(central, data.length);
            u32(central, data.length);
            u16(central, nameBytes.length);
            u16(central, 0);
            u16(central, 0);
            u16(central, 0);
            u16(central, 0);
            u32(central, ((long) e.getValue().mode << 16) & 0xFFFFFFFFL);
            u32(central, offset);
            central.write(nameBytes, 0, nameBytes.length);

            offset += 30 + nameBytes.length + data.length;
        }
        byte[] localBytes = local.toByteArray();
        byte[] centralBytes = central.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(localBytes, 0, localBytes.length);
        out.write(centralBytes, 0, centralBytes.length);
        u32(out, 0x06054b50);
        u16(out, 0);
        u16(out, 0);
        u16(out, entries.size());
        u16(out, entries.size());
        u32(out, centralBytes.length);
        u32(out, localBytes.length);
        u16(out, 0);
        return out.toByteArray();
    }

    private static void u16(ByteArrayOutputStream out, int v) {
        out.write(v & 0xff);
        out.write((v >>> 8) & 0xff);
    }

    private static void u32(ByteArrayOutputStream out, long v) {
        out.write((int) (v & 0xff));
        out.write((int) ((v >>> 8) & 0xff));
        out.write((int) ((v >>> 16) & 0xff));
        out.write((int) ((v >>> 24) & 0xff));
    }
}
