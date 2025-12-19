plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.crossguild"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")

        bundledPlugin("Git4Idea")

        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        id.set("com.crossguild.difffrog")
        name.set("DiffFrog")

        description.set("""
            🐸 <b>DiffFrog</b>: A cute way to track your Git changes.
            Features:
            <ul>
                <li>Live counter of added and deleted lines in the toolbar.</li>
                <li>Animated number transitions.</li>
                <li>Special 420 🌿 and ⚠️ alerts.</li>
            </ul>
        """.trimIndent())

        vendor {
            name.set("")
            email.set("")
            url.set("https://github.com/tu-usuario/difffrog")
        }

        ideaVersion {
            sinceBuild.set("241")
            untilBuild.set("252.*")
        }
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}