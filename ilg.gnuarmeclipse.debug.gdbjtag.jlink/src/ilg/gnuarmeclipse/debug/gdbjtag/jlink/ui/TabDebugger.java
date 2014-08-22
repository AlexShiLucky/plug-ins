/*******************************************************************************
 * Copyright (c) 2007, 2012 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Andy Jin - Hardware debugging UI improvements, bug 229946
 *     Anna Dushistova (MontaVista) - bug 241279 
 *              - Hardware Debugging: Host name or ip address not saving in 
 *                the debug configuration
 *     Andy Jin (QNX) - Added DSF debugging, bug 248593
 *     Bruce Griffith, Sage Electronic Engineering, LLC - bug 305943
 *              - API generalization to become transport-independent (e.g. to
 *                allow connections via serial ports and pipes).
 *     Liviu Ionescu - ARM version
 ******************************************************************************/

package ilg.gnuarmeclipse.debug.gdbjtag.jlink.ui;

import ilg.gnuarmeclipse.core.EclipseUtils;
import ilg.gnuarmeclipse.core.StringUtils;
import ilg.gnuarmeclipse.debug.core.DebugUtils;
import ilg.gnuarmeclipse.debug.gdbjtag.jlink.Activator;
import ilg.gnuarmeclipse.debug.gdbjtag.jlink.ConfigurationAttributes;
import ilg.gnuarmeclipse.debug.gdbjtag.jlink.WorkspacePreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.debug.gdbjtag.ui.GDBJtagImages;
import org.eclipse.cdt.debug.mi.core.IMILaunchConfigurationConstants;
import org.eclipse.cdt.debug.mi.core.MIPlugin;
import org.eclipse.cdt.debug.mi.core.command.factories.CommandFactoryDescriptor;
import org.eclipse.cdt.debug.mi.core.command.factories.CommandFactoryManager;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGdbDebugPreferenceConstants;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

/**
 * @since 7.0
 */
public class TabDebugger extends AbstractLaunchConfigurationTab {

	private static final String TAB_NAME = "Debugger";
	private static final String TAB_ID = Activator.PLUGIN_ID
			+ ".ui.debuggertab";

	private static final boolean DEBUG = false;

	private Button doStartGdbServer;
	private Text gdbClientExecutable;
	private Text gdbClientOtherOptions;
	private Text gdbClientOtherCommands;

	private Text targetIpAddress;
	private Text targetPortNumber;
	private Text gdbFlashDeviceName;
	private Button gdbEndiannessLittle;

	private Button gdbEndiannessBig;
	private Button gdbInterfaceJtag;
	private Button gdbInterfaceSwd;

	private Button gdbServerConnectionUsb;
	private Button gdbServerConnectionIp;
	private Text gdbServerConnectionAddress;

	private Button gdbServerSpeedAuto;
	private Button gdbServerSpeedAdaptive;
	private Button gdbServerSpeedFixed;
	private Text gdbServerSpeedFixedValue;

	private Button doConnectToRunning;

	private Text gdbServerGdbPort;
	private Text gdbServerSwoPort;
	private Text gdbServerTelnetPort;

	private Text gdbServerExecutable;
	private Button gdbServerBrowseButton;
	private Button gdbServerVariablesButton;

	private Button doGdbServerVerifyDownload;
	private Button doGdbServerInitRegs;
	private Button doGdbServerLocalOnly;
	private Button doGdbServerSilent;

	private Text gdbServerLog;
	private Button gdbServerLogBrowse;
	private Text gdbServerOtherOptions;

	private Button doGdbServerAllocateConsole;
	private Button doGdbServerAllocateSemihostingConsole;

	protected Button fUpdateThreadlistOnSuspend;

	private TabStartup tabStartup;

	private static int COLUMN_WIDTH = 70;

	protected TabDebugger(TabStartup tabStartup) {
		super();
		this.tabStartup = tabStartup;
	}

	@Override
	public String getName() {
		return TAB_NAME;
	}

	@Override
	public Image getImage() {
		return GDBJtagImages.getDebuggerTabImage();
	}

	@Override
	public void createControl(Composite parent) {

		if (DEBUG) {
			System.out.println("TabDebugger: createControl() ");
		}

		// gdbPrevUsbAddress = "";
		// gdbPrevIpAddress = "";

		if (EclipseUtils.isLinux()) {
			COLUMN_WIDTH = 85;
		}

		ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL
				| SWT.H_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		setControl(sc);

		Composite comp = new Composite(sc, SWT.NONE);
		sc.setContent(comp);
		GridLayout layout = new GridLayout();
		comp.setLayout(layout);

		createGdbServerGroup(comp);

		createGdbClientControls(comp);

		createRemoteControl(comp);

		fUpdateThreadlistOnSuspend = new Button(comp, SWT.CHECK);
		fUpdateThreadlistOnSuspend.setText(Messages
				.getString("DebuggerTab.update_thread_list_on_suspend_Text"));
		fUpdateThreadlistOnSuspend
				.setToolTipText(Messages
						.getString("DebuggerTab.update_thread_list_on_suspend_ToolTipText"));

		fUpdateThreadlistOnSuspend.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});
	}

	private void browseButtonSelected(String title, Text text) {
		FileDialog dialog = new FileDialog(getShell(), SWT.NONE);
		dialog.setText(title);
		String str = text.getText().trim();
		int lastSeparatorIndex = str.lastIndexOf(File.separator);
		if (lastSeparatorIndex != -1)
			dialog.setFilterPath(str.substring(0, lastSeparatorIndex));
		str = dialog.open();
		if (str != null)
			text.setText(str);
	}

	private void browseSaveButtonSelected(String title, Text text) {
		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(title);
		String str = text.getText().trim();
		int lastSeparatorIndex = str.lastIndexOf(File.separator);
		if (lastSeparatorIndex != -1)
			dialog.setFilterPath(str.substring(0, lastSeparatorIndex));
		str = dialog.open();
		if (str != null)
			text.setText(str);
	}

	private void variablesButtonSelected(Text text) {
		StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(
				getShell());
		if (dialog.open() == StringVariableSelectionDialog.OK) {
			text.insert(dialog.getVariableExpression());
		}
	}

	private void createGdbServerGroup(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		group.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		group.setText(Messages.getString("DebuggerTab.gdbServerGroup_Text"));

		Composite comp = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 5;
		layout.marginHeight = 0;
		comp.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		comp.setLayoutData(gd);

		Label label;
		Link link;
		{
			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			local.setLayout(layout);

			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns;
			local.setLayoutData(gd);

			{
				doStartGdbServer = new Button(local, SWT.CHECK);
				doStartGdbServer.setText(Messages
						.getString("DebuggerTab.doStartGdbServer_Text"));
				doStartGdbServer.setToolTipText(Messages
						.getString("DebuggerTab.doStartGdbServer_ToolTipText"));
				gd = new GridData(GridData.FILL_HORIZONTAL);
				doStartGdbServer.setLayoutData(gd);

				doConnectToRunning = new Button(local, SWT.CHECK);
				doConnectToRunning.setText(Messages
						.getString("DebuggerTab.noReset_Text"));
				doConnectToRunning.setToolTipText(Messages
						.getString("DebuggerTab.noReset_ToolTipText"));
				gd = new GridData(GridData.FILL_HORIZONTAL);
				doConnectToRunning.setLayoutData(gd);

			}
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages
					.getString("DebuggerTab.gdbServerExecutable_Label"));
			label.setToolTipText(Messages
					.getString("DebuggerTab.gdbServerExecutable_ToolTipText"));

			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			local.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			local.setLayoutData(gd);
			{
				gdbServerExecutable = new Text(local, SWT.SINGLE | SWT.BORDER);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gdbServerExecutable.setLayoutData(gd);

				gdbServerBrowseButton = new Button(local, SWT.NONE);
				gdbServerBrowseButton.setText(Messages
						.getString("DebuggerTab.gdbServerExecutableBrowse"));

				gdbServerVariablesButton = new Button(local, SWT.NONE);
				gdbServerVariablesButton.setText(Messages
						.getString("DebuggerTab.gdbServerExecutableVariable"));
			}
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages.getString("DebuggerTab.deviceName_Label")); //$NON-NLS-1$
			label.setToolTipText(Messages
					.getString("DebuggerTab.deviceName_ToolTipText"));

			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			local.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			local.setLayoutData(gd);
			{
				gdbFlashDeviceName = new Text(local, SWT.BORDER);
				gd = new GridData();
				gd.widthHint = 180;
				gdbFlashDeviceName.setLayoutData(gd);

				Composite empty = new Composite(local, SWT.NONE);
				layout = new GridLayout();
				layout.numColumns = 1;
				layout.marginHeight = 0;
				layout.marginWidth = 0;
				empty.setLayout(layout);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				empty.setLayoutData(gd);

				link = new Link(local, SWT.NONE);
				link.setText(Messages.getString("DebuggerTab.deviceName_Link"));
				gd = new GridData(SWT.RIGHT);
				link.setLayoutData(gd);
			}
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages.getString("DebuggerTab.endianness_Label")); //$NON-NLS-1$
			label.setToolTipText(Messages
					.getString("DebuggerTab.endianness_ToolTipText")); //$NON-NLS-1$

			Composite radio = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = 0;
			radio.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			radio.setLayoutData(gd);
			{
				gdbEndiannessLittle = new Button(radio, SWT.RADIO);
				gdbEndiannessLittle.setText(Messages
						.getString("DebuggerTab.endiannesslittle_Text"));
				gd = new GridData();
				gd.widthHint = COLUMN_WIDTH;
				gdbEndiannessLittle.setLayoutData(gd);

				gdbEndiannessBig = new Button(radio, SWT.RADIO);
				gdbEndiannessBig.setText(Messages
						.getString("DebuggerTab.endiannessBig_Text"));
				gd = new GridData();
				gd.widthHint = COLUMN_WIDTH;
				gdbEndiannessBig.setLayoutData(gd);

			}
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages
					.getString("DebuggerTab.gdbServerConnection_Label"));
			label.setToolTipText(Messages
					.getString("DebuggerTab.gdbServerConnection_ToolTipText"));

			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 4;
			layout.marginHeight = 0;
			// layout.marginWidth = 0;
			local.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			local.setLayoutData(gd);
			{
				gdbServerConnectionUsb = new Button(local, SWT.RADIO);
				gdbServerConnectionUsb.setText(Messages
						.getString("DebuggerTab.connectionUsb_Text"));
				gd = new GridData();
				gd.widthHint = COLUMN_WIDTH;
				gdbServerConnectionUsb.setLayoutData(gd);

				gdbServerConnectionIp = new Button(local, SWT.RADIO);
				gdbServerConnectionIp.setText(Messages
						.getString("DebuggerTab.connectionTcp_Text"));
				gd = new GridData();
				gd.widthHint = COLUMN_WIDTH;
				gdbServerConnectionIp.setLayoutData(gd);

				gdbServerConnectionAddress = new Text(local, SWT.BORDER);
				gd = new GridData();
				gd.widthHint = 145;
				gdbServerConnectionAddress.setLayoutData(gd);

				label = new Label(local, SWT.NONE);
				label.setText(Messages
						.getString("DebuggerTab.connectionAfter_Text")); //$NON-NLS-1$

			}
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages.getString("DebuggerTab.interface_Label")); //$NON-NLS-1$
			label.setToolTipText(Messages
					.getString("DebuggerTab.interface_ToolTipText"));

			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = 0;
			// layout.marginWidth = 0;
			local.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			local.setLayoutData(gd);
			{
				gdbInterfaceSwd = new Button(local, SWT.RADIO);
				gdbInterfaceSwd.setText(Messages
						.getString("DebuggerTab.interfaceSWD_Text"));
				gd = new GridData();
				gd.widthHint = COLUMN_WIDTH;
				gdbInterfaceSwd.setLayoutData(gd);

				gdbInterfaceJtag = new Button(local, SWT.RADIO);
				gdbInterfaceJtag.setText(Messages
						.getString("DebuggerTab.interfaceJtag_Text"));
				gd = new GridData();
				gd.widthHint = COLUMN_WIDTH;
				gdbInterfaceJtag.setLayoutData(gd);
			}
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages
					.getString("DebuggerTab.gdbServerSpeed_Label")); //$NON-NLS-1$
			label.setToolTipText(Messages
					.getString("DebuggerTab.gdbServerSpeed_ToolTipText"));

			Composite radio = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 5;
			layout.marginHeight = 0;
			radio.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			radio.setLayoutData(gd);
			{
				gdbServerSpeedAuto = new Button(radio, SWT.RADIO);
				gdbServerSpeedAuto.setText(Messages
						.getString("DebuggerTab.gdbServerSpeedAuto_Text"));
				gd = new GridData();
				gd.widthHint = COLUMN_WIDTH;
				gdbServerSpeedAuto.setLayoutData(gd);

				gdbServerSpeedAdaptive = new Button(radio, SWT.RADIO);
				gdbServerSpeedAdaptive.setText(Messages
						.getString("DebuggerTab.gdbServerSpeedAdaptive_Text"));
				gd.widthHint = COLUMN_WIDTH;
				gdbServerSpeedAdaptive.setLayoutData(gd);

				gdbServerSpeedFixed = new Button(radio, SWT.RADIO);
				gdbServerSpeedFixed.setText(Messages
						.getString("DebuggerTab.gdbServerSpeedFixed_Text"));
				gd.widthHint = COLUMN_WIDTH;
				gdbServerSpeedFixed.setLayoutData(gd);

				gdbServerSpeedFixedValue = new Text(radio, SWT.BORDER);
				gd = new GridData();
				gd.widthHint = 40;
				gdbServerSpeedFixedValue.setLayoutData(gd);

				label = new Label(radio, SWT.NONE);
				label.setText(Messages
						.getString("DebuggerTab.gdbServerSpeedFixedUnit_Text")); //$NON-NLS-1$
			}
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages
					.getString("DebuggerTab.gdbServerGdbPort_Label"));
			label.setToolTipText(Messages
					.getString("DebuggerTab.gdbServerGdbPort_ToolTipText"));

			gdbServerGdbPort = new Text(comp, SWT.SINGLE | SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 60;
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			gdbServerGdbPort.setLayoutData(gd);
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages
					.getString("DebuggerTab.gdbServerSwoPort_Label"));
			label.setToolTipText(Messages
					.getString("DebuggerTab.gdbServerSwoPort_ToolTipText"));

			gdbServerSwoPort = new Text(comp, SWT.SINGLE | SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 60;
			gdbServerSwoPort.setLayoutData(gd);

			Composite empty = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			empty.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			empty.setLayoutData(gd);

			doGdbServerVerifyDownload = new Button(comp, SWT.CHECK);
			doGdbServerVerifyDownload.setText(Messages
					.getString("DebuggerTab.gdbServerVerifyDownload_Label"));
			doGdbServerVerifyDownload
					.setToolTipText(Messages
							.getString("DebuggerTab.gdbServerVerifyDownload_ToolTipText"));
			gd = new GridData();
			gd.horizontalIndent = 60;
			doGdbServerVerifyDownload.setLayoutData(gd);

			doGdbServerInitRegs = new Button(comp, SWT.CHECK);
			doGdbServerInitRegs.setText(Messages
					.getString("DebuggerTab.gdbServerInitRegs_Label"));
			doGdbServerInitRegs.setToolTipText(Messages
					.getString("DebuggerTab.gdbServerInitRegs_ToolTipText"));
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages
					.getString("DebuggerTab.gdbServerTelnetPort_Label"));
			label.setToolTipText(Messages
					.getString("DebuggerTab.gdbServerTelnetPort_ToolTipText"));

			gdbServerTelnetPort = new Text(comp, SWT.SINGLE | SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 60;
			gdbServerTelnetPort.setLayoutData(gd);

			Composite empty = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			empty.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			empty.setLayoutData(gd);

			doGdbServerLocalOnly = new Button(comp, SWT.CHECK);
			doGdbServerLocalOnly.setText(Messages
					.getString("DebuggerTab.gdbServerLocalOnly_Label"));
			doGdbServerLocalOnly.setToolTipText(Messages
					.getString("DebuggerTab.gdbServerLocalOnly_ToolTipText"));
			gd = new GridData();
			gd.horizontalIndent = 60;
			doGdbServerLocalOnly.setLayoutData(gd);

			doGdbServerSilent = new Button(comp, SWT.CHECK);
			doGdbServerSilent.setText(Messages
					.getString("DebuggerTab.gdbServerSilent_Label"));
			doGdbServerSilent.setToolTipText(Messages
					.getString("DebuggerTab.gdbServerSilent_ToolTipText"));
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages.getString("DebuggerTab.gdbServerLog_Label")); //$NON-NLS-1$

			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			local.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			local.setLayoutData(gd);
			{
				gdbServerLog = new Text(local, SWT.BORDER);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gdbServerLog.setLayoutData(gd);

				gdbServerLogBrowse = new Button(local, SWT.NONE);
				gdbServerLogBrowse.setText(Messages
						.getString("DebuggerTab.gdbServerLogBrowse_Button"));
			}
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages
					.getString("DebuggerTab.gdbServerOther_Label")); //$NON-NLS-1$

			gdbServerOtherOptions = new Text(comp, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			gdbServerOtherOptions.setLayoutData(gd);
		}

		{
			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.makeColumnsEqualWidth = true;
			local.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns;
			local.setLayoutData(gd);

			doGdbServerAllocateConsole = new Button(local, SWT.CHECK);
			doGdbServerAllocateConsole.setText(Messages
					.getString("DebuggerTab.gdbServerAllocateConsole_Label"));
			doGdbServerAllocateConsole
					.setToolTipText(Messages
							.getString("DebuggerTab.gdbServerAllocateConsole_ToolTipText"));
			gd = new GridData(GridData.FILL_HORIZONTAL);
			doGdbServerAllocateConsole.setLayoutData(gd);

			doGdbServerAllocateSemihostingConsole = new Button(local, SWT.CHECK);
			doGdbServerAllocateSemihostingConsole
					.setText(Messages
							.getString("DebuggerTab.gdbServerAllocateSemihostingConsole_Label"));
			doGdbServerAllocateSemihostingConsole
					.setToolTipText(Messages
							.getString("DebuggerTab.gdbServerAllocateSemihostingConsole_ToolTipText"));
			gd = new GridData(GridData.FILL_HORIZONTAL);
			doGdbServerAllocateSemihostingConsole.setLayoutData(gd);

		}
		// ----- Actions ------------------------------------------------------

		VerifyListener numericVerifyListener = new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character) || Character
						.isISOControl(e.character));
			}
		};

		ModifyListener scheduleUpdateJobModifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		};

		SelectionAdapter scheduleUpdateJobSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduleUpdateJob();
			}
		};

		doStartGdbServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				propagateStartGdbServerChanged();

				if (doStartGdbServer.getSelection()) {
					targetIpAddress
							.setText(ConfigurationAttributes.REMOTE_IP_ADDRESS_LOCALHOST);
				}
				scheduleUpdateJob();
			}
		});

		doConnectToRunning.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// updateLaunchConfigurationDialog();

				propagateConnectToRunningChanged();
				tabStartup.doConnectToRunningChanged(doConnectToRunning
						.getSelection());

				scheduleUpdateJob();
			}
		});

		gdbServerExecutable.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob(); // provides much better performance for
										// Text listeners
			}
		});

		gdbServerBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseButtonSelected(
						Messages.getString("DebuggerTab.gdbServerExecutableBrowse_Title"),
						gdbServerExecutable);
			}
		});

		gdbServerVariablesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				variablesButtonSelected(gdbServerExecutable);
			}
		});

		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				gdbServerConnectionAddress.setText("");
				scheduleUpdateJob();
			}
		};

		gdbServerConnectionUsb.addSelectionListener(selectionAdapter);
		gdbServerConnectionIp.addSelectionListener(selectionAdapter);

		gdbServerConnectionAddress.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		gdbServerConnectionAddress.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {

				if (gdbServerConnectionUsb.getSelection()) {
					e.doit = Character.isDigit(e.character)
							|| Character.isISOControl(e.character);
				} else if (gdbServerConnectionIp.getSelection()) {
					e.doit = Character.isLetterOrDigit(e.character)
							|| e.character == '.' || e.character == '-'
							|| e.character == '_'
							|| Character.isISOControl(e.character);
				} else {
					e.doit = false;
				}
			}
		});

		gdbInterfaceSwd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				tabStartup.doInterfaceSwdChanged(true);
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		gdbInterfaceJtag.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				tabStartup.doInterfaceSwdChanged(false);
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		gdbServerSpeedAuto.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				gdbServerSpeedFixedValue.setEnabled(false);
				scheduleUpdateJob();
			}
		});

		gdbServerSpeedAdaptive.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				gdbServerSpeedFixedValue.setEnabled(false);
				scheduleUpdateJob();
			}
		});

		gdbServerSpeedFixed.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				gdbServerSpeedFixedValue.setEnabled(true);
				scheduleUpdateJob();
			}
		});

		gdbServerSpeedFixedValue.addVerifyListener(numericVerifyListener);

		gdbServerSpeedFixedValue
				.addModifyListener(scheduleUpdateJobModifyListener);

		gdbFlashDeviceName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				// this will open the hyperlink in the default web browser
				Program.launch(event.text);
			}
		});

		gdbEndiannessLittle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		gdbEndiannessBig.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		gdbServerGdbPort.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {

				// make the target port the same
				targetPortNumber.setText(gdbServerGdbPort.getText());
				scheduleUpdateJob();
			}
		});

		gdbServerGdbPort.addVerifyListener(numericVerifyListener);

		gdbServerSwoPort.addModifyListener(scheduleUpdateJobModifyListener);
		gdbServerSwoPort.addVerifyListener(numericVerifyListener);

		gdbServerTelnetPort.addModifyListener(scheduleUpdateJobModifyListener);
		gdbServerTelnetPort.addVerifyListener(numericVerifyListener);

		doGdbServerVerifyDownload
				.addSelectionListener(scheduleUpdateJobSelectionAdapter);
		doGdbServerInitRegs
				.addSelectionListener(scheduleUpdateJobSelectionAdapter);
		doGdbServerLocalOnly
				.addSelectionListener(scheduleUpdateJobSelectionAdapter);
		doGdbServerSilent
				.addSelectionListener(scheduleUpdateJobSelectionAdapter);

		gdbServerLog.addModifyListener(scheduleUpdateJobModifyListener);
		gdbServerLogBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseSaveButtonSelected(Messages
						.getString("DebuggerTab.gdbServerLogBrowse_Title"),
						gdbServerLog);
			}
		});

		gdbServerOtherOptions
				.addModifyListener(scheduleUpdateJobModifyListener);

		doGdbServerAllocateConsole
				.addSelectionListener(scheduleUpdateJobSelectionAdapter);
		doGdbServerAllocateSemihostingConsole
				.addSelectionListener(scheduleUpdateJobSelectionAdapter);
	}

	private void createGdbClientControls(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.getString("DebuggerTab.gdbSetupGroup_Text"));
		GridLayout layout = new GridLayout();
		group.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);

		Composite comp = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		comp.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		comp.setLayoutData(gd);

		Label label;
		Button browseButton;
		Button variableButton;
		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages.getString("DebuggerTab.gdbCommand_Label"));
			label.setToolTipText(Messages
					.getString("DebuggerTab.gdbCommand_ToolTipText"));

			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			local.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			local.setLayoutData(gd);
			{
				gdbClientExecutable = new Text(local, SWT.SINGLE | SWT.BORDER);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gdbClientExecutable.setLayoutData(gd);

				browseButton = new Button(local, SWT.NONE);
				browseButton.setText(Messages
						.getString("DebuggerTab.gdbCommandBrowse"));

				variableButton = new Button(local, SWT.NONE);
				variableButton.setText(Messages
						.getString("DebuggerTab.gdbCommandVariable"));
			}
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages
					.getString("DebuggerTab.gdbOtherOptions_Label"));
			label.setToolTipText(Messages
					.getString("DebuggerTab.gdbOtherOptions_ToolTipText"));
			gd = new GridData();
			label.setLayoutData(gd);

			gdbClientOtherOptions = new Text(comp, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gdbClientOtherOptions.setLayoutData(gd);
		}

		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages
					.getString("DebuggerTab.gdbOtherCommands_Label"));
			label.setToolTipText(Messages
					.getString("DebuggerTab.gdbOtherCommands_ToolTipText"));
			gd = new GridData();
			gd.verticalAlignment = SWT.TOP;
			label.setLayoutData(gd);

			gdbClientOtherCommands = new Text(comp, SWT.MULTI | SWT.WRAP
					| SWT.BORDER | SWT.V_SCROLL);
			gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint = 60;
			gdbClientOtherCommands.setLayoutData(gd);
		}

		// ----- Actions ------------------------------------------------------
		gdbClientExecutable.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {

				scheduleUpdateJob(); // provides much better performance for
										// Text listeners
			}
		});

		gdbClientOtherCommands.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		gdbClientOtherOptions.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseButtonSelected(Messages
						.getString("DebuggerTab.gdbCommandBrowse_Title"),
						gdbClientExecutable);
			}
		});

		variableButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				variablesButtonSelected(gdbClientExecutable);
			}
		});
	}

	private void createRemoteControl(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.getString("DebuggerTab.remoteGroup_Text"));
		GridLayout layout = new GridLayout();
		group.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);

		Composite comp = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		comp.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		comp.setLayoutData(gd);

		// Create entry fields for TCP/IP connections
		Label label;
		{
			label = new Label(comp, SWT.NONE);
			label.setText(Messages.getString("DebuggerTab.ipAddressLabel")); //$NON-NLS-1$

			targetIpAddress = new Text(comp, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 125;
			targetIpAddress.setLayoutData(gd);

			label = new Label(comp, SWT.NONE);
			label.setText(Messages.getString("DebuggerTab.portNumberLabel")); //$NON-NLS-1$

			targetPortNumber = new Text(comp, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 125;
			targetPortNumber.setLayoutData(gd);
		}

		// ---- Actions -------------------------------------------------------
		// Add watchers for user data entry
		targetIpAddress.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob(); // provides much better performance for
										// Text listeners
			}
		});

		targetIpAddress.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = Character.isLetterOrDigit(e.character)
						|| e.character == '.' || e.character == '-'
						|| e.character == '_'
						|| Character.isISOControl(e.character);
			}
		});

		targetPortNumber.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = Character.isDigit(e.character)
						|| Character.isISOControl(e.character);
			}
		});
		targetPortNumber.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob(); // provides much better performance for
										// Text listeners
			}
		});

	}

	private void propagateStartGdbServerChanged() {

		boolean enabled = doStartGdbServer.getSelection();

		// gdbServerSpeedAuto.setSelection(enabled);
		// gdbServerSpeedAdaptive.setSelection(enabled);
		// gdbServerSpeedFixed.setSelection(enabled);
		// gdbServerSpeedFixedValue.setEnabled(enabled);

		gdbServerExecutable.setEnabled(enabled);
		gdbServerBrowseButton.setEnabled(enabled);
		gdbServerVariablesButton.setEnabled(enabled);

		// doConnectToRunning.setEnabled(enabled);
		doConnectToRunning.setEnabled(false);

		gdbFlashDeviceName.setEnabled(enabled);

		gdbServerConnectionAddress.setEnabled(enabled);

		gdbServerGdbPort.setEnabled(enabled);
		gdbServerSwoPort.setEnabled(enabled);
		gdbServerTelnetPort.setEnabled(enabled);

		doGdbServerVerifyDownload.setEnabled(enabled);
		doGdbServerInitRegs.setEnabled(enabled);
		doGdbServerLocalOnly.setEnabled(enabled);
		doGdbServerSilent.setEnabled(enabled);

		gdbServerOtherOptions.setEnabled(enabled);

		gdbServerLog.setEnabled(enabled);
		gdbServerLogBrowse.setEnabled(enabled);

		doGdbServerAllocateConsole.setEnabled(enabled);

		doGdbServerAllocateSemihostingConsole.setEnabled(enabled);

		// Disable remote target params when the server is started
		targetIpAddress.setEnabled(!enabled);
		targetPortNumber.setEnabled(!enabled);
	}

	private void propagateConnectToRunningChanged() {

		if (doStartGdbServer.getSelection()) {

			boolean enabled = doConnectToRunning.getSelection();

			doGdbServerInitRegs.setEnabled(!enabled);
		}
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {

		if (DEBUG) {
			System.out.println("TabDebugger: initializeFrom() "
					+ configuration.getName());
		}

		try {
			Boolean booleanDefault;
			String stringDefault;

			// J-Link GDB Server Setup
			{
				// Start server locally
				booleanDefault = WorkspacePreferences
						.getGdbServerDoStart(ConfigurationAttributes.DO_START_GDB_SERVER_DEFAULT);
				doStartGdbServer.setSelection(configuration.getAttribute(
						ConfigurationAttributes.DO_START_GDB_SERVER,
						booleanDefault));

				doConnectToRunning.setSelection(configuration.getAttribute(
						ConfigurationAttributes.DO_CONNECT_TO_RUNNING,
						ConfigurationAttributes.DO_CONNECT_TO_RUNNING_DEFAULT));

				// Executable
				stringDefault = WorkspacePreferences
						.getGdbServerExecutable(ConfigurationAttributes.GDB_SERVER_EXECUTABLE_DEFAULT);
				gdbServerExecutable.setText(configuration.getAttribute(
						ConfigurationAttributes.GDB_SERVER_EXECUTABLE,
						stringDefault));

				// Device name
				stringDefault = WorkspacePreferences
						.getFlashDeviceName(ConfigurationAttributes.FLASH_DEVICE_NAME_DEFAULT);

				String defaultDeviceName = configuration.getAttribute(
						ConfigurationAttributes.FLASH_DEVICE_NAME_COMPAT,
						stringDefault);
				String deviceName = configuration.getAttribute(
						ConfigurationAttributes.GDB_SERVER_DEVICE_NAME,
						defaultDeviceName);
				gdbFlashDeviceName.setText(deviceName);

				// Endianness
				stringDefault = WorkspacePreferences
						.getGdbServerEndianness(ConfigurationAttributes.ENDIANNESS_DEFAULT);
				String defaultEndianness = configuration.getAttribute(
						ConfigurationAttributes.ENDIANNESS_COMPAT,
						stringDefault);
				String endianness = configuration.getAttribute(
						ConfigurationAttributes.GDB_SERVER_DEVICE_ENDIANNESS,
						defaultEndianness);
				if (ConfigurationAttributes.ENDIANNESS_LITTLE
						.equals(endianness))
					gdbEndiannessLittle.setSelection(true);
				else if (ConfigurationAttributes.ENDIANNESS_BIG
						.equals(endianness))
					gdbEndiannessBig.setSelection(true);
				else {
					String message = "unknown endianness " + endianness
							+ ", using little";
					Activator.log(message);
					gdbEndiannessLittle.setSelection(true);
				}

				// Connection
				stringDefault = WorkspacePreferences
						.getGdbServerConnection(ConfigurationAttributes.GDB_SERVER_CONNECTION_DEFAULT);

				String connection = configuration.getAttribute(
						ConfigurationAttributes.GDB_SERVER_CONNECTION,
						stringDefault);

				if (ConfigurationAttributes.GDB_SERVER_CONNECTION_USB
						.equals(connection)) {
					gdbServerConnectionUsb.setSelection(true);
					gdbServerConnectionIp.setSelection(false);
				} else if (ConfigurationAttributes.GDB_SERVER_CONNECTION_IP
						.equals(connection)) {
					gdbServerConnectionUsb.setSelection(false);
					gdbServerConnectionIp.setSelection(true);
				}

				// Connection address
				stringDefault = WorkspacePreferences
						.getGdbServerConnectionAddress(ConfigurationAttributes.GDB_SERVER_CONNECTION_ADDRESS_DEFAULT);

				String connectionAddress = configuration.getAttribute(
						ConfigurationAttributes.GDB_SERVER_CONNECTION_ADDRESS,
						stringDefault);
				gdbServerConnectionAddress.setText(connectionAddress);

				// Interface
				stringDefault = WorkspacePreferences
						.getGdbServerInterface(ConfigurationAttributes.INTERFACE_DEFAULT);

				String defaultPhysicalInterface = configuration
						.getAttribute(ConfigurationAttributes.INTERFACE_COMPAT,
								stringDefault);
				String physicalInterface = configuration.getAttribute(
						ConfigurationAttributes.GDB_SERVER_DEBUG_INTERFACE,
						defaultPhysicalInterface);

				if (ConfigurationAttributes.INTERFACE_SWD
						.equals(physicalInterface)) {
					gdbInterfaceSwd.setSelection(true);
					tabStartup.doInterfaceSwdChanged(true);
				} else if (ConfigurationAttributes.INTERFACE_JTAG
						.equals(physicalInterface)) {
					gdbInterfaceJtag.setSelection(true);
					tabStartup.doInterfaceSwdChanged(false);
				} else {
					String message = "unknown interface " + physicalInterface
							+ ", using swd";
					Activator.log(message);
					gdbInterfaceSwd.setSelection(true);
				}

				// Initial speed
				stringDefault = WorkspacePreferences
						.getGdbServerInitialSpeed(ConfigurationAttributes.GDB_SERVER_SPEED_DEFAULT);

				String defaultPhysicalInterfaceSpeed = configuration
						.getAttribute(
								ConfigurationAttributes.GDB_SERVER_SPEED_COMPAT,
								stringDefault);

				String physicalInterfaceSpeed = configuration.getAttribute(
						ConfigurationAttributes.GDB_SERVER_DEVICE_SPEED,
						defaultPhysicalInterfaceSpeed);

				if (ConfigurationAttributes.INTERFACE_SPEED_AUTO
						.equals(physicalInterfaceSpeed)) {
					gdbServerSpeedAuto.setSelection(true);
					gdbServerSpeedAdaptive.setSelection(false);
					gdbServerSpeedFixed.setSelection(false);

					gdbServerSpeedFixedValue.setEnabled(false);

				} else if (ConfigurationAttributes.INTERFACE_SPEED_ADAPTIVE
						.equals(physicalInterfaceSpeed)) {
					gdbServerSpeedAuto.setSelection(false);
					gdbServerSpeedAdaptive.setSelection(true);
					gdbServerSpeedFixed.setSelection(false);

					gdbServerSpeedFixedValue.setEnabled(false);
				} else {
					try {
						Integer.parseInt(physicalInterfaceSpeed);
						gdbServerSpeedAuto.setSelection(false);
						gdbServerSpeedAdaptive.setSelection(false);
						gdbServerSpeedFixed.setSelection(true);

						gdbServerSpeedFixedValue.setEnabled(true);
						gdbServerSpeedFixedValue
								.setText(physicalInterfaceSpeed);
					} catch (NumberFormatException e) {
						String message = "unknown interface speed "
								+ physicalInterfaceSpeed + ", using auto";
						Activator.log(message);
						gdbServerSpeedAuto.setSelection(true);
						gdbServerSpeedFixedValue.setEnabled(false);
					}
				}

				// Ports
				gdbServerGdbPort
						.setText(Integer.toString(configuration
								.getAttribute(
										ConfigurationAttributes.GDB_SERVER_GDB_PORT_NUMBER,
										ConfigurationAttributes.GDB_SERVER_GDB_PORT_NUMBER_DEFAULT)));

				gdbServerSwoPort
						.setText(Integer.toString(configuration
								.getAttribute(
										ConfigurationAttributes.GDB_SERVER_SWO_PORT_NUMBER,
										ConfigurationAttributes.GDB_SERVER_SWO_PORT_NUMBER_DEFAULT)));

				gdbServerTelnetPort
						.setText(Integer.toString(configuration
								.getAttribute(
										ConfigurationAttributes.GDB_SERVER_TELNET_PORT_NUMBER,
										ConfigurationAttributes.GDB_SERVER_TELNET_PORT_NUMBER_DEFAULT)));

				// Flags
				doGdbServerVerifyDownload
						.setSelection(configuration
								.getAttribute(
										ConfigurationAttributes.DO_GDB_SERVER_VERIFY_DOWNLOAD,
										ConfigurationAttributes.DO_GDB_SERVER_VERIFY_DOWNLOAD_DEFAULT));

				doGdbServerInitRegs
						.setSelection(configuration
								.getAttribute(
										ConfigurationAttributes.DO_GDB_SERVER_INIT_REGS,
										ConfigurationAttributes.DO_GDB_SERVER_INIT_REGS_DEFAULT));

				doGdbServerLocalOnly
						.setSelection(configuration
								.getAttribute(
										ConfigurationAttributes.DO_GDB_SERVER_LOCAL_ONLY,
										ConfigurationAttributes.DO_GDB_SERVER_LOCAL_ONLY_DEFAULT));

				doGdbServerSilent.setSelection(configuration.getAttribute(
						ConfigurationAttributes.DO_GDB_SERVER_SILENT,
						ConfigurationAttributes.DO_GDB_SERVER_SILENT_DEFAULT));

				// Log file
				gdbServerLog.setText(configuration.getAttribute(
						ConfigurationAttributes.GDB_SERVER_LOG,
						ConfigurationAttributes.GDB_SERVER_LOG_DEFAULT));

				// Other options
				stringDefault = WorkspacePreferences
						.getGdbServerOtherOptions(ConfigurationAttributes.GDB_SERVER_OTHER_DEFAULT);
				gdbServerOtherOptions.setText(configuration
						.getAttribute(ConfigurationAttributes.GDB_SERVER_OTHER,
								stringDefault));

				// Allocate server console
				doGdbServerAllocateConsole
						.setSelection(configuration
								.getAttribute(
										ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_CONSOLE,
										ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_CONSOLE_DEFAULT));

				// Allocate semihosting console
				doGdbServerAllocateSemihostingConsole
						.setSelection(configuration
								.getAttribute(
										ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_SEMIHOSTING_CONSOLE,
										ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_SEMIHOSTING_CONSOLE_DEFAULT));
			}

			// GDB Client Setup
			{
				// Executable
				stringDefault = WorkspacePreferences
						.getGdbClientExecutable(ConfigurationAttributes.GDB_CLIENT_EXECUTABLE_DEFAULT);
				String gdbCommandAttr = configuration.getAttribute(
						IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME,
						stringDefault);
				gdbClientExecutable.setText(gdbCommandAttr);

				// Other options
				stringDefault = WorkspacePreferences
						.getGdbClientOtherOptions(ConfigurationAttributes.GDB_CLIENT_OTHER_OPTIONS_DEFAULT);
				gdbClientOtherOptions.setText(configuration.getAttribute(
						ConfigurationAttributes.GDB_CLIENT_OTHER_OPTIONS,
						stringDefault));

				stringDefault = WorkspacePreferences
						.getGdbClientCommands(ConfigurationAttributes.GDB_CLIENT_OTHER_COMMANDS_DEFAULT);
				gdbClientOtherCommands.setText(configuration.getAttribute(
						ConfigurationAttributes.GDB_CLIENT_OTHER_COMMANDS,
						stringDefault));
			}

			// Remote Target
			{
				targetIpAddress.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_IP_ADDRESS,
						ConfigurationAttributes.REMOTE_IP_ADDRESS_DEFAULT)); //$NON-NLS-1$

				int storedPort = 0;
				storedPort = configuration.getAttribute(
						IGDBJtagConstants.ATTR_PORT_NUMBER, 0); // Default 0

				// 0 means undefined, use default
				if ((storedPort <= 0) || (65535 < storedPort)) {
					storedPort = ConfigurationAttributes.REMOTE_PORT_NUMBER_DEFAULT;
				}

				String portString = Integer.toString(storedPort); //$NON-NLS-1$
				targetPortNumber.setText(portString);

				// useRemoteChanged();
			}

			propagateStartGdbServerChanged();
			propagateConnectToRunningChanged();

			// Force thread update
			boolean updateThreadsOnSuspend = configuration
					.getAttribute(
							IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_UPDATE_THREADLIST_ON_SUSPEND,
							ConfigurationAttributes.UPDATE_THREAD_LIST_DEFAULT);
			fUpdateThreadlistOnSuspend.setSelection(updateThreadsOnSuspend);

		} catch (CoreException e) {
			Activator.log(e.getStatus());
		}

		if (DEBUG) {
			System.out.println("TabDebugger: initializeFrom() completed "
					+ configuration.getName());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 */
	@Override
	public String getId() {
		return TAB_ID;
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		if (DEBUG) {
			System.out.println("TabDebugger: activated() "
					+ workingCopy.getName());
		}
	}

	@Override
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
		if (DEBUG) {
			System.out.println("TabDebugger: deactivated() "
					+ workingCopy.getName());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {

		if (DEBUG) {
			System.out.println("TabDebugger: performApply() "
					+ configuration.getName() + ", dirty=" + isDirty());
		}

		{
			// legacy definition; although the jtag device class is not used,
			// it must be there, to avoid NPEs
			configuration.setAttribute(IGDBJtagConstants.ATTR_JTAG_DEVICE,
					ConfigurationAttributes.JTAG_DEVICE);
		}

		boolean booleanValue;
		String stringValue;

		// J-Link GDB server
		{
			// Start server
			booleanValue = doStartGdbServer.getSelection();
			configuration.setAttribute(
					ConfigurationAttributes.DO_START_GDB_SERVER, booleanValue);
			WorkspacePreferences.putGdbServerDoStart(booleanValue);

			// Connect to running
			configuration.setAttribute(
					ConfigurationAttributes.DO_CONNECT_TO_RUNNING,
					doConnectToRunning.getSelection());

			// Executable
			stringValue = gdbServerExecutable.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_EXECUTABLE, stringValue);
			WorkspacePreferences.putGdbServerExecutable(stringValue);

			// Device name
			stringValue = gdbFlashDeviceName.getText().trim();
			configuration
					.setAttribute(
							ConfigurationAttributes.GDB_SERVER_DEVICE_NAME,
							stringValue);
			WorkspacePreferences.putFlashDeviceName(stringValue);

			// Endianness
			stringValue = ConfigurationAttributes.ENDIANNESS_DEFAULT;
			if (gdbEndiannessLittle.getSelection()) {
				stringValue = ConfigurationAttributes.ENDIANNESS_LITTLE;
			} else if (gdbEndiannessBig.getSelection()) {
				stringValue = ConfigurationAttributes.ENDIANNESS_BIG;
			} else {
				String message = "endianness not selected, setting little";
				Activator.log(message);
			}
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_DEVICE_ENDIANNESS,
					stringValue);
			WorkspacePreferences.putGdbServerEndianness(stringValue);

			// Connection
			stringValue = ConfigurationAttributes.GDB_SERVER_CONNECTION_DEFAULT;
			if (gdbServerConnectionUsb.getSelection()) {
				stringValue = ConfigurationAttributes.GDB_SERVER_CONNECTION_USB;

			} else if (gdbServerConnectionIp.getSelection()) {
				stringValue = ConfigurationAttributes.GDB_SERVER_CONNECTION_IP;
			}
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_CONNECTION, stringValue);
			WorkspacePreferences.putGdbServerConnection(stringValue);

			// Connection address
			stringValue = gdbServerConnectionAddress.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_CONNECTION_ADDRESS,
					stringValue);
			WorkspacePreferences.putGdbServerConnectionAddress(stringValue);

			// Interface
			stringValue = ConfigurationAttributes.INTERFACE_DEFAULT;
			if (gdbInterfaceSwd.getSelection()) {
				stringValue = ConfigurationAttributes.INTERFACE_SWD;
			} else if (gdbInterfaceJtag.getSelection()) {
				stringValue = ConfigurationAttributes.INTERFACE_JTAG;
			} else {
				String message = "interface not selected, setting swd";
				Activator.log(message);
				gdbInterfaceSwd.setSelection(true);
			}
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_DEBUG_INTERFACE,
					stringValue);
			WorkspacePreferences.putGdbServerInterface(stringValue);

			// Initial speed
			stringValue = ConfigurationAttributes.INTERFACE_SPEED_DEFAULT;
			if (gdbServerSpeedAuto.getSelection()) {
				stringValue = ConfigurationAttributes.INTERFACE_SPEED_AUTO;
			} else if (gdbServerSpeedAdaptive.getSelection()) {
				stringValue = ConfigurationAttributes.INTERFACE_SPEED_ADAPTIVE;
			} else if (gdbServerSpeedFixed.getSelection()) {
				stringValue = gdbServerSpeedFixedValue.getText().trim();
			}
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_DEVICE_SPEED,
					stringValue);
			WorkspacePreferences.putGdbServerInitialSpeed(stringValue);

			// Ports
			int port;
			port = Integer.parseInt(gdbServerGdbPort.getText().trim());
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_GDB_PORT_NUMBER, port);

			port = Integer.parseInt(gdbServerSwoPort.getText().trim());
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_SWO_PORT_NUMBER, port);

			port = Integer.parseInt(gdbServerTelnetPort.getText().trim());
			configuration
					.setAttribute(
							ConfigurationAttributes.GDB_SERVER_TELNET_PORT_NUMBER,
							port);

			// Flags
			configuration.setAttribute(
					ConfigurationAttributes.DO_GDB_SERVER_VERIFY_DOWNLOAD,
					doGdbServerVerifyDownload.getSelection());

			configuration.setAttribute(
					ConfigurationAttributes.DO_GDB_SERVER_INIT_REGS,
					doGdbServerInitRegs.getSelection());

			configuration.setAttribute(
					ConfigurationAttributes.DO_GDB_SERVER_LOCAL_ONLY,
					doGdbServerLocalOnly.getSelection());

			configuration.setAttribute(
					ConfigurationAttributes.DO_GDB_SERVER_SILENT,
					doGdbServerSilent.getSelection());

			// Log file
			configuration.setAttribute(ConfigurationAttributes.GDB_SERVER_LOG,
					gdbServerLog.getText().trim());

			// Other options
			stringValue = gdbServerOtherOptions.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_OTHER, stringValue);
			WorkspacePreferences.putGdbServerOtherOptions(stringValue);

			// Allocate server console
			configuration.setAttribute(
					ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_CONSOLE,
					doGdbServerAllocateConsole.getSelection());

			// Allocate semihosting console
			configuration
					.setAttribute(
							ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_SEMIHOSTING_CONSOLE,
							doGdbServerAllocateSemihostingConsole
									.getSelection());
		}

		// GDB client
		{
			// always use remote
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_USE_REMOTE_TARGET,
					ConfigurationAttributes.USE_REMOTE_TARGET_DEFAULT);

			stringValue = gdbClientExecutable.getText().trim();
			// configuration.setAttribute(
			// IMILaunchConfigurationConstants.ATTR_DEBUG_NAME,
			// clientExecutable);
			configuration.setAttribute(
					IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME,
					stringValue); // DSF
			WorkspacePreferences.putGdbClientExecutable(stringValue);

			stringValue = gdbClientOtherOptions.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.GDB_CLIENT_OTHER_OPTIONS,
					stringValue);
			WorkspacePreferences.putGdbClientOtherOptions(stringValue);

			stringValue = gdbClientOtherCommands.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.GDB_CLIENT_OTHER_COMMANDS,
					stringValue);
			WorkspacePreferences.putGdbClientCommands(stringValue);
		}

		// Remote target
		{
			if (doStartGdbServer.getSelection()) {
				configuration.setAttribute(IGDBJtagConstants.ATTR_IP_ADDRESS,
						"localhost");

				try {
					int port;
					port = Integer.parseInt(gdbServerGdbPort.getText().trim());
					configuration.setAttribute(
							IGDBJtagConstants.ATTR_PORT_NUMBER, port);
				} catch (NumberFormatException e) {
					Activator.log(e);
				}
			} else {
				String ip = targetIpAddress.getText().trim();
				configuration.setAttribute(IGDBJtagConstants.ATTR_IP_ADDRESS,
						ip);

				try {
					int port = Integer.valueOf(
							targetPortNumber.getText().trim()).intValue();
					configuration.setAttribute(
							IGDBJtagConstants.ATTR_PORT_NUMBER, port);
				} catch (NumberFormatException e) {
					Activator.log(e);
				}
			}
		}

		// Force thread update
		configuration
				.setAttribute(
						IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_UPDATE_THREADLIST_ON_SUSPEND,
						fUpdateThreadlistOnSuspend.getSelection());

		WorkspacePreferences.flush();

		if (DEBUG) {
			System.out.println("TabDebugger: performApply() completed "
					+ configuration.getName() + ", dirty=" + isDirty());
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

		if (DEBUG) {
			System.out.println("TabDebugger: setDefaults() "
					+ configuration.getName());
		}

		configuration.setAttribute(IGDBJtagConstants.ATTR_JTAG_DEVICE,
				ConfigurationAttributes.JTAG_DEVICE);

		// These are inherited from the generic implementation.
		// Some might need some trimming.
		{
			CommandFactoryManager cfManager = MIPlugin.getDefault()
					.getCommandFactoryManager();
			CommandFactoryDescriptor defDesc = cfManager
					.getDefaultDescriptor(IGDBJtagConstants.DEBUGGER_ID);
			configuration
					.setAttribute(
							IMILaunchConfigurationConstants.ATTR_DEBUGGER_COMMAND_FACTORY,
							defDesc.getName());
			configuration.setAttribute(
					IMILaunchConfigurationConstants.ATTR_DEBUGGER_PROTOCOL,
					defDesc.getMIVersions()[0]);
			configuration
					.setAttribute(
							IMILaunchConfigurationConstants.ATTR_DEBUGGER_VERBOSE_MODE,
							IMILaunchConfigurationConstants.DEBUGGER_VERBOSE_MODE_DEFAULT);
			configuration
					.setAttribute(
							IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_UPDATE_THREADLIST_ON_SUSPEND,
							IGDBLaunchConfigurationConstants.DEBUGGER_UPDATE_THREADLIST_ON_SUSPEND_DEFAULT);
		}

		// J-Link GDB server setup
		{
			configuration.setAttribute(
					ConfigurationAttributes.DO_START_GDB_SERVER,
					ConfigurationAttributes.DO_START_GDB_SERVER_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.DO_CONNECT_TO_RUNNING,
					ConfigurationAttributes.DO_CONNECT_TO_RUNNING_DEFAULT);

			String sharedName = WorkspacePreferences
					.getGdbServerExecutable(ConfigurationAttributes.GDB_SERVER_EXECUTABLE_DEFAULT);
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_EXECUTABLE, sharedName);

			sharedName = WorkspacePreferences
					.getFlashDeviceName(ConfigurationAttributes.FLASH_DEVICE_NAME_DEFAULT);
			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_DEVICE_NAME, sharedName);

			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_DEVICE_ENDIANNESS,
					ConfigurationAttributes.ENDIANNESS_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_CONNECTION,
					ConfigurationAttributes.GDB_SERVER_CONNECTION_DEFAULT);

			configuration
					.setAttribute(
							ConfigurationAttributes.GDB_SERVER_CONNECTION_ADDRESS,
							ConfigurationAttributes.GDB_SERVER_CONNECTION_ADDRESS_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_DEBUG_INTERFACE,
					ConfigurationAttributes.INTERFACE_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_DEVICE_SPEED,
					ConfigurationAttributes.GDB_SERVER_SPEED_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_GDB_PORT_NUMBER,
					ConfigurationAttributes.GDB_SERVER_GDB_PORT_NUMBER_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_SWO_PORT_NUMBER,
					ConfigurationAttributes.GDB_SERVER_SWO_PORT_NUMBER_DEFAULT);

			configuration
					.setAttribute(
							ConfigurationAttributes.GDB_SERVER_TELNET_PORT_NUMBER,
							ConfigurationAttributes.GDB_SERVER_TELNET_PORT_NUMBER_DEFAULT);

			configuration
					.setAttribute(
							ConfigurationAttributes.DO_GDB_SERVER_VERIFY_DOWNLOAD,
							ConfigurationAttributes.DO_GDB_SERVER_VERIFY_DOWNLOAD_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.DO_GDB_SERVER_INIT_REGS,
					ConfigurationAttributes.DO_GDB_SERVER_INIT_REGS_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.DO_GDB_SERVER_LOCAL_ONLY,
					ConfigurationAttributes.DO_GDB_SERVER_LOCAL_ONLY_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.DO_GDB_SERVER_SILENT,
					ConfigurationAttributes.DO_GDB_SERVER_SILENT_DEFAULT);

			configuration.setAttribute(ConfigurationAttributes.GDB_SERVER_LOG,
					ConfigurationAttributes.GDB_SERVER_LOG_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.GDB_SERVER_OTHER,
					ConfigurationAttributes.GDB_SERVER_OTHER_DEFAULT);

			configuration
					.setAttribute(
							ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_CONSOLE,
							ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_CONSOLE_DEFAULT);

			configuration
					.setAttribute(
							ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_SEMIHOSTING_CONSOLE,
							ConfigurationAttributes.DO_GDB_SERVER_ALLOCATE_SEMIHOSTING_CONSOLE_DEFAULT);
		}

		// J-Link GDB client setup
		{
			configuration
					.setAttribute(
							IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME,
							WorkspacePreferences
									.getGdbClientExecutable(ConfigurationAttributes.GDB_CLIENT_EXECUTABLE_DEFAULT));

			configuration.setAttribute(
					ConfigurationAttributes.GDB_CLIENT_OTHER_OPTIONS,
					ConfigurationAttributes.GDB_CLIENT_OTHER_OPTIONS_DEFAULT);

			configuration.setAttribute(
					ConfigurationAttributes.GDB_CLIENT_OTHER_COMMANDS,
					ConfigurationAttributes.GDB_CLIENT_OTHER_COMMANDS_DEFAULT);
		}

		// Remote Target
		{
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_USE_REMOTE_TARGET,
					ConfigurationAttributes.USE_REMOTE_TARGET_DEFAULT);

			configuration.setAttribute(IGDBJtagConstants.ATTR_PORT_NUMBER,
					ConfigurationAttributes.REMOTE_PORT_NUMBER_DEFAULT);
		}

		// Force thread update
		configuration
				.setAttribute(
						IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_UPDATE_THREADLIST_ON_SUSPEND,
						ConfigurationAttributes.UPDATE_THREAD_LIST_DEFAULT);
	}

	public static String getGdbServerCommand(ILaunchConfiguration configuration) {

		String executable = null;

		try {
			if (!configuration.getAttribute(
					ConfigurationAttributes.DO_START_GDB_SERVER,
					ConfigurationAttributes.DO_START_GDB_SERVER_DEFAULT))
				return null;

			executable = configuration.getAttribute(
					ConfigurationAttributes.GDB_SERVER_EXECUTABLE,
					ConfigurationAttributes.GDB_SERVER_EXECUTABLE_DEFAULT);
			// executable = Utils.escapeWhitespaces(executable).trim();
			executable = executable.trim();
			if (executable.length() == 0)
				return null;

			executable = VariablesPlugin.getDefault()
					.getStringVariableManager()
					.performStringSubstitution(executable, false).trim();

			ICConfigurationDescription buildConfig = DebugUtils
					.getBuildConfig(configuration);
			if (buildConfig != null) {
				executable = DebugUtils.resolveAll(executable, buildConfig);
			}

		} catch (CoreException e) {
			Activator.log(e);
			return null;
		}

		return executable;
	}

	public static String getGdbServerCommandLine(
			ILaunchConfiguration configuration) {

		String cmdLineArray[] = getGdbServerCommandLineArray(configuration);

		return StringUtils.join(cmdLineArray, " ");
	}

	public static String[] getGdbServerCommandLineArray(
			ILaunchConfiguration configuration) {

		List<String> lst = new ArrayList<String>();

		try {
			if (!configuration.getAttribute(
					ConfigurationAttributes.DO_START_GDB_SERVER,
					ConfigurationAttributes.DO_START_GDB_SERVER_DEFAULT))
				return null;

			String executable = getGdbServerCommand(configuration);
			if (executable == null || executable.length() == 0)
				return null;

			lst.add(executable);

			String connection = configuration.getAttribute(
					ConfigurationAttributes.GDB_SERVER_CONNECTION,
					ConfigurationAttributes.GDB_SERVER_CONNECTION_DEFAULT);
			String connectionAddress = configuration
					.getAttribute(
							ConfigurationAttributes.GDB_SERVER_CONNECTION_ADDRESS,
							ConfigurationAttributes.GDB_SERVER_CONNECTION_ADDRESS_DEFAULT);

			connectionAddress = VariablesPlugin.getDefault()
					.getStringVariableManager()
					.performStringSubstitution(connectionAddress, false).trim();
			if (connectionAddress.length() > 0) {
				if (ConfigurationAttributes.GDB_SERVER_CONNECTION_USB
						.equals(connection)) {

					lst.add("-select");
					lst.add("usb=" + connectionAddress);
				} else if (ConfigurationAttributes.GDB_SERVER_CONNECTION_IP
						.equals(connection)) {
					lst.add("-select");
					lst.add("ip=" + connectionAddress);
				}
			}

			lst.add("-if");
			lst.add(configuration.getAttribute(
					ConfigurationAttributes.GDB_SERVER_DEBUG_INTERFACE,
					ConfigurationAttributes.INTERFACE_DEFAULT));

			String defaultName = configuration.getAttribute(
					ConfigurationAttributes.FLASH_DEVICE_NAME_COMPAT,
					ConfigurationAttributes.FLASH_DEVICE_NAME_DEFAULT);

			String name = configuration
					.getAttribute(
							ConfigurationAttributes.GDB_SERVER_DEVICE_NAME,
							defaultName).trim();
			name = VariablesPlugin.getDefault().getStringVariableManager()
					.performStringSubstitution(name, false).trim();
			if (name.length() > 0) {
				lst.add("-device");
				lst.add(name);
			}

			lst.add("-endian");
			lst.add(configuration.getAttribute(
					ConfigurationAttributes.GDB_SERVER_DEVICE_ENDIANNESS,
					ConfigurationAttributes.ENDIANNESS_DEFAULT));

			lst.add("-speed");
			lst.add(configuration.getAttribute(
					ConfigurationAttributes.GDB_SERVER_DEVICE_SPEED,
					ConfigurationAttributes.GDB_SERVER_SPEED_DEFAULT));

			lst.add("-port");
			lst.add(Integer.toString(configuration.getAttribute(
					ConfigurationAttributes.GDB_SERVER_GDB_PORT_NUMBER,
					ConfigurationAttributes.GDB_SERVER_GDB_PORT_NUMBER_DEFAULT)));

			lst.add("-swoport");
			lst.add(Integer.toString(configuration.getAttribute(
					ConfigurationAttributes.GDB_SERVER_SWO_PORT_NUMBER,
					ConfigurationAttributes.GDB_SERVER_SWO_PORT_NUMBER_DEFAULT)));

			lst.add("-telnetport");
			lst.add(Integer.toString(configuration
					.getAttribute(
							ConfigurationAttributes.GDB_SERVER_TELNET_PORT_NUMBER,
							ConfigurationAttributes.GDB_SERVER_TELNET_PORT_NUMBER_DEFAULT)));

			if (configuration
					.getAttribute(
							ConfigurationAttributes.DO_GDB_SERVER_VERIFY_DOWNLOAD,
							ConfigurationAttributes.DO_GDB_SERVER_VERIFY_DOWNLOAD_DEFAULT)) {
				lst.add("-vd");
			}

			if (configuration.getAttribute(
					ConfigurationAttributes.DO_CONNECT_TO_RUNNING,
					ConfigurationAttributes.DO_CONNECT_TO_RUNNING_DEFAULT)) {
				lst.add("-noreset");
				lst.add("-noir");
			} else {
				if (configuration
						.getAttribute(
								ConfigurationAttributes.DO_GDB_SERVER_INIT_REGS,
								ConfigurationAttributes.DO_GDB_SERVER_INIT_REGS_DEFAULT)) {
					lst.add("-ir");
				} else {
					lst.add("-noir");
				}
			}

			lst.add("-localhostonly");
			if (configuration.getAttribute(
					ConfigurationAttributes.DO_GDB_SERVER_LOCAL_ONLY,
					ConfigurationAttributes.DO_GDB_SERVER_LOCAL_ONLY_DEFAULT)) {
				lst.add("1");
			} else {
				lst.add("0");
			}

			if (configuration.getAttribute(
					ConfigurationAttributes.DO_GDB_SERVER_SILENT,
					ConfigurationAttributes.DO_GDB_SERVER_SILENT_DEFAULT)) {
				lst.add("-silent");
			}

			String logFile = configuration.getAttribute(
					ConfigurationAttributes.GDB_SERVER_LOG,
					ConfigurationAttributes.GDB_SERVER_LOG_DEFAULT).trim();

			logFile = VariablesPlugin.getDefault().getStringVariableManager()
					.performStringSubstitution(logFile, false).trim();
			if (logFile.length() > 0) {
				lst.add("-log");

				// lst.add(Utils.escapeWhitespaces(logFile));
				lst.add(logFile);
			}

			String other = configuration.getAttribute(
					ConfigurationAttributes.GDB_SERVER_OTHER,
					ConfigurationAttributes.GDB_SERVER_OTHER_DEFAULT).trim();
			other = VariablesPlugin.getDefault().getStringVariableManager()
					.performStringSubstitution(other, false).trim();
			if (other.length() > 0) {
				lst.addAll(splitOptions(other));
			}

		} catch (CoreException e) {
			Activator.log(e);
			return null;
		}

		return lst.toArray(new String[0]);
	}

	private enum State {
		None, InOption, InString
	};

	private static List<String> splitOptions(String str) {

		List<String> lst = new ArrayList<String>();
		State state = State.None;

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < str.length(); ++i) {
			char ch = str.charAt(i);

			// a small state machine to split a string in substrings,
			// preserving quoted parts
			switch (state) {
			case None:

				if (ch == '"') {
					sb.setLength(0);

					state = State.InString;
				} else if (ch != ' ') {
					sb.setLength(0);
					sb.append(ch);

					state = State.InOption;
				}
				break;

			case InOption:

				if (ch != ' ') {
					sb.append(ch);
				} else {
					lst.add(sb.toString());

					state = State.None;
				}

				break;

			case InString:

				if (ch != '"') {
					sb.append(ch);
				} else {
					lst.add(sb.toString());

					state = State.None;
				}

				break;
			}

		}

		if (state == State.InOption || state == State.InString) {
			lst.add(sb.toString());
		}
		return lst;

	}

	// --------------

	public static String getGdbClientCommand(ILaunchConfiguration configuration) {

		String executable = null;
		try {
			String defaultGdbCommand = Platform
					.getPreferencesService()
					.getString(
							GdbPlugin.PLUGIN_ID,
							IGdbDebugPreferenceConstants.PREF_DEFAULT_GDB_COMMAND,
							IGDBLaunchConfigurationConstants.DEBUGGER_DEBUG_NAME_DEFAULT,
							null);

			executable = configuration.getAttribute(
					IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME,
					defaultGdbCommand);
			executable = VariablesPlugin.getDefault()
					.getStringVariableManager()
					.performStringSubstitution(executable, false).trim();

			ICConfigurationDescription buildConfig = DebugUtils
					.getBuildConfig(configuration);
			if (buildConfig != null) {
				executable = DebugUtils.resolveAll(executable, buildConfig);
			}

		} catch (CoreException e) {
			Activator.log(e);
			return null;
		}

		return executable;
	}

	public static String[] getGdbClientCommandLineArray(
			ILaunchConfiguration configuration) {

		List<String> lst = new ArrayList<String>();

		String executable = getGdbClientCommand(configuration);
		if (executable == null || executable.length() == 0)
			return null;

		lst.add(executable);

		// We currently work with MI version 2. Don't use just 'mi' because
		// it points to the latest MI version, while we want mi2 specifically.
		lst.add("--interpreter=mi2");

		// Don't read the gdbinit file here. It is read explicitly in
		// the LaunchSequence to make it easier to customise.
		lst.add("--nx");

		String other;
		try {
			other = configuration.getAttribute(
					ConfigurationAttributes.GDB_CLIENT_OTHER_OPTIONS,
					ConfigurationAttributes.GDB_CLIENT_OTHER_OPTIONS_DEFAULT)
					.trim();
			other = VariablesPlugin.getDefault().getStringVariableManager()
					.performStringSubstitution(other, false).trim();
			if (other.length() > 0) {
				lst.addAll(splitOptions(other));
			}
		} catch (CoreException e) {
			Activator.log(e);
		}

		return lst.toArray(new String[0]);
	}

	public static String getGdbClientCommandLine(
			ILaunchConfiguration configuration) {

		String cmdLineArray[] = getGdbClientCommandLineArray(configuration);

		return StringUtils.join(cmdLineArray, " ");
	}

}
