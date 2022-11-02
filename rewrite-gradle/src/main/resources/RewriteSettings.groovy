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


import com.gradle.scan.plugin.BuildScanExtension
import org.gradle.*
import org.gradle.api.*
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.*
import org.gradle.api.attributes.plugin.*
import org.gradle.api.capabilities.*
import org.gradle.api.component.*
import org.gradle.api.credentials.*
import org.gradle.api.distribution.*
import org.gradle.api.distribution.plugins.*
import org.gradle.api.execution.*
import org.gradle.api.file.*
import org.gradle.api.initialization.*
import org.gradle.api.initialization.definition.*
import org.gradle.api.initialization.dsl.*
import org.gradle.api.initialization.resolve.*
import org.gradle.api.invocation.*
import org.gradle.api.java.archives.*
import org.gradle.api.jvm.*
import org.gradle.api.logging.*
import org.gradle.api.logging.configuration.*
import org.gradle.api.model.*
import org.gradle.api.provider.*
import org.gradle.api.reflect.*
import org.gradle.api.resources.*
import org.gradle.api.services.*
import org.gradle.api.specs.*
import org.gradle.authentication.*
import org.gradle.authentication.aws.*
import org.gradle.authentication.http.*
import org.gradle.build.event.*
import org.gradle.buildinit.*
import org.gradle.buildinit.plugins.*
import org.gradle.buildinit.tasks.*
import org.gradle.caching.*
import org.gradle.caching.configuration.*
import org.gradle.caching.http.*
import org.gradle.caching.local.*
import org.gradle.concurrent.*
import org.gradle.external.javadoc.*
import org.gradle.ivy.*
import org.gradle.maven.*
import org.gradle.model.*
import org.gradle.normalization.*
import org.gradle.platform.base.*
import org.gradle.platform.base.binary.*
import org.gradle.platform.base.component.*
import org.gradle.platform.base.plugins.*
import org.gradle.util.*
import com.gradle.scan.plugin.*
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension

interface SettingsPluginSpec {
    Plugin id(String i)
}

interface SettingsPlugin {
    Plugin version(String v)
    Plugin apply(boolean a)
}

interface GradleEnterpriseSpec extends GradleEnterpriseExtension {
    void buildScan(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=BuildScanExtension) Closure cl);
}

abstract class RewriteSettings implements Settings {
    abstract void plugins(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=SettingsPluginSpec) Closure cl)
    abstract GradleEnterpriseSpec gradleEnterprise()

    void __script__() {
}}
