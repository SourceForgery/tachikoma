plugins {
    `tachikoma-docker`
}

val tarTask =
    project(":tachikoma-webserver")
        .tasks[ApplicationPlugin.TASK_DIST_TAR_NAME]

val webserverDocker by tasks.registering(se.transmode.gradle.plugins.docker.DockerTask::class) {
    dependsOn(tarTask)

    applicationName.set("tachikoma-webserver")

    baseImage.set("ubuntu:22.04")

    setEnvironment("DEBIAN_FRONTEND", "noninteractive")

    runCommand("apt-get update && apt-get -y --no-install-recommends install curl rsyslog rsyslog-gnutls less nvi openjdk-21-jdk-headless && apt-get clean && rm -rf /var/lib/apt/lists/*")

    runCommand(
        """
        mkdir -p /etc/tachikoma/rsyslog/ &&
        chown -R syslog:syslog /etc/tachikoma/rsyslog/ &&
        touch /etc/tachikoma/rsyslog/external.conf &&
        ln -s /etc/tachikoma/rsyslog/external.conf /etc/rsyslog.d/external.conf &&
        curl -Ss https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -o /usr/bin/cloud_sql_proxy &&
        chmod 0755 /usr/bin/cloud_sql_proxy &&
        echo "LANG=C.UTF-8" > /etc/default/locale
        """.trimIndent().replace('\n', ' '),
    )

    exposePort(8443)
    exposePort(8070)

    defaultCommand(listOf("/start.sh"))

    runCommand(
        """
        sed -r "/(KLogPermitNonKernelFacility|imklog)/d" -i /etc/rsyslog.conf &&
        sed -r "s/\\|(.*)xconsole\$/\\1console/" -i /etc/rsyslog.d/50-default.conf
        """.trimIndent().replace('\n', ' '),
    )

    addFile(file("src/assets/"))

    addFiles(tarTask.outputs.files) {
        it.replace(
            "^[^/]*".toRegex(),
            "/opt/tachikoma-webserver",
        )
    }

    runCommand("chmod a+x /opt/tachikoma-webserver/bin/tachikoma-webserver")

    runCommand(
        """
        useradd webserver &&
        mkdir -p /var/log/tachikoma &&
        chown webserver:root /var/log/tachikoma
        """.trimIndent().replace('\n', ' '),
    )

    push.set(rootProject.provider { rootProject.extensions.extraProperties["dockerPush"] as Boolean })
}

tasks["dockerTask"].dependsOn(webserverDocker)
