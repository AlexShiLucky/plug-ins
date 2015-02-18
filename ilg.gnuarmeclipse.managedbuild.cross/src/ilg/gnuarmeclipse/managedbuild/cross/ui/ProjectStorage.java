/*******************************************************************************
 * Copyright (c) 2013 Liviu Ionescu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Liviu Ionescu - initial version
 *******************************************************************************/

package ilg.gnuarmeclipse.managedbuild.cross.ui;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

public class ProjectStorage {

	private static String TOOLCHAIN_PATH = "toolchain.path";
	private static String IS_TOOLCHAIN_PATH_PER_PROJECT = "is.toolchain.path.per.project";

	// Was used from PathManagedOptionValueHandler

	public static boolean isToolchainPathPerProject(IConfiguration config) {

		IProject project = (IProject) config.getManagedProject().getOwner();

		String value;
		try {
			value = project.getPersistentProperty(new QualifiedName(config
					.getId(), IS_TOOLCHAIN_PATH_PER_PROJECT));
		} catch (CoreException e) {
			// e.printStackTrace();
			System.out.println("isToolchainPathPerProject " + e.getMessage());
			return false;
		}

		if (value == null)
			value = "";

		return "true".equalsIgnoreCase(value.trim());
	}

	public static boolean putToolchainPathPerProject(IConfiguration config,
			boolean value) {

		IProject project = (IProject) config.getManagedProject().getOwner();

		try {
			project.setPersistentProperty(new QualifiedName(config.getId(),
					IS_TOOLCHAIN_PATH_PER_PROJECT), String.valueOf(value));
		} catch (CoreException e) {
			// e.printStackTrace();
			System.out.println("putToolchainPathPerProject " + e.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * Get the toolchain path for a given configuration.
	 * 
	 * @param config
	 * @return a string, possibly empty.
	 */
	public static String getToolchainPath(IConfiguration config) {

		IProject project = (IProject) config.getManagedProject().getOwner();

		String value;
		try {
			value = project.getPersistentProperty(new QualifiedName(config
					.getId(), TOOLCHAIN_PATH));
		} catch (CoreException e) {
			// e.printStackTrace();
			System.out.println("getToolchainPath() = " + e.getMessage());
			return "";
		}

		if (value == null)
			value = "";

		return value.trim();
	}

	public static boolean putToolchainPath(IConfiguration config, String value) {

		IProject project = (IProject) config.getManagedProject().getOwner();

		try {
			project.setPersistentProperty(new QualifiedName(config.getId(),
					TOOLCHAIN_PATH), value.trim());
		} catch (CoreException e) {
			// e.printStackTrace();
			System.out.println("putToolchainPath " + e.getMessage());
			return false;
		}

		return true;
	}

}
