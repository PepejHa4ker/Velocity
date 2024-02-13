plugins {
    `java-library`
    id("velocity-checkstyle") apply false
    id("velocity-spotless") apply false
}

subprojects {
    apply<JavaLibraryPlugin>()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    dependencies {
        testImplementation(rootProject.libs.junit)
    }

    tasks {
        test {
            useJUnitPlatform()
            reports {
                junitXml.required.set(true)
            }
        }
    }
}
