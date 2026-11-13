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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of tar-stream 1.6.1 lib/extract.js and lib/headers.js {@code decode}, as yarn 1.22.22 bundles them, and of
 * 1.5.4, which yarn bundles before 1.7.0.
 */
final class TarStream {

    private static final String UNEXPECTED_END = "Unexpected end of data";
    private static final String INVALID_HEADER = "Invalid tar header. Maybe the tar is corrupted or it needs to be " +
                                                 "gunzipped?";

    /**
     * The tar-stream yarn bundles: 1.6.1 from yarn 1.8.0 (1.7.0's 1.6.0 extracts as it does), 1.5.4 before 1.7.0, whose
     * {@code mixinPax} takes no {@code size} from a pax header.
     */
    enum Release {
        V1_5_4,
        V1_6_1
    }

    private TarStream() {
    }

    static final class Header {
        String name;
        @Nullable
        String type;
        @Nullable
        String linkname;
        double size;

        /**
         * Whether the archive ends inside the entry's content: an extract that finishes where its data ends hands over
         * the bytes there are, which the entry's write had reached, or not, when yarn reads it.
         */
        boolean cut;

        Header(String name, @Nullable String type, @Nullable String linkname, double size) {
            this.name = name;
            this.type = type;
            this.linkname = linkname;
            this.size = size;
        }
    }

    /**
     * An entry with its content, the {@code size} bytes of the archive from {@code offset}.
     */
    interface OnEntry {
        void entry(Header header, byte[] archive, int offset, int size);
    }

    /**
     * Every entry in the order the archive holds them, each handed over before the next is read.
     * @throws JsError the error the extract stream emits: a header whose checksum is wrong, data that ends early.
     */
    static void extract(byte[] data, OnEntry onEntry) {
        extract(data, onEntry, Release.V1_6_1, true);
    }

    /**
     * {@code extract} of tar-stream {@code release} on a Writable that calls {@code _final}
     * ({@code failsWhereTheDataEnds}, readable-stream 2.3), which fails an archive that ends inside an entry, its padding
     * or a header; without it the extract finishes where the data ends, an entry it cuts short handed over
     * {@link Header#cut}.
     * @throws JsError the error the extract stream emits: a header whose checksum is wrong, data that ends early.
     */
    static void extract(byte[] data, OnEntry onEntry, Release release, boolean failsWhereTheDataEnds) {
        Map<String, String> pax = null;
        Map<String, String> paxGlobal = null;
        String gnuLongPath = null;
        String gnuLongLinkPath = null;
        int offset = 0;
        while (offset < data.length) {
            if (data.length - offset < 512) {
                if (!failsWhereTheDataEnds) {
                    return;
                }
                throw new JsError(JsError.ERROR, UNEXPECTED_END);
            }
            Header header = decode(Arrays.copyOfRange(data, offset, offset + 512));
            offset += 512;
            if (header == null) {
                continue;
            }
            int size = (int) (Double.isNaN(header.size) ? 0 : header.size);
            if ("gnu-long-path".equals(header.type) || "gnu-long-link-path".equals(header.type) ||
                "pax-global-header".equals(header.type) || "pax-header".equals(header.type)) {
                // a body or its padding the data cuts short: the next header never comes
                if (!failsWhereTheDataEnds && size >= 0 && data.length - offset < size + padding(size)) {
                    return;
                }
                byte[] body = take(data, offset, size);
                offset = drain(data, offset + size, size);
                if ("gnu-long-path".equals(header.type)) {
                    gnuLongPath = decodeStr(body, 0, body.length);
                } else if ("gnu-long-link-path".equals(header.type)) {
                    gnuLongLinkPath = decodeStr(body, 0, body.length);
                } else if ("pax-global-header".equals(header.type)) {
                    paxGlobal = decodePax(body);
                } else {
                    pax = decodePax(body);
                    if (paxGlobal != null) {
                        Map<String, String> merged = new LinkedHashMap<>(paxGlobal);
                        merged.putAll(pax);
                        pax = merged;
                    }
                }
                continue;
            }
            if (gnuLongPath != null) {
                header.name = gnuLongPath;
                gnuLongPath = null;
            }
            if (gnuLongLinkPath != null) {
                header.linkname = gnuLongLinkPath;
                gnuLongLinkPath = null;
            }
            if (pax != null) {
                mixinPax(header, pax, release);
                pax = null;
            }
            size = (int) (Double.isNaN(header.size) ? 0 : header.size);
            if (size == 0 || "directory".equals(header.type)) {
                onEntry.entry(header, data, offset, 0);
                continue;
            }
            if (!failsWhereTheDataEnds && size > 0 && data.length - offset < size + padding(size)) {
                // the entry's stream never ends: what the data holds of it is written, and the extract finishes
                header.cut = data.length - offset < size;
                onEntry.entry(header, data, offset, Math.min(size, data.length - offset));
                return;
            }
            require(data, offset, size);
            onEntry.entry(header, data, offset, size);
            offset = drain(data, offset + size, size);
        }
    }

    private static int padding(int size) {
        int overflow = size & 511;
        return overflow == 0 ? 0 : 512 - overflow;
    }

    private static byte[] take(byte[] data, int offset, int size) {
        require(data, offset, size);
        return Arrays.copyOfRange(data, offset, offset + size);
    }

    private static void require(byte[] data, int offset, int size) {
        if (size < 0 || data.length - offset < size) {
            throw new JsError(JsError.ERROR, UNEXPECTED_END);
        }
    }

    private static int drain(byte[] data, int offset, int size) {
        int overflow = size & 511;
        int drain = overflow == 0 ? 0 : 512 - overflow;
        if (data.length - offset < drain) {
            throw new JsError(JsError.ERROR, UNEXPECTED_END);
        }
        return offset + drain;
    }

    // a pax header's size from 1.6.0
    private static void mixinPax(Header header, Map<String, String> pax, Release release) {
        if (Js.truthy(pax.get("path"))) {
            header.name = pax.get("path");
        }
        if (Js.truthy(pax.get("linkpath"))) {
            header.linkname = pax.get("linkpath");
        }
        if (release == Release.V1_6_1 && Js.truthy(pax.get("size"))) {
            header.size = Js.parseInt(pax.get("size"), 10);
        }
    }

    static Map<String, String> decodePax(byte[] buf) {
        Map<String, String> result = new LinkedHashMap<>();
        int start = 0;
        while (start < buf.length) {
            int i = start;
            while (i < buf.length && buf[i] != 32) {
                i++;
            }
            double len = Js.parseInt(new String(buf, start, i - start, StandardCharsets.UTF_8), 10);
            if (Double.isNaN(len) || len == 0) {
                return result;
            }
            int end = (int) Math.min(start + len - 1, buf.length);
            String b = end > i + 1 ? new String(buf, i + 1, end - i - 1, StandardCharsets.UTF_8) : "";
            int keyIndex = b.indexOf('=');
            if (keyIndex == -1) {
                return result;
            }
            result.put(b.substring(0, keyIndex), b.substring(keyIndex + 1));
            start = (int) Math.min(start + len, buf.length);
        }
        return result;
    }

    static @Nullable Header decode(byte[] buf) {
        int typeflag = buf[156] == 0 ? 0 : (buf[156] & 0xff) - '0';
        String name = decodeStr(buf, 0, 100);
        double size = decodeOct(buf, 124, 12);
        String type = toType(typeflag);
        String linkname = buf[157] == 0 ? null : decodeStr(buf, 157, 100);
        if (buf[345] != 0) {
            name = decodeStr(buf, 345, 155) + "/" + name;
        }
        long c = cksum(buf);
        if (c == 8 * 32) {
            return null;
        }
        if (c != decodeOct(buf, 148, 8)) {
            throw new JsError(JsError.ERROR, INVALID_HEADER);
        }
        return new Header(name, type, linkname, size);
    }

    private static @Nullable String toType(int flag) {
        switch (flag) {
            case 0:
                return "file";
            case 1:
                return "link";
            case 2:
                return "symlink";
            case 3:
                return "character-device";
            case 4:
                return "block-device";
            case 5:
                return "directory";
            case 6:
                return "fifo";
            case 7:
                return "contiguous-file";
            case 72:
                return "pax-header";
            case 55:
                return "pax-global-header";
            case 27:
                return "gnu-long-link-path";
            case 28:
            case 30:
                return "gnu-long-path";
            default:
                return null;
        }
    }

    private static long cksum(byte[] block) {
        long sum = 8 * 32;
        for (int i = 0; i < 148; i++) {
            sum += block[i] & 0xff;
        }
        for (int j = 156; j < 512; j++) {
            sum += block[j] & 0xff;
        }
        return sum;
    }

    private static double decodeOct(byte[] buf, int start, int length) {
        byte[] val = Arrays.copyOfRange(buf, start, start + length);
        if ((val[0] & 0x80) != 0) {
            return parse256(val);
        }
        int offset = 0;
        while (offset < val.length && val[offset] == 32) {
            offset++;
        }
        int end = offset;
        while (end < val.length && val[end] != 32) {
            end++;
        }
        while (offset < end && val[offset] == 0) {
            offset++;
        }
        if (end == offset) {
            return 0;
        }
        return Js.parseInt(new String(val, offset, end - offset, StandardCharsets.UTF_8), 8);
    }

    private static double parse256(byte[] buf) {
        boolean positive;
        if ((buf[0] & 0xff) == 0x80) {
            positive = true;
        } else if ((buf[0] & 0xff) == 0xff) {
            positive = false;
        } else {
            return Double.NaN;
        }
        boolean zero = false;
        List<Integer> tuple = new ArrayList<>();
        for (int i = buf.length - 1; i > 0; i--) {
            int b = buf[i] & 0xff;
            if (positive) {
                tuple.add(b);
            } else if (zero && b == 0) {
                tuple.add(0);
            } else if (zero) {
                zero = false;
                tuple.add(0x100 - b);
            } else {
                tuple.add(0xff - b);
            }
        }
        double sum = 0;
        for (int i = 0; i < tuple.size(); i++) {
            sum += tuple.get(i) * Math.pow(256, i);
        }
        return positive ? sum : -1 * sum;
    }

    private static String decodeStr(byte[] val, int offset, int length) {
        int end = offset;
        while (end < offset + length && end < val.length && val[end] != 0) {
            end++;
        }
        return new String(val, offset, end - offset, StandardCharsets.UTF_8);
    }
}
