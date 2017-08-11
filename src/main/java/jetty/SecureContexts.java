package jetty;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class SecureContexts {
	public static class SecureSchemeHandler extends AbstractHandler {
		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			// <jetty.version>9.2.5.v20141112</jetty.version>
			HttpConfiguration httpConfig = HttpChannel.getCurrentHttpChannel().getHttpConfiguration();

			if (baseRequest.isSecure()) {
				return; // all done
			}

			if (httpConfig.getSecurePort() > 0) {
				String scheme = httpConfig.getSecureScheme();
				int port = httpConfig.getSecurePort();

				String url = URIUtil.newURI(scheme, baseRequest.getServerName(), port, baseRequest.getRequestURI(),
						baseRequest.getQueryString());
				response.setContentLength(0);
				response.sendRedirect(url);
			} else {
				response.sendError(HttpStatus.FORBIDDEN_403, "!Secure");
			}

			baseRequest.setHandled(true);
		}
	}

	public static class HelloHandler extends AbstractHandler {
		private final String msg;

		public HelloHandler(String msg) {
			this.msg = msg;
		}

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			response.setContentType("text/plain");
			response.getWriter().printf("%s%n", msg);
			baseRequest.setHandled(true);
		}
	}

	public static class RootHandler extends AbstractHandler {
		private final String[] childContexts;

		public RootHandler(String... children) {
			this.childContexts = children;
		}

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			response.setContentType("text/html");
			PrintWriter out = response.getWriter();
			out.println("<html>");
			out.println("<head><title>Contexts</title></head>");
			out.println("<body>");
			out.println("<h4>Child Contexts</h4>");
			out.println("<ul>");
			for (String child : childContexts) {
				out.printf("<li><a href=\"%s\">%s</a></li>%n", child, child);
			}
			out.println("</ul>");
			out.println("</body></html>");
			baseRequest.setHandled(true);
		}
	}

	public static void main(String[] args) throws Exception {
		Server server = new Server();
		// the normal unsecured http port
		int port = 8080;
		// the secured http+ssl (https) port
		int securePort = 443;

		// Setup SSL
		SslContextFactory sslContextFactory = new SslContextFactory();
		URL resource = SecureSchemeHandler.class.getResource("/keystore");
		if (resource == null) {
			System.out.println("找不到keystore");
			System.exit(0);
		}
		String str = resource.toExternalForm();
		sslContextFactory.setKeyStorePath(str);
		sslContextFactory.setKeyStorePassword("");
		sslContextFactory.setKeyManagerPassword("");

		// Setup HTTP Configuration
		HttpConfiguration httpConf = new HttpConfiguration();
		httpConf.setSecurePort(securePort);
		httpConf.setSecureScheme("https");

		ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConf));
		httpConnector.setName("unsecured"); // named connector
		httpConnector.setPort(port);

		// Setup HTTPS Configuration
		HttpConfiguration httpsConf = new HttpConfiguration(httpConf);
		httpsConf.addCustomizer(new SecureRequestCustomizer());

		ServerConnector httpsConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConf));
		httpsConnector.setName("secured"); // named connector
		httpsConnector.setPort(securePort);

		// Add connectors
		server.setConnectors(new Connector[] { httpConnector, httpsConnector });

		// Wire up contexts for secure handling to named connector
		String secureHosts[] = new String[] { "@secured" };

		ContextHandler test1Context = new ContextHandler();
		test1Context.setContextPath("/test1");
		test1Context.setHandler(new HelloHandler("Hello1"));
		test1Context.setVirtualHosts(secureHosts);

		ContextHandler test2Context = new ContextHandler();
		test2Context.setContextPath("/test2");
		test2Context.setHandler(new HelloHandler("Hello2"));
		test2Context.setVirtualHosts(secureHosts);

		ContextHandler rootContext = new ContextHandler();
		rootContext.setContextPath("/");
		rootContext.setHandler(new RootHandler("/test1", "/test2"));
		rootContext.setVirtualHosts(secureHosts);

		// Wire up context for unsecure handling to only
		// the named 'unsecured' connector
		ContextHandler redirectHandler = new ContextHandler();
		redirectHandler.setContextPath("/");
		redirectHandler.setHandler(new SecureSchemeHandler());
		redirectHandler.setVirtualHosts(new String[] { "@unsecured" });

		ResourceHandler handler = new ResourceHandler();
		handler.setDirectoriesListed(true);
		handler.setWelcomeFiles(new String[] { "index.html", "phpmyadmin/index.php" });
		handler.setResourceBase("WebContent");

		HandlerCollection collection = new HandlerCollection();
		collection.setHandlers(new Handler[] { handler, redirectHandler });

		server.start();
		server.join();
	}
}
