package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EvalContext {
    Collection<SourceFile> generate = new ArrayList<>();
    Map<String, Object> baggage = new HashMap<>();

    public EvalContext generate(SourceFile sourceFile) {
        this.generate.add(sourceFile);
        return this;
    }

    @Nullable
    public <T> T findBaggage(String key) {
        //noinspection unchecked
        return (T) baggage.get(key);
    }

    @Nullable
    public <T> T removeBaggage(String key) {
        //noinspection unchecked
        return (T) baggage.remove(key);
    }

    public void next() {
        generate.clear();
    }
}
