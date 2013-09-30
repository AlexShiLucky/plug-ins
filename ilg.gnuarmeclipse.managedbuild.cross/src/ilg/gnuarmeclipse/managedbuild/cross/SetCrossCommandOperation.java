/*******************************************************************************
 * Copyright (c) 2009, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Doug Schaefer - initial API and implementation
 *     Marc-Andre Laperle - Moved to an operation for a custom wizard page
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
 * to set build options (prefix and path). It also clears and reruns scanner
 * discovery to account for the modified command.
 * 
 */
public class SetCrossCommandOperation implements IRunnableWithProgress {

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {

		// get local properties
		String projectName = (String) MBSCustomPageManager.getPageProperty(
				SetCrossCommandWizardPage.PAGE_ID,
				SetCrossCommandWizardPage.CROSS_PROJECT_NAME);

		String toolchainIndex = (String) MBSCustomPageManager.getPageProperty(
				SetCrossCommandWizardPage.PAGE_ID,
				SetCrossCommandWizardPage.CROSS_TOOLCHAIN_INDEX);
		String path = (String) MBSCustomPageManager.getPageProperty(
				SetCrossCommandWizardPage.PAGE_ID,
				SetCrossCommandWizardPage.CROSS_COMMAND_PATH);

		// store them on the permanent storage in 
		// workspace/.plugins/org.eclipse.cdt.core/shareddefaults.xml
		
		String pathKey = SetCrossCommandWizardPage.SHARED_CROSS_COMMAND_PATH
				+ "." + toolchainIndex;
		SharedDefaults.getInstance().getSharedDefaultsMap().put(pathKey, path);

		SharedDefaults
				.getInstance()
				.getSharedDefaultsMap()
				.put(SetCrossCommandWizardPage.SHARED_CROSS_TOOLCHAIN_INDEX,
						toolchainIndex);
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
			IToolChain toolchain = config.getToolChain();

			IOption option;
			String val;
			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".family"); //$NON-NLS-1$
			val = (String) option.getValue();
			ManagedBuildManager.setOption(config, toolchain, option, val);

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".command.prefix"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option,
					(String) option.getValue());

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".command.suffix"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option,
					(String) option.getValue());

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".command.c"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option,
					(String) option.getValue());

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".command.cpp"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option,
					(String) option.getValue());

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".command.ar"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option,
					(String) option.getValue());

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".command.objcopy"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option,
					(String) option.getValue());

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".command.objdump"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option,
					(String) option.getValue());

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".command.size"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option,
					(String) option.getValue());

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".command.make"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option,
					(String) option.getValue());

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".command.rm"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option,
					(String) option.getValue());

			option = toolchain.getOptionBySuperClassId(Activator
					.getOptionPrefix() + ".path"); //$NON-NLS-1$
			ManagedBuildManager.setOption(config, toolchain, option, path);

			ICfgScannerConfigBuilderInfo2Set cbi = CfgScannerConfigProfileManager
					.getCfgScannerConfigBuildInfo(config);
			Map<CfgInfoContext, IScannerConfigBuilderInfo2> map = cbi
					.getInfoMap();
			for (CfgInfoContext cfgInfoContext : map.keySet()) {
				IScannerConfigBuilderInfo2 bi = map.get(cfgInfoContext);
				String providerId = "specsFile"; //$NON-NLS-1$
				String runCommand = bi.getProviderRunCommand(providerId);
				// bi.setProviderRunCommand(providerId, prefix + runCommand);
				// TODO check
				bi.setProviderRunCommand(providerId, runCommand);
				try {
					bi.save();
				} catch (CoreException e) {
					Activator.log(e);
				}

				// Clear the path info that was captured at project creation
				// time
				// TODO we need an API to do this to avoid the discouraged
				// access warnings.

				DiscoveredPathInfo pathInfo = new DiscoveredPathInfo(project);
				InfoContext infoContext = cfgInfoContext.toInfoContext();

				// 1. Remove scanner info from
				// .metadata/.plugins/org.eclipse.cdt.make.core/Project.sc
				DiscoveredScannerInfoStore dsiStore = DiscoveredScannerInfoStore
						.getInstance();
				try {
					dsiStore.saveDiscoveredScannerInfoToState(project,
							infoContext, pathInfo);
				} catch (CoreException e) {
					e.printStackTrace();
				}

				// 2. Remove scanner info from CfgDiscoveredPathManager cache
				// and from the Tool
				CfgDiscoveredPathManager cdpManager = CfgDiscoveredPathManager
						.getInstance();
				cdpManager.removeDiscoveredInfo(project, cfgInfoContext);

				// 3. Remove scanner info from SI collector
				IScannerConfigBuilderInfo2 buildInfo2 = map.get(cfgInfoContext);
				if (buildInfo2 != null) {
					ScannerConfigProfileManager scpManager = ScannerConfigProfileManager
							.getInstance();
					String selectedProfileId = buildInfo2
							.getSelectedProfileId();
					SCProfileInstance profileInstance = scpManager
							.getSCProfileInstance(project, infoContext,
									selectedProfileId);

					IScannerInfoCollector collector = profileInstance
							.getScannerInfoCollector();
					if (collector instanceof IScannerInfoCollectorCleaner) {
						((IScannerInfoCollectorCleaner) collector)
								.deleteAll(project);
					}
					buildInfo2 = null;
				}
			}
		}

		ManagedBuildManager.saveBuildInfo(project, true);

		for (IConfiguration config : configs) {
			ScannerConfigBuilder.build(config,
					ScannerConfigBuilder.PERFORM_CORE_UPDATE,
					new NullProgressMonitor());
		}

	}
}
