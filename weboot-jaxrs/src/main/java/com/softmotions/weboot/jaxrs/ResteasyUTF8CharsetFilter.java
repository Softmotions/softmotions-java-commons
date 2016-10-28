package com.softmotions.weboot.jaxrs;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

/**
 * Set default charset for request content type to UTF-8
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@Provider
public class ResteasyUTF8CharsetFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty("resteasy.provider.multipart.inputpart.defaultContentType", "text/plain; charset=UTF-8");
        requestContext.setProperty("resteasy.provider.multipart.inputpart.defaultCharset", "UTF-8");
    }
}
