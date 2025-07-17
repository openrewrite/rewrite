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

import static org.openrewrite.yaml.Assertions.yaml;

class HelmTemplateParsingTest implements RewriteTest {

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
              """
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
}
