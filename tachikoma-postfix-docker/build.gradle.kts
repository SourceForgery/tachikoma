applyDocker()

val tarTask = tasks.getByPath(":tachikoma-postfix-utils:${ApplicationPlugin.TASK_DIST_TAR_NAME}")

val postfixDocker by tasks.registering(se.transmode.gradle.plugins.docker.DockerTask::class) {
    dependsOn(tarTask)
    applicationName = "tachikoma-postfix"

    baseImage = "ubuntu:19.10"

    maintainer = "tachikoma@sourceforgery.com"

    workingDir("/opt")

    setEnvironment("DEBIAN_FRONTEND", "noninteractive")
    setEnvironment("PYTHONIOENCODING", "utf-8")

    runCommand("""apt-get update && \
                  apt-get -y dist-upgrade && \
                  apt-get -y --no-install-recommends install rsyslog rsyslog-gnutls python3-pip python3-pkg-resources less nvi postfix sasl2-bin opendkim opendkim-tools openjdk-11-jdk-headless netcat-openbsd net-tools && \
                  apt-get clean && \
                  rm -rf /var/cache/apt/ /var/lib/apt/lists/*
              """)
    runCommand("pip3 install --no-cache-dir honcho==1.0.1")

    exposePort(25)

    addFile(file("src/assets/"))

    defaultCommand(listOf("/usr/local/bin/honcho", "start", "-f", "Procfile"))

    runCommand("""
        mkfifo /opt/maillog_pipe && chown syslog:postfix /opt/maillog_pipe &&
        mkdir -p /var/spool/postfix/tachikoma/ &&

        sed -r "/(KLogPermitNonKernelFacility|imklog)/d" -i /etc/rsyslog.conf &&
        sed -r "s/\\|(.*)xconsole\$/\\1console/" -i /etc/rsyslog.d/50-default.conf &&

        adduser syslog postfix &&
        adduser postfix opendkim &&
        useradd tachikoma -d /opt &&

        chown postfix:tachikoma /var/spool/postfix/tachikoma/ && chmod 0770 /var/spool/postfix/tachikoma/
        """.trimIndent().replace('\n', ' '))

    addFiles(tarTask.outputs.files) {
        it.replace(
            "^[^/]*".toRegex(),
            "/opt/tachikoma-postfix-utils"
        )
    }

    runCommand("chmod a+x /opt/tachikoma-postfix-utils/bin/tachikoma-postfix-utils")

    addInstruction("HEALTHCHECK", "CMD netstat -lnt | grep -q :::25 || exit 1")

    push = rootProject.extensions.extraProperties["dockerPush"] as Boolean
}

tasks["dockerTask"].dependsOn(postfixDocker)