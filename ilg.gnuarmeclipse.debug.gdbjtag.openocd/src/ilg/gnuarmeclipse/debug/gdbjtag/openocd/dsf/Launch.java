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

package ilg.gnuarmeclipse.debug.gdbjtag.openocd.dsf;

import ilg.gnuarmeclipse.core.EclipseUtils;
import ilg.gnuarmeclipse.core.StringUtils;
import ilg.gnuarmeclipse.debug.gdbjtag.DebugUtils;
import ilg.gnuarmeclipse.debug.gdbjtag.dsf.GnuArmLaunch;
import ilg.gnuarmeclipse.debug.gdbjtag.openocd.Activator;
import ilg.gnuarmeclipse.debug.gdbjtag.openocd.Configuration;
import ilg.gnuarmeclipse.debug.gdbjtag.openocd.ConfigurationAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.debug.gdbjtag.core.Messages;
import org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice;
import org.eclipse.cdt.dsf.concurrent.ConfinedToDsfExecutor;
import org.eclipse.cdt.dsf.concurrent.DefaultDsfExecutor;
import org.eclipse.cdt.dsf.concurrent.DsfRunnable;
import org.eclipse.cdt.dsf.concurrent.IDsfStatusConstants;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.gdb.IGdbDebugConstants;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.command.AbstractCLIProcess;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;

@SuppressWarnings("restriction")
public class Launch extends GnuArmLaunch {

	// ------------------------------------------------------------------------

	ILaunchConfiguration fConfig = null;
	private DsfSession fSession;
	private DsfServicesTracker fTracker;
	private DefaultDsfExecutor fExecutor;

	// ------------------------------------------------------------------------

	public Launch(ILaunchConfiguration launchConfiguration, String mode,
			ISourceLocator locator) {

		super(launchConfiguration, mode, locator);

		System.out.println("Launch(" + launchConfiguration.getName() + ","
				+ mode + ") " + this);

		fConfig = launchConfiguration;
		fExecutor = (DefaultDsfExecutor) getDsfExecutor();
		fSession = getSession();
	}

	// ------------------------------------------------------------------------

	@Override
	public void initialize() {

		System.out.println("Launch.initialize() " + this);

		super.initialize();

		Runnable initRunnable = new DsfRunnable() {
			@Override
			public void run() {
				fTracker = new DsfServicesTracker(GdbPlugin.getBundleContext(),
						fSession.getId());
				// fSession.addServiceEventListener(GdbLaunch.this, null);

				// fInitialized = true;
				// fireChanged();
			}
		};

		// Invoke the execution code and block waiting for the result.
		try {
			fExecutor.submit(initRunnable).get();
		} catch (InterruptedException e) {
			new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					IDsfStatusConstants.INTERNAL_ERROR,
					"Error initializing launch", e); //$NON-NLS-1$
		} catch (ExecutionException e) {
			new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					IDsfStatusConstants.INTERNAL_ERROR,
					"Error initializing launch", e); //$NON-NLS-1$
		}
	}

	@Override
	public void initializeControl() throws CoreException {

		System.out.println("Launch.initializeControl()");
		super.initializeControl();
	}

	@ConfinedToDsfExecutor("getSession().getExecutor()")
	public void shutdownSession(final RequestMonitor rm) {

		System.out.println("Launch.shutdownSession() " + this);
		super.shutdownSession(rm);
	}

	// ------------------------------------------------------------------------

	public void initializeServerConsole(IProgressMonitor monitor)
			throws CoreException {

		System.out.println("Launch.initializeServerConsole()");

		IProcess newProcess;
		boolean doAddServerConsole = getAddServerConsole(fConfig);

		if (doAddServerConsole) {

			// Add the GDB server process to the launch tree
			newProcess = addServerProcess(Configuration
					.getServerCommandName(fConfig));
			newProcess.setAttribute(IProcess.ATTR_CMDLINE,
					Configuration.getGdbServerCommandLine(fConfig));

			monitor.worked(1);
		}
	}

	public void initializeConsoles(IProgressMonitor monitor)
			throws CoreException {

		System.out.println("Launch.initializeConsoles()");

		IProcess newProcess;
		{
			// Add the GDB client process to the launch tree.
			newProcess = addClientProcess(Configuration
					.getClientCommandName(fConfig)); //$NON-NLS-1$

			newProcess.setAttribute(IProcess.ATTR_CMDLINE,
					Configuration.getGdbClientCommandLine(fConfig));

			monitor.worked(1);
		}
	}

	public IProcess addServerProcess(String label) throws CoreException {
		IProcess newProcess = null;
		try {
			// Add the server process object to the launch.
			Process serverProc = getDsfExecutor().submit(
					new Callable<Process>() {
						@Override
						public Process call() throws CoreException {
							GdbServerBackend backend = (GdbServerBackend) fTracker
									.getService(GdbServerBackend.class);
							if (backend != null) {
								return backend.getServerProcess();
							}
							return null;
						}
					}).get();

			// Need to go through DebugPlugin.newProcess so that we can use
			// the overrideable process factory to allow others to override.
			// First set attribute to specify we want to create the gdb process.
			// Bug 210366
			Map<String, String> attributes = new HashMap<String, String>();
			if (true) {
				attributes.put(IGdbDebugConstants.PROCESS_TYPE_CREATION_ATTR,
						IGdbDebugConstants.GDB_PROCESS_CREATION_VALUE);
			}
			if (serverProc != null) {
				newProcess = DebugPlugin.newProcess(this, serverProc, label,
						attributes);
			}
		} catch (InterruptedException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, 0,
					"Interrupted while waiting for get process callable.", e)); //$NON-NLS-1$
		} catch (ExecutionException e) {
			throw (CoreException) e.getCause();
		} catch (RejectedExecutionException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, 0,
					"Debugger shut down before launch was completed.", e)); //$NON-NLS-1$
		}

		return newProcess;
	}

	public IProcess ____addClientProcess(String label) throws CoreException {
		IProcess newProcess = null;
		try {
			// Add the CLI process object to the launch.
			AbstractCLIProcess cliProc = getDsfExecutor().submit(
					new Callable<AbstractCLIProcess>() {
						@Override
						public AbstractCLIProcess call() throws CoreException {
							IGDBControl gdb = fTracker
									.getService(IGDBControl.class);
							if (gdb != null) {
								return gdb.getCLIProcess();
							}
							return null;
						}
					}).get();

			// Need to go through DebugPlugin.newProcess so that we can use
			// the overrideable process factory to allow others to override.
			// First set attribute to specify we want to create the gdb process.
			// Bug 210366
			Map<String, String> attributes = new HashMap<String, String>();
			if (true) {
				attributes.put(IGdbDebugConstants.PROCESS_TYPE_CREATION_ATTR,
						IGdbDebugConstants.GDB_PROCESS_CREATION_VALUE);
			}
			newProcess = DebugPlugin.newProcess(this, cliProc, label,
					attributes);
		} catch (InterruptedException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, 0,
					"Interrupted while waiting for get process callable.", e)); //$NON-NLS-1$
		} catch (ExecutionException e) {
			throw (CoreException) e.getCause();
		} catch (RejectedExecutionException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, 0,
					"Debugger shut down before launch was completed.", e)); //$NON-NLS-1$
		}

		return newProcess;
	}

	// ------------------------------------------------------------------------

	public static boolean getStartGdbServer(ILaunchConfiguration config)
			throws CoreException {

		return config.getAttribute(ConfigurationAttributes.DO_START_GDB_SERVER,
				ConfigurationAttributes.DO_START_GDB_SERVER_DEFAULT);
	}

	public static boolean getAddServerConsole(ILaunchConfiguration config)
			throws CoreException {

		return getStartGdbServer(config)
				&& config
						.getAttribute(
								ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_CONSOLE,
								ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_CONSOLE_DEFAULT);
	}

	public static String getServerOtherConfig(ILaunchConfiguration config)
			throws CoreException {

		return config.getAttribute(ConfigurationAttributes.GDB_SERVER_OTHER,
				ConfigurationAttributes.GDB_SERVER_OTHER_DEFAULT).trim();
	}

	public static IStatus loadImage(Map<String, Object> attributes,
			IPath programPath, IGDBJtagDevice jtagDevice,
			List<String> commandsList) {

		String imageFileName = null;

		if (attributes
				.containsKey(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE)
				&& CDebugUtils.getAttribute(attributes,
						IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE,
						IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_IMAGE)) {
			if (programPath != null) {
				imageFileName = programPath.toOSString();
			}
		} else {
			imageFileName = CDebugUtils.getAttribute(attributes,
					IGDBJtagConstants.ATTR_IMAGE_FILE_NAME,
					IGDBJtagConstants.DEFAULT_IMAGE_FILE_NAME);
			if (!imageFileName.isEmpty()) {
				imageFileName = DebugUtils
						.resolveAll(imageFileName, attributes);
			} else {
				imageFileName = null;
			}
		}

		if (imageFileName == null) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1,
					Messages.getString("GDBJtagDebugger.err_no_img_file"), null); //$NON-NLS-1$
		}

		imageFileName = DebugUtils.resolveAll(imageFileName, attributes);

		if (EclipseUtils.isWindows()) {
			// Escape windows path separator characters TWICE, once
			// for Java and once for GDB.
			imageFileName = StringUtils.duplicateBackslashes(imageFileName); //$NON-NLS-1$ //$NON-NLS-2$
		}

		String imageOffset = CDebugUtils.getAttribute(attributes,
				IGDBJtagConstants.ATTR_IMAGE_OFFSET,
				IGDBJtagConstants.DEFAULT_IMAGE_OFFSET);
		if (imageOffset.length() > 0) {
			imageOffset = (imageFileName.endsWith(".elf")) ? "" : "0x"
					+ CDebugUtils.getAttribute(attributes,
							IGDBJtagConstants.ATTR_IMAGE_OFFSET,
							IGDBJtagConstants.DEFAULT_IMAGE_OFFSET); //$NON-NLS-2$ 
		}
		jtagDevice.doLoadImage(imageFileName, imageOffset, commandsList);

		return Status.OK_STATUS;
	}

	public static IStatus startRestart(Map<String, Object> attributes,
			boolean doReset, IPath programPath, IGDBJtagDevice jtagDevice,
			List<String> commandsList) {

		if (doReset) {
			if (CDebugUtils.getAttribute(attributes,
					ConfigurationAttributes.DO_SECOND_RESET,
					ConfigurationAttributes.DO_SECOND_RESET_DEFAULT)) {
				String commandStr = ConfigurationAttributes.DO_SECOND_RESET_COMMAND;
				String resetType = CDebugUtils.getAttribute(attributes,
						ConfigurationAttributes.SECOND_RESET_TYPE,
						ConfigurationAttributes.SECOND_RESET_TYPE_DEFAULT);
				commandsList.add(commandStr + resetType);

				// Although the manual claims that reset always does a
				// halt, better issue it explicitly
				commandStr = ConfigurationAttributes.HALT_COMMAND;
				commandsList.add(commandStr);
			}
		}

		if (CDebugUtils.getAttribute(attributes,
				IGDBJtagConstants.ATTR_LOAD_IMAGE,
				IGDBJtagConstants.DEFAULT_LOAD_IMAGE)
				&& CDebugUtils.getAttribute(attributes,
						ConfigurationAttributes.DO_DEBUG_IN_RAM,
						ConfigurationAttributes.DO_DEBUG_IN_RAM_DEFAULT)) {

			IStatus status = Launch.loadImage(attributes, programPath,
					jtagDevice, commandsList);

			if (!status.isOK()) {
				return status;
			}
		}

		String userCmd = CDebugUtils.getAttribute(attributes,
				ConfigurationAttributes.OTHER_RUN_COMMANDS,
				ConfigurationAttributes.OTHER_RUN_COMMANDS_DEFAULT).trim();

		userCmd = DebugUtils.resolveAll(userCmd, attributes);

		if (EclipseUtils.isWindows()) {
			userCmd = StringUtils.duplicateBackslashes(userCmd);
		}

		DebugUtils.addMultiLine(userCmd, commandsList);

		if (CDebugUtils.getAttribute(attributes,
				IGDBJtagConstants.ATTR_SET_PC_REGISTER,
				IGDBJtagConstants.DEFAULT_SET_PC_REGISTER)) {
			String pcRegister = CDebugUtils.getAttribute(
					attributes,
					IGDBJtagConstants.ATTR_PC_REGISTER,
					CDebugUtils.getAttribute(attributes,
							IGDBJtagConstants.ATTR_IMAGE_OFFSET,
							IGDBJtagConstants.DEFAULT_PC_REGISTER)).trim();
			if (!pcRegister.isEmpty()) {
				jtagDevice.doSetPC(pcRegister, commandsList);
			}
		}

		if (CDebugUtils.getAttribute(attributes,
				IGDBJtagConstants.ATTR_SET_STOP_AT,
				IGDBJtagConstants.DEFAULT_SET_STOP_AT)) {
			String stopAt = CDebugUtils.getAttribute(attributes,
					IGDBJtagConstants.ATTR_STOP_AT,
					IGDBJtagConstants.DEFAULT_STOP_AT).trim();

			if (!stopAt.isEmpty()) {
				// doAtopAt replaced by a simple tbreak
				commandsList.add("tbreak " + stopAt);
			}
		}

		commandsList.add("monitor reg");
		
		if (CDebugUtils.getAttribute(attributes,
				ConfigurationAttributes.DO_CONTINUE,
				ConfigurationAttributes.DO_CONTINUE_DEFAULT)) {
			commandsList.add(ConfigurationAttributes.DO_CONTINUE_COMMAND);
		}

		return Status.OK_STATUS;
	}
	// ------------------------------------------------------------------------
}
