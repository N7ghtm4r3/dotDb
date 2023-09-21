plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.tecknobit"
version = "1.0.2"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.1.1")
    type.set("IC")
    plugins.set(listOf())
}

dependencies {

    implementation("org.xerial:sqlite-jdbc:3.39.3.0")

}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("241.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
