package io.github.jmatsu.license.internal

import io.github.jmatsu.license.model.LicenseSeed
import io.github.jmatsu.license.model.ResolvedPomFile
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.mapNotNull

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

        val associatedUrl: String? = pomRoot.getChildByTagName("url")?.getContentAsText() ?: pomRoot.getChildByTagName("scm")?.getChildByTagName("url")?.getContentAsText()

        val displayNameCandidates =
            listOfNotNull(
                pomRoot.getChildByTagName("name")?.getContentAsText(),
                pomRoot.getChildByTagName("description")?.getContentAsText(),
                pomRoot.getChildByTagName("artifactId")?.getContentAsText(),
            )

        val licenses: List<LicenseSeed> =
            pomRoot
                .getChildByTagName("licenses")
                ?.childNodes
                ?.asList()
                .orEmpty()
                .mapNotNull { it.asElement() }
                .map {
                    val name = it.getChildByTagName("name")?.getContentAsText()
                    val url = it.getChildByTagName("url")?.getContentAsText()
                    // Is distribution node required? :thinking_face:
                    LicenseSeed(
                        name = name,
                        url = url,
                    )
                }

        val copyrightHolders =
            pomRoot
                .getChildByTagName("developers")
                ?.childNodes
                ?.asList()
                .orEmpty()
                .mapNotNull { it.asElement() }
                .mapNotNull {
                    it.getChildByTagName("name")?.getContentAsText()
                }

        require(displayNameCandidates.isNotEmpty())

        return ResolvedPomFile(
            associatedUrl = associatedUrl,
            displayNameCandidates = displayNameCandidates,
            copyrightHolders = copyrightHolders,
            licenses = licenses,
        )
    }

    private fun NodeList.asList(): List<Node> =
        (0 until length).map { idx ->
            item(idx)
        }

    private fun Node.getChildrenByTagName(name: String): List<Node> = childNodes.asList().filter { it.nodeName == name }

    private fun Node.getChildByTagName(name: String): Node? {
        val children = getChildrenByTagName(name)

        require(children.size <= 1) {
            "${children.size} $name nodes are found"
        }

        return children.firstOrNull()
    }

    private fun Node.asElement(): Element? =
        if (nodeType == Node.ELEMENT_NODE) {
            this as Element
        } else {
            null
        }

    private fun Node.getContentAsText(allowBlank: Boolean = false): String? {
        if (nodeType != Node.ELEMENT_NODE) {
            return null
        }

        return textContent.takeIf { allowBlank || it.isNotBlank() }
    }
}
