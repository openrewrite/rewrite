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

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public final class LicenseNotAcceptedException extends RuntimeException {
    private static final StackTraceElement[] DISABLE_STACKTRACE = new StackTraceElement[]{};

    private final Collection<License> unacceptedLicenses;

    LicenseNotAcceptedException(Collection<License> unacceptedLicenses) {
        super(buildMessage(unacceptedLicenses));
        this.unacceptedLicenses = unacceptedLicenses;

    }

    @SuppressWarnings("unused")
    public Collection<License> getUnacceptedLicenses() {
        return Collections.unmodifiableCollection(unacceptedLicenses);
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return DISABLE_STACKTRACE;
    }

    private static String buildMessage(Collection<License> unacceptedLicenses) {
        StringBuilder sb = new StringBuilder("Unaccepted licenses detected on the classpath:");
        for (License unacceptedLicense : unacceptedLicenses) {
            sb.append("\n    - ").append(unacceptedLicense.getName());
            if (unacceptedLicense.getUrl() != null) {
                sb.append("(")
                        .append(unacceptedLicense.getUrl())
                        .append(")");
            }
            if (!unacceptedLicense.getModules().isEmpty()) {
                sb.append(" is detected in:");
                for (String module : unacceptedLicense.getModules()) {
                    sb.append("\n        - ")
                            .append(module);
                }
            }
        }
        sb.append("\nEither add command line arguments \"")
                .append(unacceptedLicenses.stream().map(License::getName).map(LicenseVerifier::asOption).map(option -> "-Drewrite.acceptedLicense." + option.replaceAll(" ", "_")).collect(Collectors.joining(" ")))
                .append("\" to accept licenses or look at plugin specific solutions.\n")
                .append("This only needs to be done once. Previously accepted licenses can be revoked again by removing the corresponding lines from ")
                .append(System.getProperty("user.home"))
                .append("/.rewrite/licenses.properties");

        return sb.toString();
    }
}
