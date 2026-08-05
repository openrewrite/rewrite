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
package org.openrewrite.javascript.internal.lock;

import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;

/**
 * Reproduces a Yarn Berry {@code checksum: <cacheKey>/<hash>}. The hash is SHA-512 of the deterministic,
 * <em>uncompressed</em> (STORED) zip Yarn builds from a package's registry tarball — remapped under
 * {@code node_modules/<name>/}, every entry stamped with a fixed 1984 timestamp. Nothing is deflated, so the
 * bytes (and thus the hash) are fully reproducible without Yarn: fetch the same immutable {@code dist.tarball},
 * repack it Yarn's way, hash it.
 */
public final class BerryZipChecksum {

    // Yarn stamps every zip entry with a fixed instant so the archive is byte-stable: 1984-06-22 21:50:00 in
    // DOS date/time encoding.
    private static final int DOS_TIME = 44608;
    private static final int DOS_DATE = 2262;
    private static final int VERSION_MADE_BY = 0x033f; // unix, zip spec 6.3

    private BerryZipChecksum() {
    }

    /** The full {@code <cacheKey>/<sha512hex>} checksum for {@code name}'s gzipped registry tarball. */
    public static String checksum(byte[] gzippedTarball, String name, String cacheKey) {
        byte[] zip = buildZip(gzippedTarball, name);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512").digest(zip);
            return cacheKey + "/" + hex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 unavailable", e);
        }
    }

    /** Rebuild Yarn's normalized STORED zip: tarball entries remapped under {@code node_modules/<name>/}. */
    private static byte[] buildZip(byte[] gzippedTarball, String name) {
        List<Entry> entries = new ArrayList<>();
        Set<String> dirs = new LinkedHashSet<>();
        String prefix = "node_modules/" + name + "/";
        for (TarEntry tar : parseTar(gunzip(gzippedTarball))) {
            int slash = tar.name.indexOf('/');
            if (slash < 0) {
                continue; // no leading component to strip (e.g. a bare pax_global_header)
            }
            String rel = tar.name.substring(slash + 1);
            if (rel.isEmpty()) {
                continue;
            }
            String target = prefix + rel;
            if (tar.directory) {
                ensureDir(target.endsWith("/") ? target : target + "/", dirs, entries);
            } else {
                ensureDir(target.substring(0, target.lastIndexOf('/') + 1), dirs, entries);
                int mode = (tar.mode & 0111) != 0 ? 0755 : 0644;
                entries.add(new Entry(target, false, mode, tar.data));
            }
        }
        return serialize(entries);
    }

    /** Emit the directory entry and any missing ancestors, lazily, in Yarn's parent-before-child order. */
    private static void ensureDir(String dir, Set<String> dirs, List<Entry> entries) {
        if (dir.isEmpty() || dirs.contains(dir)) {
            return;
        }
        int prev = dir.lastIndexOf('/', dir.length() - 2);
        ensureDir(prev < 0 ? "" : dir.substring(0, prev + 1), dirs, entries);
        dirs.add(dir);
        entries.add(new Entry(dir, true, 0755, new byte[0]));
    }

    private static byte[] serialize(List<Entry> entries) {
        ByteArrayOutputStream local = new ByteArrayOutputStream();
        ByteArrayOutputStream central = new ByteArrayOutputStream();
        CRC32 crc = new CRC32();
        int offset = 0;
        for (Entry e : entries) {
            byte[] nameBytes = e.name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            crc.reset();
            crc.update(e.data);
            long checksum = e.directory ? 0 : crc.getValue();
            int size = e.data.length;
            int versionNeeded = e.directory ? 20 : 10;

            ByteArrayOutputStream lh = new ByteArrayOutputStream();
            u32(lh, 0x04034b50);
            u16(lh, versionNeeded);
            u16(lh, 0);
            u16(lh, 0);
            u16(lh, DOS_TIME);
            u16(lh, DOS_DATE);
            u32(lh, checksum);
            u32(lh, size);
            u32(lh, size);
            u16(lh, nameBytes.length);
            u16(lh, 0);
            writeAll(local, lh.toByteArray(), nameBytes, e.data);

            long extAttr = (((e.directory ? 040000L : 0100000L) | e.mode) << 16) & 0xFFFFFFFFL;
            u32(central, 0x02014b50);
            u16(central, VERSION_MADE_BY);
            u16(central, versionNeeded);
            u16(central, 0);
            u16(central, 0);
            u16(central, DOS_TIME);
            u16(central, DOS_DATE);
            u32(central, checksum);
            u32(central, size);
            u32(central, size);
            u16(central, nameBytes.length);
            u16(central, 0);
            u16(central, 0);
            u16(central, 0);
            u16(central, 0);
            u32(central, extAttr);
            u32(central, offset);
            central.write(nameBytes, 0, nameBytes.length);

            offset += 30 + nameBytes.length + size;
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

    // --- tar (ustar) ---------------------------------------------------------

    private static final class TarEntry {
        final String name;
        final int mode;
        final boolean directory;
        final byte[] data;

        TarEntry(String name, int mode, boolean directory, byte[] data) {
            this.name = name;
            this.mode = mode;
            this.directory = directory;
            this.data = data;
        }
    }

    private static List<TarEntry> parseTar(byte[] tar) {
        List<TarEntry> out = new ArrayList<>();
        int o = 0;
        while (o + 512 <= tar.length) {
            if (allZero(tar, o)) {
                o += 512;
                continue;
            }
            char type = (char) tar[o + 156];
            if (type == 'x' || type == 'g' || type == 'L' || type == 'K') {
                // A PAX/GNU extended header would override the following entry's path or mode; this parser does
                // not apply those, so refuse rather than derive a wrong checksum. Rare for npm tarballs.
                throw new EngineFailure(Reason.CHECKSUM_UNAVAILABLE, null,
                        "tarball uses an unsupported PAX/GNU extended header (type '" + type + "')");
            }
            String name = octalName(tar, o);
            int mode = octal(tar, o + 100, 8);
            int size = octal(tar, o + 124, 12);
            byte[] data = new byte[size];
            System.arraycopy(tar, o + 512, data, 0, size);
            out.add(new TarEntry(name, mode, type == '5', data));
            o += 512 + ((size + 511) / 512) * 512;
        }
        return out;
    }

    private static String octalName(byte[] tar, int o) {
        String name = cString(tar, o, 100);
        String prefix = cString(tar, o + 345, 155);
        return prefix.isEmpty() ? name : prefix + "/" + name;
    }

    private static boolean allZero(byte[] b, int o) {
        for (int i = o; i < o + 512; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static String cString(byte[] b, int o, int len) {
        int end = o;
        while (end < o + len && b[end] != 0) {
            end++;
        }
        return new String(b, o, end - o, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static int octal(byte[] b, int o, int len) {
        int value = 0;
        for (int i = o; i < o + len; i++) {
            byte c = b[i];
            if (c >= '0' && c <= '7') {
                value = (value << 3) | (c - '0');
            }
        }
        return value;
    }

    // --- byte helpers --------------------------------------------------------

    private static byte[] gunzip(byte[] gzipped) {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(gzipped.length * 3);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new EngineFailure(Reason.CHECKSUM_UNAVAILABLE, null, "tarball is not valid gzip: " + e.getMessage());
        }
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

    private static void writeAll(ByteArrayOutputStream out, byte[]... parts) {
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16));
            sb.append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

    private static final class Entry {
        final String name;
        final boolean directory;
        final int mode;
        final byte[] data;

        Entry(String name, boolean directory, int mode, byte[] data) {
            this.name = name;
            this.directory = directory;
            this.mode = mode;
            this.data = data;
        }
    }
}
