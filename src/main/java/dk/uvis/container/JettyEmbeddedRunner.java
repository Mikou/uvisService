package dk.uvis.container;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import dk.uvis.web.DemoServlet;
import dk.uvis.web.IntroServlet;

public class JettyEmbeddedRunner { 
	public void startServer() {
		try {

			final Server server = new Server(8088);
			
			ServletContextHandler context = new ServletContextHandler(
					ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			context.setResourceBase(System.getProperty("java.io.tmpdir"));
			server.setHandler(context);
	 
			context.addServlet(DemoServlet.class, "/service.svc/*");
			context.addServlet(IntroServlet.class, "/");
			
			String origin = "http://localhost:8082";
			
			FilterHolder cors = new FilterHolder(new CrossOriginFilter());//context.addFilter(CrossOriginFilter.class,"/*",EnumSet.of(DispatcherType.REQUEST));
			cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, origin);
			cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
			cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD,OPTIONS");
			cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");
			cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER, "true");

			context.addFilter(cors, "/*" ,EnumSet.of(DispatcherType.REQUEST));
			
			server.start();
			server.join();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
