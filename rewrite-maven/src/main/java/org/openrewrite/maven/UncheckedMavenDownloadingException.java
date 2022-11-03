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
              e.warn(pom).printAllTrimmed(), e);
        this.pomWithWarnings = e.warn(pom);
    }

    public UncheckedMavenDownloadingException(Xml.Document pom, MavenDownloadingException e) {
        super("Failed to download dependencies for " + pom.getSourcePath() + ":\n" +
              MavenDownloadingExceptions.append(null, e).warn(pom).printAllTrimmed(), e);
        this.pomWithWarnings = MavenDownloadingExceptions.append(null, e).warn(pom);
    }
}
