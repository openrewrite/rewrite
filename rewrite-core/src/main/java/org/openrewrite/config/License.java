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
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

@Value
@EqualsAndHashCode(of = "url")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class License {
    private static final String APACHEV2_URL = "https://www.apache.org/licenses/LICENSE-2.0";
    private static final String MSAL_URL = "https://docs.moderne.io/licensing/moderne-source-available-license";
    private static final String MOD_PROPRIETARY_URL = "https://docs.moderne.io/licensing/overview";

    public static final License APACHE_V2 = new License("Apache License Version 2.0", APACHEV2_URL);
    public static final License MODERNE_SOURCE_AVAILABLE = new License("Moderne Source Available", MSAL_URL);
    public static final License MODERNE_PROPRIETARY = new License("Moderne Proprietary", MOD_PROPRIETARY_URL);

    String fullName;
    String url;

    @Contract(value = "_, null -> null; !null, !null -> new; null, !null -> _", pure = true)
    public static @Nullable License of(@Nullable String fullName, @Nullable String url) {
        if (url != null) {
            switch (url) {
                case APACHEV2_URL:
                    return APACHE_V2;
                case MSAL_URL:
                    return MODERNE_SOURCE_AVAILABLE;
                case MOD_PROPRIETARY_URL:
                    return MODERNE_PROPRIETARY;
            }
            if (fullName != null) {
                return new License(fullName, url);
            }
        }
        return null;
    }
}
