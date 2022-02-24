package com.sourceforgery.tachikoma.buildsrc.docker

import java.io.Serializable

class DockerTag(
    val fullTag: String
) : Serializable {
    @Transient
    val repository: String

    @Transient
    val imageName: String

    @Transient
    val version: String

    init {
        val matcher = requireNotNull(SPLIT_TAG.matchEntire(fullTag)) {
            "Invalid fullTag: '$fullTag"
        }
        repository = matcher.groupValues[1].trimEnd('/')
        imageName = matcher.groupValues[2]
        version = matcher.groupValues[3]
        require(imageName != "null") { "No 'null' tags please: '$fullTag'" }
    }

    companion object {
        private val SPLIT_TAG = Regex("([-/.a-z0-9]+/)?([-a-z0-9]+):([-.0-9a-zA-Z]+)")
    }
}
