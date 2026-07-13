package bench;

import org.apache.maven.model.building.ModelCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * A shareable {@link ModelCache} that counts hits per tag. On every hit the {@code DefaultModelBuilder} deep-clones the
 * cached object ({@code ModelCacheTag.RAW.fromCache} -> {@code Model.clone()}; {@code IMPORT} -> {@code
 * DependencyManagement.clone()}), so a hit count for those tags is a direct count of clone-on-hit operations paid in the
 * warm path. This cache itself stores one canonical instance and never clones.
 */
class CountingModelCache implements ModelCache {

    private final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> hitsByTag = new ConcurrentHashMap<>();
    private final LongAdder misses = new LongAdder();

    @Override
    public Object get(String groupId, String artifactId, String version, String tag) {
        Object v = map.get(key(groupId, artifactId, version, tag));
        if (v != null) {
            hitsByTag.computeIfAbsent(tag, t -> new LongAdder()).increment();
        } else {
            misses.increment();
        }
        return v;
    }

    @Override
    public void put(String groupId, String artifactId, String version, String tag, Object data) {
        map.put(key(groupId, artifactId, version, tag), data);
    }

    long hits(String tag) {
        LongAdder a = hitsByTag.get(tag);
        return a == null ? 0 : a.sum();
    }

    long totalHits() {
        long n = 0;
        for (LongAdder a : hitsByTag.values()) {
            n += a.sum();
        }
        return n;
    }

    long misses() {
        return misses.sum();
    }

    private static String key(String g, String a, String v, String tag) {
        return g + ':' + a + ':' + v + ':' + tag;
    }
}
