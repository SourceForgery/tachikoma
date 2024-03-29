/**
 * Copyright 2014 Transmode AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.transmode.gradle.plugins.docker

import org.gradle.api.JavaVersion
import java.lang.IllegalArgumentException

/**
 * @author Matthias Grüter, matthias.gruter@transmode.com
 */
enum class JavaBaseImage(val imageName: String, val target: JavaVersion) {
    JAVA6("fkautz/java6-jre", JavaVersion.VERSION_1_6), JAVA7(
        "dockerfile/java",
        JavaVersion.VERSION_1_7
    ),
    JAVA8("aglover/java8-pier", JavaVersion.VERSION_1_8);

    companion object {
        fun imageFor(target: JavaVersion): JavaBaseImage {
            for (image in values()) {
                if (image.target == target) {
                    return image
                }
            }
            throw IllegalArgumentException("No default Java base image for the supplied target $target found. Base image needs to be set explicitly.")
        }
    }
}
