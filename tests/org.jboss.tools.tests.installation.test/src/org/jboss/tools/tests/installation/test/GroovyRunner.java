package org.jboss.tools.tests.installation.test;

import java.io.File;
import java.security.Permission;

import org.apache.log4j.Logger;



/**
 * Groovy runner executes a groovy script a catch its exit code.
 * 
 * @author apodhrad
 *
 */
public class GroovyRunner extends Thread {

	protected Logger log = Logger.getLogger(this.getClass());
	
	private static final String EXIT_MESSAGE = "Caught groovy exit call.";

	private volatile int status = Integer.MIN_VALUE;

	private SecurityManager systemSecurityManager = System.getSecurityManager();
	private ClassLoader systemClassLoader = Thread.currentThread().getContextClassLoader();

	private String groovyScript;
	private String[] args;

	public GroovyRunner(String groovyScript, String[] args) {
		super("Groovy Runner");
		this.groovyScript = groovyScript;
		this.args = args;
	}

	public int getStatus() {
		return status;
	}

	@Override
	public void run() {
		String debugGroovyPort = System.getProperty("debugGroovyPort");
		if (debugGroovyPort != null) {
			System.setProperty("debugPort", debugGroovyPort);
		}
		
		StringBuffer msg = new StringBuffer("groovy " + groovyScript);
		for (int i = 0; i < args.length; i++) {
			msg.append(" " + args[i]);
		}
		log.info(msg.toString());
		try {
			System.setSecurityManager(new SecurityManagerImpl(this));

			ClassLoader parent = getClass().getClassLoader();
			@SuppressWarnings("resource")
			GroovyClassLoader loader = new GroovyClassLoader(parent);
			Class<?> groovyClass = loader.parseClass(new File(groovyScript));
			GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
			groovyObject.invokeMethod("main", args);
		} catch (Throwable t) {
			if (!EXIT_MESSAGE.equals(t.getMessage())) {clazz
				throw new RuntimeException(t);
			}
		} finally {
			System.setSecurityManager(systemSecurityManager);
			Thread.currentThread().setContextClassLoader(systemClassLoader);
		}
	}

	private static class SecurityManagerImpl extends SecurityManager {

		private GroovyRunner runner = null;

		private SecurityManagerImpl(GroovyRunner runner) {
			this.runner = runner;
		}

		@Override
		public void checkExit(int status) {
			super.checkExit(status);
			runner.status = status;
			// No groovy System.exit(int) call.
			throw new SecurityException(EXIT_MESSAGE);
		}

		@Override
		public void checkPermission(Permission perm) {
			// allow all
		}

		@Override
		public void checkPermission(Permission perm, Object context) {
			// allow all
		}

	}
}
