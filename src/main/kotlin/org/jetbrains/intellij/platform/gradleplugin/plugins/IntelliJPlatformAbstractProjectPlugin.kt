// Copyright 2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradleplugin.plugins

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import org.jetbrains.intellij.platform.gradleplugin.IntelliJPluginConstants.Extensions
import org.jetbrains.intellij.platform.gradleplugin.checkGradleVersion
import org.jetbrains.intellij.platform.gradleplugin.extensions.IntelliJPlatformExtension
import org.jetbrains.intellij.platform.gradleplugin.info
import org.jetbrains.intellij.platform.gradleplugin.logCategory

abstract class IntelliJPlatformAbstractProjectPlugin(val pluginId: String) : Plugin<Project> {

    protected lateinit var context: String

    final override fun apply(project: Project) {
        context = project.logCategory()

        info(context, "Configuring plugin: $pluginId")
        checkGradleVersion()

        project.configure()
    }

    protected abstract fun Project.configure()

    protected inline fun <reified T : Task> TaskContainer.configureTask(name: String, noinline configuration: T.() -> Unit = {}) {
        info(context, "Configuring task: $name")
        val task = findByName(name) as? T ?: register<T>(name).get()
        task.configuration()
    }

    protected inline fun <reified T : Any> Any.configureExtension(name: String, noinline configuration: T.() -> Unit = {}) {
        info(context, "Configuring extension: $name")
        with((this as ExtensionAware).extensions) {
            val extension = findByName(name) as? T ?: create<T>(name)
            extension.configuration()
        }
    }

    protected val Project.intelliJPlatformExtension
        get() = extensions.getByName<IntelliJPlatformExtension>(Extensions.INTELLIJ_PLATFORM)

    protected val Project.ideVersionProvider
        get() = configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME).map { configuration ->
            configuration.resolvedConfiguration
                .resolvedArtifacts
                .firstOrNull { artifact -> artifact.name == "build" && artifact.file.name == "build.txt" }
                ?.file?.readText()?.trim()
                ?: throw Exception("build.txt doesn't exist") // TODO: TEST & FIX ME
        }.map {
            IdeVersion.createIdeVersion(it)
        }
}
