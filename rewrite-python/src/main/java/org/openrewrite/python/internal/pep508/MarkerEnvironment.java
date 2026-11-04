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
package org.openrewrite.python.internal.pep508;

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * The standard PEP 508 marker variables. A null value means the variable is unknown in
 * this environment; comparisons against it evaluate to unknown (three-valued logic).
 */
@Value
@Builder
public class MarkerEnvironment {
    @Nullable String pythonVersion;
    @Nullable String pythonFullVersion;
    @Nullable String osName;
    @Nullable String sysPlatform;
    @Nullable String platformMachine;
    @Nullable String platformSystem;
    @Nullable String platformRelease;
    @Nullable String platformVersion;
    @Nullable String platformPythonImplementation;
    @Nullable String implementationName;
    @Nullable String implementationVersion;
    @Nullable String extra;

    /**
     * The lock environment defaults: a Linux/x86_64 CPython. {@code python_full_version}
     * is derived as {@code pythonVersion + ".0"} when only the two-component version is
     * known; {@code platform_release} and {@code platform_version} are left unknown.
     */
    public static MarkerEnvironment lockDefaults(@Nullable String pythonVersion, @Nullable String pythonFullVersion) {
        String full = pythonFullVersion;
        if (full == null && pythonVersion != null) {
            full = pythonVersion + ".0";
        }
        String version = pythonVersion;
        if (version == null && full != null) {
            String[] parts = full.split("\\.");
            if (parts.length >= 2) {
                version = parts[0] + "." + parts[1];
            }
        }
        return MarkerEnvironment.builder()
                .pythonVersion(version)
                .pythonFullVersion(full)
                .osName("posix")
                .sysPlatform("linux")
                .platformMachine("x86_64")
                .platformSystem("Linux")
                .platformPythonImplementation("CPython")
                .implementationName("cpython")
                .implementationVersion(full)
                .build();
    }

    /**
     * Looks up a marker variable by its PEP 508 name; null when unknown.
     */
    public @Nullable String get(String variable) {
        switch (variable) {
            case "python_version":
                return pythonVersion;
            case "python_full_version":
                // packaging repairs non-tagged interpreter builds ending in "+".
                return pythonFullVersion != null && pythonFullVersion.endsWith("+") ?
                        pythonFullVersion + "local" : pythonFullVersion;
            case "os_name":
                return osName;
            case "sys_platform":
                return sysPlatform;
            case "platform_machine":
                return platformMachine;
            case "platform_system":
                return platformSystem;
            case "platform_release":
                return platformRelease;
            case "platform_version":
                return platformVersion;
            case "platform_python_implementation":
                return platformPythonImplementation;
            case "implementation_name":
                return implementationName;
            case "implementation_version":
                return implementationVersion;
            case "extra":
                return extra;
            default:
                return null;
        }
    }
}
