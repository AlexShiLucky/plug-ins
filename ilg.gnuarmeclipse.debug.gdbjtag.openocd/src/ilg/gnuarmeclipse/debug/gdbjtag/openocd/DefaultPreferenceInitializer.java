/*******************************************************************************
 * Copyright (c) 2015 Liviu Ionescu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Liviu Ionescu - initial version
 *******************************************************************************/

package ilg.gnuarmeclipse.debug.gdbjtag.openocd;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.osgi.service.prefs.Preferences;

/**
 * Initialisations are executed in two different moments: as the first step
 * during bundle inits and after all defaults are loaded from all possible
 * sources
 * 
 */
public class DefaultPreferenceInitializer extends AbstractPreferenceInitializer {

	// ------------------------------------------------------------------------

	/**
	 * Early inits. Preferences set here might be overridden by plug-in
	 * preferences.ini, product .ini or command line option.
	 */
	@Override
	public void initializeDefaultPreferences() {

		if (Activator.getInstance().isDebugging()) {
			System.out
					.println("DefaultPreferenceInitializer.initializeDefaultPreferences()");
		}

		// When the 'ilg.gnuarmeclipse.managedbuild.cross' node is completely
		// added to /default, a NodeChangeEvent is raised.
		// This is the moment when all final default values are in, possibly
		// set by product or command line.

		Preferences prefs = Platform.getPreferencesService().getRootNode()
				.node(DefaultScope.SCOPE);
		if (prefs instanceof IEclipsePreferences) {
			((IEclipsePreferences) prefs)
					.addNodeChangeListener(new LateInitializer());
		}
	}

	/**
	 * INodeChangeListener for late initialisations.
	 */
	private class LateInitializer implements INodeChangeListener {

		@Override
		public void added(NodeChangeEvent event) {

			if (Activator.getInstance().isDebugging()) {
				System.out.println("LateInitializer.added() " + event + " "
						+ event.getChild().name());
			}

			if (Activator.PLUGIN_ID.equals(event.getChild().name())) {

				finalizeInitializationsDefaultPreferences();

				// We're done, de-register listener.
				((IEclipsePreferences) (event.getSource()))
						.removeNodeChangeListener(this);
			}
		}

		@Override
		public void removed(NodeChangeEvent event) {

			if (Activator.getInstance().isDebugging()) {
				System.out.println("LateInitializer.removed() " + event);
			}
		}

		/**
		 * The second step of defaults initialisation.
		 */
		public void finalizeInitializationsDefaultPreferences() {

			if (Activator.getInstance().isDebugging()) {
				System.out
						.println("LateInitializer.finalizeInitializationsDefaultPreferences()");
			}

			// OpenOCD executable name
			String name = DefaultPreferences.getExecutableName();
			if (name.isEmpty()) {
				// If not defined elsewhere, get platform specific name.
				name = DefaultPreferences.getExecutableNameOs();
				if (!name.isEmpty()) {
					DefaultPreferences.putExecutableName(name);
				}
			}

			// OpenOCD install folder
			// Check if the toolchain path is explictly defined in the
			// default preferences.
			String folder = DefaultPreferences.getInstallFolder();
			if (folder.isEmpty()) {

				// Check if the search path is defined in the default
				// preferences.
				String searchPath = DefaultPreferences.getInstallSearchPath();
				if (searchPath.isEmpty()) {

					// If not defined, get the OS Specific default
					// from preferences.ini.
					searchPath = DefaultPreferences.getInstallSearchPathOs();
					if (!searchPath.isEmpty()) {
						// Store the search path in the preferences
						DefaultPreferences.putInstallSearchPath(searchPath);
					}
				}

				if (!searchPath.isEmpty()) {
					// If the search path is known, discover toolchain.
					folder = DefaultPreferences
							.discoverInstallFolder(searchPath);
					if (folder != null && !folder.isEmpty()) {
						// If the install folder was finally discovered, store
						// it in the preferences.
						DefaultPreferences.putInstallFolder(folder);
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------------
}
