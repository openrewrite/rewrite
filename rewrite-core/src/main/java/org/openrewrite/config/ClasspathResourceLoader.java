package org.openrewrite.config;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.openrewrite.SourceVisitor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class ClasspathResourceLoader implements ProfileConfigurationLoader, SourceVisitorLoader {
    private final Collection<YamlResourceLoader> yamlResourceLoaders;

    public ClasspathResourceLoader(Iterable<Path> compileClasspath) {
        yamlResourceLoaders = new ArrayList<>();

        try (ScanResult scanResult = new ClassGraph()
                .whitelistPaths("META-INF/rewrite")
                .enableMemoryMapping()
                .scan()) {
            scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) ->
                    yamlResourceLoaders.add(new YamlResourceLoader(input)));
        }

        if(compileClasspath.iterator().hasNext()) {
            try (ScanResult scanResult = new ClassGraph()
                    .overrideClasspath(compileClasspath)
                    .whitelistPaths("META-INF/rewrite")
                    .enableMemoryMapping()
                    .scan()) {
                scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) ->
                        yamlResourceLoaders.add(new YamlResourceLoader(input)));
            }
        }
    }

    @Override
    public Collection<ProfileConfiguration> loadProfiles() {
        return yamlResourceLoaders.stream().flatMap(loader -> loader.loadProfiles().stream()).collect(toList());
    }

    @Override
    public Collection<SourceVisitor<?>> loadVisitors() {
        return yamlResourceLoaders.stream().flatMap(loader -> loader.loadVisitors().stream()).collect(toList());
    }
}
