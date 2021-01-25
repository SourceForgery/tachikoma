package com.sourceforgery.tachikoma.maildelivery

import org.jsoup.Jsoup
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(JUnitPlatform::class)
class HtmlToPlainTextTest {

    fun `Convert simple html to plain text - simple`() {
        val plainText = getPlainText(simpleHtmlDoc)

        assertEquals(simpleHtmlText, plainText)
    }

    companion object {
        val simpleHtmlDoc = Jsoup.parse(
            """
            <!doctype html><html><head>
  <meta http-equiv="Content-Type" content="text/simpleHtmlDoc; charset=UTF-8">
  <meta name="viewport" content="width=device-width">
  <style>
  ol {
    padding-left: 15px;
    margin: 0;
  }
  hr {
    padding-top: 1px;
    margin: 2rem 0;
    color: #DDDDDD;
    border: none;
    background-color: #DDDDDD;
  }
  </style>
</head>
<body>

<div style="max-width: 600px;">
  <p><b>HELLO</b></p>

  <p>Lorem ipsum dolor sit amet, phasellus gravida. Class risus turpis enim euismod pulvinar</p>

  <p>Eget tellus gravida, nec wisi non vehicula placerat tristique, mauris nibh ante adipiscing, est libero donec netus <a href="http://example.com:8070/c/qgYuqgYDCNodsgYlaHR0cDovL2xtZ3RmeS5jb20vP3E9Z2F5K3Bvcm4rc3Rhci8jIbIGFBfuwijStNj52X5FXoBTZVHzg9F6">example.com</a> Ante nunc est, ante sed eros iaculis eget, mauris venenatis nulla, auctor blandit elementum, phasellus dignissim.</p>

  <p><a href="http://example.com:8070/c/qgYuqgYDCNodsgYlaHR0cDovL2xtZ3RmeS5jb20vP3E9Z2F5K3Bvcm4rc3Rhci8jIbIGFBfuwijStNj52X5FXoBTZVHzg9F6">Ab magna mauris. Litora sollicitudin et aliquet neque urna volutpat</a></p>

  <p>
     Commodo sagittis at, a purus vitae, lectus tempus consectetuer assumenda sit elit, purus a sit
  </p>

  <center>
      Lorem ipsum is a pseudo-Latin text used in web design,
  </center>

  経意責家方家閉討店暖育田庁載社転線宇<br>
  ÅÄÖ<br>
  税理投関懲北用年戦日投人<br>

</div>


<img src="http://example.com:8070/t/qgYGqgYDCNodsgYUXZxYoaar0iLOs-QkYTYkuZMv3HM" height="1" width="1"></body></simpleHtmlDoc>
"""
        )

        val simpleHtmlText =
            """
HELLO

Lorem ipsum dolor sit amet, phasellus gravida. Class risus turpis enim euismod
pulvinar

Eget tellus gravida, nec wisi non vehicula placerat tristique, mauris nibh
ante adipiscing, est libero donec netusexample.com
<http://example.com:8070/c/qgYuqgYDCNodsgYlaHR0cDovL2xtZ3RmeS5jb20vP3E9Z2F5K3Bvcm4rc3Rhci8jIbIGFBfuwijStNj52X5FXoBTZVHzg9F6>
 Ante nunc est, ante sed eros iaculis eget, mauris venenatis nulla, auctor
blandit elementum, phasellus dignissim.

Ab magna mauris. Litora sollicitudin et aliquet neque urna volutpat
<http://example.com:8070/c/qgYuqgYDCNodsgYlaHR0cDovL2xtZ3RmeS5jb20vP3E9Z2F5K3Bvcm4rc3Rhci8jIbIGFBfuwijStNj52X5FXoBTZVHzg9F6>

 Commodo sagittis at, a purus vitae, lectus tempus consectetuer assumenda sit
elit, purus a sit
 Lorem ipsum is a pseudo-Latin text used in web design,  経意責家方家閉討店暖育田庁載社転線宇
 ÅÄÖ
 税理投関懲北用年戦日投人
"""
    }
}
