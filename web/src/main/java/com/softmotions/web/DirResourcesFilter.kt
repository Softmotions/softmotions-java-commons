package com.softmotions.web

import com.softmotions.kotlin.toFile
import org.apache.commons.io.IOUtils
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

    private lateinit var rootDir: Path;

    override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
        req as HttpServletRequest
        resp as HttpServletResponse
        if (!getContent(req, resp, "HEAD" == req.method) && !resp.isCommitted) {
            chain.doFilter(req, resp)
        }
    }

    private fun getContent(req: HttpServletRequest,
                           resp: HttpServletResponse,
                           transfer: Boolean): Boolean {

        val path = req.pathInfo?.substring(1) ?: return false
        if (path.isEmpty()) {
            if (log.isDebugEnabled) {
                log.debug("Sending redirect to: ${req.requestURI + '/'}")
            }
            resp.sendRedirect(req.requestURI + '/')
            return false
        }

        val file = rootDir.resolve(path).toFile()
        if (log.isDebugEnabled) {
            log.debug("File readable=${file.canRead()} path=${file.absolutePath}")
        }
        if (!file.canRead()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND)
            return false
        }
        val mtype = req.servletContext.getMimeType(path)
        if (log.isDebugEnabled) {
            log.debug("MimeType=${mtype} for path=${path}")
        }
        if (mtype != null) {
            resp.contentType = mtype
        }
        resp.setContentLengthLong(file.length())
        if (transfer) {
            file.inputStream().use {
                IOUtils.copy(it, resp.outputStream)
            }
        }
        resp.flushBuffer()
        return true
    }

    override fun init(conf: FilterConfig) {
        val rootDir = (conf.getInitParameter("rootDir")
                ?: throw ServletException("Missing required filter config parameter: 'rootDir'")).toFile()
        if (!rootDir.isDirectory) {
            throw ServletException("rootDir: ${rootDir} is not a directory")
        }
        this.rootDir = rootDir.toPath()
    }

    override fun destroy() {
    }
}