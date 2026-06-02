// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

tasks.register("generateReleaseKeystore") {
    doLast {
        val keystoreFile = file("${rootDir}/my-upload-key.jks")
        if (!keystoreFile.exists()) {
            println("Generating release keystore...")
            val pb = java.lang.ProcessBuilder(
                "keytool", "-genkeypair",
                "-v",
                "-keystore", keystoreFile.absolutePath,
                "-alias", "upload",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-dname", "CN=wfseek, OU=Wfseek, O=Wfseek LLC, L=Lagos, S=Lagos, C=NG",
                "-storepass", "android",
                "-keypass", "android"
            )
            val process = pb.inheritIO().start()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                println("Keystore generated successfully at: ${keystoreFile.absolutePath}")
            } else {
                throw GradleException("Keytool failed with exit code $exitCode")
            }
        } else {
            println("Keystore already exists!")
        }
    }
}
