package com.softmotions.xconfig

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.w3c.dom.*
import org.xml.sax.InputSource
import java.io.*
import java.net.URI
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.concurrent.read
import kotlin.concurrent.write


/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class XConfigBuilder
constructor(private val mUrl: URL) {

    companion object {

        private val log = LoggerFactory.getLogger("com.softmotions.aconfig")

        fun basicSubstitutor(key: String): String? = when (key) {
            "cwd" -> System.getProperty("user.dir")
            "home" -> System.getProperty("user.home")
            else -> when {
                key.startsWith("env:") -> System.getenv(key.substring("env:".length))
                key.startsWith("sys:") -> System.getProperty(key.substring("sys:".length))
                else -> null
            }
        }
    }

    constructor(file: File) : this(file.toURI())

    constructor(uri: URI) : this(uri.toURL())

    private var mAutosave = false

    private var mReadOnly = true

    private var mSubstitutor: Function1<String, String?>? = null

    private var mMaster: XConfigImpl? = null

    fun substitutor(substitutor: Function1<String, String?>): XConfigBuilder {
        mSubstitutor = substitutor
        return this
    }

    fun master(master: XConfig): XConfigBuilder {
        mMaster = master as XConfigImpl
        return this
    }

    fun master(masterURL: URL, substitutor: Function1<String, String?>? = null): XConfigBuilder {
        val mcb = XConfigBuilder(masterURL)
        if (substitutor != null) {
            mcb.substitutor(substitutor)
        }
        return master(mcb.create())
    }

    fun autosave(autosave: Boolean): XConfigBuilder {
        if (autosave) allowWrites()
        mAutosave = autosave
        return this
    }

    fun allowWrites(): XConfigBuilder {
        mReadOnly = false
        return this
    }

    fun create(): XConfig {
        if (mSubstitutor != null && !mReadOnly) {
            mSubstitutor = null
            XConfigException.throwConfigurationSubstitutorCannotbetSetForWritableConfig()
        }
        if (!mReadOnly) { // Test write is supported
            try {
                if (mUrl.protocol != "file") {
                    mUrl.openConnection().getOutputStream().use {}
                } else {
                    val file = File(mUrl.toURI())
                    if (!file.exists()) file.parentFile?.mkdirs()
                    if (!file.exists() || file.length() < 1) {
                        file.writeText("""
                            <?xml version="1.0" encoding="UTF-8"?>
                            <configuration>
                            </configuration>
                        """.trimIndent())
                    } else if (!file.canWrite()) {
                        throw IOException()
                    }
                }
            } catch (e: IOException) {
                if (log.isDebugEnabled) log.debug("", e)
                log.warn("Writes to '${mUrl}' unsupported marking config as readonly")
                mReadOnly = true
            }
        }
        return XConfigImpl().also {
            mSubstitutor = null
        }
    }

    internal inner class XConfigImpl
    internal constructor(
            override val parent: XConfigImpl? = null,
            contextNode: Element? = null) : XConfig {

        private var noSave = false

        private val master = mMaster

        private val xpf = XPathFactory.newInstance()

        private var file: File? = if (mUrl.protocol == "file") File(mUrl.toURI()) else null

        override val lock: ReentrantReadWriteLock = parent?.lock ?: ReentrantReadWriteLock()

        override val uri: URI = mUrl.toURI()

        override val document: Document = parent?.document ?: DocumentBuilderFactory.newInstance().let { f ->
            f.isNamespaceAware = true
            val substitutor = mSubstitutor
            if (substitutor != null) {
                mUrl.openStream().buffered().use {
                    val data = preprocessConfigData(it.readAllBytes().toString(Charsets.UTF_8), substitutor)
                    if (log.isDebugEnabled) {
                        log.debug("Preprocessed config:\n{}", data)
                    }
                    f.newDocumentBuilder().parse(InputSource(StringReader(data)))
                }
            } else {
                mUrl.openStream().buffered().use {
                    f.newDocumentBuilder().parse(InputSource(it))
                }
            }
        }

        override val slaves = CopyOnWriteArrayList<XConfig>()

        private val node = contextNode ?: document.documentElement

        init {
            master?.slaves?.add(this)
        }

        private fun preprocessConfigData(cdata: String, substitutor: Function1<String, String?>): String {
            val p = Pattern.compile("(.)?\\{(((env|sys):)?[A-Za-z_.]+)}")
            val m = p.matcher(cdata)
            val sb = StringBuffer(cdata.length)
            while (m.find()) {
                val mg = m.group()
                val pc = m.group(1)
                if ("$" == pc) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(mg))
                } else {
                    val s = StringUtils.replaceEach(substitutor(m.group(2)), arrayOf("\\"), arrayOf("\\\\"))
                    if (s != null) {
                        if (pc != null) {
                            m.appendReplacement(sb, Matcher.quoteReplacement(pc + s))
                        } else {
                            m.appendReplacement(sb, Matcher.quoteReplacement(s))
                        }
                    } else {
                        m.appendReplacement(sb, Matcher.quoteReplacement(mg))
                    }
                }
            }
            m.appendTail(sb)
            return sb.toString()
        }

        private fun nodesByPattern(expr: String, skipMaster: Boolean, first: Boolean): List<Node> {
            var path = expr
            val idx = expr.indexOf("[@")
            val attrName = if (expr.endsWith(']') && idx != -1) {
                path = expr.substring(0, idx)
                expr.substring(idx + 2, expr.length - 1)
            } else {
                null
            }
            if (path == "." || path.isBlank()) {
                return if (attrName != null) {
                    val an = node.getAttributeNode(attrName);
                    if (an != null) listOf(an) else emptyList()
                } else {
                    listOf(node)
                }
            }
            var n = node
            StringUtils.split(path, '.').forEach { s ->
                n = n.firstChildElement { it.nodeName == s }
                        ?: return if (parent == null && master != null && !skipMaster) {
                    master.nodesByPattern(path, false, first)
                } else emptyList()
            }
            return ArrayList<Node>().apply {
                if (attrName != null) {
                    n.getAttributeNode(attrName)?.also { add(it) }
                } else {
                    add(n)
                }
                if (!first) {
                    var ns = n.nextSibling
                    while (ns != null) {
                        if (ns is Element && ns.nodeName == n.nodeName) {
                            if (attrName != null) {
                                ns.getAttributeNode(attrName)?.also { add(it) }
                            } else {
                                add(ns)
                            }
                        }
                        ns = ns.nextSibling
                    }
                }
            }
        }

        private fun nodesByXPath(expr: String, skipMaster: Boolean): List<Node> {
            return (xpf.newXPath().evaluate(expr, node, XPathConstants.NODESET) as? NodeList)?.let {
                if (it.length < 1 && parent == null && master != null) {
                    master.nodesByXPath(expr, skipMaster)
                } else {
                    ArrayList<Node>(it.length).apply {
                        (0 until it.length).forEach { idx -> add(it.item(idx)) }
                    }
                }
            } ?: emptyList()
        }

        private fun nodesBy(expr: String, type: XCPath, skipMaster: Boolean, first: Boolean): List<Node> = when (type) {
            XCPath.PATTERN -> nodesByPattern(expr, skipMaster, first)
            XCPath.XPATH -> nodesByXPath(expr, skipMaster)
        }

        override operator fun get(expr: String, type: XCPath): String? = lock.read {
            nodesBy(expr, type, false, true).firstOrNull()?.text()
        }

        override operator fun <T> set(expr: String, v: T): Unit = lock.write {
            var path = expr
            val idx = expr.indexOf("[@")
            val attrName = if (expr.endsWith(']') && idx != -1) {
                path = expr.substring(0, idx)
                expr.substring(idx + 2, expr.length - 1)
            } else {
                null
            }
            var n = node
            if (path != "." && !path.isBlank()) {
                StringUtils.split(path, '.').forEach { s ->
                    n = n.firstChildElement { it.nodeName == s }
                            ?: n.appendChild(document.createElementNS(n.namespaceURI, s)) as Element
                }
            }
            if (attrName != null) {
                when (v) {
                    null -> n.removeAttribute(attrName)
                    is Attr -> n.setAttribute(attrName, v.value)
                    else -> n.setAttribute(attrName, v.toString())
                }
            } else {
                n.set(v)
            }
            if (mAutosave) {
                save()
            }
        }

        private fun setNodesAttrs(nodes: List<Node>, pairs: Array<out Pair<String, Any>>): Int {
            if (nodes.isEmpty()) return 0
            var c = 0
            nodes.forEach { n ->
                if (n is Element) {
                    pairs.forEach { p -> n.set(attr(p.first, p.second.toString())) }
                    c++
                }
            }
            if (mAutosave) {
                save()
            }
            return c
        }

        override fun setAttrs(expr: String, vararg pairs: Pair<String, Any>) = lock.write {
            setNodesAttrs(nodesByPattern(expr, true, false), pairs)
        }

        override fun setAttrsXPath(expr: String, vararg pairs: Pair<String, Any>) = lock.write {
            setNodesAttrs(nodesByXPath(expr, true), pairs)
        }

        override fun attr(name: String, value: String): Attr {
            return document.createAttribute(name).also {
                it.value = value
            }
        }

        override fun has(expr: String, type: XCPath): Boolean = lock.read {
            !nodesBy(expr, type, false, true).isEmpty()
        }

        override fun nodes(expr: String, type: XCPath): List<Node> = lock.read {
            nodesBy(expr, type, false, false)
        }

        override fun detach(expr: String, type: XCPath) = lock.write {
            val nodes = nodesBy(expr, type, true, false)
            if (nodes.isEmpty()) {
                return
            }
            nodes.forEach { it.detach() }
            if (mAutosave) {
                save()
            }
        }

        override fun text(expr: String, dval: String?, type: XCPath): String? = lock.read {
            get(expr, type) ?: dval
        }

        override fun bool(expr: String, dval: Boolean?, type: XCPath): Boolean = lock.read {
            BooleanUtils.toBoolean(text(expr, dval?.toString() ?: "false"))
        }

        override fun number(expr: String, dval: Long?, type: XCPath): Long? = lock.read {
            text(expr, null, type)?.toLongOrNull() ?: dval
        }

        override fun sub(expr: String, type: XCPath): List<XConfig> = lock.read {
            nodesBy(expr, type, false, false)
                    .filter { it is Element }
                    .map { XConfigImpl(this, it as Element) }
        }

        override fun sub(el: Element): XConfig {
            return XConfigImpl(this, el)
        }

        override fun list(expr: String, type: XCPath): List<String> {
            return nodes(expr, type).asSequence()
                    .map { it.text() }
                    .filterNotNull()
                    .toList()
        }

        override fun <T> setPattern(expr: String, v: T) = set(expr, v)

        override fun listXPath(expr: String): List<String> = list(expr, XCPath.XPATH)

        override fun listPattern(expr: String): List<String> = list(expr, XCPath.PATTERN)

        override fun subXPath(expr: String): List<XConfig> = sub(expr, XCPath.XPATH)

        override fun subPattern(expr: String): List<XConfig> = sub(expr, XCPath.PATTERN)

        override fun nodesXPath(expr: String): List<Node> = nodes(expr, XCPath.XPATH)

        override fun nodesPattern(expr: String): List<Node> = nodes(expr, XCPath.PATTERN)

        override fun detachXPath(expr: String) = detach(expr, XCPath.XPATH)

        override fun detachPattern(expr: String) = detach(expr, XCPath.PATTERN)

        override fun textXPath(expr: String, dval: String?): String? = text(expr, dval, XCPath.XPATH)

        override fun textPattern(expr: String, dval: String?): String? = text(expr, dval, XCPath.PATTERN)

        override fun text(expr: String): String? = text(expr, null, XCPath.PATTERN)

        override fun boolXPath(expr: String, dval: Boolean?): Boolean = bool(expr, dval, XCPath.XPATH)

        override fun boolPattern(expr: String, dval: Boolean?): Boolean = bool(expr, dval, XCPath.PATTERN)

        override fun bool(expr: String): Boolean = bool(expr, null, XCPath.PATTERN)

        override fun numberXPath(expr: String, dval: Long?): Long? = number(expr, dval, XCPath.XPATH)

        override fun numberPattern(expr: String, dval: Long?): Long? = number(expr, dval, XCPath.PATTERN)

        override fun number(expr: String): Long? = number(expr, null, XCPath.PATTERN)

        override fun hasPattern(expr: String): Boolean = has(expr, XCPath.PATTERN)

        private fun openOutputStream(): OutputStream {
            return (file?.outputStream() ?: mUrl.openConnection().getOutputStream()).buffered()
        }

        override fun <T> batch(action: (cfg: XConfig) -> T): T {
            val ret = lock.read {
                noSave = true
                try {
                    action(this)
                } finally {
                    noSave = false
                }
            }
            if (!mReadOnly && mAutosave) {
                save()
            }
            return ret
        }

        private fun createWriteTransformer(): Transformer {
            val t = TransformerFactory.newInstance().newTransformer()
            t.setOutputProperty(OutputKeys.INDENT, "yes")
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            return t
        }

        override fun writeTo(out: Writer) {
            lock.write {
                createWriteTransformer().transform(DOMSource(document), StreamResult(out))
            }
            out.flush()
        }

        override fun save() = lock.write {
            if (noSave) return
            if (mReadOnly) XConfigException.throwReadOnlyConfiguration()
            openOutputStream().use {
                createWriteTransformer().transform(DOMSource(document), StreamResult(it))
                it.flush()
            }
        }

        override fun toString(): String {
            return "XConfig(uri=$uri, node=$node)"
        }
    }
}