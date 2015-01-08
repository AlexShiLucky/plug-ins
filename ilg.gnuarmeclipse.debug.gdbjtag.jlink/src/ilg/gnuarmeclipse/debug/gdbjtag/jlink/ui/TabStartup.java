/*******************************************************************************
 * Copyright (c) 2007 - 2010 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Andy Jin - Hardware debugging UI improvements, bug 229946
 *     Andy Jin - Added DSF debugging, bug 248593
 *     Liviu Ionescu - ARM version
 *******************************************************************************/

package ilg.gnuarmeclipse.debug.gdbjtag.jlink.ui;

import ilg.gnuarmeclipse.debug.gdbjtag.DebugUtils;
import ilg.gnuarmeclipse.debug.gdbjtag.jlink.Activator;
import ilg.gnuarmeclipse.debug.gdbjtag.jlink.ConfigurationAttributes;
import ilg.gnuarmeclipse.debug.gdbjtag.jlink.EclipseDefaults;
import ilg.gnuarmeclipse.debug.gdbjtag.jlink.WorkspacePersistentValues;

import java.io.File;

import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.debug.gdbjtag.ui.GDBJtagImages;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class TabStartup extends AbstractLaunchConfigurationTab {

	// ------------------------------------------------------------------------

	private static final String TAB_NAME = "Startup";
	private static final String TAB_ID = Activator.PLUGIN_ID + ".ui.startuptab";

	private static final boolean DEBUG = false;

	private Text fInitCommands;
	// Text delay;
	// Button doReset;
	// Button doHalt;

	private Button fDoFirstReset;
	private Text fFirstResetType;
	private Text fFirstResetSpeed;

	private Button fInterfaceSpeedAuto;
	private Button fInterfaceSpeedAdaptive;
	private Button fInterfaceSpeedFixed;
	private Text fInterfaceSpeedFixedValue;

	private Button fDoSecondReset;
	private Text fSecondResetType;
	private Label fSecondResetWarning;

	private Button fEnableFlashBreakpoints;

	private Button fEnableSemihosting;

	private Button fSemihostingTelnet;
	private Button fSemihostingGdbClient;

	private Button fEnableSwo;
	private Text fSwoEnableTargetCpuFreq;
	private Text fSwoEnableTargetSwoFreq;
	private Text fSwoEnableTargetPortMask;

	private Button fLoadExecutable;
	private Text fImageFileName;
	private Button fImageFileBrowseWs;
	private Button fImageFileBrowse;
	private Text fImageOffset;

	private Button fLoadSymbols;
	private Text fSymbolsFileName;

	private Button fSymbolsFileBrowseWs;
	private Button fSymbolsFileBrowse;
	private Text fSymbolsOffset;

	private Button fSetPcRegister;
	private Text fPcRegister;

	private Button fSetStopAt;
	private Text fStopAt;

	private Text fRunCommands;
	private Button fDoContinue;
	private Button fDoDebugInRam;

	// New GUI added to address bug 310304
	private Button fUseProjectBinaryForImage;
	private Button fUseFileForImage;
	private Button fUseProjectBinaryForSymbols;
	private Button fUseFileForSymbols;
	private Label fImageOffsetLabel;
	private Label fSymbolsOffsetLabel;
	private Label fProjBinaryLabel1;
	private Label fProjBinaryLabel2;

	private String fSavedProgName;

	// ------------------------------------------------------------------------

	public TabStartup() {
		super();

		fSavedProgName = null;
	}

	// ------------------------------------------------------------------------

	@Override
	public String getName() {
		return TAB_NAME;
	}

	@Override
	public Image getImage() {
		return GDBJtagImages.getStartupTabImage();
	}

	@Override
	public void createControl(Composite parent) {

		if (DEBUG) {
			System.out.println("TabStartup: createControl() ");
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

		createInitGroup(comp);
		createLoadGroup(comp);
		createRunOptionGroup(comp);
		createRunGroup(comp);

		Link restoreDefaults;
		GridData gd;
		{
			restoreDefaults = new Link(comp, SWT.NONE);
			restoreDefaults.setText(Messages
					.getString("DebuggerTab.restoreDefaults_Link"));
			restoreDefaults.setToolTipText(Messages
					.getString("DebuggerTab.restoreDefaults_ToolTipText"));

			gd = new GridData();
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = SWT.RIGHT;
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns;
			restoreDefaults.setLayoutData(gd);
		}

		sc.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		// --------------------------------------------------------------------

		restoreDefaults.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				initializeFromDefaults();
				scheduleUpdateJob();
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

	private void browseWsButtonSelected(String title, Text text) {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				getShell(), new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
		dialog.setTitle(title);
		dialog.setMessage(Messages.getString("StartupTab.FileBrowseWs_Message"));
		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		if (dialog.open() == IDialogConstants.OK_ID) {
			IResource resource = (IResource) dialog.getFirstResult();
			String arg = resource.getFullPath().toOSString();
			String fileLoc = VariablesPlugin.getDefault()
					.getStringVariableManager()
					.generateVariableExpression("workspace_loc", arg); //$NON-NLS-1$
			text.setText(fileLoc);
		}
	}

	public void createInitGroup(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.getString("StartupTab.initGroup_Text"));
		GridLayout layout = new GridLayout();
		group.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);

		Composite comp = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		comp.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		comp.setLayoutData(gd);

		{
			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 6;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			local.setLayout(layout);
			// local.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			fDoFirstReset = new Button(local, SWT.CHECK);
			fDoFirstReset.setText(Messages
					.getString("StartupTab.doFirstReset_Text"));
			fDoFirstReset.setToolTipText(Messages
					.getString("StartupTab.doFirstReset_ToolTipText"));

			Label label = new Label(local, SWT.NONE);
			label.setText(Messages.getString("StartupTab.firstResetType_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.firstResetType_ToolTipText"));

			fFirstResetType = new Text(local, SWT.BORDER);
			fFirstResetType.setToolTipText(Messages
					.getString("StartupTab.firstResetType_ToolTipText"));
			gd = new GridData();
			gd.widthHint = 100;
			fFirstResetType.setLayoutData(gd);

			label = new Label(local, SWT.NONE);
			label.setText(Messages.getString("StartupTab.firstResetSpeed_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.firstResetSpeed_ToolTipText"));

			fFirstResetSpeed = new Text(local, SWT.BORDER);
			fFirstResetSpeed.setToolTipText(Messages
					.getString("StartupTab.firstResetSpeed_ToolTipText"));
			gd = new GridData();
			gd.widthHint = 40;
			fFirstResetSpeed.setLayoutData(gd);

			label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.firstResetSpeedUnit_Text"));
		}

		{
			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 6;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			local.setLayout(layout);

			Label label = new Label(local, SWT.NONE);
			label.setText(Messages.getString("StartupTab.interfaceSpeed_Label")); //$NON-NLS-1$
			label.setToolTipText(Messages
					.getString("StartupTab.interfaceSpeed_ToolTipText"));

			fInterfaceSpeedAuto = new Button(local, SWT.RADIO);
			fInterfaceSpeedAuto.setText(Messages
					.getString("StartupTab.interfaceSpeedAuto_Text"));

			fInterfaceSpeedAdaptive = new Button(local, SWT.RADIO);
			fInterfaceSpeedAdaptive.setText(Messages
					.getString("StartupTab.interfaceSpeedAdaptive_Text"));

			fInterfaceSpeedFixed = new Button(local, SWT.RADIO);
			fInterfaceSpeedFixed.setText(Messages
					.getString("StartupTab.interfaceSpeedFixed_Text"));

			fInterfaceSpeedFixedValue = new Text(local, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 40;
			fInterfaceSpeedFixedValue.setLayoutData(gd);

			label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.interfaceSpeedFixedUnit_Text"));
		}

		{
			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			local.setLayout(layout);

			fEnableFlashBreakpoints = new Button(local, SWT.CHECK);
			fEnableFlashBreakpoints.setText(Messages
					.getString("StartupTab.enableFlashBreakpoints_Text"));
			fEnableFlashBreakpoints
					.setToolTipText(Messages
							.getString("StartupTab.enableFlashBreakpoints_ToolTipText"));
		}

		{
			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 4;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			local.setLayout(layout);
			// local.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			fEnableSemihosting = new Button(local, SWT.CHECK);
			fEnableSemihosting.setText(Messages
					.getString("StartupTab.enableSemihosting_Text"));
			fEnableSemihosting.setToolTipText(Messages
					.getString("StartupTab.enableSemihosting_ToolTipText"));

			Label label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.enableSemihostingRouted_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.enableSemihostingRouted_ToolTipText"));

			fSemihostingTelnet = new Button(local, SWT.CHECK);
			fSemihostingTelnet.setText(Messages
					.getString("StartupTab.semihostingTelnet_Text"));
			fSemihostingTelnet.setToolTipText(Messages
					.getString("StartupTab.semihostingTelnet_ToolTipText"));

			fSemihostingGdbClient = new Button(local, SWT.CHECK);
			fSemihostingGdbClient.setText(Messages
					.getString("StartupTab.semihostingGdbClient_Text"));
			fSemihostingGdbClient.setToolTipText(Messages
					.getString("StartupTab.semihostingGdbClient_ToolTipText"));
		}

		{
			Composite local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 9;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			local.setLayout(layout);
			// local.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			fEnableSwo = new Button(local, SWT.CHECK);
			fEnableSwo.setText(Messages.getString("StartupTab.enableSwo_Text"));
			fEnableSwo.setToolTipText(Messages
					.getString("StartupTab.enableSwo_ToolTipText"));

			Label label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.swoEnableTargetCpuFreq_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.swoEnableTargetCpuFreq_ToolTipText"));

			fSwoEnableTargetCpuFreq = new Text(local, SWT.BORDER);
			fSwoEnableTargetCpuFreq
					.setToolTipText(Messages
							.getString("StartupTab.swoEnableTargetCpuFreq_ToolTipText"));
			gd = new GridData();
			gd.widthHint = 80;
			fSwoEnableTargetCpuFreq.setLayoutData(gd);

			label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.swoEnableTargetCpuFreqUnit_Text"));

			label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.swoEnableTargetSwoFreq_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.swoEnableTargetSwoFreq_ToolTipText"));

			fSwoEnableTargetSwoFreq = new Text(local, SWT.BORDER);
			fSwoEnableTargetSwoFreq
					.setToolTipText(Messages
							.getString("StartupTab.swoEnableTargetSwoFreq_ToolTipText"));
			gd = new GridData();
			gd.widthHint = 60;
			fSwoEnableTargetSwoFreq.setLayoutData(gd);

			label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.swoEnableTargetSwoFreqUnit_Text"));

			label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.swoEnableTargetPortMask_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.swoEnableTargetPortMask_ToolTipText"));

			fSwoEnableTargetPortMask = new Text(local, SWT.BORDER);
			fSwoEnableTargetPortMask
					.setToolTipText(Messages
							.getString("StartupTab.swoEnableTargetPortMask_ToolTipText"));
			gd = new GridData();
			gd.widthHint = 80;
			fSwoEnableTargetPortMask.setLayoutData(gd);
		}

		{
			fInitCommands = new Text(comp, SWT.MULTI | SWT.WRAP | SWT.BORDER
					| SWT.V_SCROLL);
			fInitCommands.setToolTipText(Messages
					.getString("StartupTab.initCommands_ToolTipText"));
			gd = new GridData(GridData.FILL_BOTH);
			gd.heightHint = 60;
			fInitCommands.setLayoutData(gd);
		}

		// Actions
		fDoFirstReset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// doResetChanged();
				doFirstResetChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		VerifyListener numericVerifyListener = new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character) || Character
						.isISOControl(e.character));
			}
		};

		VerifyListener hexVerifyListener = new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character)
						|| Character.isISOControl(e.character) || "abcdefx"
						.contains(String.valueOf(e.character).toLowerCase()));
			}
		};

		ModifyListener scheduleUpdateJobModifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		};

		fFirstResetType.addVerifyListener(numericVerifyListener);
		fFirstResetType.addModifyListener(scheduleUpdateJobModifyListener);

		fFirstResetSpeed.addVerifyListener(numericVerifyListener);
		fFirstResetSpeed.addModifyListener(scheduleUpdateJobModifyListener);

		fInterfaceSpeedAuto.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fInterfaceSpeedFixedValue.setEnabled(false);
				scheduleUpdateJob();
			}
		});

		fInterfaceSpeedAdaptive.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fInterfaceSpeedFixedValue.setEnabled(false);
				scheduleUpdateJob();
			}
		});

		fInterfaceSpeedFixed.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fInterfaceSpeedFixedValue.setEnabled(true);
				scheduleUpdateJob();
			}
		});

		fInterfaceSpeedFixedValue.addVerifyListener(numericVerifyListener);

		fInterfaceSpeedFixedValue
				.addModifyListener(scheduleUpdateJobModifyListener);

		fEnableSemihosting.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doEnableSemihostingChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		fSemihostingTelnet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});
		fSemihostingGdbClient.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		fEnableSwo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doEnableSwoChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		fSwoEnableTargetCpuFreq.addVerifyListener(numericVerifyListener);
		fSwoEnableTargetCpuFreq.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {

				scheduleUpdateJob();
			}
		});

		fSwoEnableTargetSwoFreq.addVerifyListener(numericVerifyListener);
		fSwoEnableTargetSwoFreq.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {

				scheduleUpdateJob();
			}
		});

		fSwoEnableTargetPortMask.addVerifyListener(hexVerifyListener);
		fSwoEnableTargetPortMask
				.addModifyListener(scheduleUpdateJobModifyListener);

		fInitCommands.addModifyListener(scheduleUpdateJobModifyListener);
	}

	private void doFirstResetChanged() {

		boolean enabled = fDoFirstReset.getSelection();

		fFirstResetType.setEnabled(enabled);
		fFirstResetSpeed.setEnabled(enabled);
	}

	private void doEnableSemihostingChanged() {

		boolean enabled = fEnableSemihosting.getSelection();

		fSemihostingTelnet.setEnabled(enabled);
		fSemihostingGdbClient.setEnabled(enabled);
	}

	private void doEnableSwoChanged() {

		boolean enabled = fEnableSwo.getSelection() && fEnableSwo.getEnabled();

		fSwoEnableTargetCpuFreq.setEnabled(enabled);
		fSwoEnableTargetSwoFreq.setEnabled(enabled);
		fSwoEnableTargetPortMask.setEnabled(enabled);
	}

	private void createLoadGroup(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		group.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		group.setText(Messages.getString("StartupTab.loadGroup_Text"));

		Composite comp = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		comp.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		comp.setLayoutData(gd);

		Composite local;

		{
			fLoadSymbols = new Button(comp, SWT.CHECK);
			fLoadSymbols.setText(Messages
					.getString("StartupTab.loadSymbols_Text"));
		}

		{
			local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 4;
			layout.marginHeight = 0;
			local.setLayout(layout);
			local.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			{
				fUseProjectBinaryForSymbols = new Button(local, SWT.RADIO);
				fUseProjectBinaryForSymbols.setText(Messages
						.getString("StartupTab.useProjectBinary_Label"));
				fUseProjectBinaryForSymbols.setToolTipText(Messages
						.getString("StartupTab.useProjectBinary_ToolTip"));

				fProjBinaryLabel2 = new Label(local, SWT.NONE);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = ((GridLayout) local.getLayout()).numColumns - 1;
				fProjBinaryLabel2.setLayoutData(gd);
			}

			{
				fUseFileForSymbols = new Button(local, SWT.RADIO);
				fUseFileForSymbols.setText(Messages
						.getString("StartupTab.useFile_Label"));

				fSymbolsFileName = new Text(local, SWT.BORDER);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				fSymbolsFileName.setLayoutData(gd);

				fSymbolsFileBrowseWs = createPushButton(local,
						Messages.getString("StartupTab.FileBrowseWs_Label"),
						null);

				fSymbolsFileBrowse = createPushButton(local,
						Messages.getString("StartupTab.FileBrowse_Label"), null);
			}

			{
				fSymbolsOffsetLabel = new Label(local, SWT.NONE);
				fSymbolsOffsetLabel.setText(Messages
						.getString("StartupTab.symbolsOffsetLabel_Text"));

				fSymbolsOffset = new Text(local, SWT.BORDER);
				gd = new GridData();
				gd.horizontalSpan = ((GridLayout) local.getLayout()).numColumns - 1;
				gd.widthHint = 100;
				fSymbolsOffset.setLayoutData(gd);
			}
		}

		{
			fLoadExecutable = new Button(comp, SWT.CHECK);
			fLoadExecutable.setText(Messages
					.getString("StartupTab.loadImage_Text"));
		}

		{
			local = new Composite(comp, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 4;
			layout.marginHeight = 0;
			local.setLayout(layout);
			local.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			{
				fUseProjectBinaryForImage = new Button(local, SWT.RADIO);
				fUseProjectBinaryForImage.setText(Messages
						.getString("StartupTab.useProjectBinary_Label"));
				fUseProjectBinaryForImage.setToolTipText(Messages
						.getString("StartupTab.useProjectBinary_ToolTipText"));

				fProjBinaryLabel1 = new Label(local, SWT.NONE);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = ((GridLayout) local.getLayout()).numColumns - 1;
				fProjBinaryLabel1.setLayoutData(gd);
			}

			{
				fUseFileForImage = new Button(local, SWT.RADIO);
				fUseFileForImage.setText(Messages
						.getString("StartupTab.useFile_Label"));

				fImageFileName = new Text(local, SWT.BORDER);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				fImageFileName.setLayoutData(gd);

				fImageFileBrowseWs = createPushButton(local,
						Messages.getString("StartupTab.FileBrowseWs_Label"),
						null);

				fImageFileBrowse = createPushButton(local,
						Messages.getString("StartupTab.FileBrowse_Label"), null);
			}

			{
				fImageOffsetLabel = new Label(local, SWT.NONE);
				fImageOffsetLabel.setText(Messages
						.getString("StartupTab.imageOffsetLabel_Text"));

				fImageOffset = new Text(local, SWT.BORDER);
				gd = new GridData();
				gd.horizontalSpan = ((GridLayout) local.getLayout()).numColumns - 1;
				gd.widthHint = ((GridData) fSymbolsOffset.getLayoutData()).widthHint;
				fImageOffset.setLayoutData(gd);
			}
		}

		// ----- Actions ------------------------------------------------------

		fLoadExecutable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadExecutableChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		SelectionListener radioButtonListener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// updateLaunchConfigurationDialog();
				updateUseFileEnablement();
				scheduleUpdateJob();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};

		fUseProjectBinaryForImage.addSelectionListener(radioButtonListener);

		fUseFileForImage.addSelectionListener(radioButtonListener);
		fUseProjectBinaryForSymbols.addSelectionListener(radioButtonListener);
		fUseFileForSymbols.addSelectionListener(radioButtonListener);

		fImageFileName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		fImageFileBrowseWs.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseWsButtonSelected(Messages
						.getString("StartupTab.imageFileBrowseWs_Title"),
						fImageFileName);
			}
		});

		fImageFileBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseButtonSelected(
						Messages.getString("StartupTab.imageFileBrowse_Title"),
						fImageFileName);
			}
		});

		fImageOffset.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character)
						|| Character.isISOControl(e.character) || "abcdef"
						.contains(String.valueOf(e.character).toLowerCase()));
			}
		});
		fImageOffset.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		fLoadSymbols.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadSymbolsChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		fSymbolsFileName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		fSymbolsFileBrowseWs.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseWsButtonSelected(Messages
						.getString("StartupTab.symbolsFileBrowseWs_Title"),
						fSymbolsFileName);
			}
		});

		fSymbolsFileBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseButtonSelected(Messages
						.getString("StartupTab.symbolsFileBrowse_Title"),
						fSymbolsFileName);
			}
		});

		fSymbolsOffset.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character)
						|| Character.isISOControl(e.character) || "abcdef"
						.contains(String.valueOf(e.character).toLowerCase()));
			}
		});
		fSymbolsOffset.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

	}

	private void updateUseFileEnablement() {

		boolean enabled = fLoadExecutable.getSelection()
				&& fUseFileForImage.getSelection();
		fImageFileName.setEnabled(enabled);
		fImageFileBrowseWs.setEnabled(enabled);
		fImageFileBrowse.setEnabled(enabled);

		enabled = fLoadSymbols.getSelection()
				&& fUseFileForSymbols.getSelection();
		fSymbolsFileName.setEnabled(enabled);
		fSymbolsFileBrowseWs.setEnabled(enabled);
		fSymbolsFileBrowse.setEnabled(enabled);
	}

	public void createRunOptionGroup(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.getString("StartupTab.runOptionGroup_Text"));
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

		fDoDebugInRam = new Button(comp, SWT.CHECK);
		fDoDebugInRam.setText(Messages
				.getString("StartupTab.doDebugInRam_Text"));
		fDoDebugInRam.setToolTipText(Messages
				.getString("StartupTab.doDebugInRam_ToolTipText"));

		// ----- Actions ------------------------------------------------------

		fDoDebugInRam.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduleUpdateJob();
			}
		});
	}

	private void doSecondResetChanged() {
		boolean enabled = fDoSecondReset.getSelection();
		fSecondResetType.setEnabled(enabled);
		fSecondResetWarning.setEnabled(enabled);
	}

	private void loadExecutableChanged() {
		boolean enabled = fLoadExecutable.getSelection();
		fUseProjectBinaryForImage.setEnabled(enabled);
		fUseFileForImage.setEnabled(enabled);
		fImageOffset.setEnabled(enabled);
		fImageOffsetLabel.setEnabled(enabled);
		updateUseFileEnablement();
	}

	private void loadSymbolsChanged() {
		boolean enabled = fLoadSymbols.getSelection();
		fUseProjectBinaryForSymbols.setEnabled(enabled);
		fUseFileForSymbols.setEnabled(enabled);
		fSymbolsOffset.setEnabled(enabled);
		fSymbolsOffsetLabel.setEnabled(enabled);
		updateUseFileEnablement();
	}

	private void pcRegisterChanged() {
		fPcRegister.setEnabled(fSetPcRegister.getSelection());
	}

	private void stopAtChanged() {
		fStopAt.setEnabled(fSetStopAt.getSelection());
	}

	public void createRunGroup(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.getString("StartupTab.runGroup_Text"));
		GridLayout layout = new GridLayout();
		group.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);

		Composite comp = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 4;
		layout.marginHeight = 0;
		comp.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		comp.setLayoutData(gd);

		{
			fDoSecondReset = new Button(comp, SWT.CHECK);
			fDoSecondReset.setText(Messages
					.getString("StartupTab.doSecondReset_Text"));
			fDoSecondReset.setToolTipText(Messages
					.getString("StartupTab.doSecondReset_ToolTipText"));

			Label label = new Label(comp, SWT.NONE);
			label.setText(Messages.getString("StartupTab.secondResetType_Text"));

			fSecondResetType = new Text(comp, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 100;
			fSecondResetType.setLayoutData(gd);

			fSecondResetWarning = new Label(comp, SWT.NONE);
			fSecondResetWarning.setText(Messages
					.getString("StartupTab.secondResetWarning_Text"));
			gd = new GridData();
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 3;
			fSecondResetWarning.setLayoutData(gd);
		}

		{
			fRunCommands = new Text(comp, SWT.MULTI | SWT.WRAP | SWT.BORDER
					| SWT.V_SCROLL);
			fRunCommands.setToolTipText(Messages
					.getString("StartupTab.runCommands_ToolTipText"));
			gd = new GridData(GridData.FILL_BOTH);
			gd.heightHint = 60;
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns;
			fRunCommands.setLayoutData(gd);
		}

		{
			fSetPcRegister = new Button(comp, SWT.CHECK);
			fSetPcRegister.setText(Messages
					.getString("StartupTab.setPcRegister_Text"));
			fSetPcRegister.setToolTipText(Messages
					.getString("StartupTab.setPcRegister_ToolTipText"));

			fPcRegister = new Text(comp, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 100;
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			fPcRegister.setLayoutData(gd);
		}

		{
			fSetStopAt = new Button(comp, SWT.CHECK);
			fSetStopAt.setText(Messages.getString("StartupTab.setStopAt_Text"));
			fSetStopAt.setToolTipText(Messages
					.getString("StartupTab.setStopAt_ToolTipText"));

			fStopAt = new Text(comp, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 100;
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			fStopAt.setLayoutData(gd);
		}

		{
			fDoContinue = new Button(comp, SWT.CHECK);
			fDoContinue.setText(Messages
					.getString("StartupTab.doContinue_Text"));
			fDoContinue.setToolTipText(Messages
					.getString("StartupTab.doContinue_ToolTipText"));

			gd = new GridData();
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns;
			fDoContinue.setLayoutData(gd);
		}

		// ---- Actions -------------------------------------------------------

		fDoSecondReset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// doResetChanged();
				doSecondResetChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		fSecondResetType.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character) || Character
						.isISOControl(e.character));
			}
		});

		fSetPcRegister.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pcRegisterChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		fPcRegister.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character)
						|| Character.isISOControl(e.character) || "abcdef"
						.contains(String.valueOf(e.character).toLowerCase()));
			}
		});

		fPcRegister.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		fSetStopAt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stopAtChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		fStopAt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

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

		fSecondResetType.addModifyListener(scheduleUpdateJobModifyListener);

		fRunCommands.addModifyListener(scheduleUpdateJobModifyListener);

		fDoContinue.addSelectionListener(scheduleUpdateJobSelectionAdapter);
	}

	public void doConnectToRunningChanged(boolean flag) {

		// System.out.println(flag);
		fDoFirstReset.setEnabled(!flag);
		fFirstResetType.setEnabled(!flag);

		fDoSecondReset.setEnabled(!flag);
		fSecondResetType.setEnabled(!flag);
		fSecondResetWarning.setEnabled(!flag);

		fLoadExecutable.setEnabled(!flag);
	}

	// flag is true when interface is SWD
	public void doInterfaceSwdChanged(boolean flag) {

		fEnableSwo.setEnabled(flag);

		doEnableSwoChanged();
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		if (!super.isValid(launchConfig))
			return false;
		setErrorMessage(null);
		setMessage(null);

		if (fLoadExecutable.getSelection()) {
			if (!fUseProjectBinaryForImage.getSelection()) {
				if (fImageFileName.getText().trim().length() == 0) {
					setErrorMessage(Messages
							.getString("StartupTab.imageFileName_not_specified"));
					return false;
				}

				try {
					String path = fImageFileName.getText().trim();
					path = DebugUtils.resolveAll(path,
							launchConfig.getAttributes());
					IPath filePath = new Path(path);
					if (!filePath.toFile().exists()) {
						setErrorMessage(Messages
								.getString("StartupTab.imageFileName_does_not_exist"));
						return false;
					}
				} catch (CoreException e) { // string substitution throws this
											// if expression doesn't resolve
					setErrorMessage(Messages
							.getString("StartupTab.imageFileName_does_not_exist"));
					return false;
				}
			}
		} else {
			setErrorMessage(null);
		}
		if (fLoadSymbols.getSelection()) {
			if (!fUseProjectBinaryForSymbols.getSelection()) {
				if (fSymbolsFileName.getText().trim().length() == 0) {
					setErrorMessage(Messages
							.getString("StartupTab.symbolsFileName_not_specified"));
					return false;
				}

				try {
					String path = fSymbolsFileName.getText().trim();
					path = DebugUtils.resolveAll(path,
							launchConfig.getAttributes());
					IPath filePath = new Path(path);
					if (!filePath.toFile().exists()) {
						setErrorMessage(Messages
								.getString("StartupTab.symbolsFileName_does_not_exist"));
						return false;
					}
				} catch (CoreException e) { // string substitution throws this
											// if expression doesn't resolve
					setErrorMessage(Messages
							.getString("StartupTab.symbolsFileName_does_not_exist"));
					return false;
				}
			}
		} else {
			setErrorMessage(null);
		}

		if (fSetPcRegister.getSelection()) {
			if (fPcRegister.getText().trim().length() == 0) {
				setErrorMessage(Messages
						.getString("StartupTab.pcRegister_not_specified"));
				return false;
			}
		} else {
			setErrorMessage(null);
		}
		if (fSetStopAt.getSelection()) {
			if (fStopAt.getText().trim().length() == 0) {
				setErrorMessage(Messages
						.getString("StartupTab.stopAt_not_specified"));
			}
		} else {
			setErrorMessage(null);
		}
		return true;
	}

	@Override
	public String getId() {
		return TAB_ID;
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {

		if (DEBUG) {
			System.out.println("TabStartup: initializeFrom() "
					+ configuration.getName() + ", dirty=" + isDirty());
		}

		try {
			String stringDefault;
			boolean booleanDefault;
			int intDefault;

			// Initialisation Commands
			{
				// Do initial reset
				booleanDefault = WorkspacePersistentValues
						.getJLinkDoInitialReset(ConfigurationAttributes.DO_FIRST_RESET_DEFAULT);
				fDoFirstReset
						.setSelection(configuration.getAttribute(
								ConfigurationAttributes.DO_FIRST_RESET,
								booleanDefault));

				// Reset type
				stringDefault = WorkspacePersistentValues
						.getJLinkInitialResetType(ConfigurationAttributes.FIRST_RESET_TYPE_DEFAULT);
				fFirstResetType.setText(configuration
						.getAttribute(ConfigurationAttributes.FIRST_RESET_TYPE,
								stringDefault));

				intDefault = WorkspacePersistentValues
						.getJLinkInitialResetSpeed(ConfigurationAttributes.FIRST_RESET_SPEED_DEFAULT);

				// Type change from string to int, compatibility preserved
				try {
					int i = configuration.getAttribute(
							ConfigurationAttributes.FIRST_RESET_SPEED,
							intDefault);
					fFirstResetSpeed.setText(String.valueOf(i));
				} catch (CoreException e) {
					try {
						stringDefault = configuration
								.getAttribute(
										ConfigurationAttributes.FIRST_RESET_SPEED,
										String.valueOf(ConfigurationAttributes.FIRST_RESET_SPEED_DEFAULT));
						fFirstResetSpeed.setText(stringDefault);
					} catch (CoreException e2) {
						fFirstResetSpeed
								.setText(String
										.valueOf(ConfigurationAttributes.FIRST_RESET_SPEED_DEFAULT));
					}
				}
				// Speed
				stringDefault = WorkspacePersistentValues
						.getJLinkSpeed(ConfigurationAttributes.INTERFACE_SPEED_DEFAULT);
				String physicalInterfaceSpeed = configuration.getAttribute(
						ConfigurationAttributes.INTERFACE_SPEED, stringDefault);

				if (ConfigurationAttributes.INTERFACE_SPEED_AUTO
						.equals(physicalInterfaceSpeed)) {
					fInterfaceSpeedAuto.setSelection(true);
					fInterfaceSpeedFixedValue.setEnabled(false);

				} else if (ConfigurationAttributes.INTERFACE_SPEED_ADAPTIVE
						.equals(physicalInterfaceSpeed)) {
					fInterfaceSpeedAdaptive.setSelection(true);
					fInterfaceSpeedFixedValue.setEnabled(false);
				} else {
					try {
						Integer.parseInt(physicalInterfaceSpeed);
						fInterfaceSpeedFixed.setSelection(true);
						fInterfaceSpeedFixedValue.setEnabled(true);
						fInterfaceSpeedFixedValue
								.setText(physicalInterfaceSpeed);
					} catch (NumberFormatException e) {
						String message = "unknown interface speed "
								+ physicalInterfaceSpeed + ", using auto";
						Activator.log(message);
						fInterfaceSpeedAuto.setSelection(true);
						fInterfaceSpeedFixedValue.setEnabled(false);
					}
				}

				// Enable flash breakpoints
				booleanDefault = WorkspacePersistentValues
						.getJLinkEnableFlashBreakpoints(ConfigurationAttributes.ENABLE_FLASH_BREAKPOINTS_DEFAULT);
				fEnableFlashBreakpoints
						.setSelection(configuration
								.getAttribute(
										ConfigurationAttributes.ENABLE_FLASH_BREAKPOINTS,
										booleanDefault));

				// Enable semihosting
				booleanDefault = WorkspacePersistentValues
						.getJLinkEnableSemihosting(ConfigurationAttributes.ENABLE_SEMIHOSTING_DEFAULT);
				fEnableSemihosting.setSelection(configuration.getAttribute(
						ConfigurationAttributes.ENABLE_SEMIHOSTING,
						booleanDefault));

				booleanDefault = WorkspacePersistentValues
						.getJLinkSemihostingTelnet(ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_TELNET_DEFAULT);
				fSemihostingTelnet
						.setSelection(configuration
								.getAttribute(
										ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_TELNET,
										booleanDefault));

				booleanDefault = WorkspacePersistentValues
						.getJLinkSemihostingClient(ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_GDBCLIENT_DEFAULT);
				fSemihostingGdbClient
						.setSelection(configuration
								.getAttribute(
										ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_GDBCLIENT,
										booleanDefault));

				booleanDefault = WorkspacePersistentValues
						.getJLinkEnableSwo(ConfigurationAttributes.ENABLE_SWO_DEFAULT);
				// System.out.println("getJLinkEnableSwo()="+booleanDefault+" "+configuration.getName());
				fEnableSwo.setSelection(configuration.getAttribute(
						ConfigurationAttributes.ENABLE_SWO, booleanDefault));

				intDefault = WorkspacePersistentValues
						.getJLinkSwoEnableTargetCpuFreq(ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ_DEFAULT);
				fSwoEnableTargetCpuFreq
						.setText(String.valueOf(configuration
								.getAttribute(
										ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ,
										intDefault)));
				intDefault = WorkspacePersistentValues
						.getJLinkSwoEnableTargetSwoFreq(ConfigurationAttributes.SWO_ENABLETARGET_SWOFREQ_DEFAULT);
				fSwoEnableTargetSwoFreq
						.setText(String.valueOf(configuration
								.getAttribute(
										ConfigurationAttributes.SWO_ENABLETARGET_SWOFREQ,
										intDefault)));

				stringDefault = WorkspacePersistentValues
						.getJLinkSwoEnableTargetPortMask(ConfigurationAttributes.SWO_ENABLETARGET_PORTMASK_DEFAULT);
				Object oValue = configuration.getAttribute(
						ConfigurationAttributes.SWO_ENABLETARGET_PORTMASK,
						stringDefault);
				String sValue;
				sValue = String.valueOf(oValue);
				if (sValue.length() == 0) {
					sValue = ConfigurationAttributes.SWO_ENABLETARGET_PORTMASK_DEFAULT;
				}

				fSwoEnableTargetPortMask.setText(sValue);

				// Other commands
				stringDefault = WorkspacePersistentValues
						.getJLinkInitOther(ConfigurationAttributes.OTHER_INIT_COMMANDS_DEFAULT);
				fInitCommands.setText(configuration.getAttribute(
						ConfigurationAttributes.OTHER_INIT_COMMANDS,
						stringDefault));
			}

			// Load Symbols & Image
			{
				fLoadSymbols.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_LOAD_SYMBOLS,
						IGDBJtagConstants.DEFAULT_LOAD_SYMBOLS));
				fUseProjectBinaryForSymbols
						.setSelection(configuration
								.getAttribute(
										IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS,
										IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_SYMBOLS));
				fUseFileForSymbols.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_USE_FILE_FOR_SYMBOLS,
						IGDBJtagConstants.DEFAULT_USE_FILE_FOR_SYMBOLS));
				fSymbolsFileName.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME,
						IGDBJtagConstants.DEFAULT_SYMBOLS_FILE_NAME));
				fSymbolsOffset.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_SYMBOLS_OFFSET,
						IGDBJtagConstants.DEFAULT_SYMBOLS_OFFSET));

				fLoadExecutable.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_LOAD_IMAGE,
						IGDBJtagConstants.DEFAULT_LOAD_IMAGE));
				fUseProjectBinaryForImage
						.setSelection(configuration
								.getAttribute(
										IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE,
										IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_IMAGE));
				fUseFileForImage.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_USE_FILE_FOR_IMAGE,
						IGDBJtagConstants.DEFAULT_USE_FILE_FOR_IMAGE));
				fImageFileName.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_IMAGE_FILE_NAME,
						IGDBJtagConstants.DEFAULT_IMAGE_FILE_NAME));
				fImageOffset.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_IMAGE_OFFSET,
						IGDBJtagConstants.DEFAULT_IMAGE_OFFSET));

				String programName = CDebugUtils.getProgramName(configuration);
				if (programName != null) {
					int lastSlash = programName.indexOf('\\');
					if (lastSlash >= 0) {
						programName = programName.substring(lastSlash + 1);
					}
					lastSlash = programName.indexOf('/');
					if (lastSlash >= 0) {
						programName = programName.substring(lastSlash + 1);
					}
					fProjBinaryLabel1.setText(programName);
					fProjBinaryLabel2.setText(programName);
				}
				fSavedProgName = programName;
			}

			// Runtime Options
			{
				booleanDefault = WorkspacePersistentValues
						.getJLinkDebugInRam(ConfigurationAttributes.DO_DEBUG_IN_RAM_DEFAULT);
				fDoDebugInRam.setSelection(configuration
						.getAttribute(ConfigurationAttributes.DO_DEBUG_IN_RAM,
								booleanDefault));
			}

			// Run Commands
			{
				// Do pre-run reset
				booleanDefault = WorkspacePersistentValues
						.getJLinkDoPreRunReset(ConfigurationAttributes.DO_SECOND_RESET_DEFAULT);
				fDoSecondReset.setSelection(configuration
						.getAttribute(ConfigurationAttributes.DO_SECOND_RESET,
								booleanDefault));

				// Pre-run reset type
				stringDefault = WorkspacePersistentValues
						.getJLinkPreRunResetType(ConfigurationAttributes.SECOND_RESET_TYPE_DEFAULT);
				fSecondResetType.setText(configuration.getAttribute(
						ConfigurationAttributes.SECOND_RESET_TYPE,
						stringDefault));

				// Other commands
				stringDefault = WorkspacePersistentValues
						.getJLinkPreRunOther(ConfigurationAttributes.OTHER_RUN_COMMANDS_DEFAULT);
				fRunCommands.setText(configuration.getAttribute(
						ConfigurationAttributes.OTHER_RUN_COMMANDS,
						stringDefault));

				fSetPcRegister.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_SET_PC_REGISTER,
						IGDBJtagConstants.DEFAULT_SET_PC_REGISTER));
				fPcRegister.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_PC_REGISTER,
						IGDBJtagConstants.DEFAULT_PC_REGISTER));

				fSetStopAt.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_SET_STOP_AT,
						ConfigurationAttributes.DO_STOP_AT_DEFAULT));
				fStopAt.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_STOP_AT,
						ConfigurationAttributes.STOP_AT_NAME_DEFAULT));

				// Do continue
				fDoContinue.setSelection(configuration.getAttribute(
						ConfigurationAttributes.DO_CONTINUE,
						ConfigurationAttributes.DO_CONTINUE_DEFAULT));
			}

			doFirstResetChanged();
			doEnableSemihostingChanged();
			doEnableSwoChanged();

			doSecondResetChanged();
			loadExecutableChanged();
			loadSymbolsChanged();
			pcRegisterChanged();
			stopAtChanged();

			updateUseFileEnablement();

		} catch (CoreException e) {
			System.out.println(e.getStatus());
			Activator.log(e.getStatus());
		}

		if (DEBUG) {
			System.out.println("TabStartup: initializeFrom() completed "
					+ configuration.getName() + ", dirty=" + isDirty());
		}
	}

	public void initializeFromDefaults() {

		// String stringDefault;
		boolean booleanDefault;
		// int intDefault;

		// Initialisation Commands
		{
			// Do initial reset
			fDoFirstReset
					.setSelection(ConfigurationAttributes.DO_FIRST_RESET_DEFAULT);

			// Reset type
			fFirstResetType
					.setText(ConfigurationAttributes.FIRST_RESET_TYPE_DEFAULT);

			// Type change from string to int, compatibility preserved
			fFirstResetSpeed
					.setText(String
							.valueOf(ConfigurationAttributes.FIRST_RESET_SPEED_DEFAULT));

			// Speed
			String physicalInterfaceSpeed = ConfigurationAttributes.INTERFACE_SPEED_DEFAULT;

			if (ConfigurationAttributes.INTERFACE_SPEED_AUTO
					.equals(physicalInterfaceSpeed)) {
				fInterfaceSpeedAuto.setSelection(true);
				fInterfaceSpeedFixedValue.setEnabled(false);

			} else if (ConfigurationAttributes.INTERFACE_SPEED_ADAPTIVE
					.equals(physicalInterfaceSpeed)) {
				fInterfaceSpeedAdaptive.setSelection(true);
				fInterfaceSpeedFixedValue.setEnabled(false);
			} else {
				try {
					Integer.parseInt(physicalInterfaceSpeed);
					fInterfaceSpeedFixed.setSelection(true);
					fInterfaceSpeedFixedValue.setEnabled(true);
					fInterfaceSpeedFixedValue.setText(physicalInterfaceSpeed);
				} catch (NumberFormatException e) {
					String message = "unknown interface speed "
							+ physicalInterfaceSpeed + ", using auto";
					Activator.log(message);
					fInterfaceSpeedAuto.setSelection(true);
					fInterfaceSpeedFixedValue.setEnabled(false);
				}
			}

			// Enable flash breakpoints
			fEnableFlashBreakpoints
					.setSelection(ConfigurationAttributes.ENABLE_FLASH_BREAKPOINTS_DEFAULT);

			// Enable semihosting
			booleanDefault = EclipseDefaults
					.getJLinkEnableSemihosting(ConfigurationAttributes.ENABLE_SEMIHOSTING_DEFAULT);
			fEnableSemihosting.setSelection(booleanDefault);

			fSemihostingTelnet
					.setSelection(ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_TELNET_DEFAULT);

			fSemihostingGdbClient
					.setSelection(ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_GDBCLIENT_DEFAULT);

			booleanDefault = EclipseDefaults
					.getJLinkEnableSwo(ConfigurationAttributes.ENABLE_SWO_DEFAULT);
			// System.out.println("getJLinkEnableSwo()="+booleanDefault+" "+configuration.getName());
			fEnableSwo.setSelection(booleanDefault);

			fSwoEnableTargetCpuFreq
					.setText(String
							.valueOf(ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ_DEFAULT));
			fSwoEnableTargetSwoFreq
					.setText(String
							.valueOf(ConfigurationAttributes.SWO_ENABLETARGET_SWOFREQ_DEFAULT));

			fSwoEnableTargetPortMask
					.setText(ConfigurationAttributes.SWO_ENABLETARGET_PORTMASK_DEFAULT);

			// Other commands
			fInitCommands
					.setText(ConfigurationAttributes.OTHER_INIT_COMMANDS_DEFAULT);
		}

		// Load Symbols & Image
		{
			fLoadSymbols.setSelection(IGDBJtagConstants.DEFAULT_LOAD_SYMBOLS);
			fUseProjectBinaryForSymbols
					.setSelection(IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_SYMBOLS);
			fUseFileForSymbols
					.setSelection(IGDBJtagConstants.DEFAULT_USE_FILE_FOR_SYMBOLS);
			fSymbolsFileName
					.setText(IGDBJtagConstants.DEFAULT_SYMBOLS_FILE_NAME);
			fSymbolsOffset.setText(IGDBJtagConstants.DEFAULT_SYMBOLS_OFFSET);

			fLoadExecutable.setSelection(IGDBJtagConstants.DEFAULT_LOAD_IMAGE);
			fUseProjectBinaryForImage
					.setSelection(IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_IMAGE);
			fUseFileForImage
					.setSelection(IGDBJtagConstants.DEFAULT_USE_FILE_FOR_IMAGE);
			fImageFileName.setText(IGDBJtagConstants.DEFAULT_IMAGE_FILE_NAME);
			fImageOffset.setText(IGDBJtagConstants.DEFAULT_IMAGE_OFFSET);

			String programName = fSavedProgName;
			if (programName != null) {
				fProjBinaryLabel1.setText(programName);
				fProjBinaryLabel2.setText(programName);
			}
		}

		// Runtime Options
		{
			fDoDebugInRam
					.setSelection(ConfigurationAttributes.DO_DEBUG_IN_RAM_DEFAULT);
		}

		// Run Commands
		{
			// Do pre-run reset
			fDoSecondReset
					.setSelection(ConfigurationAttributes.DO_SECOND_RESET_DEFAULT);

			// Pre-run reset type
			fSecondResetType
					.setText(ConfigurationAttributes.SECOND_RESET_TYPE_DEFAULT);

			// Other commands
			fRunCommands
					.setText(ConfigurationAttributes.OTHER_RUN_COMMANDS_DEFAULT);

			fSetPcRegister
					.setSelection(IGDBJtagConstants.DEFAULT_SET_PC_REGISTER);
			fPcRegister.setText(IGDBJtagConstants.DEFAULT_PC_REGISTER);

			fSetStopAt.setSelection(ConfigurationAttributes.DO_STOP_AT_DEFAULT);
			fStopAt.setText(ConfigurationAttributes.STOP_AT_NAME_DEFAULT);

			// Do continue
			fDoContinue
					.setSelection(ConfigurationAttributes.DO_CONTINUE_DEFAULT);
		}

		doFirstResetChanged();
		doEnableSemihostingChanged();
		doEnableSwoChanged();

		doSecondResetChanged();
		loadExecutableChanged();
		loadSymbolsChanged();
		pcRegisterChanged();
		stopAtChanged();

		updateUseFileEnablement();
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		if (DEBUG) {
			System.out.println("TabStartup: activated() "
					+ workingCopy.getName());
		}
	}

	@Override
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
		if (DEBUG) {
			System.out.println("TabStartup: deactivated() "
					+ workingCopy.getName());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {

		if (DEBUG) {
			System.out.println("TabStartup: performApply() "
					+ configuration.getName() + ", dirty=" + isDirty());
		}

		// ILaunchConfigurationDialog dialog = getLaunchConfigurationDialog();

		boolean booleanValue;
		String stringValue;
		int intValue;

		// Initialisation Commands
		{
			// Do first reset
			booleanValue = fDoFirstReset.getSelection();
			configuration.setAttribute(ConfigurationAttributes.DO_FIRST_RESET,
					booleanValue);
			WorkspacePersistentValues.putJLinkDoInitialReset(booleanValue);

			// First reset type
			stringValue = fFirstResetType.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.FIRST_RESET_TYPE, stringValue);
			WorkspacePersistentValues.putJLinkInitialResetType(stringValue);

			// First reset speed
			try {
				intValue = Integer.valueOf(fFirstResetSpeed.getText());
			} catch (NumberFormatException e) {
				intValue = ConfigurationAttributes.FIRST_RESET_SPEED_DEFAULT;
			}
			configuration.setAttribute(
					ConfigurationAttributes.FIRST_RESET_SPEED, intValue);
			WorkspacePersistentValues.putJLinkInitialResetSpeed(intValue);

			// Interface speed
			stringValue = ConfigurationAttributes.INTERFACE_SPEED_DEFAULT;
			if (fInterfaceSpeedAuto.getSelection()) {
				stringValue = ConfigurationAttributes.INTERFACE_SPEED_AUTO;
			} else if (fInterfaceSpeedAdaptive.getSelection()) {
				stringValue = ConfigurationAttributes.INTERFACE_SPEED_ADAPTIVE;
			} else if (fInterfaceSpeedFixed.getSelection()) {
				stringValue = fInterfaceSpeedFixedValue.getText().trim();
			}
			configuration.setAttribute(ConfigurationAttributes.INTERFACE_SPEED,
					stringValue);
			WorkspacePersistentValues.putJLinkSpeed(stringValue);

			// Enable flash breakpoints
			booleanValue = fEnableFlashBreakpoints.getSelection();
			configuration.setAttribute(
					ConfigurationAttributes.ENABLE_FLASH_BREAKPOINTS,
					booleanValue);
			WorkspacePersistentValues
					.putJLinkEnableFlashBreakpoints(booleanValue);

			// Enable semihosting
			booleanValue = fEnableSemihosting.getSelection();
			configuration.setAttribute(
					ConfigurationAttributes.ENABLE_SEMIHOSTING, booleanValue);
			WorkspacePersistentValues.putJLinkEnableSemihosting(booleanValue);

			// Semihosting via telnet
			booleanValue = fSemihostingTelnet.getSelection();
			configuration.setAttribute(
					ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_TELNET,
					booleanValue);
			WorkspacePersistentValues.putJLinkSemihostingTelnet(booleanValue);

			// Semihosting via client
			booleanValue = fSemihostingGdbClient.getSelection();
			configuration
					.setAttribute(
							ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_GDBCLIENT,
							booleanValue);
			WorkspacePersistentValues.putJLinkSemihostingClient(booleanValue);

			// Enable swo
			booleanValue = fEnableSwo.getSelection();
			configuration.setAttribute(ConfigurationAttributes.ENABLE_SWO,
					booleanValue);
			// System.out.println("putJLinkEnableSwo "+booleanValue+" "+configuration.getName());
			WorkspacePersistentValues.putJLinkEnableSwo(booleanValue);

			// target speed
			try {
				intValue = Integer.parseInt(fSwoEnableTargetCpuFreq.getText());
			} catch (NumberFormatException e) {
				intValue = ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ_DEFAULT;
			}
			configuration.setAttribute(
					ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ, intValue);
			WorkspacePersistentValues.putJLinkSwoEnableTargetCpuFreq(intValue);

			// Swo speed
			try {
				intValue = Integer.parseInt(fSwoEnableTargetSwoFreq.getText());
			} catch (NumberFormatException e) {
				intValue = ConfigurationAttributes.SWO_ENABLETARGET_SWOFREQ_DEFAULT;
			}
			configuration.setAttribute(
					ConfigurationAttributes.SWO_ENABLETARGET_SWOFREQ, intValue);
			WorkspacePersistentValues.putJLinkSwoEnableTargetSwoFreq(intValue);

			// Swo port mask
			stringValue = fSwoEnableTargetPortMask.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.SWO_ENABLETARGET_PORTMASK,
					stringValue);
			WorkspacePersistentValues
					.putJLinkSwoEnableTargetPortMask(stringValue);

			// Other commands
			stringValue = fInitCommands.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.OTHER_INIT_COMMANDS, stringValue);
			WorkspacePersistentValues.putJLinkInitOther(stringValue);
		}

		// Load Symbols & Image...
		{
			configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_SYMBOLS,
					fLoadSymbols.getSelection());
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS,
					fUseProjectBinaryForSymbols.getSelection());
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_USE_FILE_FOR_SYMBOLS,
					fUseFileForSymbols.getSelection());
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME, fSymbolsFileName
							.getText().trim());

			configuration.setAttribute(IGDBJtagConstants.ATTR_SYMBOLS_OFFSET,
					fSymbolsOffset.getText());

			configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_IMAGE,
					fLoadExecutable.getSelection());
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE,
					fUseProjectBinaryForImage.getSelection());
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_USE_FILE_FOR_IMAGE,
					fUseFileForImage.getSelection());
			configuration.setAttribute(IGDBJtagConstants.ATTR_IMAGE_FILE_NAME,
					fImageFileName.getText().trim());

			configuration.setAttribute(IGDBJtagConstants.ATTR_IMAGE_OFFSET,
					fImageOffset.getText());

		}

		// Runtime Options
		{
			booleanValue = fDoDebugInRam.getSelection();
			configuration.setAttribute(ConfigurationAttributes.DO_DEBUG_IN_RAM,
					booleanValue);
			WorkspacePersistentValues.putJLinkDebugInRam(booleanValue);
		}

		// Run Commands
		{
			// Pre-run reset
			booleanValue = fDoSecondReset.getSelection();
			configuration.setAttribute(ConfigurationAttributes.DO_SECOND_RESET,
					fDoSecondReset.getSelection());
			WorkspacePersistentValues.putJLinkDoPreRunReset(booleanValue);

			// reset type
			stringValue = fSecondResetType.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.SECOND_RESET_TYPE, stringValue);
			WorkspacePersistentValues.putJLinkPreRunResetType(stringValue);

			// Other commands
			stringValue = fRunCommands.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.OTHER_RUN_COMMANDS, stringValue);
			WorkspacePersistentValues.putJLinkPreRunOther(stringValue);

			configuration.setAttribute(IGDBJtagConstants.ATTR_SET_PC_REGISTER,
					fSetPcRegister.getSelection());
			configuration.setAttribute(IGDBJtagConstants.ATTR_PC_REGISTER,
					fPcRegister.getText());
			configuration.setAttribute(IGDBJtagConstants.ATTR_SET_STOP_AT,
					fSetStopAt.getSelection());
			configuration.setAttribute(IGDBJtagConstants.ATTR_STOP_AT,
					fStopAt.getText());

			// Continue
			configuration.setAttribute(ConfigurationAttributes.DO_CONTINUE,
					fDoContinue.getSelection());
		}

		WorkspacePersistentValues.flush();

		if (DEBUG) {
			System.out.println("TabStartup: performApply() completed "
					+ configuration.getName() + ", dirty=" + isDirty());
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

		if (DEBUG) {
			System.out.println("TabStartup: setDefaults() "
					+ configuration.getName());
		}

		// Initialisation Commands
		configuration.setAttribute(ConfigurationAttributes.DO_FIRST_RESET,
				ConfigurationAttributes.DO_FIRST_RESET_DEFAULT);
		configuration.setAttribute(ConfigurationAttributes.FIRST_RESET_TYPE,
				ConfigurationAttributes.FIRST_RESET_TYPE_DEFAULT);
		configuration.setAttribute(ConfigurationAttributes.FIRST_RESET_SPEED,
				ConfigurationAttributes.FIRST_RESET_SPEED_DEFAULT);

		configuration.setAttribute(ConfigurationAttributes.INTERFACE_SPEED,
				ConfigurationAttributes.INTERFACE_SPEED_DEFAULT);

		configuration.setAttribute(
				ConfigurationAttributes.ENABLE_FLASH_BREAKPOINTS,
				ConfigurationAttributes.ENABLE_FLASH_BREAKPOINTS_DEFAULT);

		configuration.setAttribute(ConfigurationAttributes.ENABLE_SEMIHOSTING,
				ConfigurationAttributes.ENABLE_SEMIHOSTING_DEFAULT);
		configuration
				.setAttribute(
						ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_TELNET,
						ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_TELNET_DEFAULT);
		configuration
				.setAttribute(
						ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_GDBCLIENT,
						ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_GDBCLIENT_DEFAULT);

		configuration.setAttribute(ConfigurationAttributes.ENABLE_SWO,
				ConfigurationAttributes.ENABLE_SWO_DEFAULT);

		int sharedCpuFreq = WorkspacePersistentValues
				.getJLinkSwoEnableTargetCpuFreq(ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ_DEFAULT);
		configuration
				.setAttribute(ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ,
						sharedCpuFreq);

		int sharedSwoFreq = WorkspacePersistentValues
				.getJLinkSwoEnableTargetSwoFreq(ConfigurationAttributes.SWO_ENABLETARGET_SWOFREQ_DEFAULT);
		configuration
				.setAttribute(ConfigurationAttributes.SWO_ENABLETARGET_SWOFREQ,
						sharedSwoFreq);

		configuration.setAttribute(
				ConfigurationAttributes.SWO_ENABLETARGET_PORTMASK,
				ConfigurationAttributes.SWO_ENABLETARGET_PORTMASK_DEFAULT);

		configuration.setAttribute(ConfigurationAttributes.OTHER_INIT_COMMANDS,
				ConfigurationAttributes.OTHER_INIT_COMMANDS_DEFAULT);

		// Load Image...
		configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_IMAGE,
				IGDBJtagConstants.DEFAULT_LOAD_IMAGE);
		configuration.setAttribute(
				IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE,
				IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_IMAGE);
		configuration.setAttribute(IGDBJtagConstants.ATTR_USE_FILE_FOR_IMAGE,
				IGDBJtagConstants.DEFAULT_USE_FILE_FOR_IMAGE);
		configuration.setAttribute(IGDBJtagConstants.ATTR_IMAGE_FILE_NAME,
				IGDBJtagConstants.DEFAULT_IMAGE_FILE_NAME);
		configuration.setAttribute(IGDBJtagConstants.ATTR_IMAGE_OFFSET,
				IGDBJtagConstants.DEFAULT_IMAGE_OFFSET);

		// .. and Symbols
		configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_SYMBOLS,
				IGDBJtagConstants.DEFAULT_LOAD_SYMBOLS);
		configuration.setAttribute(
				IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS,
				IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_SYMBOLS);
		configuration.setAttribute(IGDBJtagConstants.ATTR_USE_FILE_FOR_SYMBOLS,
				IGDBJtagConstants.DEFAULT_USE_FILE_FOR_SYMBOLS);
		configuration.setAttribute(IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME,
				IGDBJtagConstants.DEFAULT_SYMBOLS_FILE_NAME);
		configuration.setAttribute(IGDBJtagConstants.ATTR_SYMBOLS_OFFSET,
				IGDBJtagConstants.DEFAULT_SYMBOLS_OFFSET);

		// Runtime Options
		configuration.setAttribute(ConfigurationAttributes.DO_DEBUG_IN_RAM,
				ConfigurationAttributes.DO_DEBUG_IN_RAM_DEFAULT);

		// Run Commands
		configuration.setAttribute(ConfigurationAttributes.DO_SECOND_RESET,
				ConfigurationAttributes.DO_SECOND_RESET_DEFAULT);
		configuration.setAttribute(ConfigurationAttributes.SECOND_RESET_TYPE,
				ConfigurationAttributes.SECOND_RESET_TYPE_DEFAULT);

		configuration.setAttribute(ConfigurationAttributes.OTHER_RUN_COMMANDS,
				ConfigurationAttributes.OTHER_RUN_COMMANDS_DEFAULT);

		configuration.setAttribute(IGDBJtagConstants.ATTR_SET_PC_REGISTER,
				IGDBJtagConstants.DEFAULT_SET_PC_REGISTER);
		configuration.setAttribute(IGDBJtagConstants.ATTR_PC_REGISTER,
				IGDBJtagConstants.DEFAULT_PC_REGISTER);
		configuration.setAttribute(IGDBJtagConstants.ATTR_SET_STOP_AT,
				ConfigurationAttributes.DO_STOP_AT_DEFAULT);
		configuration.setAttribute(IGDBJtagConstants.ATTR_STOP_AT,
				ConfigurationAttributes.STOP_AT_NAME_DEFAULT);
		configuration.setAttribute(IGDBJtagConstants.ATTR_SET_RESUME,
				IGDBJtagConstants.DEFAULT_SET_RESUME);

		configuration.setAttribute(ConfigurationAttributes.DO_CONTINUE,
				ConfigurationAttributes.DO_CONTINUE_DEFAULT);
	}
}
