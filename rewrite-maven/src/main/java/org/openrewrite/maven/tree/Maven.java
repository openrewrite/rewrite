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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openrewrite.Formatting;
import org.openrewrite.Metadata;
import org.openrewrite.SourceVisitor;
import org.openrewrite.maven.MavenSourceVisitor;
import org.openrewrite.maven.internal.MavenDownloader;
import org.openrewrite.xml.XmlSourceVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

public class Maven extends Xml.Document {
    private final transient Pom model;
    private final transient Collection<Pom> modules;
    private final transient MavenDownloader downloader;

    public Maven(Xml.Document document, MavenDownloader downloader) {
        super(
                document.getId(),
                document.getSourcePath(),
                document.getMetadata(),
                document.getProlog(),
                document.getRoot(),
                document.getFormatting()
        );

        this.downloader = downloader;

        model = getMetadata(Pom.class);
        assert model != null;

        Modules modulesContainer = getMetadata(Modules.class);
        modules = modulesContainer == null ? emptyList() :
                modulesContainer.getModules();
    }

    @JsonIgnore
    public Pom getModel() {
        return model;
    }

    @JsonIgnore
    public Collection<Pom> getModules() {
        return modules;
    }

    @JsonIgnore
    public MavenDownloader getDownloader() {
        return downloader;
    }

    @Override
    public <R> R accept(SourceVisitor<R> v) {
        if (v instanceof MavenSourceVisitor) {
            return ((MavenSourceVisitor<R>) v).visitMaven(this);
        } else if (v instanceof XmlSourceVisitor) {
            return super.accept(v);
        }
        return v.defaultTo(null);
    }

    @Override
    public Maven withRoot(Tag root) {
        Document m = super.withRoot(root);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m, downloader);
    }

    @Override
    public Maven withMetadata(Collection<Metadata> metadata) {
        Document m = super.withMetadata(metadata);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m, downloader);
    }

    @Override
    public Maven withFormatting(Formatting formatting) {
        Document m = super.withFormatting(formatting);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m, downloader);
    }

    @Override
    public Maven withProlog(Prolog prolog) {
        Document m = super.withProlog(prolog);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m, downloader);
    }

    public Maven withModel(Pom model) {
        Pom existing = getMetadata(Pom.class);
        List<Metadata> metadata = new ArrayList<>(getMetadata());
        for (int i = 0; i < metadata.size(); i++) {
            Metadata datum = metadata.get(i);
            if (datum == existing) {
                metadata.set(i, model);
            }
        }
        return withMetadata(metadata);
    }
}
