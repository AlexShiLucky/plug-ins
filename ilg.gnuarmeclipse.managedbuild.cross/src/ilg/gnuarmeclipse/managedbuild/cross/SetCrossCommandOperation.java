/*******************************************************************************
 * Copyright (c) 2009, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Doug Schaefer - initial API and implementation
 *     Marc-Andre Laperle - Moved to an operation for a custom wizard page
 *     Liviu Ionescu - ARM version
 *******************************************************************************/

package ilg.gnuarmeclipse.managedbuild.cross;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.cdt.build.core.scannerconfig.CfgInfoContext;
import org.eclipse.cdt.build.core.scannerconfig.ICfgScannerConfigBuilderInfo2Set;
import org.eclipse.cdt.build.core.scannerconfig.ScannerConfigBuilder;
import org.eclipse.cdt.build.internal.core.scannerconfig.CfgDiscoveredPathManager;
import org.eclipse.cdt.build.internal.core.scannerconfig2.CfgScannerConfigProfileManager;
import org.eclipse.cdt.core.templateengine.SharedDefaults;
import org.eclipse.cdt.make.core.scannerconfig.IScannerConfigBuilderInfo2;
import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollector;
import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollectorCleaner;
import org.eclipse.cdt.make.core.scannerconfig.InfoContext;
import org.eclipse.cdt.make.internal.core.scannerconfig.DiscoveredPathInfo;
import org.eclipse.cdt.make.internal.core.scannerconfig.DiscoveredScannerInfoStore;
import org.eclipse.cdt.make.internal.core.scannerconfig2.SCProfileInstance;
import org.eclipse.cdt.make.internal.core.scannerconfig2.ScannerConfigProfileManager;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSCustomPageManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * An operation that runs when the new project wizard finishes for the Cross GCC
 * toolchain. It reuses the information from {@link SetCrossCommandWizardPage}
 * to store options (index and path) in persistent storage.
 * 
 */
public class SetCrossCommandOperation implements IRunnableWithProgress {

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {

		System.out.println("SetCrossCommandOperation.run() begin");

		// get local properties
		String projectName = (String) MBSCustomPageManager.getPageProperty(
				SetCrossCommandWizardPage.PAGE_ID,
				SetCrossCommandWizardPage.CROSS_PROJECT_NAME);

		// String toolchainIndex = (String)
		// MBSCustomPageManager.getPageProperty(
		// SetCrossCommandWizardPage.PAGE_ID,
		// SetCrossCommandWizardPage.CROSS_TOOLCHAIN_INDEX);
		String toolchainName = (String) MBSCustomPageManager.getPageProperty(
				SetCrossCommandWizardPage.PAGE_ID,
				SetCrossCommandWizardPage.CROSS_TOOLCHAIN_NAME);
		String path = (String) MBSCustomPageManager.getPageProperty(
				SetCrossCommandWizardPage.PAGE_ID,
				SetCrossCommandWizardPage.CROSS_COMMAND_PATH);

		// store them on the permanent storage in
		// workspace/.plugins/org.eclipse.cdt.core/shareddefaults.xml

		String pathKey = SetCrossCommandWizardPage.SHARED_CROSS_COMMAND_PATH
				+ "." + toolchainName.hashCode();
		SharedDefaults.getInstance().getSharedDefaultsMap().put(pathKey, path);

		SharedDefaults
				.getInstance()
				.getSharedDefaultsMap()
				.put(SetCrossCommandWizardPage.SHARED_CROSS_TOOLCHAIN_NAME,
						toolchainName);
		SharedDefaults.getInstance().updateShareDefaultsMap(
				SharedDefaults.getInstance().getSharedDefaultsMap());

		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		if (!project.exists())
			return;

		IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
		if (buildInfo == null)
			return;

		IConfiguration[] configs = buildInfo.getManagedProject()
				.getConfigurations();
		for (IConfiguration config : configs) {

			try {
				updateOptions(config);
			} catch (BuildException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		ManagedBuildManager.saveBuildInfo(project, true);

		if (true) {
			for (IConfiguration config : configs) {
				ScannerConfigBuilder.build(config,
						ScannerConfigBuilder.PERFORM_CORE_UPDATE,
						new NullProgressMonitor());
			}
		}

		System.out.println("SetCrossCommandOperation.run() end");

	}

	private void updateOptions(IConfiguration config) throws BuildException {

		String sToolchainName = (String) MBSCustomPageManager.getPageProperty(
				SetCrossCommandWizardPage.PAGE_ID,
				SetCrossCommandWizardPage.CROSS_TOOLCHAIN_NAME);

		int toolchainIndex;
		try {
			toolchainIndex = ToolchainDefinition
					.findToolchainByName(sToolchainName);
		} catch (IndexOutOfBoundsException e) {
			toolchainIndex = ToolchainDefinition.getDefault();
		}

		IOption option;
		IToolChain toolchain = config.getToolChain();

		String sId;
		sId = Activator.getOptionPrefix() + ".toolchain.index";
		option = toolchain.getOptionBySuperClassId(sId); //$NON-NLS-1$
		String val = sId + "." + String.valueOf(toolchainIndex);
		// Do not use config.setOption() to DO NOT save it on .cproject...
		option.setValue(val);
		//config.setOption(toolchain, option, val); // temporarily

		Utils.updateOptions(config, toolchainIndex);

		String path = (String) MBSCustomPageManager.getPageProperty(
				SetCrossCommandWizardPage.PAGE_ID,
				SetCrossCommandWizardPage.CROSS_COMMAND_PATH);
		option = toolchain.getOptionBySuperClassId(Activator.getOptionPrefix()
				+ ".path"); //$NON-NLS-1$
		// Do not use config.setOption() to DO NOT save it on .cproject...
		option.setValue(path);

		// ... instead save it to the workspace project storage
		PathManagedOptionValueHandler.putPersistent(config, path);
	}

}
