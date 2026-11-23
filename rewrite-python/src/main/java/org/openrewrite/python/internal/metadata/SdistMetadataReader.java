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
package org.openrewrite.python.internal.metadata;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StreamUtils;
import org.openrewrite.ipc.http.HttpSender;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads an sdist and parses its top-level {@code PKG-INFO}. Whether the resulting
 * metadata is trustworthy is the caller's concern via {@link CoreMetadata#hasStaticRequiresDist()}.
 */
public class SdistMetadataReader {

    private SdistMetadataReader() {
    }

    public static @Nullable CoreMetadata read(HttpSender http, String sdistUrl) {
        String path = sdistUrl.toLowerCase(Locale.ROOT);
        int cut = path.indexOf('?');
        if (cut >= 0) {
            path = path.substring(0, cut);
        }
        cut = path.indexOf('#');
        if (cut >= 0) {
            path = path.substring(0, cut);
        }
        boolean zip = path.endsWith(".zip");
        boolean tarGz = path.endsWith(".tar.gz") || path.endsWith(".tgz");
        if (!zip && !tarGz) {
            return null;
        }
        try (HttpSender.Response response = http.send(http.get(sdistUrl).build())) {
            if (!response.isSuccessful()) {
                return null;
            }
            byte[] pkgInfo = zip ? pkgInfoFromZip(response.getBody()) : pkgInfoFromTarGz(response.getBody());
            return pkgInfo == null ? null : MetadataParser.parse(pkgInfo);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The requirements setuptools recorded in the sdist's {@code *.egg-info/requires.txt}, as PEP 508 strings. An
     * egg-info directory without that file records none. Null when the sdist has no egg-info or cannot be read.
     */
    public static @Nullable List<String> eggInfoRequirements(HttpSender http, String sdistUrl) {
        String path = sdistUrl.toLowerCase(Locale.ROOT).replaceFirst("[?#].*$", "");
        boolean zip = path.endsWith(".zip");
        if (!zip && !path.endsWith(".tar.gz") && !path.endsWith(".tgz")) {
            return null;
        }
        Map<String, byte[]> entries = new HashMap<>();
        try (HttpSender.Response response = http.send(http.get(sdistUrl).build())) {
            if (!response.isSuccessful()) {
                return null;
            }
            if (zip) {
                try (ZipInputStream in = new ZipInputStream(response.getBody())) {
                    ZipEntry entry;
                    while ((entry = in.getNextEntry()) != null) {
                        collectEggInfo(entry.getName(), entry.isDirectory(), in, entries);
                    }
                }
            } else {
                try (InputStream in = new GZIPInputStream(response.getBody())) {
                    byte[] header = new byte[512];
                    while (readFully(in, header) && !isZeroBlock(header)) {
                        long size = parseOctal(header, 124, 12);
                        if (size < 0) {
                            break;
                        }
                        byte type = header[156];
                        String name = tarName(header);
                        long padded = (size + 511) / 512 * 512;
                        if ((type == '0' || type == 0) && name.endsWith(".egg-info/requires.txt")) {
                            byte[] data = new byte[(int) size];
                            if (!readFully(in, data)) {
                                break;
                            }
                            collectEggInfo(name, false, new java.io.ByteArrayInputStream(data), entries);
                            skipFully(in, padded - size);
                        } else {
                            collectEggInfo(name, type == '5', null, entries);
                            skipFully(in, padded);
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        if (!entries.containsKey("egg-info")) {
            return null;
        }
        byte[] requires = entries.get("requires.txt");
        return requires == null ? new ArrayList<>() : parseRequiresTxt(new String(requires, StandardCharsets.UTF_8));
    }

    // an egg-info directory directly under the sdist's top-level directory belongs to the sdist itself
    private static void collectEggInfo(String name, boolean directory, @Nullable InputStream data,
                                       Map<String, byte[]> entries) throws IOException {
        String[] parts = (name.startsWith("./") ? name.substring(2) : name).split("/");
        if (parts.length < 2 || !parts[1].endsWith(".egg-info")) {
            return;
        }
        entries.put("egg-info", new byte[0]);
        if (!directory && parts.length == 3 && "requires.txt".equals(parts[2]) && data != null) {
            entries.put("requires.txt", StreamUtils.readAllBytes(data));
        }
    }

    /** {@code requires.txt} sections {@code [extra]}, {@code [:marker]} and {@code [extra:marker]} become markers. */
    static List<String> parseRequiresTxt(String text) {
        List<String> requirements = new ArrayList<>();
        String extra = null;
        String marker = null;
        for (String raw : text.split("\r?\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                String section = line.substring(1, line.length() - 1);
                int colon = section.indexOf(':');
                extra = colon >= 0 ? section.substring(0, colon).trim() : section.trim();
                marker = colon >= 0 ? section.substring(colon + 1).trim() : null;
                extra = extra.isEmpty() ? null : extra;
                continue;
            }
            List<String> conditions = new ArrayList<>();
            if (marker != null && !marker.isEmpty()) {
                conditions.add("(" + marker + ")");
            }
            if (extra != null) {
                conditions.add("extra == '" + extra + "'");
            }
            requirements.add(conditions.isEmpty() ? line : line + "; " + String.join(" and ", conditions));
        }
        return requirements;
    }

    private static byte @Nullable [] pkgInfoFromZip(InputStream body) throws IOException {
        byte[] best = null;
        int bestDepth = Integer.MAX_VALUE;
        try (ZipInputStream zip = new ZipInputStream(body)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                int depth = pkgInfoDepth(entry.getName());
                if (depth >= 0 && depth < bestDepth) {
                    best = StreamUtils.readAllBytes(zip);
                    bestDepth = depth;
                }
            }
        }
        return best;
    }

    private static byte @Nullable [] pkgInfoFromTarGz(InputStream body) throws IOException {
        byte[] best = null;
        int bestDepth = Integer.MAX_VALUE;
        try (InputStream in = new GZIPInputStream(body)) {
            byte[] header = new byte[512];
            while (readFully(in, header)) {
                if (isZeroBlock(header)) {
                    break;
                }
                long size = parseOctal(header, 124, 12);
                if (size < 0) {
                    break;
                }
                byte type = header[156];
                // '0' or NUL are regular files; pax/gnu long-name special entries fall through to skip
                boolean regular = type == '0' || type == 0;
                String name = tarName(header);
                int depth = regular ? pkgInfoDepth(name) : -1;
                long padded = (size + 511) / 512 * 512;
                if (depth >= 0 && depth < bestDepth) {
                    byte[] data = new byte[(int) size];
                    if (!readFully(in, data)) {
                        break;
                    }
                    best = data;
                    bestDepth = depth;
                    skipFully(in, padded - size);
                } else {
                    skipFully(in, padded);
                }
            }
        }
        return best;
    }

    private static String tarName(byte[] header) {
        String name = nulTerminated(header, 0, 100);
        String prefix = nulTerminated(header, 345, 155);
        return prefix.isEmpty() ? name : prefix + "/" + name;
    }

    /**
     * Slash count when the name is a {@code {anything}/PKG-INFO} candidate, -1 otherwise.
     * The shallowest candidate is the sdist's own PKG-INFO.
     */
    private static int pkgInfoDepth(String name) {
        if (name.startsWith("./")) {
            name = name.substring(2);
        }
        if (!name.endsWith("/PKG-INFO")) {
            return -1;
        }
        int depth = 0;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '/') {
                depth++;
            }
        }
        return depth;
    }

    private static String nulTerminated(byte[] b, int off, int len) {
        int end = off;
        while (end < off + len && b[end] != 0) {
            end++;
        }
        return new String(b, off, end - off, StandardCharsets.UTF_8).trim();
    }

    private static long parseOctal(byte[] b, int off, int len) {
        long value = 0;
        boolean seen = false;
        for (int i = off; i < off + len; i++) {
            byte c = b[i];
            if (c == 0 || c == ' ') {
                if (seen) {
                    break;
                }
                continue;
            }
            if (c < '0' || c > '7') {
                return -1;
            }
            value = value * 8 + (c - '0');
            seen = true;
        }
        return seen ? value : -1;
    }

    private static boolean isZeroBlock(byte[] block) {
        for (byte b : block) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean readFully(InputStream in, byte[] buf) throws IOException {
        int read = 0;
        while (read < buf.length) {
            int n = in.read(buf, read, buf.length - read);
            if (n < 0) {
                return false;
            }
            read += n;
        }
        return true;
    }


    // stand-in for InputStream.skipNBytes (Java 12); returns silently on EOF so a
    // truncated archive surfaces as a failed entry lookup rather than an exception
    private static void skipFully(InputStream in, long count) throws IOException {
        byte[] buf = new byte[8192];
        while (count > 0) {
            int n = in.read(buf, 0, (int) Math.min(buf.length, count));
            if (n < 0) {
                return;
            }
            count -= n;
        }
    }
}
