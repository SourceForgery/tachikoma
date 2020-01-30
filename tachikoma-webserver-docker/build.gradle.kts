applyDocker()

val tarTask = tasks.getByPath(":tachikoma-webserver:${ApplicationPlugin.TASK_DIST_TAR_NAME}")

val webserverDocker by tasks.registering(se.transmode.gradle.plugins.docker.DockerTask::class) {
    dependsOn(tarTask)

    applicationName = "tachikoma-webserver"

    baseImage = "ubuntu:19.10"
    maintainer = "tachikoma@sourceforgery.com"

    setEnvironment("DEBIAN_FRONTEND", "noninteractive")

    runCommand("apt-get update && apt-get -y --no-install-recommends install rsyslog rsyslog-gnutls less nvi openjdk-11-jdk-headless && apt-get clean && rm -rf /var/lib/apt/lists/*")

    runCommand("""
        mkdir -p /etc/tachikoma/rsyslog/ &&
        chown -R syslog:syslog /etc/tachikoma/rsyslog/ &&
        touch /etc/tachikoma/rsyslog/external.conf &&
        ln -s /etc/tachikoma/rsyslog/external.conf /etc/rsyslog.d/external.conf
        """.trimIndent().replace('\n', ' ')
    )

    exposePort(8443)
    exposePort(8070)

    defaultCommand(listOf("bash", "-c", "/usr/sbin/rsyslogd && /opt/tachikoma-webserver/bin/tachikoma-webserver"))

    runCommand("""
        sed -r "/(KLogPermitNonKernelFacility|imklog)/d" -i /etc/rsyslog.conf &&
        sed -r "s/\\|(.*)xconsole\$/\\1console/" -i /etc/rsyslog.d/50-default.conf
    """.trimIndent().replace('\n', ' '))

    addFile(file("src/assets/"))

    addFiles(tarTask.outputs.files) {
        it.replace(
            "^[^/]*".toRegex(),
            "/opt/tachikoma-webserver"
        )
    }

    runCommand("chmod a+x /opt/tachikoma-webserver/bin/tachikoma-webserver")

    runCommand("""
        useradd webserver &&
        mkdir -p /var/log/tachikoma &&
        chown webserver:root /var/log/tachikoma
        """.trimIndent().replace('\n', ' ')
    )

    push = rootProject.extensions.extraProperties["dockerPush"] as Boolean
}

tasks["dockerTask"].dependsOn(webserverDocker)
