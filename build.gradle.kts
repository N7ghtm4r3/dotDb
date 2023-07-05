plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.14.2"
}

group = "com.tecknobit"
version = "1.0.1"

repositories {
    mavenCentral()
}

intellij {
    version.set("2022.1.4")
    type.set("IC")
    plugins.set(listOf())
}

dependencies {

    implementation("org.xerial:sqlite-jdbc:3.39.3.0")

}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("221")
        untilBuild.set("231.*")
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
