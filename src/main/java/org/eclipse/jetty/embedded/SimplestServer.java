package org.eclipse.jetty.embedded;

import org.eclipse.jetty.server.Server;

/**
 * The simplest possible Jetty server.
 */
public class SimplestServer {
	public static void main(String[] args) throws Exception {
		Server server = new Server(8080);
		server.start();
		server.dumpStdErr();
		server.join();
	}
}