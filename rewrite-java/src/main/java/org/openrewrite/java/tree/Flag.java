/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;

public enum Flag {
    Public("public"),
    Private("private"),
    Protected("protected"),
    Static("static"),
    Final("final"),
    Synchronized("synchronized"),
    Volatile("volatile"),
    Transient("transient"),
    Abstract("abstract");

    private final String keyword;

    Flag(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return this.keyword;
    }

    @Nullable
    public static Flag fromKeyword(String keyword) {
        for (Flag flag : values()) {
            if (flag.keyword.equals(keyword)) {
                return flag;
            }
        }
        return null;
    }
}
