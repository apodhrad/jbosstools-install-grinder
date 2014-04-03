/*******************************************************************************
 * Copyright (c) 2012-2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.tests.installation;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swtbot.eclipse.finder.SWTBotEclipseTestCase;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotMultiPageEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.ListResult;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This is a bot scenario which performs install through JBoss Central.
 *
 * @author Mickael Istria
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class InstallFromCentralTest extends SWTBotEclipseTestCase {


	private static int installationTimeout = 60 * 60000;

	@BeforeClass
	public static void setUpBeforeClass() {
		String timeoutPropertyValue = System.getProperty(InstallTest.INSTALLATION_TIMEOUT_IN_MINUTES_PROPERTY);
		if (timeoutPropertyValue != null) {
			installationTimeout = Integer.parseInt(timeoutPropertyValue) * 60000;
		}
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		if (this.bot.activeView().getTitle().equals("Welcome")) {
			this.bot.viewByTitle("Welcome").close();
		}
	}

	@Test
	public void testInstall() throws Exception {
		SWTBotMenu helpMenu = this.bot.menu("Help");
		helpMenu.menu("JBoss Central").click();
		SWTBotMultiPageEditor centralEditor = (SWTBotMultiPageEditor) this.bot.multipageEditorByTitle("JBoss Central");
		centralEditor.show();
		centralEditor.activatePage("Software/Update");
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() throws Exception {
				return bot.checkBox("Show Installed").isEnabled();
			}
			
			@Override
			public String getFailureMessage() {
				return "Could not load catalog";
			}
		}, installationTimeout);
		
		String all = "Features Available";
		try{
			SWTBotLabel label  = centralEditor.bot().label(all);
			new SWTBotLabelExt(label).check();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		bot.button("Install").click();
		this.bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				return bot.activeShell().getText().equals("Install") || bot.activeShell().getText().equals("Problem Occured");
			}

			@Override
			public void init(SWTBot bot) {
			}

			@Override
			public String getFailureMessage() {
				return "Blocking while calculating deps";
			}
		}, installationTimeout); // 15 minutes timeout
		if (this.bot.activeShell().getText().equals("Problem Occured")) {
			String reason = this.bot.text().getText();
			Assert.fail("Could not install Central content from " + System.getProperty("org.jboss.tools.central.discovery") + "\n" + reason);
		}
		this.bot.button("Next >").click();
		InstallTest.continueInstall(bot);
	}
	
	private class SWTBotLabelExt extends SWTBotLabel {

		public SWTBotLabelExt(SWTBotLabel swtBotLabel) throws WidgetNotFoundException {
			super(swtBotLabel.widget);
		}
		
		public void check() {
			List<SWTBotCheckBox> checkBoxes = syncExec(new ListResult<SWTBotCheckBox>() {

				@Override
				public List<SWTBotCheckBox> run() {
					return getCheckBoxes();
				}
			});
			for(SWTBotCheckBox checkBox: checkBoxes) {
				checkBox.click();
			}
		}
		
		private List<SWTBotCheckBox> getCheckBoxes() {
			List<SWTBotCheckBox> list = new ArrayList<SWTBotCheckBox>();
			Stack<Composite> stack = new Stack<Composite>();
			// Initial push
			stack.push(widget.getParent());
			// Depth first search
			while (!stack.isEmpty()) {
				Composite composite = stack.pop();
				if (composite == null) {
					continue;
				}
				for (Control c : composite.getChildren()) {
					if (c instanceof Button && (c.getStyle() & SWT.CHECK) != 0 && ((Button) c).getText().equals(" ")) {
						list.add(new SWTBotCheckBox((Button) c));
					}
					if (c instanceof Composite) {
						stack.push((Composite) c);
					}
				}
			}
			return list;
		}
		
	}

}
