package de.hpi.tdgt.test.story.atom.assertion

import de.hpi.tdgt.requesthandling.RestResult
import org.apache.logging.log4j.LogManager
import org.htmlcleaner.CleanerProperties
import org.htmlcleaner.DomSerializer
import org.htmlcleaner.HtmlCleaner
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class XPATHAssertion : Assertion() {
    var xPath: String? = null
    override fun check(restResult: RestResult?, testid: Long) {
        if(restResult!=null) {
            val tagNode = HtmlCleaner().clean(String(restResult.response))
            val doc = DomSerializer(
                    CleanerProperties()
            ).createDOM(tagNode)
            val xPathInterpreter: XPath = XPathFactory.newInstance().newXPath()
                try {
                    val str = xPathInterpreter.evaluate(
                            xPath,
                            doc, XPathConstants.STRING
                    ) as String
                    if(str.isBlank()){
                        log.error("Failed xpath assertion\"$name\": expected \"$xPath\" to find text but nothing was returned")
                        AssertionStorage.instance.addFailure(name, "xpath \"${xPath}\" returned empty result", testid)
                    }
                } catch (e: Exception) {
                    log.error("Failed xpath assertion\"$name\": xpath \"$xPath\" is invalid :${e.message}")
                    AssertionStorage.instance.addFailure(name, "xpath \"${xPath}\" is invalid :${e.message}", testid)
                }
        }
    }
    companion object {
        private val log =
                LogManager.getLogger(XPATHAssertion::class.java)
    }

}