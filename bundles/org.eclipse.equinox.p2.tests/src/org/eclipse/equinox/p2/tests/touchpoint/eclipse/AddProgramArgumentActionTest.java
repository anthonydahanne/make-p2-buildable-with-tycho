/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.util.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.AddProgramArgumentAction;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class AddProgramArgumentActionTest extends AbstractProvisioningTest {

	public AddProgramArgumentActionTest(String name) {
		super(name);
	}

	public AddProgramArgumentActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Map parameters = new HashMap();
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		Properties profileProperties = new Properties();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, getTempFolder().toString());
		IProfile profile = createProfile("test", null, profileProperties);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, createIU("test"));
		touchpoint.initializePhase(null, profile, "test", parameters);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, operand, parameters);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		String programArg = "-test";
		assertFalse(Arrays.asList(manipulator.getLauncherData().getProgramArgs()).contains(programArg));
		parameters.put(ActionConstants.PARM_PROGRAM_ARG, programArg);
		parameters = Collections.unmodifiableMap(parameters);

		AddProgramArgumentAction action = new AddProgramArgumentAction();
		action.execute(parameters);
		assertTrue(Arrays.asList(manipulator.getLauncherData().getProgramArgs()).contains(programArg));
		action.undo(parameters);
		assertFalse(Arrays.asList(manipulator.getLauncherData().getProgramArgs()).contains(programArg));
	}

}