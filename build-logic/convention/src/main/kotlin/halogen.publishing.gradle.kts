plugins {
    id("com.vanniktech.maven.publish")
}

group = "me.mmckenna.halogen"
version = "0.2.0"

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    if (project.hasProperty("signing.keyId") ||
        System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null
    ) {
        signAllPublications()
    }

    coordinates("me.mmckenna.halogen", project.name, version.toString())

    pom {
        url.set("https://github.com/himattm/halogen")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("himattm")
                name.set("Matt McKenna")
                url.set("https://blog.mmckenna.me")
            }
        }
        scm {
            url.set("https://github.com/himattm/halogen")
            connection.set("scm:git:git://github.com/himattm/halogen.git")
            developerConnection.set("scm:git:ssh://github.com/himattm/halogen.git")
        }
    }
}
