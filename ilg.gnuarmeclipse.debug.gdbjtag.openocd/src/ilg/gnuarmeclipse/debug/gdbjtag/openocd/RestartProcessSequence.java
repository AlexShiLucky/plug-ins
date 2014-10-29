package ilg.gnuarmeclipse.debug.gdbjtag.openocd;

import ilg.gnuarmeclipse.debug.gdbjtag.DebugUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.dsf.concurrent.CountingRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DsfExecutor;
import org.eclipse.cdt.dsf.concurrent.IDsfStatusConstants;
import org.eclipse.cdt.dsf.concurrent.ReflectionSequence;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IContainerDMContext;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.gdb.service.IGDBProcesses;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.IMICommandControl;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.mi.service.command.commands.CLICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class RestartProcessSequence extends ReflectionSequence {

	private IGDBControl fCommandControl;
	private CommandFactory fCommandFactory;
	private IGDBProcesses fProcService;
	// private IReverseRunControl fReverseService;
	// private IGDBBackend fBackend;

	private DsfServicesTracker fTracker;

	// This variable will be used to store the original container context,
	// but once the new process is started (restarted), it will contain the new
	// container context. This new container context has for parent the process
	// context, which holds the new pid.
	private IContainerDMContext fContainerDmc;

	// If the user requested a stop_on_main, this variable will hold the
	// breakpoint
	// private MIBreakpoint fUserBreakpoint;
	// Since the stop_on_main option allows the user to set the breakpoint on
	// any
	// symbol, we use this variable to know if the stop_on_main breakpoint was
	// really
	// on the main() method.
	// private boolean fUserBreakpointIsOnMain;

	// private boolean fReverseEnabled;
	private final Map<String, Object> fAttributes;

	// private final boolean fRestart;
	// private final DataRequestMonitor<IContainerDMContext>
	// fDataRequestMonitor;

	protected IContainerDMContext getContainerContext() {
		return fContainerDmc;
	}

	public RestartProcessSequence(DsfExecutor executor,
			IContainerDMContext containerDmc, Map<String, Object> attributes,
			boolean restart, DataRequestMonitor<IContainerDMContext> rm) {
		super(executor, rm);

		assert executor != null;
		assert containerDmc != null;
		if (attributes == null) {
			// If no attributes are specified, simply use an empty map.
			attributes = new HashMap<String, Object>();
		}

		fContainerDmc = containerDmc;
		fAttributes = attributes;
		// fRestart = restart;
		// fDataRequestMonitor = rm;
	}

	/** utility method; cuts down on clutter */
	private void queueCommands(List<String> commands, RequestMonitor rm) {
		if (!commands.isEmpty()) {
			fCommandControl.queueCommand(
					new CLICommand<MIInfo>(fCommandControl.getContext(),
							DebugUtils.composeCommandWithLf(commands)),
					new DataRequestMonitor<MIInfo>(getExecutor(), rm));
		} else {
			rm.done();
		}
	}

	@Override
	protected String[] getExecutionOrder(String group) {
		if (GROUP_TOP_LEVEL.equals(group)) {
			return new String[] { //
			"stepInitializeBaseSequence", //$NON-NLS-1$
					"stepRestartCommands", //$NON-NLS-1$
			};
		}
		return null;
	}

	/**
	 * Initialise the members of the StartOrRestartProcessSequence_7_0 class.
	 * This step is mandatory for the rest of the sequence to complete.
	 */
	@Execute
	public void stepInitializeBaseSequence(RequestMonitor rm) {
		fTracker = new DsfServicesTracker(GdbPlugin.getBundleContext(),
				fContainerDmc.getSessionId());
		fCommandControl = fTracker.getService(IGDBControl.class);
		fCommandFactory = fTracker.getService(IMICommandControl.class)
				.getCommandFactory();
		fProcService = fTracker.getService(IGDBProcesses.class);
		// fBackend = fTracker.getService(IGDBBackend.class);

		if (fCommandControl == null || fCommandFactory == null
				|| fProcService == null) {
			rm.setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					IDsfStatusConstants.INTERNAL_ERROR,
					"Cannot obtain service", null)); //$NON-NLS-1$
			rm.done();
			return;
		}

		/*
		 * fReverseService = fTracker.getService(IReverseRunControl.class); if
		 * (fReverseService != null) { // Although the option to use reverse
		 * debugging could be on, we only check // it if we actually have a
		 * reverse debugging service. There is no point // in trying to handle
		 * reverse debugging if it is not available. fReverseEnabled =
		 * CDebugUtils.getAttribute(fAttributes,
		 * IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_REVERSE,
		 * IGDBLaunchConfigurationConstants.DEBUGGER_REVERSE_DEFAULT); }
		 */
		rm.done();
	}

	@Execute
	public void stepRestartCommands(final RequestMonitor rm) {

		List<String> commandsList = new ArrayList<String>();

		commandsList.add("monitor halt");

		if (CDebugUtils.getAttribute(fAttributes,
				IGDBJtagConstants.ATTR_SET_STOP_AT,
				ConfigurationAttributes.DO_STOP_AT_DEFAULT)) {

			String stopAtName = CDebugUtils.getAttribute(fAttributes,
					IGDBJtagConstants.ATTR_STOP_AT,
					ConfigurationAttributes.STOP_AT_NAME_DEFAULT).trim();

			if (stopAtName.length() > 0) {
				commandsList.add("tbreak " + stopAtName);
			}
		}

		String commandStr = ConfigurationAttributes.DO_SECOND_RESET_COMMAND;
		String resetType = "";

		if (CDebugUtils.getAttribute(fAttributes,
				ConfigurationAttributes.DO_SECOND_RESET,
				ConfigurationAttributes.DO_SECOND_RESET_DEFAULT)) {
			resetType = CDebugUtils.getAttribute(fAttributes,
					ConfigurationAttributes.SECOND_RESET_TYPE,
					ConfigurationAttributes.SECOND_RESET_TYPE_DEFAULT);
		}
		commandsList.add(commandStr + resetType);

		String otherCmds = CDebugUtils.getAttribute(fAttributes,
				ConfigurationAttributes.OTHER_RUN_COMMANDS,
				ConfigurationAttributes.OTHER_RUN_COMMANDS_DEFAULT).trim();

		try {
			DebugUtils.addMultiLine(otherCmds, commandsList);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		commandsList.add("continue");

		CountingRequestMonitor crm = new CountingRequestMonitor(getExecutor(),
				rm);
		crm.setDoneCount(commandsList.size());
		queueCommands(commandsList, rm);
	}

}
