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
package org.openrewrite.maven;

import lombok.Getter;
import org.openrewrite.xml.tree.Xml;

@Getter
public class UncheckedMavenDownloadingException extends RuntimeException {
    private final Xml.Document pomWithWarnings;

    public UncheckedMavenDownloadingException(Xml.Document pom, MavenDownloadingExceptions e) {
        super("Failed to download dependencies for " + pom.getSourcePath() + ":\n" +
              warn(pom, e).printAllTrimmed(), e);
        this.pomWithWarnings = warn(pom, e);
    }

    public UncheckedMavenDownloadingException(Xml.Document pom, MavenDownloadingException e) {
        this(pom, MavenDownloadingExceptions.append(null, e));
    }

    private static Xml.Document warn(Xml.Document pom, MavenDownloadingExceptions mde) {
        try {
            return mde.warn(pom);
        } catch (Exception e) {
            return pom;
        }
    }
}
