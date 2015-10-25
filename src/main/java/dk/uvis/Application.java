package dk.uvis;

import java.io.IOException;

import dk.uvis.container.JettyEmbeddedRunner;

public class Application {
	public static void main(String[] args) throws IOException {

		System.out.println("### STARTING WEB SERVER ###");
		new JettyEmbeddedRunner().startServer();

	}

}
