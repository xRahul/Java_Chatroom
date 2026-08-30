plugins {
    `java-library`
}

group = "com.chatroom"
version = "1.0.0"

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.4")
        testImplementation("org.mockito:mockito-core:5.23.0")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    tasks.named<JavaCompile>("compileJava") {
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    tasks.named<JavaCompile>("compileTestJava") {
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }
}

project(":server") {
    apply(plugin = "application")
    dependencies {
        implementation(project(":shared"))
    }
    configure<JavaApplication> {
        mainClass.set("com.chatroom.server.StartingPointServer")
    }
}

project(":client") {
    apply(plugin = "application")
    dependencies {
        implementation(project(":shared"))
    }
    configure<JavaApplication> {
        mainClass.set("com.chatroom.client.StartingPointClient")
    }
}
