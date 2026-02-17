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
        
        // Merge META-INF/services files so GraalVM's ServiceLoader discovery works
        mergeServiceFiles()
        
        // Relocate SnakeYAML to avoid conflicts with Paper's bundled version
        relocate("org.yaml.snakeyaml", "xyz.rishabhvenu.abilityengine.libs.snakeyaml")
        
        // GraalVM must NOT be relocated — it uses ServiceLoader/module system
        // for language discovery, which breaks when packages are renamed
    }
    
    build {
        dependsOn(shadowJar)
    }
}
