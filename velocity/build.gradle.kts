dependencies {
  implementation(project(":api"))
  implementation(project(":common"))

  compileOnly(rootProject.libs.velocity)
  // Bundled privately (not compileOnly) and relocated below: some Velocity forks (e.g.
  // Velocity-CTD) ship an older adventure-nbt where BinaryTagType is an interface, while
  // this project's pinned adventure-nbt (4.24.0) compiles it as an abstract class --
  // relying on the platform-provided copy throws IncompatibleClassChangeError at runtime
  // on those forks. Paper's module already isolates itself the same way; Velocity needs it too.
  implementation(rootProject.libs.adventure.nbt)

  implementation(rootProject.libs.bstats.velocity)
  implementation(rootProject.libs.libby.velocity)
}

tasks {
  processResources {
    val props = mapOf(
      "version" to rootProject.version.toString(),
      "description" to rootProject.description,
      "url" to "https://sonar.top/discord/",
      "main" to "xyz.jonesdev.sonar.velocity.SonarVelocityPlugin"
    )
    inputs.properties(props)
    filesMatching("velocity-plugin.json") {
      expand(props)
    }
  }

  shadowJar {
    relocate("net.kyori.adventure.nbt", "xyz.jonesdev.sonar.libs.kyori.adventure.nbt")
  }
}
