/*
 * Copyright 2021 the original author or authors.
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

import lombok.Value;
import org.openrewrite.internal.lang.Nullable;

import java.io.Serializable;

@Value
public class License implements Serializable {
    String name;
    Type type;

    public static License fromName(@Nullable String license) {
        if (license == null) {
            return new License("", Type.Unknown);
        }

        switch (license) {
            case "Apache License, Version 2.0":
            case "The Apache Software License, Version 2.0":
                return new License(license, Type.Apache2);
            case "GNU Lesser General Public License":
            case "GNU Library General Public License":
                // example Lanterna
                return new License(license, Type.LGPL);
            case "Public Domain":
                return new License(license, Type.PublicDomain);
            default:
                if (license.contains("LGPL")) {
                    // example Checkstyle
                    return new License(license, Type.LGPL);
                } else if (license.contains("GPL") || license.contains("GNU General Public License")) {
                    // example com.buschmais.jqassistant:jqassistant-maven-plugin
                    // example com.github.mtakaki:dropwizard-circuitbreaker
                    return new License(license, Type.GPL);
                } else if (license.contains("CDDL")) {
                    return new License(license, Type.CDDL);
                } else if (license.contains("Creative Commons") || license.contains("CC0")) {
                    return new License(license, Type.CreativeCommons);
                } else if (license.contains("BSD")) {
                    return new License(license, Type.BSD);
                } else if (license.contains("MIT")) {
                    return new License(license, Type.MIT);
                } else if (license.contains("Eclipse") || license.contains("EPL")) {
                    return new License(license, Type.Eclipse);
                } else if (license.contains("Apache") || license.contains("ASF")) {
                    return new License(license, Type.Apache2);
                } else if (license.contains("Mozilla")) {
                    return new License(license, Type.Mozilla);
                } else if (license.toLowerCase().contains("GNU Lesser General Public License".toLowerCase()) ||
                        license.contains("GNU Library General Public License")) {
                    return new License(license, Type.LGPL);
                }
                return new License(license, Type.Unknown);
        }
    }

    public enum Type {
        Apache2,
        BSD,
        CDDL,
        CreativeCommons,
        Eclipse,
        GPL,
        LGPL,
        MIT,
        Mozilla,
        PublicDomain,
        Unknown
    }
}
