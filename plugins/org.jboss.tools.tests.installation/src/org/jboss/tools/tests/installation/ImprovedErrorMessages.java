package org.jboss.tools.tests.installation;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * 
 * A rule for better error messages.
 *
 * @author Andrej Podhradsky (apodhrad@redhat.com)
 *
 */
public class ImprovedErrorMessages implements TestRule {
	
	private SWTWorkbenchBot bot = new SWTWorkbenchBot();

	public Statement apply(final Statement base, Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				try {
					base.evaluate();
				} catch (Throwable t) {
					checkAddRepository(t);
					checkRepositoryError(t);
					throw t;
				}
			}
		};
	}
	
	public void checkAddRepository(Throwable t) throws InstallFailureException {
		if (bot.activeShell().getText().equals("Add Repository") && !bot.button("OK").isEnabled()) {
			throw new InstallFailureException("Cannot add site '" + bot.text(1).getText() + "'", t);
		}
	}
	
	public void checkRepositoryError(Throwable t) throws InstallFailureException {
		if (bot.activeShell().getText().equals("Error Contacting Site")) {
			throw new InstallFailureException(bot.text().getText(), t);
		}
	}

}
