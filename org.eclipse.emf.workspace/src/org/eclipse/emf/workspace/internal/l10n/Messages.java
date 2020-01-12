/**
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.workspace.internal.l10n;

import org.eclipse.osgi.util.NLS;


/**
 * Localized messages for the EMF Workbench plug-in.
 * 
 * @author Christian W. Damus (cdamus)
 */
public class Messages
	extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.emf.workspace.internal.l10n.Messages"; //$NON-NLS-1$

	public static String executeInterrupted;
	public static String executeRolledBack;
	public static String undoInterrupted;
	public static String undoRolledBack;
	public static String redoInterrupted;
	public static String redoRolledBack;
	
	public static String undoRecoveryFailed;
	public static String cannotRedo;
	public static String redoRecoveryFailed;
	public static String cannotUndo;
	public static String rollbackFailed;
	
	public static String precommitFailed;
	
	public static String exceptionHandlerFailed;
	
	public static String setLabel;
	public static String addLabel;
	public static String removeLabel;
	public static String moveLabel;
	public static String replaceLabel;
	
	public static String resCtxLabel;
	public static String cmdStkCtxLabel;
	public static String cmdStkSaveCtxLabel;
	
	public static String synchJobName;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

}
