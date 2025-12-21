package io.github.jmatsu.license.internal

import io.github.jmatsu.license.model.LicenseSeed
import io.github.jmatsu.license.model.ResolvedPomFile
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PomParser(
    private val file: File,
) {
    fun parse(): ResolvedPomFile {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isValidating = false
                isNamespaceAware = false
            }
        val pomRoot =
            factory.newDocumentBuilder().parse(file).documentElement.apply {
                normalize()
            }

        val associatedUrl: String? = pomRoot.getOptionalAttribute("url") ?: pomRoot.getOptionalAttribute("scm.url")

        val displayNameCandidates =
            listOfNotNull(
                pomRoot.getOptionalAttribute("name"),
                pomRoot.getOptionalAttribute("description"),
                pomRoot.getOptionalAttribute("artifactId"),
            )

        val licenses: List<LicenseSeed> =
            pomRoot
                .getChildElementsOfTag("licenses")
                .map {
                    val name = it.getOptionalAttribute("name")
                    val url = it.getOptionalAttribute("url")
                    // Is distribution node required? :thinking_face:
                    LicenseSeed(
                        name = name,
                        url = url,
                    )
                }

        val copyrightHolders =
            pomRoot
                .getChildElementsOfTag("developers")
                .mapNotNull {
                    it.getOptionalAttribute("name")
                }

        require(displayNameCandidates.isNotEmpty())

        return ResolvedPomFile(
            associatedUrl = associatedUrl,
            displayNameCandidates = displayNameCandidates,
            copyrightHolders = copyrightHolders,
            licenses = licenses,
        )
    }

    private fun Element.getOptionalAttribute(
        name: String,
        allowBlank: Boolean = false,
    ): String? =
        if (hasAttribute(name)) {
            getAttribute(name).takeIf { allowBlank || it.isNotBlank() }
        } else {
            null
        }

    private fun NodeList.asList(): List<Node> =
        (0 until length).map { idx ->
            item(idx)
        }

    private fun Element.getChildElementsOfTag(name: String): List<Element> {
        val nodes = getElementsByTagName(name)

        return nodes
            .asList()
            .flatMap { it.childNodes.asList() }
            .mapNotNull {
                if (it.nodeType == Node.ELEMENT_NODE) {
                    it as Element
                } else {
                    null
                }
            }
    }
}
