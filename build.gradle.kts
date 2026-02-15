plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8" apply false
}

allprojects {
    group = "xyz.rishabhvenu.abilityengine"
    version = "1.0.0-SNAPSHOT"
    
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "java")
    
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
    
    dependencies {
        compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    }
    
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
