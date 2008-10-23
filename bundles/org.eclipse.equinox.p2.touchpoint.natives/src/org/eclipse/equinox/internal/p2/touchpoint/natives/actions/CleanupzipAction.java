/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;

public class CleanupzipAction extends ProvisioningAction {
	public IStatus execute(Map parameters) {
		return NativeTouchpoint.cleanupzip(parameters);
	}

	public IStatus undo(Map parameters) {
		return NativeTouchpoint.unzip(parameters);
	}
}