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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Port of @yarnpkg/core sources/tgzUtils.ts {@code extractArchiveTo} over node-tar 6.1.11's Parse (Yarn 4.10.3).
 */
final class TgzUtils {

    private static final int MAX_META_ENTRY_SIZE = 1024 * 1024;
    private static final Pattern NUL_AND_REST = V8RegExp.of("\\0.*");
    private static final Pattern NUL_TO_END = V8RegExp.of("\\0.*$");

    private TgzUtils() {
    }

    /**
     * An entry as node-tar's ReadEntry presents it.
     */
    static final class TarEntry {
        final String type;
        final String path;
        final @Nullable Integer mode;
        final @Nullable String linkpath;
        final byte[] body;

        TarEntry(String type, String path, @Nullable Integer mode, @Nullable String linkpath, byte[] body) {
            this.type = type;
            this.path = path;
            this.mode = mode;
            this.linkpath = linkpath;
            this.body = body;
        }
    }

    static ZipArchive convertToZip(byte[] tgz, String prefixPath, int stripComponents) {
        ZipArchive zip = new ZipArchive();
        extractArchiveTo(tgz, zip, prefixPath, stripComponents);
        return zip;
    }

    static void extractArchiveTo(byte[] tgz, ZipArchive targetFs, String prefixPath, int stripComponents) {
        for (TarEntry entry : parseTar(maybeGunzip(tgz))) {
            if (ignore(entry, stripComponents)) {
                continue;
            }
            String[] parts = PosixPath.normalize(entry.path).replaceAll("/\\z", "").split("/", -1);
            if (parts.length <= stripComponents) {
                continue;
            }
            StringBuilder slicePath = new StringBuilder();
            for (int i = stripComponents; i < parts.length; i++) {
                slicePath.append(i > stripComponents ? "/" : "").append(parts[i]);
            }
            String mappedPath = PosixPath.normalize(prefixPath + "/" + slicePath);
            int mode = 0644;
            if ("Directory".equals(entry.type) || ((entry.mode == null ? 0 : entry.mode) & 0111) != 0) {
                mode |= 0111;
            }
            String parent = mappedPath.contains("/") ? mappedPath.substring(0, mappedPath.lastIndexOf('/')) : ".";
            switch (entry.type) {
                case "Directory":
                    targetFs.mkdirp(parent);
                    targetFs.mkdir(mappedPath, mode);
                    break;
                case "OldFile":
                case "File":
                    targetFs.mkdirp(parent);
                    targetFs.writeFile(mappedPath, entry.body, mode);
                    break;
                case "SymbolicLink":
                    targetFs.mkdirp(parent);
                    targetFs.symlink(entry.linkpath == null ? "" : entry.linkpath, mappedPath);
                    break;
                default:
                    break;
            }
        }
    }

    private static boolean ignore(TarEntry entry, int stripComponents) {
        if (entry.path.startsWith("/")) {
            return true;
        }
        String[] parts = entry.path.split("/", -1);
        for (String part : parts) {
            if ("..".equals(part)) {
                return true;
            }
        }
        return parts.length <= stripComponents;
    }

    /**
     * Parse's first write: a stream that starts with the gzip magic bytes is inflated, any other is read as a tar.
     * @throws ReportError when the gzip stream is corrupt, as Yarn fails to fetch the package
     */
    static byte[] maybeGunzip(byte[] tgz) {
        if (tgz.length < 2 || tgz[0] != (byte) 0x1f || tgz[1] != (byte) 0x8b) {
            return tgz;
        }
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(tgz))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(tgz.length * 3);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (EOFException e) {
            throw new ReportError(ReportError.EXCEPTION, "zlib: unexpected end of file");
        } catch (IOException e) {
            String message = String.valueOf(e.getMessage());
            throw new ReportError(ReportError.EXCEPTION, "zlib: " + ("Unsupported compression method".equals(message) ?
                    "unknown compression method" : "Corrupt GZIP trailer".equals(message) ? "incorrect data check" :
                    message));
        }
    }

    /**
     * node-tar's Parse over an uncompressed archive: each entry it emits, in order.
     */
    static List<TarEntry> parseTar(byte[] tar) {
        List<TarEntry> out = new ArrayList<>();
        Map<String, Object> ex = null;
        Map<String, Object> gex = null;
        boolean sawNullBlock = false;
        int position = 0;
        while (position + 512 <= tar.length) {
            Header header = new Header(tar, position, ex, gex);
            position += 512;
            if (header.nullBlock) {
                if (sawNullBlock) {
                    break;
                }
                sawNullBlock = true;
                continue;
            }
            sawNullBlock = false;
            if (!header.cksumValid || header.path == null || header.path.isEmpty()) {
                continue;
            }
            String type = header.type();
            boolean link = "Link".equals(type) || "SymbolicLink".equals(type);
            if (link == (header.linkpath == null || header.linkpath.isEmpty())) {
                continue;
            }
            long size = header.size == null ? 0 : header.size;
            // ReadEntry's blockRemain: below zero, any byte written to the entry is more than it takes
            if (size < 0 && 512 * (long) Math.ceil(size / 512.0) < 0 && position < tar.length) {
                throw new ReportError(ReportError.EXCEPTION, "writing more to entry than is appropriate");
            }
            byte[] body = new byte[(int) Math.max(0, Math.min(size, tar.length - position))];
            System.arraycopy(tar, position, body, 0, body.length);
            position = size >= tar.length - position ? tar.length :
                    (int) Math.min(tar.length, position + 512 * (long) Math.ceil(size / 512.0));

            boolean meta = false;
            boolean ignored = false;
            switch (type) {
                case "File":
                case "OldFile":
                case "Link":
                case "SymbolicLink":
                case "CharacterDevice":
                case "BlockDevice":
                case "Directory":
                case "FIFO":
                case "ContiguousFile":
                case "GNUDumpDir":
                    break;
                case "NextFileHasLongLinkpath":
                case "NextFileHasLongPath":
                case "OldGnuLongPath":
                case "GlobalExtendedHeader":
                case "ExtendedHeader":
                case "OldExtendedHeader":
                    meta = true;
                    break;
                default:
                    ignored = true;
            }
            if (meta) {
                if (size > MAX_META_ENTRY_SIZE || size == 0) {
                    continue;
                }
                String metaText = new String(body, StandardCharsets.UTF_8);
                switch (type) {
                    case "ExtendedHeader":
                    case "OldExtendedHeader":
                        ex = pax(metaText, ex);
                        break;
                    case "GlobalExtendedHeader":
                        gex = pax(metaText, gex);
                        break;
                    case "NextFileHasLongPath":
                    case "OldGnuLongPath":
                        ex = ex == null ? new LinkedHashMap<>() : ex;
                        ex.put("path", V8RegExp.replace(metaText, NUL_AND_REST, false, ""));
                        break;
                    default:
                        ex = ex == null ? new LinkedHashMap<>() : ex;
                        ex.put("linkpath", V8RegExp.replace(metaText, NUL_AND_REST, false, ""));
                }
                continue;
            }
            Map<String, Object> entryEx = ex;
            ex = null;
            if (ignored) {
                continue;
            }
            // ReadEntry slurps the extended headers again over the header's own fields, after the ustar prefix
            String path = header.path;
            String linkpath = header.linkpath;
            Integer mode = header.mode == null || header.mode == 0 ? null : (int) (header.mode & 07777);
            if (entryEx != null && entryEx.get("path") != null) {
                path = (String) entryEx.get("path");
            }
            if (entryEx != null && entryEx.get("linkpath") != null) {
                linkpath = (String) entryEx.get("linkpath");
            }
            if (gex != null && gex.get("linkpath") != null) {
                linkpath = (String) gex.get("linkpath");
            }
            out.add(new TarEntry(type, path, mode, linkpath, body));
        }
        return out;
    }

    // Pax.parse(string, previous): the records over the previous header's, numeric values as numbers
    private static Map<String, Object> pax(String string, @Nullable Map<String, Object> previous) {
        Map<String, Object> kv = new LinkedHashMap<>();
        for (String line : string.replaceAll("\n\\z", "").split("\n", -1)) {
            double n = Js.parseInt(line, 10);
            if (n != line.getBytes(StandardCharsets.UTF_8).length + 1) {
                continue;
            }
            line = Js.slice(line, (Js.numberToString(n) + " ").length());
            int eq = line.indexOf('=');
            String k = (eq == -1 ? line : line.substring(0, eq)).replaceAll("^SCHILY\\.(dev|ino|nlink)", "$1");
            if (k.isEmpty()) {
                continue;
            }
            String v = eq == -1 ? "" : line.substring(eq + 1);
            kv.put(k, v.matches("[0-9]+") ? (Object) Js.toNumber(v) : v);
        }
        Map<String, Object> merged = previous == null ? new LinkedHashMap<>() : previous;
        merged.putAll(kv);
        Map<String, Object> result = new LinkedHashMap<>();
        for (String field : new String[]{"path", "linkpath", "size"}) {
            Object value = merged.get(field);
            if (Js.truthy(value)) {
                result.put(field, "size".equals(field) ? value : Js.string(value));
            }
        }
        return result;
    }

    /**
     * node-tar's Header {@code decode}.
     */
    private static final class Header {
        @Nullable String path;
        @Nullable Long mode;
        @Nullable Long size;
        @Nullable String linkpath;
        String typeFlag;
        boolean cksumValid;
        boolean nullBlock;

        Header(byte[] buf, int off, @Nullable Map<String, Object> ex, @Nullable Map<String, Object> gex) {
            path = decString(buf, off, 100);
            mode = decNumber(buf, off + 100, 8);
            size = decNumber(buf, off + 124, 12);
            Long cksum = decNumber(buf, off + 148, 12);
            slurp(ex, false);
            slurp(gex, true);
            typeFlag = decString(buf, off + 156, 1);
            if (typeFlag.isEmpty()) {
                typeFlag = "0";
            }
            if ("0".equals(typeFlag) && path != null && path.endsWith("/")) {
                typeFlag = "5";
            }
            if ("5".equals(typeFlag)) {
                size = 0L;
            }
            linkpath = decString(buf, off + 157, 100);
            if ("ustar\u000000".equals(new String(buf, off + 257, 8, StandardCharsets.UTF_8))) {
                if (buf[off + 475] != 0) {
                    path = decString(buf, off + 345, 155) + "/" + path;
                } else {
                    String prefix = decString(buf, off + 345, 130);
                    if (!prefix.isEmpty()) {
                        path = prefix + "/" + path;
                    }
                }
            }
            long sum = 8 * 0x20;
            for (int i = off; i < off + 148; i++) {
                sum += buf[i] & 0xff;
            }
            for (int i = off + 156; i < off + 512; i++) {
                sum += buf[i] & 0xff;
            }
            cksumValid = cksum != null && sum == cksum;
            if (cksum == null && sum == 8 * 0x20) {
                nullBlock = true;
            }
        }

        private void slurp(@Nullable Map<String, Object> ex, boolean global) {
            if (ex == null) {
                return;
            }
            for (Map.Entry<String, Object> field : ex.entrySet()) {
                if (field.getValue() == null || global && "path".equals(field.getKey())) {
                    continue;
                }
                switch (field.getKey()) {
                    case "path":
                        path = (String) field.getValue();
                        break;
                    case "linkpath":
                        linkpath = (String) field.getValue();
                        break;
                    case "size":
                        size = ((Number) field.getValue()).longValue();
                        break;
                    default:
                        break;
                }
            }
        }

        String type() {
            switch (typeFlag) {
                case "0":
                    return "File";
                case "1":
                    return "Link";
                case "2":
                    return "SymbolicLink";
                case "3":
                    return "CharacterDevice";
                case "4":
                    return "BlockDevice";
                case "5":
                    return "Directory";
                case "6":
                    return "FIFO";
                case "7":
                    return "ContiguousFile";
                case "g":
                    return "GlobalExtendedHeader";
                case "x":
                    return "ExtendedHeader";
                case "A":
                    return "SolarisACL";
                case "D":
                    return "GNUDumpDir";
                case "I":
                    return "Inode";
                case "K":
                    return "NextFileHasLongLinkpath";
                case "L":
                    return "NextFileHasLongPath";
                case "M":
                    return "ContinuationFile";
                case "N":
                    return "OldGnuLongPath";
                case "S":
                    return "SparseFile";
                case "V":
                    return "TapeVolumeHeader";
                case "X":
                    return "OldExtendedHeader";
                default:
                    return typeFlag;
            }
        }

        private static String decString(byte[] buf, int off, int size) {
            return V8RegExp.replace(new String(buf, off, size, StandardCharsets.UTF_8), NUL_AND_REST, false, "");
        }

        private static @Nullable Long decNumber(byte[] buf, int off, int size) {
            if ((buf[off] & 0x80) != 0) {
                long value = 0;
                for (int i = off + 1; i < off + size; i++) {
                    value = value * 256 + (buf[i] & 0xff);
                }
                return value;
            }
            String field = V8RegExp.replace(new String(buf, off, size, StandardCharsets.UTF_8), NUL_TO_END, false, "");
            double value = Js.parseInt(Js.trim(field), 8);
            return Double.isNaN(value) ? null : (long) value;
        }
    }
}
