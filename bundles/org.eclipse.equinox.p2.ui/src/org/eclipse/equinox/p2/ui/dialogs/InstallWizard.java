/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.LicenseManager;
import org.eclipse.equinox.p2.ui.ProvUIImages;

/**
 * @since 3.4
 */
public class InstallWizard extends UpdateOrInstallWizard {

	ProvisioningPlan plan;

	public InstallWizard(String profileId, IInstallableUnit[] ius, ProvisioningPlan initialProvisioningPlan, LicenseManager licenseManager) {
		super(profileId, ius, licenseManager);
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL));
		this.plan = initialProvisioningPlan;
	}

	protected UpdateOrInstallWizardPage createMainPage(String profileId, IInstallableUnit[] ius) {
		return new InstallWizardPage(ius, profileId, plan, this);
	}
}
