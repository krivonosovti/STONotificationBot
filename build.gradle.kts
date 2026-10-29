plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    kotlin("plugin.jpa") version "2.2.20"
    id("org.springframework.boot") version "4.0.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.17.0"
}

group = "ru.andryukha"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
    compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") }
}
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
repositories { mavenCentral() }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform { excludeTags("integration") }
}
tasks.register<Test>("integrationTest") {
    description = "HTTP, migrations and repository checks on real PostgreSQL (Docker required)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("integration") }
    shouldRunAfter(tasks.test)
}
tasks.bootJar { archiveFileName.set("andryukha.jar") }

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set(layout.projectDirectory.file("src/main/resources/openapi/telegram-bot-api.yaml").asFile.path)
    outputDir.set(layout.buildDirectory.dir("generated/telegram").get().asFile.path)
    modelPackage.set("andryukha.integration.telegram.generated.model")
    globalProperties.set(mapOf("models" to "", "modelDocs" to "false", "modelTests" to "false"))
    configOptions.set(mapOf(
        "serializationLibrary" to "jackson",
        "dateLibrary" to "java8",
        "useBeanValidation" to "false",
        "additionalModelTypeAnnotations" to "@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)",
    ))
}
kotlin.sourceSets.main { kotlin.srcDir(layout.buildDirectory.dir("generated/telegram/src/main/kotlin")) }
tasks.named("compileKotlin") { dependsOn(tasks.named("openApiGenerate")) }
tasks.named("check") { dependsOn(tasks.named("openApiValidate")) }
openApiValidate {
    inputSpec.set(layout.projectDirectory.file("src/main/resources/openapi/telegram-bot-api.yaml").asFile.path)
}
