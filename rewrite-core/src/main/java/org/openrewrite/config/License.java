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

import java.util.Objects;

public class License {
    private static final String APACHEV2 = "https://www.apache.org/licenses/LICENSE-2.0";
    private static final String MSAL = "https://docs.moderne.io/licensing/moderne-source-available-license";
    private static final String MOD_PROPRIETARY = "https://docs.moderne.io/licensing/overview";

    public static final License apache2 = new License("Apache License Version 2.0", APACHEV2);
    public static final License msal = new License("Moderne Source Available", MSAL);
    public static final License moderneProprietary = new License("Moderne Proprietary", MOD_PROPRIETARY);

    private final String fullName;
    private final String url;

    private License(String fullName, String url) {
        this.fullName = fullName;
        this.url = url;
    }

    public static License representing(String fullName, String url) {
        switch (url) {
            case APACHEV2:
                return apache2;
            case MSAL:
                return msal;
            case MOD_PROPRIETARY:
                return moderneProprietary;
            default:
                return new License(fullName, url);
        }
    }

    public String fullName() {
        return fullName;
    }

    public String url() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        License license = (License) o;
        return Objects.equals(url, license.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }
}
