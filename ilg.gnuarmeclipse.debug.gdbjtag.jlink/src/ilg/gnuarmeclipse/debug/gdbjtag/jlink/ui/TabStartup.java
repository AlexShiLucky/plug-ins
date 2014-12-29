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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class TabStartup extends AbstractLaunchConfigurationTab {

	private static final String TAB_NAME = "Startup";
	private static final String TAB_ID = Activator.PLUGIN_ID + ".ui.startuptab";

	private static final boolean DEBUG = false;

	private Text initCommands;
	// Text delay;
	// Button doReset;
	// Button doHalt;

	private Button doFirstReset;
	private Text firstResetType;
	private Text firstResetSpeed;

	private Button interfaceSpeedAuto;
	private Button interfaceSpeedAdaptive;
	private Button interfaceSpeedFixed;
	private Text interfaceSpeedFixedValue;

	private Button doSecondReset;
	private Text secondResetType;
	private Label secondResetWarning;

	private Button enableFlashBreakpoints;

	private Button enableSemihosting;

	private Button semihostingTelnet;
	private Button semihostingGdbClient;

	private Button enableSwo;
	private Text swoEnableTargetCpuFreq;
	private Text swoEnableTargetSwoFreq;
	private Text swoEnableTargetPortMask;

	private Button loadExecutable;
	private Text imageFileName;
	private Button imageFileBrowseWs;
	private Button imageFileBrowse;
	private Text imageOffset;

	private Button loadSymbols;
	private Text symbolsFileName;

	private Button symbolsFileBrowseWs;
	private Button symbolsFileBrowse;
	private Text symbolsOffset;

	private Button setPcRegister;
	private Text pcRegister;

	private Button setStopAt;
	private Text stopAt;

	private Text runCommands;
	private Button doContinue;
	private Button doDebugInRam;

	// New GUI added to address bug 310304
	private Button useProjectBinaryForImage;
	private Button useFileForImage;
	private Button useProjectBinaryForSymbols;
	private Button useFileForSymbols;
	private Label imageOffsetLabel;
	private Label symbolsOffsetLabel;
	private Label projBinaryLabel1;
	private Label projBinaryLabel2;

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

		sc.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
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

			doFirstReset = new Button(local, SWT.CHECK);
			doFirstReset.setText(Messages
					.getString("StartupTab.doFirstReset_Text"));
			doFirstReset.setToolTipText(Messages
					.getString("StartupTab.doFirstReset_ToolTipText"));

			Label label = new Label(local, SWT.NONE);
			label.setText(Messages.getString("StartupTab.firstResetType_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.firstResetType_ToolTipText"));

			firstResetType = new Text(local, SWT.BORDER);
			firstResetType.setToolTipText(Messages
					.getString("StartupTab.firstResetType_ToolTipText"));
			gd = new GridData();
			gd.widthHint = 100;
			firstResetType.setLayoutData(gd);

			label = new Label(local, SWT.NONE);
			label.setText(Messages.getString("StartupTab.firstResetSpeed_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.firstResetSpeed_ToolTipText"));

			firstResetSpeed = new Text(local, SWT.BORDER);
			firstResetSpeed.setToolTipText(Messages
					.getString("StartupTab.firstResetSpeed_ToolTipText"));
			gd = new GridData();
			gd.widthHint = 40;
			firstResetSpeed.setLayoutData(gd);

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

			interfaceSpeedAuto = new Button(local, SWT.RADIO);
			interfaceSpeedAuto.setText(Messages
					.getString("StartupTab.interfaceSpeedAuto_Text"));

			interfaceSpeedAdaptive = new Button(local, SWT.RADIO);
			interfaceSpeedAdaptive.setText(Messages
					.getString("StartupTab.interfaceSpeedAdaptive_Text"));

			interfaceSpeedFixed = new Button(local, SWT.RADIO);
			interfaceSpeedFixed.setText(Messages
					.getString("StartupTab.interfaceSpeedFixed_Text"));

			interfaceSpeedFixedValue = new Text(local, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 40;
			interfaceSpeedFixedValue.setLayoutData(gd);

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

			enableFlashBreakpoints = new Button(local, SWT.CHECK);
			enableFlashBreakpoints.setText(Messages
					.getString("StartupTab.enableFlashBreakpoints_Text"));
			enableFlashBreakpoints
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

			enableSemihosting = new Button(local, SWT.CHECK);
			enableSemihosting.setText(Messages
					.getString("StartupTab.enableSemihosting_Text"));
			enableSemihosting.setToolTipText(Messages
					.getString("StartupTab.enableSemihosting_ToolTipText"));

			Label label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.enableSemihostingRouted_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.enableSemihostingRouted_ToolTipText"));

			semihostingTelnet = new Button(local, SWT.CHECK);
			semihostingTelnet.setText(Messages
					.getString("StartupTab.semihostingTelnet_Text"));
			semihostingTelnet.setToolTipText(Messages
					.getString("StartupTab.semihostingTelnet_ToolTipText"));

			semihostingGdbClient = new Button(local, SWT.CHECK);
			semihostingGdbClient.setText(Messages
					.getString("StartupTab.semihostingGdbClient_Text"));
			semihostingGdbClient.setToolTipText(Messages
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

			enableSwo = new Button(local, SWT.CHECK);
			enableSwo.setText(Messages.getString("StartupTab.enableSwo_Text"));
			enableSwo.setToolTipText(Messages
					.getString("StartupTab.enableSwo_ToolTipText"));

			Label label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.swoEnableTargetCpuFreq_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.swoEnableTargetCpuFreq_ToolTipText"));

			swoEnableTargetCpuFreq = new Text(local, SWT.BORDER);
			swoEnableTargetCpuFreq.setToolTipText(Messages
					.getString("StartupTab.swoEnableTargetCpuFreq_ToolTipText"));
			gd = new GridData();
			gd.widthHint = 80;
			swoEnableTargetCpuFreq.setLayoutData(gd);

			label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.swoEnableTargetCpuFreqUnit_Text"));

			label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.swoEnableTargetSwoFreq_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.swoEnableTargetSwoFreq_ToolTipText"));

			swoEnableTargetSwoFreq = new Text(local, SWT.BORDER);
			swoEnableTargetSwoFreq.setToolTipText(Messages
					.getString("StartupTab.swoEnableTargetSwoFreq_ToolTipText"));
			gd = new GridData();
			gd.widthHint = 60;
			swoEnableTargetSwoFreq.setLayoutData(gd);

			label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.swoEnableTargetSwoFreqUnit_Text"));

			label = new Label(local, SWT.NONE);
			label.setText(Messages
					.getString("StartupTab.swoEnableTargetPortMask_Text"));
			label.setToolTipText(Messages
					.getString("StartupTab.swoEnableTargetPortMask_ToolTipText"));

			swoEnableTargetPortMask = new Text(local, SWT.BORDER);
			swoEnableTargetPortMask.setToolTipText(Messages
					.getString("StartupTab.swoEnableTargetPortMask_ToolTipText"));
			gd = new GridData();
			gd.widthHint = 80;
			swoEnableTargetPortMask.setLayoutData(gd);
		}

		{
			initCommands = new Text(comp, SWT.MULTI | SWT.WRAP | SWT.BORDER
					| SWT.V_SCROLL);
			initCommands.setToolTipText(Messages
					.getString("StartupTab.initCommands_ToolTipText"));
			gd = new GridData(GridData.FILL_BOTH);
			gd.heightHint = 60;
			initCommands.setLayoutData(gd);
		}

		// Actions
		doFirstReset.addSelectionListener(new SelectionAdapter() {
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

		firstResetType.addVerifyListener(numericVerifyListener);
		firstResetType.addModifyListener(scheduleUpdateJobModifyListener);

		firstResetSpeed.addVerifyListener(numericVerifyListener);
		firstResetSpeed.addModifyListener(scheduleUpdateJobModifyListener);

		interfaceSpeedAuto.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				interfaceSpeedFixedValue.setEnabled(false);
				scheduleUpdateJob();
			}
		});

		interfaceSpeedAdaptive.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				interfaceSpeedFixedValue.setEnabled(false);
				scheduleUpdateJob();
			}
		});

		interfaceSpeedFixed.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				interfaceSpeedFixedValue.setEnabled(true);
				scheduleUpdateJob();
			}
		});

		interfaceSpeedFixedValue.addVerifyListener(numericVerifyListener);

		interfaceSpeedFixedValue
				.addModifyListener(scheduleUpdateJobModifyListener);

		enableSemihosting.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doEnableSemihostingChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		semihostingTelnet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});
		semihostingGdbClient.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		enableSwo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doEnableSwoChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		swoEnableTargetCpuFreq.addVerifyListener(numericVerifyListener);
		swoEnableTargetCpuFreq.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {

				scheduleUpdateJob();
			}
		});

		swoEnableTargetSwoFreq.addVerifyListener(numericVerifyListener);
		swoEnableTargetSwoFreq.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {

				scheduleUpdateJob();
			}
		});

		swoEnableTargetPortMask.addVerifyListener(hexVerifyListener);
		swoEnableTargetPortMask
				.addModifyListener(scheduleUpdateJobModifyListener);

		initCommands.addModifyListener(scheduleUpdateJobModifyListener);
	}

	private void doFirstResetChanged() {

		boolean enabled = doFirstReset.getSelection();

		firstResetType.setEnabled(enabled);
		firstResetSpeed.setEnabled(enabled);
	}

	private void doEnableSemihostingChanged() {

		boolean enabled = enableSemihosting.getSelection();

		semihostingTelnet.setEnabled(enabled);
		semihostingGdbClient.setEnabled(enabled);
	}

	private void doEnableSwoChanged() {

		boolean enabled = enableSwo.getSelection() && enableSwo.getEnabled();

		swoEnableTargetCpuFreq.setEnabled(enabled);
		swoEnableTargetSwoFreq.setEnabled(enabled);
		swoEnableTargetPortMask.setEnabled(enabled);
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
			loadSymbols = new Button(comp, SWT.CHECK);
			loadSymbols.setText(Messages
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
				useProjectBinaryForSymbols = new Button(local, SWT.RADIO);
				useProjectBinaryForSymbols.setText(Messages
						.getString("StartupTab.useProjectBinary_Label"));
				useProjectBinaryForSymbols.setToolTipText(Messages
						.getString("StartupTab.useProjectBinary_ToolTip"));

				projBinaryLabel2 = new Label(local, SWT.NONE);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = ((GridLayout) local.getLayout()).numColumns - 1;
				projBinaryLabel2.setLayoutData(gd);
			}

			{
				useFileForSymbols = new Button(local, SWT.RADIO);
				useFileForSymbols.setText(Messages
						.getString("StartupTab.useFile_Label"));

				symbolsFileName = new Text(local, SWT.BORDER);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				symbolsFileName.setLayoutData(gd);

				symbolsFileBrowseWs = createPushButton(local,
						Messages.getString("StartupTab.FileBrowseWs_Label"),
						null);

				symbolsFileBrowse = createPushButton(local,
						Messages.getString("StartupTab.FileBrowse_Label"), null);
			}

			{
				symbolsOffsetLabel = new Label(local, SWT.NONE);
				symbolsOffsetLabel.setText(Messages
						.getString("StartupTab.symbolsOffsetLabel_Text"));

				symbolsOffset = new Text(local, SWT.BORDER);
				gd = new GridData();
				gd.horizontalSpan = ((GridLayout) local.getLayout()).numColumns - 1;
				gd.widthHint = 100;
				symbolsOffset.setLayoutData(gd);
			}
		}

		{
			loadExecutable = new Button(comp, SWT.CHECK);
			loadExecutable.setText(Messages
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
				useProjectBinaryForImage = new Button(local, SWT.RADIO);
				useProjectBinaryForImage.setText(Messages
						.getString("StartupTab.useProjectBinary_Label"));
				useProjectBinaryForImage.setToolTipText(Messages
						.getString("StartupTab.useProjectBinary_ToolTipText"));

				projBinaryLabel1 = new Label(local, SWT.NONE);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = ((GridLayout) local.getLayout()).numColumns - 1;
				projBinaryLabel1.setLayoutData(gd);
			}

			{
				useFileForImage = new Button(local, SWT.RADIO);
				useFileForImage.setText(Messages
						.getString("StartupTab.useFile_Label"));

				imageFileName = new Text(local, SWT.BORDER);
				gd = new GridData(GridData.FILL_HORIZONTAL);
				imageFileName.setLayoutData(gd);

				imageFileBrowseWs = createPushButton(local,
						Messages.getString("StartupTab.FileBrowseWs_Label"),
						null);

				imageFileBrowse = createPushButton(local,
						Messages.getString("StartupTab.FileBrowse_Label"), null);
			}

			{
				imageOffsetLabel = new Label(local, SWT.NONE);
				imageOffsetLabel.setText(Messages
						.getString("StartupTab.imageOffsetLabel_Text"));

				imageOffset = new Text(local, SWT.BORDER);
				gd = new GridData();
				gd.horizontalSpan = ((GridLayout) local.getLayout()).numColumns - 1;
				gd.widthHint = ((GridData) symbolsOffset.getLayoutData()).widthHint;
				imageOffset.setLayoutData(gd);
			}
		}

		// ----- Actions ------------------------------------------------------

		loadExecutable.addSelectionListener(new SelectionAdapter() {
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

		useProjectBinaryForImage.addSelectionListener(radioButtonListener);

		useFileForImage.addSelectionListener(radioButtonListener);
		useProjectBinaryForSymbols.addSelectionListener(radioButtonListener);
		useFileForSymbols.addSelectionListener(radioButtonListener);

		imageFileName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		imageFileBrowseWs.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseWsButtonSelected(Messages
						.getString("StartupTab.imageFileBrowseWs_Title"),
						imageFileName);
			}
		});

		imageFileBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseButtonSelected(
						Messages.getString("StartupTab.imageFileBrowse_Title"),
						imageFileName);
			}
		});

		imageOffset.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character)
						|| Character.isISOControl(e.character) || "abcdef"
						.contains(String.valueOf(e.character).toLowerCase()));
			}
		});
		imageOffset.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		loadSymbols.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadSymbolsChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		symbolsFileName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		symbolsFileBrowseWs.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseWsButtonSelected(Messages
						.getString("StartupTab.symbolsFileBrowseWs_Title"),
						symbolsFileName);
			}
		});

		symbolsFileBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseButtonSelected(Messages
						.getString("StartupTab.symbolsFileBrowse_Title"),
						symbolsFileName);
			}
		});

		symbolsOffset.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character)
						|| Character.isISOControl(e.character) || "abcdef"
						.contains(String.valueOf(e.character).toLowerCase()));
			}
		});
		symbolsOffset.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

	}

	private void updateUseFileEnablement() {

		boolean enabled = loadExecutable.getSelection()
				&& useFileForImage.getSelection();
		imageFileName.setEnabled(enabled);
		imageFileBrowseWs.setEnabled(enabled);
		imageFileBrowse.setEnabled(enabled);

		enabled = loadSymbols.getSelection()
				&& useFileForSymbols.getSelection();
		symbolsFileName.setEnabled(enabled);
		symbolsFileBrowseWs.setEnabled(enabled);
		symbolsFileBrowse.setEnabled(enabled);
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

		doDebugInRam = new Button(comp, SWT.CHECK);
		doDebugInRam
				.setText(Messages.getString("StartupTab.doDebugInRam_Text"));
		doDebugInRam.setToolTipText(Messages
				.getString("StartupTab.doDebugInRam_ToolTipText"));

		// ----- Actions ------------------------------------------------------

		doDebugInRam.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduleUpdateJob();
			}
		});
	}

	private void doSecondResetChanged() {
		boolean enabled = doSecondReset.getSelection();
		secondResetType.setEnabled(enabled);
		secondResetWarning.setEnabled(enabled);
	}

	private void loadExecutableChanged() {
		boolean enabled = loadExecutable.getSelection();
		useProjectBinaryForImage.setEnabled(enabled);
		useFileForImage.setEnabled(enabled);
		imageOffset.setEnabled(enabled);
		imageOffsetLabel.setEnabled(enabled);
		updateUseFileEnablement();
	}

	private void loadSymbolsChanged() {
		boolean enabled = loadSymbols.getSelection();
		useProjectBinaryForSymbols.setEnabled(enabled);
		useFileForSymbols.setEnabled(enabled);
		symbolsOffset.setEnabled(enabled);
		symbolsOffsetLabel.setEnabled(enabled);
		updateUseFileEnablement();
	}

	private void pcRegisterChanged() {
		pcRegister.setEnabled(setPcRegister.getSelection());
	}

	private void stopAtChanged() {
		stopAt.setEnabled(setStopAt.getSelection());
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
			doSecondReset = new Button(comp, SWT.CHECK);
			doSecondReset.setText(Messages
					.getString("StartupTab.doSecondReset_Text"));
			doSecondReset.setToolTipText(Messages
					.getString("StartupTab.doSecondReset_ToolTipText"));

			Label label = new Label(comp, SWT.NONE);
			label.setText(Messages.getString("StartupTab.secondResetType_Text"));

			secondResetType = new Text(comp, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 100;
			secondResetType.setLayoutData(gd);

			secondResetWarning = new Label(comp, SWT.NONE);
			secondResetWarning.setText(Messages
					.getString("StartupTab.secondResetWarning_Text"));
			gd = new GridData();
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 3;
			secondResetWarning.setLayoutData(gd);
		}

		{
			runCommands = new Text(comp, SWT.MULTI | SWT.WRAP | SWT.BORDER
					| SWT.V_SCROLL);
			runCommands.setToolTipText(Messages
					.getString("StartupTab.runCommands_ToolTipText"));
			gd = new GridData(GridData.FILL_BOTH);
			gd.heightHint = 60;
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns;
			runCommands.setLayoutData(gd);
		}

		{
			setPcRegister = new Button(comp, SWT.CHECK);
			setPcRegister.setText(Messages
					.getString("StartupTab.setPcRegister_Text"));
			setPcRegister.setToolTipText(Messages
					.getString("StartupTab.setPcRegister_ToolTipText"));

			pcRegister = new Text(comp, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 100;
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			pcRegister.setLayoutData(gd);
		}

		{
			setStopAt = new Button(comp, SWT.CHECK);
			setStopAt.setText(Messages.getString("StartupTab.setStopAt_Text"));
			setStopAt.setToolTipText(Messages
					.getString("StartupTab.setStopAt_ToolTipText"));

			stopAt = new Text(comp, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 100;
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns - 1;
			stopAt.setLayoutData(gd);
		}

		{
			doContinue = new Button(comp, SWT.CHECK);
			doContinue
					.setText(Messages.getString("StartupTab.doContinue_Text"));
			doContinue.setToolTipText(Messages
					.getString("StartupTab.doContinue_ToolTipText"));

			gd = new GridData();
			gd.horizontalSpan = ((GridLayout) comp.getLayout()).numColumns;
			doContinue.setLayoutData(gd);
		}

		// ---- Actions -------------------------------------------------------

		doSecondReset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// doResetChanged();
				doSecondResetChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		secondResetType.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character) || Character
						.isISOControl(e.character));
			}
		});

		setPcRegister.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pcRegisterChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		pcRegister.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = (Character.isDigit(e.character)
						|| Character.isISOControl(e.character) || "abcdef"
						.contains(String.valueOf(e.character).toLowerCase()));
			}
		});

		pcRegister.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
		});

		setStopAt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stopAtChanged();
				scheduleUpdateJob(); // updateLaunchConfigurationDialog();
			}
		});

		stopAt.addModifyListener(new ModifyListener() {
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

		secondResetType.addModifyListener(scheduleUpdateJobModifyListener);

		runCommands.addModifyListener(scheduleUpdateJobModifyListener);

		doContinue.addSelectionListener(scheduleUpdateJobSelectionAdapter);
	}

	public void doConnectToRunningChanged(boolean flag) {

		// System.out.println(flag);
		doFirstReset.setEnabled(!flag);
		firstResetType.setEnabled(!flag);

		doSecondReset.setEnabled(!flag);
		secondResetType.setEnabled(!flag);
		secondResetWarning.setEnabled(!flag);

		loadExecutable.setEnabled(!flag);
	}

	// flag is true when interface is SWD
	public void doInterfaceSwdChanged(boolean flag) {

		enableSwo.setEnabled(flag);

		doEnableSwoChanged();
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		if (!super.isValid(launchConfig))
			return false;
		setErrorMessage(null);
		setMessage(null);

		if (loadExecutable.getSelection()) {
			if (!useProjectBinaryForImage.getSelection()) {
				if (imageFileName.getText().trim().length() == 0) {
					setErrorMessage(Messages
							.getString("StartupTab.imageFileName_not_specified"));
					return false;
				}

				try {
					String path = imageFileName.getText().trim();
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
		if (loadSymbols.getSelection()) {
			if (!useProjectBinaryForSymbols.getSelection()) {
				if (symbolsFileName.getText().trim().length() == 0) {
					setErrorMessage(Messages
							.getString("StartupTab.symbolsFileName_not_specified"));
					return false;
				}

				try {
					String path = symbolsFileName.getText().trim();
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

		if (setPcRegister.getSelection()) {
			if (pcRegister.getText().trim().length() == 0) {
				setErrorMessage(Messages
						.getString("StartupTab.pcRegister_not_specified"));
				return false;
			}
		} else {
			setErrorMessage(null);
		}
		if (setStopAt.getSelection()) {
			if (stopAt.getText().trim().length() == 0) {
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
				doFirstReset
						.setSelection(configuration.getAttribute(
								ConfigurationAttributes.DO_FIRST_RESET,
								booleanDefault));

				// Reset type
				stringDefault = WorkspacePersistentValues
						.getJLinkInitialResetType(ConfigurationAttributes.FIRST_RESET_TYPE_DEFAULT);
				firstResetType.setText(configuration
						.getAttribute(ConfigurationAttributes.FIRST_RESET_TYPE,
								stringDefault));

				intDefault = WorkspacePersistentValues
						.getJLinkInitialResetSpeed(ConfigurationAttributes.FIRST_RESET_SPEED_DEFAULT);

				// Type change from string to int, compatibility preserved
				try {
					int i = configuration.getAttribute(
							ConfigurationAttributes.FIRST_RESET_SPEED,
							intDefault);
					firstResetSpeed.setText(String.valueOf(i));
				} catch (CoreException e) {
					try {
						stringDefault = configuration
								.getAttribute(
										ConfigurationAttributes.FIRST_RESET_SPEED,
										String.valueOf(ConfigurationAttributes.FIRST_RESET_SPEED_DEFAULT));
						firstResetSpeed.setText(stringDefault);
					} catch (CoreException e2) {
						firstResetSpeed
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
					interfaceSpeedAuto.setSelection(true);
					interfaceSpeedFixedValue.setEnabled(false);

				} else if (ConfigurationAttributes.INTERFACE_SPEED_ADAPTIVE
						.equals(physicalInterfaceSpeed)) {
					interfaceSpeedAdaptive.setSelection(true);
					interfaceSpeedFixedValue.setEnabled(false);
				} else {
					try {
						Integer.parseInt(physicalInterfaceSpeed);
						interfaceSpeedFixed.setSelection(true);
						interfaceSpeedFixedValue.setEnabled(true);
						interfaceSpeedFixedValue
								.setText(physicalInterfaceSpeed);
					} catch (NumberFormatException e) {
						String message = "unknown interface speed "
								+ physicalInterfaceSpeed + ", using auto";
						Activator.log(message);
						interfaceSpeedAuto.setSelection(true);
						interfaceSpeedFixedValue.setEnabled(false);
					}
				}

				// Enable flash breakpoints
				booleanDefault = WorkspacePersistentValues
						.getJLinkEnableFlashBreakpoints(ConfigurationAttributes.ENABLE_FLASH_BREAKPOINTS_DEFAULT);
				enableFlashBreakpoints.setSelection(configuration.getAttribute(
						ConfigurationAttributes.ENABLE_FLASH_BREAKPOINTS,
						booleanDefault));

				// Enable semihosting
				booleanDefault = WorkspacePersistentValues
						.getJLinkEnableSemihosting(ConfigurationAttributes.ENABLE_SEMIHOSTING_DEFAULT);
				enableSemihosting.setSelection(configuration.getAttribute(
						ConfigurationAttributes.ENABLE_SEMIHOSTING,
						booleanDefault));

				booleanDefault = WorkspacePersistentValues
						.getJLinkSemihostingTelnet(ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_TELNET_DEFAULT);
				semihostingTelnet
						.setSelection(configuration
								.getAttribute(
										ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_TELNET,
										booleanDefault));

				booleanDefault = WorkspacePersistentValues
						.getJLinkSemihostingClient(ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_GDBCLIENT_DEFAULT);
				semihostingGdbClient
						.setSelection(configuration
								.getAttribute(
										ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_GDBCLIENT,
										booleanDefault));

				booleanDefault = WorkspacePersistentValues
						.getJLinkEnableSwo(ConfigurationAttributes.ENABLE_SWO_DEFAULT);
				// System.out.println("getJLinkEnableSwo()="+booleanDefault+" "+configuration.getName());
				enableSwo.setSelection(configuration.getAttribute(
						ConfigurationAttributes.ENABLE_SWO, booleanDefault));

				intDefault = WorkspacePersistentValues
						.getJLinkSwoEnableTargetCpuFreq(ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ_DEFAULT);
				swoEnableTargetCpuFreq
						.setText(String.valueOf(configuration
								.getAttribute(
										ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ,
										intDefault)));
				intDefault = WorkspacePersistentValues
						.getJLinkSwoEnableTargetSwoFreq(ConfigurationAttributes.SWO_ENABLETARGET_SWOFREQ_DEFAULT);
				swoEnableTargetSwoFreq
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

				swoEnableTargetPortMask.setText(sValue);

				// Other commands
				stringDefault = WorkspacePersistentValues
						.getJLinkInitOther(ConfigurationAttributes.OTHER_INIT_COMMANDS_DEFAULT);
				initCommands.setText(configuration.getAttribute(
						ConfigurationAttributes.OTHER_INIT_COMMANDS,
						stringDefault));
			}

			// Load Symbols & Image
			{
				loadSymbols.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_LOAD_SYMBOLS,
						IGDBJtagConstants.DEFAULT_LOAD_SYMBOLS));
				useProjectBinaryForSymbols
						.setSelection(configuration
								.getAttribute(
										IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS,
										IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_SYMBOLS));
				useFileForSymbols.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_USE_FILE_FOR_SYMBOLS,
						IGDBJtagConstants.DEFAULT_USE_FILE_FOR_SYMBOLS));
				symbolsFileName.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME,
						IGDBJtagConstants.DEFAULT_SYMBOLS_FILE_NAME));
				symbolsOffset.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_SYMBOLS_OFFSET,
						IGDBJtagConstants.DEFAULT_SYMBOLS_OFFSET));

				loadExecutable.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_LOAD_IMAGE,
						IGDBJtagConstants.DEFAULT_LOAD_IMAGE));
				useProjectBinaryForImage
						.setSelection(configuration
								.getAttribute(
										IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE,
										IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_IMAGE));
				useFileForImage.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_USE_FILE_FOR_IMAGE,
						IGDBJtagConstants.DEFAULT_USE_FILE_FOR_IMAGE));
				imageFileName.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_IMAGE_FILE_NAME,
						IGDBJtagConstants.DEFAULT_IMAGE_FILE_NAME));
				imageOffset.setText(configuration.getAttribute(
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
					projBinaryLabel1.setText(programName);
					projBinaryLabel2.setText(programName);
				}
			}

			// Runtime Options
			{
				booleanDefault = WorkspacePersistentValues
						.getJLinkDebugInRam(ConfigurationAttributes.DO_DEBUG_IN_RAM_DEFAULT);
				doDebugInRam.setSelection(configuration
						.getAttribute(ConfigurationAttributes.DO_DEBUG_IN_RAM,
								booleanDefault));
			}

			// Run Commands
			{
				// Do pre-run reset
				booleanDefault = WorkspacePersistentValues
						.getJLinkDoPreRunReset(ConfigurationAttributes.DO_SECOND_RESET_DEFAULT);
				doSecondReset.setSelection(configuration
						.getAttribute(ConfigurationAttributes.DO_SECOND_RESET,
								booleanDefault));

				// Pre-run reset type
				stringDefault = WorkspacePersistentValues
						.getJLinkPreRunResetType(ConfigurationAttributes.SECOND_RESET_TYPE_DEFAULT);
				secondResetType.setText(configuration.getAttribute(
						ConfigurationAttributes.SECOND_RESET_TYPE,
						stringDefault));

				// Other commands
				stringDefault = WorkspacePersistentValues
						.getJLinkPreRunOther(ConfigurationAttributes.OTHER_RUN_COMMANDS_DEFAULT);
				runCommands.setText(configuration.getAttribute(
						ConfigurationAttributes.OTHER_RUN_COMMANDS,
						stringDefault));

				setPcRegister.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_SET_PC_REGISTER,
						IGDBJtagConstants.DEFAULT_SET_PC_REGISTER));
				pcRegister.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_PC_REGISTER,
						IGDBJtagConstants.DEFAULT_PC_REGISTER));

				setStopAt.setSelection(configuration.getAttribute(
						IGDBJtagConstants.ATTR_SET_STOP_AT,
						ConfigurationAttributes.DO_STOP_AT_DEFAULT));
				stopAt.setText(configuration.getAttribute(
						IGDBJtagConstants.ATTR_STOP_AT,
						ConfigurationAttributes.STOP_AT_NAME_DEFAULT));

				// Do continue
				doContinue.setSelection(configuration.getAttribute(
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
			booleanValue = doFirstReset.getSelection();
			configuration.setAttribute(ConfigurationAttributes.DO_FIRST_RESET,
					booleanValue);
			WorkspacePersistentValues.putJLinkDoInitialReset(booleanValue);

			// First reset type
			stringValue = firstResetType.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.FIRST_RESET_TYPE, stringValue);
			WorkspacePersistentValues.putJLinkInitialResetType(stringValue);

			// First reset speed
			try {
				intValue = Integer.valueOf(firstResetSpeed.getText());
			} catch (NumberFormatException e) {
				intValue = ConfigurationAttributes.FIRST_RESET_SPEED_DEFAULT;
			}
			configuration.setAttribute(
					ConfigurationAttributes.FIRST_RESET_SPEED, intValue);
			WorkspacePersistentValues.putJLinkInitialResetSpeed(intValue);

			// Interface speed
			stringValue = ConfigurationAttributes.INTERFACE_SPEED_DEFAULT;
			if (interfaceSpeedAuto.getSelection()) {
				stringValue = ConfigurationAttributes.INTERFACE_SPEED_AUTO;
			} else if (interfaceSpeedAdaptive.getSelection()) {
				stringValue = ConfigurationAttributes.INTERFACE_SPEED_ADAPTIVE;
			} else if (interfaceSpeedFixed.getSelection()) {
				stringValue = interfaceSpeedFixedValue.getText().trim();
			}
			configuration.setAttribute(ConfigurationAttributes.INTERFACE_SPEED,
					stringValue);
			WorkspacePersistentValues.putJLinkSpeed(stringValue);

			// Enable flash breakpoints
			booleanValue = enableFlashBreakpoints.getSelection();
			configuration.setAttribute(
					ConfigurationAttributes.ENABLE_FLASH_BREAKPOINTS,
					booleanValue);
			WorkspacePersistentValues
					.putJLinkEnableFlashBreakpoints(booleanValue);

			// Enable semihosting
			booleanValue = enableSemihosting.getSelection();
			configuration.setAttribute(
					ConfigurationAttributes.ENABLE_SEMIHOSTING, booleanValue);
			WorkspacePersistentValues.putJLinkEnableSemihosting(booleanValue);

			// Semihosting via telnet
			booleanValue = semihostingTelnet.getSelection();
			configuration.setAttribute(
					ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_TELNET,
					booleanValue);
			WorkspacePersistentValues.putJLinkSemihostingTelnet(booleanValue);

			// Semihosting via client
			booleanValue = semihostingGdbClient.getSelection();
			configuration
					.setAttribute(
							ConfigurationAttributes.ENABLE_SEMIHOSTING_IOCLIENT_GDBCLIENT,
							booleanValue);
			WorkspacePersistentValues.putJLinkSemihostingClient(booleanValue);

			// Enable swo
			booleanValue = enableSwo.getSelection();
			configuration.setAttribute(ConfigurationAttributes.ENABLE_SWO,
					booleanValue);
			// System.out.println("putJLinkEnableSwo "+booleanValue+" "+configuration.getName());
			WorkspacePersistentValues.putJLinkEnableSwo(booleanValue);

			// target speed
			try {
				intValue = Integer.parseInt(swoEnableTargetCpuFreq.getText());
			} catch (NumberFormatException e) {
				intValue = ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ_DEFAULT;
			}
			configuration.setAttribute(
					ConfigurationAttributes.SWO_ENABLETARGET_CPUFREQ, intValue);
			WorkspacePersistentValues.putJLinkSwoEnableTargetCpuFreq(intValue);

			// Swo speed
			try {
				intValue = Integer.parseInt(swoEnableTargetSwoFreq.getText());
			} catch (NumberFormatException e) {
				intValue = ConfigurationAttributes.SWO_ENABLETARGET_SWOFREQ_DEFAULT;
			}
			configuration.setAttribute(
					ConfigurationAttributes.SWO_ENABLETARGET_SWOFREQ, intValue);
			WorkspacePersistentValues.putJLinkSwoEnableTargetSwoFreq(intValue);

			// Swo port mask
			stringValue = swoEnableTargetPortMask.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.SWO_ENABLETARGET_PORTMASK,
					stringValue);
			WorkspacePersistentValues
					.putJLinkSwoEnableTargetPortMask(stringValue);

			// Other commands
			stringValue = initCommands.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.OTHER_INIT_COMMANDS, stringValue);
			WorkspacePersistentValues.putJLinkInitOther(stringValue);
		}

		// Load Symbols & Image...
		{
			configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_SYMBOLS,
					loadSymbols.getSelection());
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS,
					useProjectBinaryForSymbols.getSelection());
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_USE_FILE_FOR_SYMBOLS,
					useFileForSymbols.getSelection());
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME, symbolsFileName
							.getText().trim());

			configuration.setAttribute(IGDBJtagConstants.ATTR_SYMBOLS_OFFSET,
					symbolsOffset.getText());

			configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_IMAGE,
					loadExecutable.getSelection());
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE,
					useProjectBinaryForImage.getSelection());
			configuration.setAttribute(
					IGDBJtagConstants.ATTR_USE_FILE_FOR_IMAGE,
					useFileForImage.getSelection());
			configuration.setAttribute(IGDBJtagConstants.ATTR_IMAGE_FILE_NAME,
					imageFileName.getText().trim());

			configuration.setAttribute(IGDBJtagConstants.ATTR_IMAGE_OFFSET,
					imageOffset.getText());

		}

		// Runtime Options
		{
			booleanValue = doDebugInRam.getSelection();
			configuration.setAttribute(ConfigurationAttributes.DO_DEBUG_IN_RAM,
					booleanValue);
			WorkspacePersistentValues.putJLinkDebugInRam(booleanValue);
		}

		// Run Commands
		{
			// Pre-run reset
			booleanValue = doSecondReset.getSelection();
			configuration.setAttribute(ConfigurationAttributes.DO_SECOND_RESET,
					doSecondReset.getSelection());
			WorkspacePersistentValues.putJLinkDoPreRunReset(booleanValue);

			// reset type
			stringValue = secondResetType.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.SECOND_RESET_TYPE, stringValue);
			WorkspacePersistentValues.putJLinkPreRunResetType(stringValue);

			// Other commands
			stringValue = runCommands.getText().trim();
			configuration.setAttribute(
					ConfigurationAttributes.OTHER_RUN_COMMANDS, stringValue);
			WorkspacePersistentValues.putJLinkPreRunOther(stringValue);

			configuration.setAttribute(IGDBJtagConstants.ATTR_SET_PC_REGISTER,
					setPcRegister.getSelection());
			configuration.setAttribute(IGDBJtagConstants.ATTR_PC_REGISTER,
					pcRegister.getText());
			configuration.setAttribute(IGDBJtagConstants.ATTR_SET_STOP_AT,
					setStopAt.getSelection());
			configuration.setAttribute(IGDBJtagConstants.ATTR_STOP_AT,
					stopAt.getText());

			// Continue
			configuration.setAttribute(ConfigurationAttributes.DO_CONTINUE,
					doContinue.getSelection());
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
