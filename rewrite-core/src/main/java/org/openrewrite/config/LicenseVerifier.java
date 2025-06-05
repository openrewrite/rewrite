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

import lombok.Getter;
import org.openrewrite.internal.StringUtils;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LicenseVerifier {

    private static final String APACHE_LICENSE = "Apache License Version 2.0";
    private static final String MSAL_LICENSE = "Moderne Source Available License";
    private static final String MSAL_LICENSE_SHORT = "MSAL";
    private static final String MSAL_LICENSE_LOWERCASE = "moderne source available license";
    private static final String MPL_LICENSE = "Moderne Proprietary License";
    private static final String MPL_LICENSE_SHORT = "MPL";
    private static final String MPL_LICENSE_LOWERCASE = "moderne proprietary license";

    private static final String LICENSES_PATH = System.getProperty("user.home") + "/.rewrite/licenses.properties";

    private final Map<String, LicenseAcceptation> acceptedLicenses = new HashMap<>();
    private final Map<String, License> requiredLicenses;

    public static void verifyAllLicensesAccepted(Environment environment, Collection<String> additionallyAcceptedLicense) {
        new LicenseVerifier(environment.listLicenses(), additionallyAcceptedLicense).verify();
    }

    private LicenseVerifier(Set<License> requiredLicenses, Collection<String> additionallyAcceptedLicenses) {
        File userHomeRewriteLicenseConfig = new File(LICENSES_PATH);
        if (userHomeRewriteLicenseConfig.exists()) {
            try (BufferedReader licenses = new BufferedReader(new FileReader(userHomeRewriteLicenseConfig))) {
                licenses.lines().filter(Objects::nonNull).filter(s -> !s.trim().isEmpty()).map(LicenseAcceptation::ofProperty).forEach(license -> acceptedLicenses.put(license.getLicense(), license));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        this.requiredLicenses = requiredLicenses.stream()
                .filter(license -> !isAccepted(license.getName()))
                .collect(Collectors.toMap(License::getName, Function.identity()));

        acceptSystemPropertyLicenses();
        if (!this.requiredLicenses.isEmpty()) {
            additionallyAcceptedLicenses.forEach(this::acceptLicenseIfRequired);
        }
    }

    private void acceptLicenseIfRequired(String license) {
        String licenseName = asLicenseName(license);
        License requiredLicense = requiredLicenses.values().stream().filter(name -> name.getName().equalsIgnoreCase(licenseName)).findFirst().orElse(null);
        if (requiredLicense == null) {
            return;
        }
        acceptedLicenses.put(requiredLicense.getName(), new LicenseAcceptation(requiredLicense.getName(), Instant.now())); //renew acceptation timestamp
        requiredLicenses.remove(requiredLicense.getName());
    }

    private void verify() {
        if (!acceptedLicenses.isEmpty()) {
            File userHomeRewriteLicenseConfig = new File(LICENSES_PATH);
            userHomeRewriteLicenseConfig.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(userHomeRewriteLicenseConfig))) {
                for (LicenseAcceptation license : acceptedLicenses.values()) {
                    writer.write(license.getLicense() + "=" + license.getAcceptedAt().getEpochSecond());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (!requiredLicenses.isEmpty()) {
            throw new LicenseNotAcceptedException(requiredLicenses.values());
        }
    }

    private boolean isAccepted(String license) {
        String licenseName = asLicenseName(license);
        if (APACHE_LICENSE.equals(licenseName)) {
            return true;
        }
        if ((MSAL_LICENSE.equals(licenseName) || MPL_LICENSE.equals(licenseName)) && LicenseKey.ofModerneCli().isPresent()) {
            return true;
        }
        return acceptedLicenses.containsKey(licenseName);
    }

    private void acceptSystemPropertyLicenses() {
        if (!requiredLicenses.isEmpty()) {
            for (Object systemProp : System.getProperties().keySet()) {
                if (systemProp instanceof String) {
                    String key = (String) systemProp;
                    if (key.toLowerCase().startsWith("rewrite.acceptedlicense.")) {
                        String license = System.getProperty(key);
                        if (license != null) {
                            acceptLicenseIfRequired(key.substring("rewrite.acceptedlicense.".length()).replaceAll("_", " "));
                        }
                    }
                }
            }
        }
    }

    @Getter
    private static class LicenseAcceptation {
        private final String license;
        private final Instant acceptedAt;

        private LicenseAcceptation(String license, Instant acceptedAt) {
            this.license = asLicenseName(license);
            this.acceptedAt = acceptedAt;
        }

        private static LicenseAcceptation ofProperty(String property) {
            int splitOn = property.lastIndexOf("=");
            if (splitOn == -1) {
                throw new IllegalArgumentException("Invalid license property: " + property + "(does not contain a '=')");
            }
            String licenseName = property.substring(0, splitOn);
            String acceptedAt = property.substring(splitOn + 1);
            if (licenseName.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid license property: " + property + "(license name empty)");
            }
            if (acceptedAt.trim().isEmpty() || !StringUtils.isNumeric(acceptedAt)) {
                throw new IllegalArgumentException("Invalid license property: " + property + "(not an epoch-second/numeric value)");
            }

            return new LicenseAcceptation(licenseName, Instant.ofEpochSecond(Long.parseLong(acceptedAt)));
        }

    }

    public static String asLicenseName(String license) {
        switch (license.toUpperCase()) {
            case MSAL_LICENSE_SHORT: return MSAL_LICENSE;
            case MPL_LICENSE_SHORT: return MPL_LICENSE;
            default: return license;
        }
    }

    public static String asOption(String license) {
        switch (license.toLowerCase()) {
            case MSAL_LICENSE_LOWERCASE: return MSAL_LICENSE_SHORT;
            case MPL_LICENSE_LOWERCASE: return MPL_LICENSE_SHORT;
            default: return license;
        }
    }
}
