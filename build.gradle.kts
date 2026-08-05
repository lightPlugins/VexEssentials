import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    base
}

allprojects {
    group = "dev.vexsoft"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    plugins.withId("java") {
        dependencies {
            add("compileOnly", "org.projectlombok:lombok:1.18.42")
            add("annotationProcessor", "org.projectlombok:lombok:1.18.42")
            add("testCompileOnly", "org.projectlombok:lombok:1.18.42")
            add("testAnnotationProcessor", "org.projectlombok:lombok:1.18.42")
        }

        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(25))
            withSourcesJar()
        }

        if (project.name == "vexessentials-api") {
            pluginManager.apply("maven-publish")
            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("mavenJava") {
                        artifactId = "vexessentials-api"
                        from(components["java"])
                    }
                }
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
