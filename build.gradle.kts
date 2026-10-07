import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "de.sasbe.subtabs"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2025.3")
        bundledPlugin("JavaScript")
        bundledModule("intellij.platform.vcs.impl")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:1.13.4")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(21)
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    patchPluginXml {
        sinceBuild.set("253")
        untilBuild.set("261.*")
    }

    listOf("prepareSandbox", "prepareSandbox_runIdeBackend", "prepareSandbox_runIdeFrontend").forEach { taskName ->
        named<PrepareSandboxTask>(taskName) {
            // Bundled Kubernetes uses split frontend RPC; standard sandbox has no backend → log spam on demo start.
            disabledPlugins.add("com.intellij.kubernetes")
            dependsOn("npmInstallDemo")
            doLast { installDemoSandboxIdeConfig(layout.projectDirectory.asFile) }
        }
    }

    val npmInstallDemo by registering(Exec::class) {
        val demoDir = layout.projectDirectory.dir("demo-project")
        workingDir = demoDir.asFile
        val npm = if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"
        commandLine(npm, "ci")
        inputs.file(demoDir.file("package-lock.json"))
        inputs.file(demoDir.file("package.json"))
        outputs.dir(demoDir.dir("node_modules"))
    }

    runIde {
        dependsOn("npmInstallDemo")
        val demoProjectPath = layout.projectDirectory.dir("demo-project").asFile.absolutePath
        argumentProviders += CommandLineArgumentProvider {
            listOf(demoProjectPath)
        }
        // Demo project must not attach Git to the plugin repo; otherwise the sandbox spams remote-fetch errors.
        val demoReplayDir = layout.projectDirectory.dir("demo-replay").asFile.absolutePath
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Dgit4idea.fetch.automatically=false",
                "-Didea.git.automatic.branchupdate=false",
                "-Dsubtabs.demo.replay=record",
                "-Dsubtabs.demo.replay.dir=$demoReplayDir",
            )
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdePerf") {
            sandboxDirectory = layout.buildDirectory.dir("perf-sandbox")
            task {
                dependsOn("npmInstallDemo")
                val demoProjectPath = layout.projectDirectory.dir("demo-project").asFile.absolutePath
                val perfOutput = providers.gradleProperty("subtabsPerf")
                    .orElse(layout.buildDirectory.dir("perf").map { it.asFile.absolutePath })
                val perfConfigs = providers.gradleProperty("subtabsPerfConfigs").orElse("on,off")
                val perfSplits = providers.gradleProperty("subtabsPerfSplits").orElse("1,2,3,4")
                val perfRepaintTrace = providers.gradleProperty("subtabsPerfRepaintTrace").orElse("false")
                val perfSplittab = providers.gradleProperty("subtabsPerfSplittab").orElse("")
                val perfOtherPair = providers.gradleProperty("subtabsPerfOtherPair").orElse("")
                argumentProviders += CommandLineArgumentProvider { listOf(demoProjectPath) }
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Dgit4idea.fetch.automatically=false",
                        "-Didea.git.automatic.branchupdate=false",
                        "-Dsubtabs.perf.harness=${perfOutput.get()}",
                        "-Dsubtabs.perf.configs=${perfConfigs.get()}",
                        "-Dsubtabs.perf.splits=${perfSplits.get()}",
                        "-Dsubtabs.perf.repaintTrace=${perfRepaintTrace.get()}",
                        "-Dsubtabs.perf.splittab=${perfSplittab.get()}",
                        "-Dsubtabs.perf.otherPair=${perfOtherPair.get()}",
                        "-Dsubtabs.perf.skipSplittabOpen=${providers.gradleProperty("subtabsPerfSkipSplittabOpen").orElse("false").get()}",
                        "-Dsubtabs.perf.exit=true",
                    )
                }
            }
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Familia"
        version = project.version.toString()
    }
}

private fun installDemoSandboxIdeConfig(projectDirectory: java.io.File) {
    val template = projectDirectory.resolve("demo-sandbox-config/options")
    if (!template.isDirectory) {
        return
    }
    val sandboxRoot = projectDirectory.resolve(".intellijPlatform/sandbox/component-subtabs")
    if (!sandboxRoot.isDirectory) {
        return
    }
    sandboxRoot.listFiles()?.filter { it.isDirectory }?.forEach { ideHome ->
        val target = ideHome.resolve("config/options")
        target.mkdirs()
        template.listFiles()?.forEach { file ->
            file.copyTo(target.resolve(file.name), overwrite = true)
        }
    }
}
