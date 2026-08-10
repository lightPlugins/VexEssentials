plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.0"
}

dependencies {
    implementation(project(":vexessentials-api")) {
        exclude(group = "dev.vexsoft", module = "vexcore-api")
    }
    compileOnly("dev.vexsoft:vexcore-paper-api:1.0.0-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:26.2.build.84-stable")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")

    testImplementation("io.papermc.paper:paper-api:26.2.build.84-stable")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveBaseName.set("VexEssentials")
    archiveClassifier.set("")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
