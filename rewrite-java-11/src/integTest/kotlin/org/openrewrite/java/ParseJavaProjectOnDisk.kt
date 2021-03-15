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
package org.openrewrite.java

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.MetricsDestinations
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import kotlin.streams.toList

object ParseJavaProjectOnDisk {
    @JvmStatic
    fun main(args: Array<String>) {
        val meterRegistry = MetricsDestinations.prometheus()

        meterRegistry.config().meterFilter(object: MeterFilter {
            override fun configure(id: Meter.Id, config: DistributionStatisticConfig): DistributionStatisticConfig? {
                if(id.name == "rewrite.parse") {
                    return DistributionStatisticConfig.builder()
                        .percentilesHistogram(true)
                        .build()
                        .merge(config)
                }
                return config
            }
        })

        val srcDir = Paths.get(args[0])
        val predicate = BiPredicate<Path, BasicFileAttributes> { p, bfa ->
            bfa.isRegularFile && p.fileName.toString().endsWith(".java")
        }

        val paths = Files.find(srcDir, 999, predicate)
            .limit(if (args.size > 1) args[1].toLong() else Long.MAX_VALUE)
            .toList()

        var start = System.nanoTime()
        val parser: JavaParser = JavaParser.fromJavaVersion().build()

        println("Loaded ${paths.size} files in ${(System.nanoTime() - start) * 1e-6}ms")

        start = System.nanoTime()
        parser.parse(paths, srcDir, InMemoryExecutionContext())
        println("Parsed ${paths.size} files in ${(System.nanoTime() - start) * 1e-6}ms")

        //Sleep long enough for prometheus to scape the final metrics.
        Thread.sleep(11000)
    }
}
