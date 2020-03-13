package org.openrewrite.ge;

import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.rsocket.PrometheusRSocketClient;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.apache.commons.cli.*;
import org.apache.commons.io.Charsets;
import org.kohsuke.github.*;
import org.openrewrite.ChangePublisher;
import org.openrewrite.Refactor;
import org.openrewrite.RefactorModule;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import reactor.netty.tcp.TcpClient;

import java.io.*;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

public class Main {
    public static void main(String[] args) throws IOException, ParseException {
        CompositeMeterRegistry meterRegistry = new CompositeMeterRegistry();
        PrometheusRSocketClient metricsClient = null;

        try {
            CommandLineParser parser = new DefaultParser();
            Options options = new Options();
            options.addRequiredOption("u", "user", true, "Github username");
            options.addRequiredOption("p", "password", true, "Github password or personal access token");
            options.addOption("f", "file", true, "Configuration YML file");
            options.addOption("c", "config", true, "Configuration YML");
            options.addOption("l", "limit", true, "Limit number of files processed");
            options.addOption("m", "metrics", false, "Publish metrics");

            CommandLine line = parser.parse(options, args);

            if (line.hasOption("m")) {
                PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
                metricsClient = new PrometheusRSocketClient(prometheusMeterRegistry,
                        TcpClientTransport.create(TcpClient.create().host("localhost").port(7001)),
                        c -> c.retryBackoff(Long.MAX_VALUE, Duration.ofSeconds(10), Duration.ofMinutes(10)));
                meterRegistry.add(prometheusMeterRegistry);

                new JvmGcMetrics().bindTo(meterRegistry);
                new ProcessorMetrics().bindTo(meterRegistry);
            }


            Yaml yaml = new Yaml(new Constructor(Configuration.class));
            Configuration configuration;
            if (line.hasOption("f")) {
                try (InputStream is = new FileInputStream(new File(line.getOptionValue("f")))) {
                    configuration = yaml.load(is);
                }
            } else if (line.hasOption("c")) {
                try (InputStream is = new ByteArrayInputStream(line.getOptionValue("c").getBytes(Charsets.UTF_8))) {
                    configuration = yaml.load(is);
                }
            } else {
                throw new IllegalArgumentException("Supply either a config YML file via -f or an inline config via -c");
            }

            List<Refactor<?, ?>> refactorPlan = RefactorModule.plan(emptyList(),
                    new AddGradleEnterpriseToMavenProject(configuration));

            GitHubBuilder githubBuilder = new GitHubBuilder().withEndpoint(configuration.getGithub().getEndpoint());

            GitHub github = (line.getOptionValue("p").matches("[a-f0-9]{40}") ?
                    githubBuilder.withOAuthToken(line.getOptionValue("p"), line.getOptionValue("u")) :
                    githubBuilder.withPassword(line.getOptionValue("p"), line.getOptionValue("u"))
            ).build();

            Set<GHRepository> withoutGradleEnterprise = mavenRepositoriesWithoutGradleEnterprise(github);

            GHRepository repository = withoutGradleEnterprise.iterator().next();
            GHBranch branch = repository.getBranch(repository.getDefaultBranch());
            String headCommit = branch.getSHA1();

            // TODO GitHub provides the tree ID as part of the branch API response under commit, but not populated in Kohsuke library.
            // Instead, this is a separate API call. As an optimization, we can eliminate this extra call.
            String headTree = repository.getTree(headCommit).getSha();

            // FIXME add a publisher here
            ChangePublisher publisher = null;
            publisher.refactorAndPublish(refactorPlan);
        } finally {
            if (metricsClient != null) {
                metricsClient.pushAndClose();
            }
        }
    }

    private static Set<GHRepository> mavenRepositoriesWithoutGradleEnterprise(GitHub github) {
        Set<GHRepository> withGradleEnterprise = stream(github.searchContent().filename("gradle-enterprise.xml").list().spliterator(), false)
                .map(GHContent::getOwner)
                .collect(toSet());

        Set<GHRepository> withMaven = stream(github.searchContent().filename("pom.xml").list().spliterator(), false)
                .map(GHContent::getOwner)
                .collect(toSet());

        withMaven.removeAll(withGradleEnterprise);
        return withMaven;
    }
}
