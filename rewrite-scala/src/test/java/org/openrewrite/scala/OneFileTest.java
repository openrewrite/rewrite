package org.openrewrite.scala;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.tree.ParseError;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

class OneFileTest {
    @Test
    void one() throws Exception {
        String[] paths = {
          "/tmp/scala-corpus/cats-effect/kernel/jvm/src/main/scala/cats/effect/kernel/AsyncPlatform.scala",
          "/tmp/scala-corpus/cats-effect/core/shared/src/main/scala/cats/effect/IO.scala"};
        StringBuilder sb = new StringBuilder();
        for (String ps : paths) {
            Path path = Paths.get(ps);
            List<Parser.Input> in = List.of(new Parser.Input(path, () -> {
                try { return Files.newInputStream(path); } catch (Exception e) { throw new RuntimeException(e); }
            }));
            for (SourceFile sf : ScalaParser.builder().build()
                  .parseInputs(in, null, new InMemoryExecutionContext(t -> {})).collect(Collectors.toList())) {
                sb.append("==== ").append(ps.substring(ps.lastIndexOf('/') + 1)).append('\n');
                if (sf instanceof ParseError) {
                    String m = sf.getMarkers().findFirst(ParseExceptionResult.class)
                      .map(ParseExceptionResult::getMessage).orElse("?");
                    for (String l : m.split("\n")) {
                        if (l.startsWith("-") || l.startsWith("+") || l.startsWith("@@")) sb.append(l).append('\n');
                    }
                } else sb.append("OK\n");
            }
        }
        Files.write(Paths.get("/tmp/one.txt"), sb.toString().getBytes());
    }
}
