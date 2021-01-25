package com.sourceforgery.tachikoma.maildelivery

import kotlin.test.fail

fun String.homogenize(): String =
    replace("\r\n", "\n")
        .replace("\r", "\n")
        .trimStart('\n', ' ')
        .trimEnd('\n', ' ')

class SampleMessage(
    envelope: String,
    htmlText: String,
    plainText: String
) {
    val envelope = SampleMessage::class.java.getResourceAsStream("/emails/messages/$envelope")
        ?.use { it.readAllBytes() }
        ?: fail("Could not load $envelope")
    val htmlText = htmlText.homogenize()
    val plainText = plainText.homogenize()
}

val m1001 = SampleMessage(
    envelope = "m1001.txt",
    plainText =
        """
        |Die Hasen und die Frösche
        |
        |Die Hasen klagten einst über ihre mißliche Lage; "wir leben", sprach ein
        |Redner, "in steter Furcht vor Menschen und Tieren, eine Beute der Hunde,
        |der Adler, ja fast aller Raubtiere! Unsere stete Angst ist ärger als der
        |Tod selbst. Auf, laßt uns ein für allemal sterben."
        |
        |In einem nahen Teich wollten sie sich nun ersäufen; sie eilten ihm zu;
        |allein das außerordentliche Getöse und ihre wunderbare Gestalt
        |erschreckte eine Menge Frösche, die am Ufer saßen, so sehr, daß sie aufs
        |schnellste untertauchten.
        |
        |"Halt", rief nun eben dieser Sprecher, "wir wollen das Ersäufen noch ein
        |wenig aufschieben, denn auch uns fürchten, wie ihr seht, einige Tiere,
        |welche also wohl noch unglücklicher sein müssen als wir."
        """.trimMargin(),
    htmlText = ""
)

val m1005 = SampleMessage(
    envelope = "m1005.txt",
    plainText =
        """
            |[blue ball]
            |
            |Die Hasen und die Frösche
            |
            |Die Hasen klagten einst über ihre mißliche Lage; "wir leben", sprach ein
            |Redner, "in steter Furcht vor Menschen und Tieren, eine Beute der Hunde,
            |der Adler, ja fast aller Raubtiere! Unsere stete Angst ist ärger als der
            |Tod selbst. Auf, laßt uns ein für allemal sterben."
            |
            |In einem nahen Teich wollten sie sich nun ersäufen; sie eilten ihm zu;
            |allein das außerordentliche Getöse und ihre wunderbare Gestalt
            |erschreckte eine Menge Frösche, die am Ufer saßen, so sehr, daß sie aufs
            |schnellste untertauchten.
            |
            |"Halt", rief nun eben dieser Sprecher, "wir wollen das Ersäufen noch ein
            |wenig aufschieben, denn auch uns fürchten, wie ihr seht, einige Tiere,
            |welche also wohl noch unglücklicher sein müssen als wir."
            |
            |[Image]
            |
            |
            """.trimMargin(),

    htmlText =
        """
            |<!doctype html public "-//w3c//dtd html 4.0 transitional//en">
            |<html>
            |<img SRC="cid:part1.39235FC5.E71D8178@example.com" ALT="blue ball" height=27 width=27><b></b>
            |<p><b>Die Hasen und die Fr&ouml;sche</b>
            |<p>Die Hasen klagten einst &uuml;ber ihre mi&szlig;liche Lage; "wir leben",
            |sprach ein Redner, "in steter Furcht vor Menschen und Tieren, eine Beute
            |der Hunde, der Adler, ja fast aller Raubtiere! Unsere stete Angst ist &auml;rger
            |als der Tod selbst. Auf, la&szlig;t uns ein f&uuml;r allemal sterben."
            |<p>In einem nahen Teich wollten sie sich nun ers&auml;ufen; sie eilten
            |ihm zu; allein das au&szlig;erordentliche Get&ouml;se und ihre wunderbare
            |Gestalt erschreckte eine Menge Fr&ouml;sche, die am Ufer sa&szlig;en, so
            |sehr, da&szlig; sie aufs schnellste untertauchten.
            |<p>"Halt", rief nun eben dieser Sprecher, "wir wollen das Ers&auml;ufen
            |noch ein wenig aufschieben, denn auch uns f&uuml;rchten, wie ihr seht,
            |einige Tiere, welche also wohl noch ungl&uuml;cklicher sein m&uuml;ssen
            |als wir."
            |<p><img SRC="cid:part2.39235FC5.E71D8178@example.com" height=27 width=27>
            |<br>&nbsp;
            |<br>&nbsp;</html>
        """.trimMargin()
)

val m1006 = SampleMessage(
    envelope = "m1006.txt",
    plainText =
        """
        |Die Hasen und die Frösche 
        |
        |Die Hasen klagten einst über ihre mißliche Lage; "wir leben", sprach ein
        |Redner, "in steter Furcht vor Menschen und Tieren, eine Beute der Hunde, der
        |Adler, ja fast aller Raubtiere! Unsere stete Angst ist ärger als der Tod
        |selbst. Auf, laßt uns ein für allemal sterben."
        |
        |In einem nahen Teich wollten sie sich nun ersäufen; sie eilten ihm zu; allein
        |das außerordentliche Getöse und ihre wunderbare Gestalt erschreckte eine Menge
        |Frösche, die am Ufer saßen, so sehr, daß sie aufs schnellste untertauchten.
        |
        |"Halt", rief nun eben dieser Sprecher, "wir wollen das Ersäufen noch ein wenig
        |aufschieben, denn auch uns fürchten, wie ihr seht, einige Tiere, welche also
        |wohl noch unglücklicher sein müssen als wir."
    """.trimMargin(),
    htmlText =
        """
        |<!doctype html public "-//w3c//dtd html 4.0 transitional//en">
        |<html>
        |<img SRC="cid:part1.39236103.1B697A54@example.com" ALT="blue ball" height=27 width=27><b></b>
        |<p><b><font size=+2>Die Hasen und die Fr&ouml;sche</font></b>
        |<p>Die Hasen klagten einst &uuml;ber ihre mi&szlig;liche Lage; "wir leben",
        |sprach ein Redner, "in steter Furcht vor Menschen und Tieren, eine Beute
        |der Hunde, der Adler, ja fast aller Raubtiere! Unsere stete Angst ist &auml;rger
        |als der Tod selbst. Auf, la&szlig;t uns ein f&uuml;r allemal sterben."
        |<p>In einem nahen Teich wollten sie sich nun ers&auml;ufen; sie eilten
        |ihm zu; allein das au&szlig;erordentliche Get&ouml;se und ihre wunderbare
        |Gestalt erschreckte eine Menge Fr&ouml;sche, die am Ufer sa&szlig;en, so
        |sehr, da&szlig; sie aufs schnellste untertauchten.
        |<p>"Halt", rief nun eben dieser Sprecher, "wir wollen das Ers&auml;ufen
        |noch ein wenig aufschieben, denn auch uns f&uuml;rchten, wie ihr seht,
        |einige Tiere, welche also wohl noch ungl&uuml;cklicher sein m&uuml;ssen
        |als wir."
        |<p><img SRC="cid:part2.39236103.1B697A54@example.com" ALT="red ball" height=27 width=27></html>
    """.trimMargin()
)

val m2008 = SampleMessage(
    envelope = "m2008.txt",
    plainText =
        """
        |Die Hasen und die Frösche
        |
        |Die Hasen klagten einst über ihre mißliche Lage; "wir leben", sprach ein Redner, "in steter Furcht vor Menschen und Tieren, eine Beute der Hunde, der Adler, ja fast aller Raubtiere! Unsere stete Angst ist ärger als der Tod selbst. Auf, laßt uns ein für allemal sterben." 
        |
        |In einem nahen Teich wollten sie sich nun ersäufen; sie eilten ihm zu; allein das außerordentliche Getöse und ihre wunderbare Gestalt erschreckte eine Menge Frösche, die am Ufer saßen, so sehr, daß sie aufs schnellste untertauchten. 
        |
        |"Halt", rief nun eben dieser Sprecher, "wir wollen das Ersäufen noch ein wenig aufschieben, denn auch uns fürchten, wie ihr seht, einige Tiere, welche also wohl noch unglücklicher sein müssen als wir." 
        |
        |2aa5e03a.png2aa5e044.png
    """.trimMargin(),
    htmlText = ""
)
