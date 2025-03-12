/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum License {
    Apache2("Apache License Version 2.0","https://www.apache.org/licenses/LICENSE-2.0"),
    MSAL("Moderne Source Available", "https://docs.moderne.io/licensing/moderne-source-available-license"),
    Proprietary("Moderne Proprietary","https://docs.moderne.io/licensing/overview");
    private final String fullName;
    private final String url;

    public static License fromFullName(String name) {
        for (License license : values()) {
            if (license.fullName.equalsIgnoreCase(name)) {
                return license;
            }
        }
        throw new IllegalArgumentException("Invalid license name: " + name);
    }

    public static License fromUrl(String url) {
        for (License license : values()) {
            if (license.url.equalsIgnoreCase(url)) {
                return license;
            }
        }
        throw new IllegalArgumentException("Invalid license url: " + url);
    }
}
