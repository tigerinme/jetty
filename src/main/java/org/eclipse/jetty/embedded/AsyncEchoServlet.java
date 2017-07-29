package org.eclipse.jetty.embedded;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AsyncEchoServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2259480964101718719L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doDelete(req, resp);
		resp.setContentType("text/html");
		resp.getWriter().println("<h1>hello world</h1>");
	}
}
