import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

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
        intellijIdea("2025.3")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testImplementation("junit:junit:4.13.2")
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

    runIde {
        val demoProjectPath = layout.projectDirectory.dir("demo-project").asFile.absolutePath
        argumentProviders += CommandLineArgumentProvider {
            listOf(demoProjectPath)
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "SubTabs"
        version = project.version.toString()
    }
}
