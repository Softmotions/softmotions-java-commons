package com.softmotions.web

import com.softmotions.kotlin.toFile
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import java.nio.file.Path
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
class DirResourcesFilter : Filter {

    private val log = LoggerFactory.getLogger(DirResourcesFilter::class.java)

    private lateinit var rootDir: Path

    private var stripPrefix: String = ""

    private var welcomeFile = "index.html"

    override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
        req as HttpServletRequest
        resp as HttpServletResponse
        if (!getContent(req, resp, "HEAD" != req.method) && !resp.isCommitted) {
            chain.doFilter(req, resp)
        }
    }

    private fun getContent(req: HttpServletRequest,
                           resp: HttpServletResponse,
                           transfer: Boolean): Boolean {

        var path = req.requestURI.removePrefix(stripPrefix)
        if (path.isEmpty()) {
            if (log.isDebugEnabled) {
                log.debug("Sending redirect to: ${req.requestURI + '/'}")
            }
            resp.sendRedirect(req.requestURI + '/')
            return false
        }
        if (path == "/") {
            path = "index.html"
        }
        path = path.removePrefix("/")
        val file = rootDir.resolve(path).toFile()
        if (log.isDebugEnabled) {
            log.debug("Serving '${file.absolutePath}' canRead=${file.canRead()} mtype=${req.servletContext.getMimeType(path)}")
        }
        if (!file.canRead()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND)
            return false
        }
        val mtype = req.servletContext.getMimeType(path)
        if (mtype != null) {
            resp.contentType = mtype
        }

        if (transfer) {
            file.inputStream().use {
                val output = ByteArrayOutputStream()
                IOUtils.copy(it, output)
                resp.setContentLength(output.size())
                if (transfer) {
                    output.writeTo(resp.outputStream)
                    resp.outputStream.flush()
                }
            }
        }
        resp.flushBuffer()
        return true
    }

    override fun init(conf: FilterConfig) {
        val rootDir = (conf.getInitParameter("rootDir")
                ?: throw ServletException("Missing required filter config parameter: 'rootDir'")).toFile()
        if (rootDir.path.contains("..")) {
            throw ServletException("Paths containing upper level relatives '/../' are prohibited. Path: ${rootDir.path}")
        }
        if (!rootDir.isDirectory) {
            throw ServletException("rootDir: ${rootDir} is not a directory")
        }
        this.rootDir = rootDir.toPath()
        stripPrefix = conf.getInitParameter("stripPrefix") ?: stripPrefix
        welcomeFile = conf.getInitParameter("welcomeFile") ?: welcomeFile
        if (log.isInfoEnabled) {
            log.info("Root: ${this.rootDir}")
            log.info("stripPrefix: '${stripPrefix}'")
            log.info("welcomeFile: '${welcomeFile}'")
        }
    }

    override fun destroy() {
    }
}