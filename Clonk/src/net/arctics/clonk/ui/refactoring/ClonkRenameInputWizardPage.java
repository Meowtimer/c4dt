/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.arctics.clonk.ui.refactoring;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

class ClonkRenameInputWizardPage extends UserInputWizardPage {

	protected ClonkRenameInputWizardPage(String name) {
		super(name);
		setTitle("Supply the new name");
		setDescription("The new name the field will have after refactoring");
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		Label labelObj = new Label(container, SWT.NULL);
		labelObj.setText("New Name:");
		Text result = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		result.setLayoutData(gd);
	}

}
