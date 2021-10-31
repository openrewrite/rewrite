package org.openrewrite.java.cache;

public class JvmTypeCache extends ClasspathJavaTypeCache {
    private static final ClasspathJavaTypeCache JAVA8_TYPE_CACHE = new ClasspathJavaTypeCache(null,
            p -> p.endsWith("ct.sym"));

    private static final ClasspathJavaTypeCache JAVA11_TYPE_CACHE = new ClasspathJavaTypeCache(null,
            p -> p.startsWith("/modules"));

    public static JavaTypeCache fromJavaVersion(JavaTypeCache next) {
        return (System.getProperty("java.version").startsWith("1.8") ? JAVA8_TYPE_CACHE : JAVA11_TYPE_CACHE)
                .withNext(next);
    }
}
