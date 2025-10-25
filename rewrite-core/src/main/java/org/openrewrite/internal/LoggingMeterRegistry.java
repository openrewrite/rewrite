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
package org.openrewrite.internal;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.cumulative.CumulativeCounter;
import io.micrometer.core.instrument.cumulative.CumulativeDistributionSummary;
import io.micrometer.core.instrument.cumulative.CumulativeFunctionTimer;
import io.micrometer.core.instrument.cumulative.CumulativeTimer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.internal.DefaultGauge;
import io.micrometer.core.instrument.internal.DefaultLongTaskTimer;
import io.micrometer.core.instrument.util.TimeUtils;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.stream.StreamSupport;

import static io.micrometer.core.instrument.util.DoubleFormat.decimalOrNan;
import static java.util.stream.Collectors.joining;

public class LoggingMeterRegistry extends MeterRegistry {
    private final long startInterval;
    private final TimeUnit baseTimeUnit;
    private final Consumer<String> loggingSink;
    private final Function<Meter, String> meterIdPrinter;

    public LoggingMeterRegistry(TimeUnit baseTimeUnit, Consumer<String> loggingSink, @Nullable Function<Meter, String> meterIdPrinter) {
        super(Clock.SYSTEM);
        this.baseTimeUnit = baseTimeUnit;
        this.startInterval = clock.monotonicTime();
        this.loggingSink = loggingSink;
        this.meterIdPrinter = meterIdPrinter != null ? meterIdPrinter : defaultMeterIdPrinter();
        config().namingConvention(NamingConvention.dot);
    }

    private Function<Meter, String> defaultMeterIdPrinter() {
        return meter -> getConventionName(meter.getId()) + getConventionTags(meter.getId()).stream()
                .map(t -> t.getKey() + "=" + t.getValue())
                .collect(joining(",", "{", "}"));
    }

    public void print() {
        getMeters().stream()
                .sorted((m1, m2) -> {
                    int typeComp = m1.getId().getType().compareTo(m2.getId().getType());
                    if (typeComp == 0) {
                        return m1.getId().getName().compareTo(m2.getId().getName());
                    }
                    return typeComp;
                })
                .forEach(m -> {
                    Printer print = new Printer(m);
                    m.use(
                            gauge -> loggingSink.accept(print.id() + "\n    value=" + print.value(gauge.value())),
                            counter -> {
                                double count = counter.count();
                                if (count == 0) return;
                                loggingSink.accept(print.id() +
                                        "\n    sum=" + print.value(count) +
                                        "\n    rate=" + print.rate(count));
                            },
                            timer -> {
                                HistogramSnapshot snapshot = timer.takeSnapshot();
                                long count = snapshot.count();
                                if (count == 0) return;
                                loggingSink.accept(print.id() +
                                        "\n    sum=" + count +
                                        "\n    rate=" + print.unitlessRate(count) +
                                        "\n    mean=" + print.time(snapshot.mean(getBaseTimeUnit())) +
                                        "\n    max=" + print.time(snapshot.max(getBaseTimeUnit())));
                            },
                            summary -> {
                                HistogramSnapshot snapshot = summary.takeSnapshot();
                                long count = snapshot.count();
                                if (count == 0) return;
                                loggingSink.accept(print.id() +
                                        "\n    sum=" + count +
                                        "\n    rate=" + print.unitlessRate(count) +
                                        "\n    mean=" + print.value(snapshot.mean()) +
                                        "\n    max=" + print.value(snapshot.max()));
                            },
                            longTaskTimer -> {
                                int activeTasks = longTaskTimer.activeTasks();
                                if (activeTasks == 0) return;
                                loggingSink.accept(print.id() +
                                        "\n    active=" + print.value(activeTasks) +
                                        "\n    duration=" + print.time(longTaskTimer.duration(getBaseTimeUnit())));
                            },
                            timeGauge -> {
                                double value = timeGauge.value(getBaseTimeUnit());
                                if (value == 0) return;
                                loggingSink.accept(print.id() + "\n    value=" + print.time(value));
                            },
                            counter -> {
                                double count = counter.count();
                                if (count == 0) return;
                                loggingSink.accept(print.id() +
                                        "\n    sum=" + print.value(count) +
                                        "\n    rate=" + print.rate(count));
                            },
                            timer -> {
                                double count = timer.count();
                                if (count == 0) return;
                                loggingSink.accept(print.id() +
                                        "\n    sum=" + print.value(count) +
                                        "\n    rate=" + print.rate(count) +
                                        "\n    mean=" + print.time(timer.mean(getBaseTimeUnit())));
                            },
                            meter -> loggingSink.accept(writeMeter(meter, print))
                    );
                });
    }

    String writeMeter(Meter meter, Printer print) {
        return StreamSupport.stream(meter.measure().spliterator(), false)
                .map(ms -> {
                    String msLine = ms.getStatistic().getTagValueRepresentation() + "=";
                    switch (ms.getStatistic()) {
                        case TOTAL:
                        case MAX:
                        case VALUE:
                            return msLine + print.value(ms.getValue());
                        case TOTAL_TIME:
                        case DURATION:
                            return msLine + print.time(ms.getValue());
                        case COUNT:
                            return "\n    sum=" + print.value(ms.getValue()) +
                                    "\n    rate=" + print.rate(ms.getValue());
                        default:
                            return msLine + decimalOrNan(ms.getValue());
                    }
                })
                .collect(joining(", ", print.id() + " ", ""));
    }

    class Printer {
        private final Duration interval;
        private final Meter meter;

        Printer(Meter meter) {
            interval = Duration.ofNanos(clock.monotonicTime() - startInterval);
            this.meter = meter;
        }

        String id() {
            return meterIdPrinter.apply(meter);
        }

        String time(double time) {
            return TimeUtils.format(Duration.ofNanos((long) TimeUtils.convert(time, getBaseTimeUnit(), TimeUnit.NANOSECONDS)));
        }

        String rate(double rate) {
            return humanReadableBaseUnit(rate / ((double) interval.getNano() / 1e9)) + "/s";
        }

        String unitlessRate(double rate) {
            return decimalOrNan(rate / ((double) interval.getNano() / 1e9)) + "/s";
        }

        String value(double value) {
            return humanReadableBaseUnit(value);
        }

        // see https://stackoverflow.com/a/3758880/510017
        String humanReadableByteCount(double bytes) {
            int unit = 1024;
            if (bytes < unit || Double.isNaN(bytes)) return decimalOrNan(bytes) + " B";
            int exp = (int) (Math.log(bytes) / Math.log(unit));
            String pre = "KMGTPE".charAt(exp - 1) + "i";
            return decimalOrNan(bytes / Math.pow(unit, exp)) + " " + pre + "B";
        }

        String humanReadableBaseUnit(double value) {
            String baseUnit = meter.getId().getBaseUnit();
            if (BaseUnits.BYTES.equals(baseUnit)) {
                return humanReadableByteCount(value);
            }
            return decimalOrNan(value) + (baseUnit != null ? " " + baseUnit : "");
        }
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return baseTimeUnit;
    }

    @Override
    protected DistributionStatisticConfig defaultHistogramConfig() {
        return DistributionStatisticConfig.DEFAULT;
    }

    @Override
    protected <T> Gauge newGauge(Meter.Id id, @Nullable T obj, ToDoubleFunction<T> valueFunction) {
        return new DefaultGauge<>(id, obj, valueFunction);
    }

    @Override
    protected Counter newCounter(Meter.Id id) {
        return new CumulativeCounter(id);
    }

    @Override
    protected LongTaskTimer newLongTaskTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig) {
        return new DefaultLongTaskTimer(id, clock, baseTimeUnit, distributionStatisticConfig, false);
    }

    @Override
    protected Timer newTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig, PauseDetector pauseDetector) {
        return new CumulativeTimer(id, clock, distributionStatisticConfig, pauseDetector, getBaseTimeUnit(), false);
    }

    @Override
    protected DistributionSummary newDistributionSummary(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig, double scale) {
        return new CumulativeDistributionSummary(id, clock, distributionStatisticConfig, scale, false);
    }

    @Override
    protected Meter newMeter(Meter.Id id, Meter.Type type, Iterable<Measurement> measurements) {
        throw new UnsupportedOperationException("Does not support custom meter types.");
    }

    @Override
    protected <T> FunctionTimer newFunctionTimer(Meter.Id id, T obj, ToLongFunction<T> countFunction, ToDoubleFunction<T> totalTimeFunction, TimeUnit totalTimeFunctionUnit) {
        return new CumulativeFunctionTimer<>(id, obj, countFunction, totalTimeFunction, totalTimeFunctionUnit, baseTimeUnit);
    }

    @Override
    protected <T> FunctionCounter newFunctionCounter(Meter.Id id, T obj, ToDoubleFunction<T> countFunction) {
        return null;
    }

    public static LoggingMeterRegistry.Builder builder() {
        return new LoggingMeterRegistry.Builder();
    }

    public static class Builder {
        private TimeUnit baseTimeUnit = TimeUnit.MILLISECONDS;
        private Consumer<String> loggingSink = System.out::println;

        @Nullable
        private Function<Meter, String> meterIdPrinter;

        public LoggingMeterRegistry.Builder baseTimeUnit(TimeUnit baseTimeUnit) {
            this.baseTimeUnit = baseTimeUnit;
            return this;
        }

        public LoggingMeterRegistry.Builder loggingSink(Consumer<String> loggingSink) {
            this.loggingSink = loggingSink;
            return this;
        }

        public LoggingMeterRegistry.Builder meterIdPrinter(Function<Meter, String> meterIdPrinter) {
            this.meterIdPrinter = meterIdPrinter;
            return this;
        }

        public LoggingMeterRegistry build() {
            return new LoggingMeterRegistry(baseTimeUnit, loggingSink, meterIdPrinter);
        }
    }
}
