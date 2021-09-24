/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.ByteSequence;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static java.lang.Math.toIntExact;

public class NpmRegistryModuleLoader extends PolyglotResourceLoader {

    private static final String JS = "js";
    private static final String JAVA = "java";

    public NpmRegistryModuleLoader(String registry, String... modules) {
        if (!registry.endsWith("/")) {
            registry += "/";
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (String module : modules) {
            try {
                if (!registry.startsWith("http")) {
                    Path path = Paths.get(registry, module, "package.json");
                    PackageDescriptor packageDescriptor = mapper.readValue(path.toFile(), PackageDescriptor.class);
                    String main = packageDescriptor.getMain();

                    Path jsPath = Paths.get(registry, module, main);
                    evalPolyglotRecipe(Source.newBuilder(JS, jsPath.toFile()).name(jsPath.toString()).build());
                } else {
                    URI srcUri = URI.create(registry + module);
                    PackagesDescriptor packages = mapper.readValue(srcUri.toURL(), PackagesDescriptor.class);
                    String latestVersion = packages.getDistTags().get("latest");
                    PackageDescriptor packageDescriptor = packages.getVersions().get(latestVersion);
                    String main = packageDescriptor.getMain();

                    String tarball = (String) packageDescriptor.getDist().get("tarball");
                    URI tarballUri = URI.create(tarball);
                    try (TarArchiveInputStream tgzIn = new TarArchiveInputStream(new GZIPInputStream(tarballUri.toURL().openStream()))) {
                        for (TarArchiveEntry e; (e = tgzIn.getNextTarEntry()) != null; ) {
                            String name = e.getName();
                            if (!name.endsWith(main) || !name.endsWith(JS) || !tgzIn.canReadEntryData(e)) {
                                continue;
                            }

                            byte[] buff = new byte[toIntExact(e.getSize())];
                            //noinspection StatementWithEmptyBody
                            for (int i = 0; i < buff.length; i += tgzIn.read(buff, i, tgzIn.getRecordSize())) {
                            }
                            evalPolyglotRecipe(Source.newBuilder(JS, ByteSequence.create(buff), name).build());
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Data
    private static class PackagesDescriptor {
        @JsonProperty("dist-tags")
        Map<String, String> distTags;
        @JsonProperty("versions")
        Map<String, PackageDescriptor> versions;
    }

    @Data
    private static class PackageDescriptor {
        @JsonProperty("description")
        String description;
        @JsonProperty("dist")
        Map<String, Object> dist;
        @JsonProperty("main")
        String main;
    }

}
