/*
 * Copyright 2021 the original author or authors.
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
//file:noinspection UnstableApiUsage


import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension
import org.gradle.api.initialization.Settings

interface PluginManagementSpec extends org.gradle.plugin.management.PluginManagementSpec {
    void repositories(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=RepositoryHandlerSpec) Closure cl)
    void plugins(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=PluginSpec) Closure cl)
}

interface GradleEnterpriseSpec extends GradleEnterpriseExtension {
    void buildScan(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=BuildScanExtension) Closure cl);
}

abstract class RewriteSettings extends groovy.lang.Script implements Settings {
    abstract void pluginManagement(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=PluginManagementSpec) Closure cl)
    abstract void plugins(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=PluginSpec) Closure cl)
    abstract void gradleEnterprise(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=GradleEnterpriseSpec) Closure cl)
}
