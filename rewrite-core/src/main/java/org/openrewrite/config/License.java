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
