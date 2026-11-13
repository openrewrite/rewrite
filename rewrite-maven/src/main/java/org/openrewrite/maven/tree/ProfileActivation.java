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
package org.openrewrite.maven.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.VersionRequirement;

import static java.util.Collections.singletonList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class ProfileActivation {
    @Nullable
    Boolean activeByDefault;

    @Nullable
    String jdk;

    @Nullable
    Property property;

    public static boolean isActive(@Nullable String id, Iterable<String> activeProfiles,
                                   @Nullable ProfileActivation activation) {
        if (isDeactivated(id, activeProfiles)) {
            return false;
        }
        boolean anyExplicitlyActivated = false;
        for (String activeProfile : activeProfiles) {
            String profile = activeProfile.trim();
            if (isDeactivation(profile)) {
                continue;
            }
            anyExplicitlyActivated = true;
            if (profile.equals(id)) {
                return true;
            }
        }
        return activation != null &&
               (activation.isActive() ||
                // Active by default is *only* enabled when no other profile is marked active by any other mechanism
                // So even this check for any other explicit activation is overly broad
                (Boolean.TRUE.equals(activation.getActiveByDefault()) && !anyExplicitlyActivated));
    }

    /**
     * Maven's {@code -P !id} (equivalently {@code -P -id}) suppresses a profile regardless of how it would
     * otherwise have activated, including one activated by {@code -P} in the same invocation.
     *
     * @param id             The id of the profile being considered.
     * @param activeProfiles Profiles named on the command line, deactivations included with their prefix intact.
     */
    public static boolean isDeactivated(@Nullable String id, Iterable<String> activeProfiles) {
        if (id == null) {
            return false;
        }
        for (String activeProfile : activeProfiles) {
            String profile = activeProfile.trim();
            if (isDeactivation(profile) && profile.substring(1).trim().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDeactivation(String profile) {
        return !profile.isEmpty() && (profile.charAt(0) == '!' || profile.charAt(0) == '-');
    }

    public boolean isActive() {
        return isActiveByJdk() || isActiveByProperty();
    }

    private boolean isActiveByJdk() {
        if (jdk == null) {
            return false;
        }

        String version = System.getProperty("java.version");
        if (version.startsWith(jdk)) {
            return true;
        }

        try {
            return version.equals(VersionRequirement.fromVersion(jdk, 0).resolve(() -> singletonList(version)));
        } catch (MavenDownloadingException e) {
            // unreachable
            return false;
        }
    }

    private boolean isActiveByProperty() {
        if (property == null) {
            return false;
        }

        String name = property.getName().trim();

        if (name.startsWith("!")) {
            return !System.getenv().containsKey(name.replace("!", ""));
        }

        if (property.getValue() == null) {
            return System.getenv().containsKey(name);
        }

        return property.getValue().equals(System.getenv(name));
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Property {
        String name;

        @Nullable
        String value;
    }
}
