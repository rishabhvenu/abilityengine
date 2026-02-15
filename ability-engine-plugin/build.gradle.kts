plugins {
    id("io.github.goooler.shadow")
}

dependencies {
    implementation(project(":ability-engine-api"))
    implementation(project(":ability-engine-core"))
    implementation(project(":ability-engine-config"))
    implementation(project(":ability-engine-script"))
    implementation(project(":ability-engine-module-loader"))
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("AbilityEngine")
        
        // Relocate SnakeYAML to avoid conflicts with Paper's bundled version
        relocate("org.yaml.snakeyaml", "xyz.rishabhvenu.abilityengine.libs.snakeyaml")
        
        // Relocate GraalVM to avoid conflicts with other plugins
        relocate("org.graalvm", "xyz.rishabhvenu.abilityengine.libs.graalvm")
    }
    
    build {
        dependsOn(shadowJar)
    }
}
