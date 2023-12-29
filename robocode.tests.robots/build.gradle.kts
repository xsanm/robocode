plugins {
    id("net.sf.robocode.java-conventions")
    `java-library`
}

dependencies {
    implementation(project(":robocode.api"))
    implementation(project(":robocode.samples"))
    implementation("org.jeasy:easy-rules-core:4.0.0")
}

description = "Robocode Tested Robots"

tasks {
    register("copyContent", Copy::class) {
        from("src/main/resources") {
            include("**/*.*")
        }
        into("../.sandbox/robots")
    }
    register("copyClasses", Copy::class) {
        dependsOn(configurations.runtimeClasspath)

        from(compileJava)
        into("../.sandbox/robots")
    }
    jar {
        dependsOn("copyContent")
        dependsOn("copyClasses")
    }
}

tasks {
    publishMavenJavaPublicationToSonatypeRepository {
        enabled = false
    }
}