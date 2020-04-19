package de.hpi.tdgt.test.story.atom.assertion

import de.hpi.tdgt.Stats.Endpoint
import de.hpi.tdgt.requesthandling.RestResult
import de.hpi.tdgt.test.story.atom.RequestAtom
import de.hpi.tdgt.test.time_measurement.TimeStorage
import org.apache.logging.log4j.LogManager
import org.htmlcleaner.CleanerProperties
import org.htmlcleaner.DomSerializer
import org.htmlcleaner.HtmlCleaner
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class XPATHAssertion : Assertion() {
    var xPath: String? = null
    var returnPage = false
    override suspend fun check(endpoint: Endpoint, restResult: RestResult, parent: RequestAtom) {
        val response = String(restResult.response)
        val tagNode = HtmlCleaner().clean(response)
        val doc = DomSerializer(
                CleanerProperties()
        ).createDOM(tagNode)
        val xPathInterpreter: XPath = XPathFactory.newInstance().newXPath()
        try {
            val str = xPathInterpreter.evaluate(
                    parent.replaceWithKnownParams(xPath ?: "", enquoteInsertedValue = true, sanitizeXPATH = true),
                    doc, XPathConstants.STRING
            ) as String
            if (str.isBlank()) {
                if (!returnPage) {
                    log.error("Failed xpath assertion\"$name\": expected \"$xPath\" to find text but nothing was returned")
                    TimeStorage.instance.addError(endpoint, "XPATH result is empty");
                } else {
                    log.error("Failed xpath assertion\"$name\": returned $response")
                    TimeStorage.instance.addError(endpoint, "XPATH wrong result");
                }
            }
        } catch (e: Exception) {
            log.error("Failed xpath assertion\"$name\": xpath \"$xPath\" is invalid :${e.message}")
            TimeStorage.instance.addError(endpoint, e.toString());
        }
    }

    companion object {
        private val log =
                LogManager.getLogger(XPATHAssertion::class.java)
    }

}