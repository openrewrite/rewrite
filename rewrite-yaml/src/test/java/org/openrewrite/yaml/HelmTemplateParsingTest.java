/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.tree.Yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class HelmTemplateParsingTest implements RewriteTest {

    @Test
    void modifyHelmTemplate() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.metadata.name", "{{ .Release.Tag }}-service", null)),
          yaml(
            """
              apiVersion: v1
              kind: Service
              metadata:
                name: {{ .Release.Name }}-service
              """,
            """
              apiVersion: v1
              kind: Service
              metadata:
                name: {{ .Release.Tag }}-service
              """
          )
        );
    }

    @Test
    void helmInComment() {
        rewriteRun(
          yaml(
            """
              apiVersion: v1
              kind: Service
              metadata:
                #name: {{ .Release.Name }}-service
                name: Foo
              """
          )
        );
    }

    @Test
    void parseSimpleHelmTemplate() {
        rewriteRun(
          yaml(
            """
              apiVersion: v1
              kind: Service
              metadata:
                name: {{ .Release.Name }}-service
                labels:
                  app: {{ .Values.appName }}
              spec:
                selector:
                  app: {{ .Values.appName }}
                ports:
                  - port: {{ .Values.service.port }}
                    targetPort: {{ .Values.service.targetPort }}
              """,
            spec -> spec.afterRecipe(docs -> {
                Yaml.Mapping root = (Yaml.Mapping) docs.getDocuments().getFirst().getBlock();
                Yaml.Mapping metadata = (Yaml.Mapping) root.getEntries().get(2).getValue();
                Yaml.Scalar name = (Yaml.Scalar) metadata.getEntries().getFirst().getValue();
                assertThat(name.getValue()).contains("{{ .Release.Name }}-service");
            })
          )
        );
    }

    @Test
    void parseHelmTemplateWithQuotes() {
        rewriteRun(
          yaml(
            """
              apiVersion: apps/v1
              kind: Deployment
              metadata:
                name: "{{ .Release.Name }}-deployment"
                namespace: '{{ .Values.namespace }}'
              spec:
                replicas: {{ .Values.replicaCount }}
                template:
                  metadata:
                    annotations:
                      checksum/config: "{{ include (print $.Template.BasePath "/configmap.yaml") . | sha256sum }}"
              """
          )
        );
    }

    @ExpectedToFail("Invalid YAML syntax with Helm template expressions")
    @Test
    void parseHelmTemplateWithFunctions() {
        rewriteRun(
          yaml(
            """
              metadata:
                name: {{ include "mychart.fullname" . }}
                labels:
                  {{- include "mychart.labels" . | nindent 4 }}
              spec:
                {{- if .Values.autoscaling.enabled }}
                replicas: {{ .Values.autoscaling.minReplicas }}
                {{- else }}
                replicas: {{ .Values.replicaCount }}
                {{- end }}
              """
          )
        );
    }

    @ExpectedToFail("Invalid YAML syntax with Helm template expressions")
    @Test
    void parseHelmTemplateWithRange() {
        rewriteRun(
          yaml(
            """
              env:
                {{- range $key, $value := .Values.env }}
                - name: {{ $key }}
                  value: {{ $value | quote }}
                {{- end }}
              volumes:
                {{- range .Values.volumes }}
                - name: {{ .name }}
                  {{- if .configMap }}
                  configMap:
                    name: {{ .configMap.name }}
                  {{- end }}
                {{- end }}
              """
          )
        );
    }

    @Test
    void parseHelmTemplateWithComplexExpressions() {
        rewriteRun(
          yaml(
            """
              apiVersion: v1
              kind: ConfigMap
              metadata:
                name: {{ .Release.Name }}-config
              data:
                database-url: {{ .Values.database.host }}:{{ .Values.database.port }}/{{ .Values.database.name }}
                app-config: |
                  {{- if .Values.config }}
                  {{ .Values.config | nindent 4 }}
                  {{- else }}
                  # Default configuration
                  debug: false
                  {{- end }}
              """
          )
        );
    }

    @Test
    void parseHelmTemplateWithDefault() {
        rewriteRun(
          yaml(
            """
              spec:
                image: {{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}
                imagePullPolicy: {{ .Values.image.pullPolicy | default "IfNotPresent" }}
                resources:
                  limits:
                    memory: {{ .Values.resources.limits.memory | default "512Mi" }}
                    cpu: {{ .Values.resources.limits.cpu | default "500m" }}
              """
          )
        );
    }

    @Test
    void parseHelmTemplateInFlowStyle() {
        rewriteRun(
          yaml(
            """
              metadata:
                labels: { app: {{ .Values.appName }}, version: {{ .Values.version }} }
                annotations: {
                  "helm.sh/hook": post-install,
                  "helm.sh/hook-weight": "{{ .Values.hookWeight }}"
                }
              data:
                config.json: '{"apiUrl": "{{ .Values.apiUrl }}", "debug": {{ .Values.debug }}}'
              """
          )
        );
    }

    @ExpectedToFail("Invalid YAML syntax with Helm template expressions")
    @Test
    void parseHelmTemplateWithToYaml() {
        rewriteRun(
          yaml(
            """
              spec:
                {{- with .Values.nodeSelector }}
                nodeSelector:
                  {{- toYaml . | nindent 8 }}
                {{- end }}
                {{- with .Values.affinity }}
                affinity:
                  {{- toYaml . | nindent 8 }}
                {{- end }}
                {{- with .Values.tolerations }}
                tolerations:
                  {{- toYaml . | nindent 8 }}
                {{- end }}
              """
          )
        );
    }

    @Test
    void preserveFormattingWithHelmTemplates() {
        rewriteRun(
          yaml(
            """
              metadata:
                name:     {{ .Release.Name }}
                namespace:    {{ .Values.namespace }}
                labels:
                  app:    {{ .Values.appName }}
                  chart:    {{ .Chart.Name }}-{{ .Chart.Version }}
              spec:
                replicas:    {{ .Values.replicas }}
              """
          )
        );
    }

    @Test
    void helmTemplatesWithComments() {
        rewriteRun(
          yaml(
            """
              # This is a Helm chart
              apiVersion: v1
              kind: Service
              metadata:
                name: {{ .Release.Name }} # The release name
                labels:
                  # Standard labels
                  app: {{ .Values.appName }}
                  # Chart information
                  chart: {{ .Chart.Name }}
              spec:
                # Service configuration
                type: {{ .Values.service.type | default "ClusterIP" }} # Default to ClusterIP
                ports:
                  - port: {{ .Values.service.port }} # The service port
              """
          )
        );
    }

    @Test
    void multilineHelmTemplates() {
        rewriteRun(
          yaml(
            """
              data:
                script.sh: |
                  {{- if .Values.enableDebug }}
                  #!/bin/bash
                  set -x
                  {{- end }}
                  echo "Starting {{ .Values.appName }}"
                  {{- range .Values.commands }}
                  {{ . }}
                  {{- end }}
              """
          )
        );
    }

    @Test
    void conditionalWithWhitespace() {
        rewriteRun(
          yaml(
            """
              data:
                script.sh: |
                  {{ if PIPELINE }}
                    # Do something
                  {{ else if OTHER PIPELINE }}
                    # Do something else
                  {{ else }}
                    # Default case
                  {{ end }}
              """
          )
        );
    }

    @Test
    void nestedHelmTemplates() {
        rewriteRun(
          yaml(
            """
              spec:
                containers:
                - name: {{ .Values.container.name }}
                  image: {{ include "app.image" (dict "imageRoot" .Values.image "global" .Values.global) }}
                  env:
                    - name: CONFIG
                      value: {{ include "app.config" (merge (dict "specific" .Values.specific) .) | quote }}
              """
          )
        );
    }

    @Test
    void helmTemplatesInMappingKeys() {
        rewriteRun(
          yaml(
            """
              myconfig:
                {{ .Values.keyName }}: "some value"
                {{ .Values.dynamicKey }}: {{ .Values.dynamicValue }}
                staticKey: {{ .Values.someValue }}
              annotations:
                {{ .Values.annotationKey }}: "enabled"
                "{{ .Values.quotedKey }}": 'quoted value'
              labels:
                {{ include "app.labelKey" . }}: {{ include "app.labelValue" . }}
              """
          )
        );
    }

    @Test
    void helmTemplateComplexFunctions() {
        rewriteRun(
          yaml(
            """
              apiVersion: apps/v1
              kind: Deployment
              metadata:
                name: {{ include "app.fullname" . }}
                namespace: {{ .Values.namespace | default "default" }}
                labels:
                  app.kubernetes.io/name: {{ include "app.name" . }}
                  app.kubernetes.io/instance: {{ .Release.Name }}
                  app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
              spec:
                replicas: {{ .Values.replicaCount | default 1 }}
                selector:
                  matchLabels:
                    app.kubernetes.io/name: {{ include "app.name" . }}
                    app.kubernetes.io/instance: {{ .Release.Name }}
                template:
                  metadata:
                    labels:
                      app.kubernetes.io/name: {{ include "app.name" . }}
                      app.kubernetes.io/instance: {{ .Release.Name }}
                    annotations:
                      checksum/config: {{ include (print $.Template.BasePath "/configmap.yaml") . | sha256sum }}
                      helmVersion: {{ .Capabilities.HelmVersion }}
                  spec:
                    serviceAccountName: {{ include "app.serviceAccountName" . }}
                    containers:
                      - name: {{ .Chart.Name }}
                        image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
                        imagePullPolicy: {{ .Values.image.pullPolicy | default "IfNotPresent" }}
                        ports:
                          - name: http
                            containerPort: {{ .Values.service.port | default 8080 }}
                            protocol: TCP
                        livenessProbe:
                          httpGet:
                            path: {{ .Values.livenessProbe.path | default "/health" }}
                            port: http
                          initialDelaySeconds: {{ .Values.livenessProbe.initialDelaySeconds | default 30 }}
                          periodSeconds: {{ .Values.livenessProbe.periodSeconds | default 10 }}
                        resources:
                          limits:
                            cpu: {{ .Values.resources.limits.cpu | default "1000m" }}
                            memory: {{ .Values.resources.limits.memory | default "512Mi" }}
                          requests:
                            cpu: {{ .Values.resources.requests.cpu | default "100m" }}
                            memory: {{ .Values.resources.requests.memory | default "128Mi" }}
                        env:
                          - name: ENVIRONMENT
                            value: {{ ternary "production" "development" (eq .Values.environment "prod") }}
                          - name: LOG_LEVEL
                            value: {{ coalesce .Values.logLevel .Values.global.logLevel "info" }}
                          - name: DATABASE_URL
                            value: {{ required "database.url is required" .Values.database.url }}
                          - name: FEATURE_FLAG_A
                            value: {{ and .Values.features.flagA (not .Values.features.disableAll) | quote }}
                          - name: FEATURE_FLAG_B
                            value: {{ or .Values.features.flagB .Values.features.defaultEnabled | quote }}
                          - name: API_VERSION
                            value: {{ .Values.apiVersion | default "v1" | upper }}
                          - name: MAX_REPLICAS
                            value: {{ max .Values.minReplicas .Values.maxReplicas | quote }}
                          - name: MIN_REPLICAS
                            value: {{ min .Values.minReplicas .Values.maxReplicas | quote }}
                          - name: DEPLOYMENT_REGION
                            value: {{ first .Values.regions | default "us-east-1" }}
                          - name: IS_PRODUCTION
                            value: {{ has .Values.environment (list "prod" "production") | quote }}
                          - name: CONFIG_HASH
                            value: {{ .Values.config | toJson | sha256sum | trunc 8 }}
                          - name: APP_URL
                            value: {{ printf "https://%s.%s" .Values.subdomain .Values.domain }}
                          - name: VERSION_TAG
                            value: {{ regexReplaceAll "^v" .Values.version "" }}
                          - name: ENABLED_FEATURES
                            value: {{ join "," .Values.enabledFeatures }}
                          - name: WORKER_COUNT
                            value: {{ add .Values.baseWorkers .Values.extraWorkers | quote }}
                          - name: TIMEOUT_SECONDS
                            value: {{ mul .Values.timeoutMinutes 60 | quote }}
              """
          )
        );
    }
}
