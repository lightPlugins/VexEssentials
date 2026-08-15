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
    testImplementation("dev.vexsoft:vexcore-paper-api:1.0.0-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:26.2.build.84-stable")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")
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
