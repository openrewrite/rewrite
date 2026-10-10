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

import org.openrewrite.javascript.internal.lock.JsError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Port of gunzip-maybe 1.4.1 with is-gzip 1.0.0 and is-deflate 1.0.0, as yarn 1.22.22 bundles them.
 */
final class GunzipMaybe {

    private GunzipMaybe() {
    }

    /**
     * The bytes with every gzip or zlib layer taken off: the recursion limit resets itself once it goes below 0.
     * @throws JsError zlib's error on a layer that does not inflate.
     */
    static byte[] gunzip(byte[] data) {
        byte[] out = data;
        while (isGzip(out) || isDeflate(out)) {
            out = inflate(out, isGzip(out));
        }
        return out;
    }

    static boolean isGzip(byte[] buf) {
        return buf.length >= 3 && buf[0] == 31 && (buf[1] & 0xff) == 139 && buf[2] == 8;
    }

    static boolean isDeflate(byte[] buf) {
        return buf.length >= 2 && buf[0] == 0x78 && (buf[1] == 1 || (buf[1] & 0xff) == 0x9c ||
                                                      (buf[1] & 0xff) == 0xda);
    }

    private static byte[] inflate(byte[] data, boolean gzip) {
        try (InputStream in = gzip ? new GZIPInputStream(new ByteArrayInputStream(data)) :
                new InflaterInputStream(new ByteArrayInputStream(data))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (EOFException e) {
            throw new JsError(JsError.ERROR, "unexpected end of file");
        } catch (IOException e) {
            throw new JsError(JsError.ERROR, String.valueOf(e.getMessage()));
        }
    }
}
