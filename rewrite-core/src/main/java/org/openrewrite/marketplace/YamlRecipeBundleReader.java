package org.openrewrite.marketplace;

import lombok.Getter;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

public class YamlRecipeBundleReader implements RecipeBundleReader {
    private final YamlResourceLoader yaml;
    private final @Getter RecipeBundle bundle;

    public YamlRecipeBundleReader(InputStream yaml, URI source, RecipeBundle bundle) {
        this.yaml = new YamlResourceLoader(yaml, source, );
        this.bundle = bundle;
    }

    @Override
    public RecipeMarketplace read() {
        return null;
    }

    @Override
    public RecipeDescriptor describe(RecipeListing listing) {
        return null;
    }

    @Override
    public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
        return null;
    }
}
