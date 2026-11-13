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
package org.openrewrite.python.internal.uvlock;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

/**
 * An {@code sdist} or wheel entry. Registry artifacts carry url/hash/size (+ upload-time
 * from revision 2 on); flat-index artifacts carry only a relative {@code path}.
 */
@Value
@With
@Builder(toBuilder = true)
public class UvLockArtifact {

    @Nullable
    String url;

    @Nullable
    String path;

    @Nullable
    String hash;

    @Nullable
    Long size;

    /**
     * RFC3339 timestamp verbatim from the file (uv trims trailing fractional zeros,
     * e.g. {@code "2026-07-07T14:33:57.9Z"}); never parsed or reformatted.
     */
    @Nullable
    String uploadTime;

    public static UvLockArtifact remote(String url, String hash, long size, @Nullable String uploadTime) {
        return new UvLockArtifact(url, null, hash, size, uploadTime);
    }

    public static UvLockArtifact local(String path) {
        return new UvLockArtifact(null, path, null, null, null);
    }
}
