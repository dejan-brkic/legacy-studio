/*******************************************************************************
 * Crafter Studio Web-content authoring solution
 *     Copyright (C) 2007-2013 Crafter Software Corporation.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.craftercms.cstudio.share.forms.impl.orbeon;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * This filter allows forwarding requests from your web app to an separate
 * Orbeon Forms context.
 * 
 * This filter must remain very simple and have as little dependencies as
 * possible, because it must be easily deployable into any web application.
 * <p>
 * This class is partially a copy and paste due to the fact that orbeon's OOTB
 * filter does a forward which does not work for us, we need to use an include
 */
public class OrbeonXFormsFilter implements Filter {

	public static final String OPS_XFORMS_RENDERER_DEPLOYMENT = "oxf.xforms.renderer.deployment";
	public static final String OPS_XFORMS_RENDERER_REQUEST_CONTEXT_PATH = "oxf.xforms.renderer.request.context-path";
	public static final String OPS_XFORMS_RENDERER_DOCUMENT_ATTRIBUTE_NAME = "oxf.xforms.renderer.document";
	public static final String OPS_XFORMS_RENDERER_BASE_URI_ATTRIBUTE_NAME = "oxf.xforms.renderer.base-uri";
	public static final String OPS_XFORMS_RENDERER_CONTENT_TYPE_ATTRIBUTE_NAME = "oxf.xforms.renderer.content-type";
	public static final String OPS_XFORMS_RENDERER_HAS_SESSION_ATTRIBUTE_NAME = "oxf.xforms.renderer.has-session";

	public static final String OPS_SERVLET_CONTEXT_ATTRIBUTE_NAME = "oxf.servlet.context";
	public static final String OPS_RENDERER_PATH = "/xforms-renderer";

	private static final String OPS_XFORMS_RENDERER_CONTEXT_PARAMETER_NAME = "oxf.xforms.renderer.context";
	private static final String DEFAULT_ENCODING = "UTF-8"; // must be this
//	private static final String DEFAULT_ENCODING = "ISO-8859-1"; // must be this
																	// per
																	// Servlet
																	// spec

	private ServletContext servletContext;
	private String opsContextPath;

	public void init(FilterConfig filterConfig) throws ServletException {
		servletContext = filterConfig.getServletContext();
		opsContextPath = filterConfig
				.getInitParameter(OPS_XFORMS_RENDERER_CONTEXT_PARAMETER_NAME);

		// TODO: check opsContextPath format: starts with /, doesn't end with
		// one, etc.
	}

	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {

		final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
		final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

		final String requestPath = getRequestPathInfo(httpRequest);

		if (isOPSResourceRequest(requestPath)) {
			// Directly forward all requests meant for Orbeon Forms resources
			final String subRequestPath = requestPath.substring(opsContextPath
					.length());
			getOPSDispatcher(subRequestPath).forward(httpRequest, httpResponse);
		} else {
			// Forward the request to the Orbeon Forms renderer
			final MyHttpServletResponseWrapper responseWrapper = new MyHttpServletResponseWrapper(
					httpResponse);

			// Execute filter
			filterChain.doFilter(new MyHttpServletRequestWrapper(httpRequest),
					responseWrapper);

			// Set document if not present AND output was intercepted
			if (httpRequest
					.getAttribute(OPS_XFORMS_RENDERER_DOCUMENT_ATTRIBUTE_NAME) == null) {
				final String content = responseWrapper.getContent();
				
				if (content != null) {
					httpRequest.setAttribute(
							OPS_XFORMS_RENDERER_DOCUMENT_ATTRIBUTE_NAME,
							content);
				}
			}

			// Set whether deployment is integrated or separate
			httpRequest.setAttribute(OPS_XFORMS_RENDERER_DEPLOYMENT,
					(getOPSContext() == servletContext) ? "integrated"
							: "separate");

			// Set servlet context for renderer
			httpRequest.setAttribute(OPS_XFORMS_RENDERER_REQUEST_CONTEXT_PATH,
					httpRequest.getContextPath());

			// Override Orbeon Forms context so that rewriting works correctly
			if (opsContextPath != null)
				httpRequest.setAttribute(OPS_SERVLET_CONTEXT_ATTRIBUTE_NAME,
						httpRequest.getContextPath() + opsContextPath);

			// Tell whether there is a session
			httpRequest.setAttribute(
					OPS_XFORMS_RENDERER_HAS_SESSION_ATTRIBUTE_NAME, Boolean
							.toString(httpRequest.getSession(false) != null));

			// Provide media type if available
			if (responseWrapper.getMediaType() != null)
				httpRequest.setAttribute(
						OPS_XFORMS_RENDERER_CONTENT_TYPE_ATTRIBUTE_NAME,
						responseWrapper.getMediaType());

			// Set base URI
			httpRequest.setAttribute(
					OPS_XFORMS_RENDERER_BASE_URI_ATTRIBUTE_NAME, requestPath);

			// Forward to Orbeon Forms for rendering only of there is content to
			// be rendered, otherwise just return and
			// let the filterChain finish its life naturally, assuming that when
			// sendRedirect is used, no content is
			// available in the response object
			if (!isBlank(responseWrapper.getContent()))
				getOPSDispatcher(OPS_RENDERER_PATH).include(httpRequest,
						httpResponse);
		}
	}

	// NOTE: this method copied from Apache StringUtils 2.3 as we don't want a
	// dependency on the JAR
	// http://www.apache.org/licenses/LICENSE-2.0
	public static boolean isBlank(String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if ((Character.isWhitespace(str.charAt(i)) == false)) {
				return false;
			}
		}
		return true;
	}

	public void destroy() {
	}

	private ServletContext getOPSContext() throws ServletException {
		final ServletContext opsContext = (opsContextPath != null) ? servletContext
				.getContext(opsContextPath)
				: servletContext;
		if (opsContext == null)
			throw new ServletException(
					"Can't find Orbeon Forms context called '"
							+ opsContextPath
							+ "'. Check the '"
							+ OPS_XFORMS_RENDERER_CONTEXT_PARAMETER_NAME
							+ "' filter initialization parameter and the <Context crossContext=\"true\"/> attribute.");

		return opsContext;
	}

	private RequestDispatcher getOPSDispatcher(String path)
			throws ServletException {
		final RequestDispatcher dispatcher = getOPSContext()
				.getRequestDispatcher(path);
		if (dispatcher == null)
			throw new ServletException(
					"Can't find Orbeon Forms request dispatcher.");

		return dispatcher;
	}

	private boolean isOPSResourceRequest(String requestPath) {
		if (opsContextPath == null)
			return false;

		return requestPath != null
				&& requestPath.startsWith(opsContextPath + "/");
	}

	// NOTE: This is borrowed from NetUtils but we don't want the dependency
	private static String getRequestPathInfo(HttpServletRequest request) {

		// Get servlet path and path info
		String servletPath = request.getServletPath();
		if (servletPath == null)
			servletPath = "";
		String pathInfo = request.getPathInfo();
		if (pathInfo == null)
			pathInfo = "";

		// Concatenate servlet path and path info, avoiding a double slash
		String requestPath = servletPath.endsWith("/")
				&& pathInfo.startsWith("/") ? servletPath
				+ pathInfo.substring(1) : servletPath + pathInfo;

		// Add starting slash if missing
		if (!requestPath.startsWith("/"))
			requestPath = "/" + requestPath;

		return requestPath;
	}

	// NOTE: This is borrowed from NetUtils but we don't want the dependency
	public static String getContentTypeCharset(String contentType) {
		if (contentType == null)
			return null;
		int semicolumnIndex = contentType.indexOf(";");
		if (semicolumnIndex == -1)
			return null;
		int charsetIndex = contentType.indexOf("charset=", semicolumnIndex);
		if (charsetIndex == -1)
			return null;

		// FIXME: There may be other attributes after charset, right?
		String afterCharset = contentType.substring(charsetIndex + 8);
		afterCharset = afterCharset.replace('"', ' ');
		return afterCharset.trim();
	}

	// NOTE: This is borrowed from NetUtils but we don't want the dependency
	public static String getContentTypeMediaType(String contentType) {
		if (contentType == null
				|| contentType.equalsIgnoreCase("content/unknown"))
			return null;
		int semicolumnIndex = contentType.indexOf(";");
		if (semicolumnIndex == -1)
			return contentType;

		return contentType.substring(0, semicolumnIndex).trim();
	}

	private static class MyHttpServletRequestWrapper extends
			HttpServletRequestWrapper {

		private Enumeration headerNames;

		public MyHttpServletRequestWrapper(HttpServletRequest httpServletRequest) {
			super(httpServletRequest);
		}

		public String getHeader(String s) {
			// Filter conditional get headers so that we always get content
			if (s.toLowerCase().startsWith("if-"))
				return null;
			else
				return super.getHeader(s);
		}

		public Enumeration getHeaders(String s) {
			// Filter conditional get headers so that we always get content
			if (s.toLowerCase().startsWith("if-"))
				return null;
			else
				return super.getHeaders(s);
		}

		public Enumeration getHeaderNames() {
			if (headerNames == null) {
				// Filter conditional get headers so that we always get content
				final List newHeaderNames = new ArrayList();
				for (Enumeration e = super.getHeaderNames(); e
						.hasMoreElements();) {
					final String currentName = (String) e.nextElement();
					if (!currentName.toLowerCase().startsWith("if-"))
						newHeaderNames.add(currentName);
				}
				headerNames = Collections.enumeration(newHeaderNames);

			}
			return headerNames;
		}

		public long getDateHeader(String s) {
			// Filter conditional get headers so that we always get content
			if (s.toLowerCase().startsWith("if-"))
				return -1;
			else
				return super.getDateHeader(s);
		}
	}

	private static class MyHttpServletResponseWrapper extends
			HttpServletResponseWrapper {

		public MyHttpServletResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		private ByteArrayOutputStream byteArrayOutputStream;
		private ServletOutputStream servletOutputStream;

		private StringWriter stringWriter;
		private PrintWriter printWriter;

		private String encoding;
		private String mediatype;

		public void addCookie(Cookie cookie) {
			super.addCookie(cookie);
		}

		public boolean containsHeader(String string) {
			return super.containsHeader(string);
		}

		public String encodeURL(String string) {
			return super.encodeURL(string);
		}

		public String encodeRedirectURL(String string) {
			return super.encodeRedirectURL(string);
		}

		public String encodeUrl(String string) {
			return super.encodeUrl(string);
		}

		public String encodeRedirectUrl(String string) {
			return super.encodeRedirectUrl(string);
		}

		public void sendError(int i, String string) throws IOException {
			// TODO
		}

		public void sendError(int i) throws IOException {
			// TODO
		}

		public void sendRedirect(String string) throws IOException {
			super.sendRedirect(string);
		}

		public void setDateHeader(String string, long l) {
			// TODO
		}

		public void addDateHeader(String string, long l) {
			// TODO
		}

		public void setHeader(String string, String string1) {
			// TODO
		}

		public void addHeader(String string, String string1) {
			// TODO
		}

		public void setIntHeader(String string, int i) {
			// TODO
		}

		public void addIntHeader(String string, int i) {

			// TODO
		}

		public void setStatus(int i) {
			// TODO
		}

		public void setStatus(int i, String string) {
			// TODO
		}

		public ServletResponse getResponse() {
			return super.getResponse();
		}

		public void setResponse(ServletResponse servletResponse) {
			super.setResponse(servletResponse);
		}

		public String getCharacterEncoding() {
			// TODO: we don't support setLocale()
			return "UTF-8";//DEFAULT_ENCODING; // (encoding == null) ? DEFAULT_ENCODING :
										// encoding;
		}

		public String getMediaType() {
			return mediatype;
		}

		public ServletOutputStream getOutputStream() throws IOException {
			if (byteArrayOutputStream == null) {
				byteArrayOutputStream = new ByteArrayOutputStream();
				servletOutputStream = new ServletOutputStream() {
					public void write(int i) throws IOException {
						byteArrayOutputStream.write(i);
					}
				};
			}
			return servletOutputStream;
		}

		public PrintWriter getWriter() throws IOException {
			if (printWriter == null) {
				stringWriter = new StringWriter();
				printWriter = new PrintWriter(stringWriter);
			}
			return printWriter;
		}

		public void setContentLength(int i) {
			// NOP
		}

		public void setContentType(String contentType) {
			this.encoding = getContentTypeCharset(contentType);
			this.mediatype = getContentTypeMediaType(contentType);
		}

		public void setBufferSize(int i) {
			// NOP
		}

		public int getBufferSize() {
			// We have a buffer, but it is infinite
			return Integer.MAX_VALUE;
		}

		public void flushBuffer() throws IOException {
			// NOPE
		}

		public boolean isCommitted() {
			// We buffer everything so return false all the time
			return false;
		}

		public void reset() {
			resetBuffer();
		}

		public void resetBuffer() {
			if (byteArrayOutputStream != null) {
				try {
					servletOutputStream.flush();
				} catch (IOException e) {
					// ignore?
				}
				byteArrayOutputStream.reset();
			} else if (stringWriter != null) {
				printWriter.flush();
				final StringBuffer sb = stringWriter.getBuffer();
				sb.delete(0, sb.length());
			}
		}

		public void setLocale(Locale locale) {
			// TODO
		}

		public Locale getLocale() {
			return super.getLocale();
		}

		public String getContent() throws IOException {
			if (stringWriter != null) {
				// getWriter() was used
				printWriter.flush();
				return stringWriter.toString();
			} else if (servletOutputStream != null) {
				// getOutputStream() was used
				servletOutputStream.flush();
				return new String(byteArrayOutputStream.toByteArray(),
						getCharacterEncoding());
			} else {
				return null;
			}
		}
	}
}
