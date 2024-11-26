plugins {
    `tachikoma-docker`
}

val tarTask = tasks.getByPath(":tachikoma-postfix-utils:${ApplicationPlugin.TASK_DIST_TAR_NAME}")

val postfixDocker by tasks.registering(se.transmode.gradle.plugins.docker.DockerTask::class) {
    dependsOn(tarTask)
    applicationName.set("tachikoma-postfix")
    tagVersion.set(project.provider { project.version.toString() })
    baseImage.set("golang:1.23.3-bookworm")
    runCommand("CGO_ENABLED=0 go install github.com/mattn/goreman@v0.3.16")

    from("ubuntu:24.04", "name")
    workingDir("/opt")

    setEnvironment("DEBIAN_FRONTEND", "noninteractive")

    runCommand(
        """
        apt-get update &&
        apt-get -y dist-upgrade &&
        apt-get -y --no-install-recommends install rsyslog rsyslog-gnutls less nvi postfix sasl2-bin opendkim opendkim-tools openjdk-21-jdk-headless netcat-openbsd net-tools &&
        apt-get clean &&
        rm -rf /var/cache/apt/ /var/lib/apt/lists/* &&
        echo "LANG=C.UTF-8" > /etc/default/locale
        """.trimMargin().replace('\n', ' '),
    )

    exposePort(25)

    addFile(file("src/assets/"))
    copy("/go/bin/goreman", "/usr/local/bin/goreman", "0")

    defaultCommand(listOf("/usr/local/bin/goreman", "start"))

    runCommand(
        """
        mkfifo /opt/maillog_pipe && chown syslog:postfix /opt/maillog_pipe &&
        mkdir -p /var/spool/postfix/tachikoma/ &&

        sed -r "/(KLogPermitNonKernelFacility|imklog)/d" -i /etc/rsyslog.conf &&
        sed -r "s/\\|(.*)xconsole\$/\\1console/" -i /etc/rsyslog.d/50-default.conf &&

        adduser syslog postfix &&
        adduser postfix opendkim &&
        useradd tachikoma -d /opt &&

        chown postfix:tachikoma /var/spool/postfix/tachikoma/ && chmod 0770 /var/spool/postfix/tachikoma/
        """.trimIndent().replace(
            '\n',
            ' ',
        ),
    )

    addFiles(tarTask.outputs.files) {
        it.replace(
            "^[^/]*".toRegex(),
            "/opt/tachikoma-postfix-utils",
        )
    }

    runCommand("chmod a+x /opt/tachikoma-postfix-utils/bin/tachikoma-postfix-utils")

    addInstruction("HEALTHCHECK", "CMD netstat -lnt | grep -q :::25 || exit 1")

    push.set(rootProject.provider { rootProject.extensions.extraProperties["dockerPush"] as Boolean })
}

tasks["dockerTask"].dependsOn(postfixDocker)
