import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "de.sasbe.subtabs"
version = "0.1.0"

val tabzIdeVersion = providers.gradleProperty("tabzIdeVersion").orElse("2025.3")
/** Wenn gesetzt (WebStorm | Rider): Plugin Verifier nur gegen diese IDE. Ohne Property: beide. */
val tabzVerifyOnly = providers.gradleProperty("tabzVerifyOnly")
/** Marketplace release channel for closed tests (default: closed-beta). Stable later: default / empty stable channel. */
val tabzPublishChannel = providers.gradleProperty("tabzPublishChannel").orElse("closed-beta")
val tabzPublishHidden = providers.gradleProperty("tabzPublishHidden").map { it.equals("true", ignoreCase = true) }

private data class TabzIdeTarget(
    val slug: String,
    val type: IntelliJPlatformType,
    val frameworks: List<TestFrameworkType>,
)

private fun ideTestFrameworks(vararg extras: TestFrameworkType): List<TestFrameworkType> =
    listOf(TestFrameworkType.Platform, TestFrameworkType.JUnit5, *extras)

private fun ideaFamilyTestFrameworks(vararg extras: TestFrameworkType): List<TestFrameworkType> =
    ideTestFrameworks(*extras)

private val tabzIdeTargets = listOf(
    TabzIdeTarget(
        "Idea",
        IntelliJPlatformType.IntellijIdea,
        ideaFamilyTestFrameworks(TestFrameworkType.Plugin.JavaScript),
    ),
    TabzIdeTarget(
        "WebStorm",
        IntelliJPlatformType.WebStorm,
        ideaFamilyTestFrameworks(TestFrameworkType.Plugin.JavaScript),
    ),
    TabzIdeTarget(
        "Rider",
        IntelliJPlatformType.Rider,
        ideaFamilyTestFrameworks(TestFrameworkType.Plugin.ReSharper),
    ),
    TabzIdeTarget(
        "PyCharm",
        IntelliJPlatformType.PyCharm,
        ideaFamilyTestFrameworks(),
    ),
    TabzIdeTarget(
        "PhpStorm",
        IntelliJPlatformType.PhpStorm,
        ideaFamilyTestFrameworks(),
    ),
    TabzIdeTarget(
        "GoLand",
        IntelliJPlatformType.GoLand,
        ideaFamilyTestFrameworks(TestFrameworkType.Plugin.Go),
    ),
    TabzIdeTarget(
        "CLion",
        IntelliJPlatformType.CLion,
        ideaFamilyTestFrameworks(TestFrameworkType.Plugin.CLion),
    ),
    TabzIdeTarget(
        "RubyMine",
        IntelliJPlatformType.RubyMine,
        ideaFamilyTestFrameworks(TestFrameworkType.Plugin.Ruby),
    ),
    TabzIdeTarget(
        "RustRover",
        IntelliJPlatformType.RustRover,
        ideaFamilyTestFrameworks(),
    ),
    TabzIdeTarget(
        "DataGrip",
        IntelliJPlatformType.DataGrip,
        ideaFamilyTestFrameworks(),
    ),
    TabzIdeTarget(
        "DataSpell",
        IntelliJPlatformType.DataSpell,
        ideaFamilyTestFrameworks(),
    ),
    TabzIdeTarget(
        "AndroidStudio",
        IntelliJPlatformType.AndroidStudio,
        ideaFamilyTestFrameworks(),
    ),
)

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(tabzIdeVersion)
        bundledPlugin("JavaScript")
        bundledModule("intellij.platform.vcs.impl")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.JavaScript)
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

    named<Test>("test") {
        description = "Runs tests against IntelliJ IDEA (tabzIdeVersion, default 2025.3)."
    }

    register("testAllJetBrainsIdes") {
        group = "verification"
        description =
            "Runs the full test suite in sandboxes for every configured JetBrains IDE (downloads IDEs on first run)."
        tabzIdeTargets.forEach { target ->
            dependsOn("testIde${target.slug}")
        }
    }

    patchPluginXml {
        sinceBuild.set("253")
        untilBuild.set("261.*")
    }

    withType<PrepareSandboxTask>().configureEach {
        // Bundled Kubernetes uses split frontend RPC; standard sandbox has no backend → log spam on demo start.
        disabledPlugins.add("com.intellij.kubernetes")
        dependsOn("npmInstallDemo")
        doLast { installDemoSandboxIdeConfig(layout.projectDirectory.asFile) }
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
    tabzIdeTargets.forEach { target ->
        testIde.register("testIde${target.slug}") {
            type = target.type
            version = tabzIdeVersion
            target.frameworks.forEach { testFramework(it) }
            plugins {
                bundledPlugin("JavaScript")
                bundledModule("intellij.platform.vcs.impl")
                disablePlugin("com.intellij.kubernetes")
            }
        }

        runIde.register("runIde${target.slug}") {
            type = target.type
            version = tabzIdeVersion
            sandboxDirectory = layout.buildDirectory.dir("sandbox-${target.slug.lowercase()}")
            task {
                dependsOn("npmInstallDemo")
                val demoProjectPath = layout.projectDirectory.dir("demo-project").asFile.absolutePath
                argumentProviders += CommandLineArgumentProvider { listOf(demoProjectPath) }
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Dgit4idea.fetch.automatically=false",
                        "-Didea.git.automatic.branchupdate=false",
                    )
                }
            }
        }
    }

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
        name = "TabZ"
        version = project.version.toString()
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = tabzPublishChannel.map { listOf(it) }
        hidden = tabzPublishHidden.orElse(false)
    }
    signing {
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
        val certDir = layout.projectDirectory.dir("certificate")
        privateKeyFile = certDir.file("private.pem")
        certificateChainFile = certDir.file("chain.crt")
    }
    pluginVerification {
        ides {
            val only = tabzVerifyOnly.orNull?.trim()?.lowercase()
            fun include(slug: String) = only.isNullOrEmpty() || only == slug.lowercase()
            if (include("webstorm")) {
                create(IntelliJPlatformType.WebStorm, tabzIdeVersion)
            }
            if (include("rider")) {
                create(IntelliJPlatformType.Rider, tabzIdeVersion) {
                    useInstaller = false
                }
            }
        }
    }
}

tasks.register("prepareClosedTestRelease") {
    group = "tabz"
    description =
        "Builds a distributable ZIP for closed testers (buildPlugin; signPlugin if signing env vars are set)."
    dependsOn("buildPlugin", "signPlugin")
    doLast {
        val dist = layout.buildDirectory.dir("distributions").get().asFile
        logger.lifecycle("")
        logger.lifecycle("TabZ closed-test ZIP: ${dist.absolutePath}")
        logger.lifecycle("Nur Build — kein Marketplace-Upload. Upload: upload-closed-test.bat oder Browser.")
        logger.lifecycle("See docs/CLOSED-TEST.md")
        logger.lifecycle("")
    }
}

tasks.register("prepareTabzJetBrains") {
    group = "tabz"
    description =
        "Einmalig: WebStorm + Rider laden, Test-/Run-Sandboxes bauen, Plugin bauen, Verifier fuer beide IDEs."
    dependsOn(
        "npmInstallDemo",
        "buildPlugin",
        "prepareSandbox_testIdeWebStorm",
        "prepareSandbox_testIdeRider",
        "prepareSandbox_runIdeWebStorm",
        "prepareSandbox_runIdeRider",
        "verifyPlugin",
    )
}

listOf("WebStorm", "Rider").forEach { slug ->
    tasks.register("checkTabz$slug") {
        group = "verification"
        description =
            "Sandbox + buildPlugin + verifyPlugin nur fuer $slug (Gradle: -PtabzVerifyOnly=$slug)."
        dependsOn("prepareSandbox_testIde$slug", "buildPlugin", "verifyPlugin")
    }
    tasks.register("verifyTabz$slug") {
        group = "verification"
        description = "Alias fuer checkTabz$slug (Verifier nur $slug wenn -PtabzVerifyOnly=$slug gesetzt ist)."
        dependsOn("checkTabz$slug")
    }
}

tabzIdeTargets
    .filter { it.slug !in setOf("Idea", "WebStorm", "Rider") }
    .forEach { target ->
        tasks.register("verifyTabz${target.slug}") {
            group = "verification"
            description = "Not configured yet — use test-${target.slug.lowercase()}.bat after adding ${target.slug} to pluginVerification."
            doFirst {
                throw GradleException(
                    "Plugin Verifier for ${target.slug} is not configured yet. " +
                            "Supported: WebStorm, Rider (see build.gradle.kts pluginVerification.ides)."
                )
            }
        }
    }

tabzIdeTargets.forEach { target ->
    tasks.matching { it.name == "testIde${target.slug}" }.configureEach {
        group = "verification"
        description = "Runs tests in a ${target.slug} sandbox (tabzIdeVersion, default 2025.3)."
    }
}

tasks.named<VerifyPluginTask>("verifyPlugin") {
    failureLevel.set(listOf(VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS))
    doFirst {
        val version = tabzIdeVersion.get()
        val only = tabzVerifyOnly.orNull?.trim()
        val targets = when {
            only.isNullOrEmpty() -> "WebStorm + Rider"
            else -> only
        }
        logger.lifecycle("")
        logger.lifecycle("TabZ Plugin Verifier — IDE $version, Ziele: $targets")
        logger.lifecycle(
            "Beim ersten Lauf werden IDE-Archive geladen (prepareTabzJetBrains / prepare-jetbrains.bat).",
        )
        logger.lifecycle("")
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
