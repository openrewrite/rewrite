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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads {@code *.dist-info/METADATA} out of a remote wheel with HTTP Range requests
 * (the pip fast-deps / uv / poetry lazy-wheel technique): tail read for the end-of-central-directory
 * record and central directory, then a ranged read of just the METADATA entry. When the server
 * ignores Range, the single full response is streamed instead — never a second full download.
 */
public class LazyWheelMetadataReader {
    private static final int DEFAULT_TAIL_BYTES = 128 * 1024;
    private static final Pattern METADATA_ENTRY = Pattern.compile("[^/]+\\.dist-info/METADATA");

    private static final long EOCD_SIG = 0x06054b50L;
    private static final long CEN_SIG = 0x02014b50L;
    private static final long LOC_SIG = 0x04034b50L;
    private static final long ZIP64_EOCD_SIG = 0x06064b50L;
    private static final long ZIP64_LOCATOR_SIG = 0x07064b50L;

    private LazyWheelMetadataReader() {
    }

    public static @Nullable CoreMetadata read(HttpSender http, String wheelUrl) {
        return read(http, wheelUrl, DEFAULT_TAIL_BYTES);
    }

    static @Nullable CoreMetadata read(HttpSender http, String wheelUrl, int tailBytes) {
        try (HttpSender.Response response = http.send(http.get(wheelUrl)
                .withHeader("Range", "bytes=-" + tailBytes)
                .build())) {
            if (response.getCode() == 200) {
                // Range ignored: this response body is the whole wheel
                return extractFromStream(response.getBody());
            }
            if (response.getCode() != 206) {
                return null;
            }
            long tailStart = contentRangeStart(response);
            byte[] tail = response.getBodyAsBytes();
            return readFromTail(http, wheelUrl, tail, tailStart);
        } catch (Exception e) {
            return null;
        }
    }

    private static @Nullable CoreMetadata readFromTail(HttpSender http, String wheelUrl, byte[] tail, long tailStart) {
        int eocd = findEocd(tail);
        if (eocd < 0) {
            return null;
        }
        long cdOffset = u32(tail, eocd + 16);
        long cdSize = u32(tail, eocd + 12);
        boolean zip64 = cdOffset == 0xFFFFFFFFL || cdSize == 0xFFFFFFFFL || u16(tail, eocd + 10) == 0xFFFF;
        if (zip64) {
            int locator = eocd - 20;
            if (locator < 0 || u32(tail, locator) != ZIP64_LOCATOR_SIG) {
                return null;
            }
            long recordOffset = u64(tail, locator + 8);
            byte[] record;
            int recordPos;
            if (tailStart >= 0 && recordOffset >= tailStart) {
                record = tail;
                recordPos = (int) (recordOffset - tailStart);
            } else {
                Ranged r = rangeGet(http, wheelUrl, recordOffset, 56);
                if (r == null) {
                    return null;
                }
                if (r.whole != null) {
                    return r.whole;
                }
                record = r.bytes;
                recordPos = 0;
            }
            if (record == null || recordPos + 56 > record.length || u32(record, recordPos) != ZIP64_EOCD_SIG) {
                return null;
            }
            cdSize = u64(record, recordPos + 40);
            cdOffset = u64(record, recordPos + 48);
        } else if (tailStart < 0) {
            // No Content-Range: in a plain zip the central directory ends where the EOCD begins
            tailStart = cdOffset + cdSize - eocd;
        }

        byte[] cd;
        if (tailStart >= 0 && cdOffset >= tailStart && cdOffset + cdSize <= tailStart + tail.length) {
            cd = Arrays.copyOfRange(tail, (int) (cdOffset - tailStart), (int) (cdOffset - tailStart + cdSize));
        } else {
            Ranged r = rangeGet(http, wheelUrl, cdOffset, cdSize);
            if (r == null) {
                return null;
            }
            if (r.whole != null) {
                return r.whole;
            }
            cd = r.bytes;
            if (cd == null) {
                return null;
            }
        }
        return findAndFetchEntry(http, wheelUrl, cd, tail, tailStart);
    }

    private static @Nullable CoreMetadata findAndFetchEntry(HttpSender http, String wheelUrl, byte[] cd,
                                                            byte[] tail, long tailStart) {
        int p = 0;
        while (p + 46 <= cd.length && u32(cd, p) == CEN_SIG) {
            int method = u16(cd, p + 10);
            long compressedSize = u32(cd, p + 20);
            long uncompressedSize = u32(cd, p + 24);
            int nameLen = u16(cd, p + 28);
            int extraLen = u16(cd, p + 30);
            int commentLen = u16(cd, p + 32);
            long localHeaderOffset = u32(cd, p + 42);
            if (p + 46 + nameLen > cd.length) {
                return null;
            }
            String name = new String(cd, p + 46, nameLen, StandardCharsets.UTF_8);
            if (METADATA_ENTRY.matcher(name).matches()) {
                if (compressedSize == 0xFFFFFFFFL || uncompressedSize == 0xFFFFFFFFL || localHeaderOffset == 0xFFFFFFFFL) {
                    // ZIP64 extra field: 8-byte replacements appear in fixed order for each 0xFFFFFFFF field
                    int ep = p + 46 + nameLen;
                    int end = ep + extraLen;
                    while (ep + 4 <= end) {
                        int id = u16(cd, ep);
                        int size = u16(cd, ep + 2);
                        if (id == 0x0001) {
                            int fp = ep + 4;
                            if (uncompressedSize == 0xFFFFFFFFL) {
                                uncompressedSize = u64(cd, fp);
                                fp += 8;
                            }
                            if (compressedSize == 0xFFFFFFFFL) {
                                compressedSize = u64(cd, fp);
                                fp += 8;
                            }
                            if (localHeaderOffset == 0xFFFFFFFFL) {
                                localHeaderOffset = u64(cd, fp);
                            }
                            break;
                        }
                        ep += 4 + size;
                    }
                }
                return fetchEntry(http, wheelUrl, tail, tailStart, localHeaderOffset, compressedSize, method);
            }
            p += 46 + nameLen + extraLen + commentLen;
        }
        return null;
    }

    private static @Nullable CoreMetadata fetchEntry(HttpSender http, String wheelUrl, byte[] tail, long tailStart,
                                                     long localHeaderOffset, long compressedSize, int method) {
        byte[] buf;
        long bufStart;
        if (tailStart >= 0 && localHeaderOffset >= tailStart) {
            buf = tail;
            bufStart = tailStart;
        } else {
            // Local name/extra lengths may differ from the central directory's; over-fetch a little
            // slack so one request usually covers header + data (servers clamp past-EOF ranges).
            Ranged r = rangeGet(http, wheelUrl, localHeaderOffset, 30 + compressedSize + 8 * 1024);
            if (r == null) {
                return null;
            }
            if (r.whole != null) {
                return r.whole;
            }
            buf = r.bytes;
            if (buf == null) {
                return null;
            }
            bufStart = localHeaderOffset;
        }
        int hp = (int) (localHeaderOffset - bufStart);
        if (hp + 30 > buf.length || u32(buf, hp) != LOC_SIG) {
            return null;
        }
        long dataOffset = localHeaderOffset + 30 + u16(buf, hp + 26) + u16(buf, hp + 28);
        byte[] compressed;
        if (dataOffset - bufStart + compressedSize <= buf.length) {
            compressed = Arrays.copyOfRange(buf, (int) (dataOffset - bufStart), (int) (dataOffset - bufStart + compressedSize));
        } else {
            Ranged r = rangeGet(http, wheelUrl, dataOffset, compressedSize);
            if (r == null) {
                return null;
            }
            if (r.whole != null) {
                return r.whole;
            }
            compressed = r.bytes;
            if (compressed == null || compressed.length < compressedSize) {
                return null;
            }
        }
        byte[] metadata = decompress(compressed, method);
        return metadata == null ? null : MetadataParser.parse(metadata);
    }

    private static byte @Nullable [] decompress(byte[] compressed, int method) {
        if (method == ZipEntry.STORED) {
            return compressed;
        }
        if (method != ZipEntry.DEFLATED) {
            return null;
        }
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(compressed);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while (!inflater.finished()) {
                int n = inflater.inflate(buf);
                if (n > 0) {
                    out.write(buf, 0, n);
                } else if (inflater.needsInput() || inflater.needsDictionary()) {
                    return null;
                }
            }
            return out.toByteArray();
        } catch (DataFormatException e) {
            return null;
        } finally {
            inflater.end();
        }
    }

    private static @Nullable CoreMetadata extractFromStream(InputStream body) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(body)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (METADATA_ENTRY.matcher(entry.getName()).matches()) {
                    return MetadataParser.parse(StreamUtils.readAllBytes(zip));
                }
            }
        }
        return null;
    }

    private static int findEocd(byte[] tail) {
        int fallback = -1;
        for (int p = tail.length - 22; p >= 0; p--) {
            if (u32(tail, p) == EOCD_SIG) {
                if (p + 22 + u16(tail, p + 20) == tail.length) {
                    return p;
                }
                if (fallback < 0) {
                    fallback = p;
                }
            }
        }
        return fallback;
    }

    private static long contentRangeStart(HttpSender.Response response) {
        for (Map.Entry<String, List<String>> header : response.getHeaders().entrySet()) {
            if (header.getKey() == null || !"content-range".equalsIgnoreCase(header.getKey()) || header.getValue().isEmpty()) {
                continue;
            }
            String value = header.getValue().get(0).trim();
            if (value.startsWith("bytes ")) {
                int dash = value.indexOf('-');
                if (dash > 6) {
                    try {
                        return Long.parseLong(value.substring(6, dash).trim());
                    } catch (NumberFormatException ignored) {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    private static @Nullable Ranged rangeGet(HttpSender http, String url, long start, long length) {
        try (HttpSender.Response response = http.send(http.get(url)
                .withHeader("Range", "bytes=" + start + "-" + (start + length - 1))
                .build())) {
            if (response.getCode() == 206) {
                return new Ranged(response.getBodyAsBytes(), null);
            }
            if (response.getCode() == 200) {
                // Server stopped honoring Range mid-flight; use this full body rather than re-downloading
                return new Ranged(null, extractFromStream(response.getBody()));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static final class Ranged {
        final byte @Nullable [] bytes;
        final @Nullable CoreMetadata whole;

        Ranged(byte @Nullable [] bytes, @Nullable CoreMetadata whole) {
            this.bytes = bytes;
            this.whole = whole;
        }
    }

    private static int u16(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static long u32(byte[] b, int off) {
        return (long) u16(b, off) | ((long) u16(b, off + 2) << 16);
    }

    private static long u64(byte[] b, int off) {
        return u32(b, off) | (u32(b, off + 4) << 32);
    }
}
