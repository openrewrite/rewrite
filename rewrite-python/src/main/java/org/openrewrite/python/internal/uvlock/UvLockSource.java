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

import lombok.Value;
import lombok.With;

/**
 * A package {@code source} inline table, e.g. {@code { registry = "https://pypi.org/simple" }}.
 * The value (URL or path) is recorded verbatim, including trailing-slash presence. For {@code git}
 * the value carries the resolved commit as a {@code #<sha>} suffix; for {@code url} it is the
 * direct download URL of the pinned sdist/wheel; for {@code directory} it is a local directory path.
 */
@Value
@With
public class UvLockSource {

    public enum Type {
        REGISTRY("registry"),
        VIRTUAL("virtual"),
        EDITABLE("editable"),
        PATH("path"),
        DIRECTORY("directory"),
        GIT("git"),
        URL("url");

        private final String key;

        Type(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public static Type fromKey(String key) {
            for (Type type : values()) {
                if (type.key.equals(key)) {
                    return type;
                }
            }
            throw new UvLockFormatException("Unrecognized package source type: " + key);
        }
    }

    Type type;
    String value;

    public static UvLockSource registry(String url) {
        return new UvLockSource(Type.REGISTRY, url);
    }

    public static UvLockSource virtual(String path) {
        return new UvLockSource(Type.VIRTUAL, path);
    }

    public static UvLockSource editable(String path) {
        return new UvLockSource(Type.EDITABLE, path);
    }
}
