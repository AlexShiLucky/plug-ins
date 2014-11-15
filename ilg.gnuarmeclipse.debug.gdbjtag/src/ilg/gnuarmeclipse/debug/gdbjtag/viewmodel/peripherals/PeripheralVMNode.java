/*******************************************************************************
 * Copyright (c) 2014 Liviu Ionescu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Liviu Ionescu - initial version
 *     		(many thanks to Code Red for providing the inspiration)
 *******************************************************************************/

package ilg.gnuarmeclipse.debug.gdbjtag.viewmodel.peripherals;

import ilg.gnuarmeclipse.core.EclipseUtils;
import ilg.gnuarmeclipse.debug.gdbjtag.Activator;
import ilg.gnuarmeclipse.debug.gdbjtag.datamodel.IPeripheralDMContext;
import ilg.gnuarmeclipse.debug.gdbjtag.datamodel.PeripheralDMContext;
import ilg.gnuarmeclipse.debug.gdbjtag.render.peripherals.PeripheralsColumnPresentation;
import ilg.gnuarmeclipse.debug.gdbjtag.services.IPeripheralsService;

import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.eclipse.cdt.dsf.concurrent.ConfinedToDsfExecutor;
import org.eclipse.cdt.dsf.concurrent.DsfRunnable;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.ui.concurrent.ViewerDataRequestMonitor;
import org.eclipse.cdt.dsf.ui.viewmodel.VMDelta;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.AbstractDMVMNode;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.AbstractDMVMProvider;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.IDMVMContext;
import org.eclipse.cdt.dsf.ui.viewmodel.properties.IElementPropertiesProvider;
import org.eclipse.cdt.dsf.ui.viewmodel.properties.IPropertiesUpdate;
import org.eclipse.cdt.dsf.ui.viewmodel.properties.LabelAttribute;
import org.eclipse.cdt.dsf.ui.viewmodel.properties.LabelColumnInfo;
import org.eclipse.cdt.dsf.ui.viewmodel.properties.LabelImage;
import org.eclipse.cdt.dsf.ui.viewmodel.properties.LabelText;
import org.eclipse.cdt.dsf.ui.viewmodel.properties.PropertiesBasedLabelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ICheckUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementEditor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Composite;

@SuppressWarnings("restriction")
public class PeripheralVMNode extends AbstractDMVMNode implements
		IElementLabelProvider, IElementPropertiesProvider, IElementEditor {

	// ------------------------------------------------------------------------
	// This node uses properties to store content, and these are the names.

	public static final String PROPERTY_NAME = "prop.name";
	public static final String PROPERTY_ADDRESS = "prop.address";
	public static final String PROPERTY_DESCRIPTION = "prop.description";

	public static final String PROPERTY_ISSYSTEM = "prop.isSystem";

	private IElementLabelProvider fLabelProvider = createLabelProvider();

	private IDMContext[] fPeripherals;

	// ------------------------------------------------------------------------

	/**
	 * Peripheral view model context. Contains a reference to the data model.
	 */
	public class PeripheralVMContext extends AbstractDMVMNode.DMVMContext {

		protected PeripheralVMContext(IDMContext context) {
			super(context);
			// System.out.println("PeripheralVMContext(" + context + ")");
		}
	}

	protected IDMVMContext createVMContext(IDMContext context) {
		return new PeripheralVMContext(context);
	}

	// ------------------------------------------------------------------------

	public PeripheralVMNode(AbstractDMVMProvider provider, DsfSession session,
			Class<? extends IDMContext> dmcClassType) {
		super(provider, session, dmcClassType);
	}

	public PeripheralVMNode(AbstractDMVMProvider provider, DsfSession session) {
		super(provider, session, IPeripheralDMContext.class);
	}

	@Override
	public int getDeltaFlags(Object event) {

		if ((event instanceof IRunControl.ISuspendedDMEvent)) {
			return IModelDelta.CONTENT;
		}

		return 0;
	}

	@Override
	public void buildDelta(Object event, VMDelta parent, int nodeOffset,
			RequestMonitor requestMonitor) {

		if ((event instanceof IRunControl.ISuspendedDMEvent)) {
			parent.setFlags(parent.getFlags() | IModelDelta.CONTENT);
		}
		requestMonitor.done();
	}

	@Override
	public CellEditor getCellEditor(IPresentationContext context,
			String columnId, Object element, Composite parent) {

		return null; // No cell editor.
	}

	@Override
	public ICellModifier getCellModifier(IPresentationContext context,
			Object element) {

		return null; // No cell modifier.
	}

	@Override
	public void update(final IPropertiesUpdate[] updates) {

		System.out.println("PeripheralVMNode.update() properties " + this
				+ ", " + updates.length + " objs");

		try {
			getSession().getExecutor().execute(new DsfRunnable() {

				public void run() {
					updatePropertiesInSessionThread(updates);
				}
			});
		} catch (RejectedExecutionException e) {
			for (IPropertiesUpdate update : updates)
				handleFailedUpdate(update);
		}
	}

	@Override
	public void update(ILabelUpdate[] updates) {

		System.out.println("PeripheralVMNode.update() labels " + this + ", "
				+ updates.length + " objs");

		{
			for (int i = 0; i < updates.length; i++) {

				ILabelUpdate update = updates[i];
				IPresentationContext presentationContext = update
						.getPresentationContext();
				TreePath path = update.getElementPath();
				if (((update instanceof ICheckUpdate))
						&& (Boolean.TRUE.equals(presentationContext
								.getProperty("org.eclipse.debug.ui.check"))))
					try {
						boolean checked = getChecked(path, presentationContext);
						boolean grayed = getGrayed(path, presentationContext);

						// Update the check button
						((ICheckUpdate) update).setChecked(checked, grayed);
					} catch (CoreException e) {
					}
			}
		}
		// Update the tree content, using the updates
		fLabelProvider.update(updates);
	}

	@Override
	protected boolean checkUpdate(IViewerUpdate update) {

		// No longer used, prent check is enough
		if (!super.checkUpdate(update))
			return false;

		return true;
	}

	@Override
	protected void updateElementsInSessionThread(final IChildrenUpdate update) {

		System.out.println("updateElementsInSessionThread() " + this + " "
				+ update);

		DsfServicesTracker tracker = getServicesTracker();
		IPeripheralsService peripheralsService = (IPeripheralsService) tracker
				.getService(IPeripheralsService.class);
		System.out.println("got service " + peripheralsService);

		IRunControl.IContainerDMContext containerDMContext = (IRunControl.IContainerDMContext) findDmcInPath(
				update.getViewerInput(), update.getElementPath(),
				IRunControl.IContainerDMContext.class);

		if ((peripheralsService == null) || (containerDMContext == null)) {
			// Leave the view empty. This also happens after closing the
			// session.
			handleFailedUpdate(update);
			return;
		}

		if (fPeripherals != null) {
			// On subsequent calls, use cached values.
			fillUpdateWithVMCs(update, fPeripherals);
			update.done();
			return;
		}

		// Get peripherals only on first call.
		peripheralsService.getPeripherals(containerDMContext,
				new ViewerDataRequestMonitor<IPeripheralDMContext[]>(
						getSession().getExecutor(), update) {

					public void handleCompleted() {

						if (isSuccess()) {
							fPeripherals = (IDMContext[]) getData();
							fillUpdateWithVMCs(update, fPeripherals);
							update.done();
						} else {
							EclipseUtils.showStatusErrorMessage(getStatus()
									.getMessage());
							handleFailedUpdate(update);
						}
					}
				});
	}

	@ConfinedToDsfExecutor("getSession().getExecutor()")
	protected void updatePropertiesInSessionThread(IPropertiesUpdate[] updates) {

		System.out
				.println("PeripheralVMNode.updatePropertiesInSessionThread() "
						+ this + ", " + updates.length + " objs");

		for (final IPropertiesUpdate update : updates) {

			IPeripheralDMContext peripheralDMContext = (IPeripheralDMContext) findDmcInPath(
					update.getViewerInput(), update.getElementPath(),
					IPeripheralDMContext.class);

			if (peripheralDMContext == null) {
				handleFailedUpdate(update);
				return;
			}

			setProperties(update, peripheralDMContext);
			// System.out.println("updatePropertiesInSessionThread() "
			// + propertiesUpdate);
			update.done();
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Create the label provider, that will assign content to the table columns
	 * from the properties of the node.
	 * 
	 * @return the label provider.
	 */
	protected IElementLabelProvider createLabelProvider() {

		PropertiesBasedLabelProvider labelProvider = new PropertiesBasedLabelProvider();

		LabelAttribute labelAttributes[];
		LabelColumnInfo labelColumnInfo;

		LabelImage labelImage = new LabelImage() {

			@Override
			public void updateAttribute(ILabelUpdate update, int columnIndex,
					IStatus status, Map<String, Object> properties) {

				ImageDescriptor descriptor = null;
				Boolean isSystem = (Boolean) properties.get(PROPERTY_ISSYSTEM);
				if (isSystem != null && isSystem.booleanValue()) {
					descriptor = Activator.imageDescriptorFromPlugin(
							Activator.PLUGIN_ID, "icons/system_peripheral.png");
				} else {
					descriptor = Activator.imageDescriptorFromPlugin(
							Activator.PLUGIN_ID, "icons/peripheral.png");
				}

				if (descriptor != null) {
					update.setImageDescriptor(descriptor, columnIndex);
				}
			}
		};

		// The PROPERTY_ISSYSTEM was added, although not used here, because
		// it needs to be referred somewhere to be available for tests in the
		// above updateAttribute().
		labelAttributes = new LabelAttribute[] {
				new LabelText("{0}", new String[] { PROPERTY_NAME,
						PROPERTY_ISSYSTEM }), labelImage };
		labelColumnInfo = new LabelColumnInfo(labelAttributes);

		// Define content for "Peripheral" column
		labelProvider.setColumnInfo(
				PeripheralsColumnPresentation.COLUMN_PERIPHERAL_ID,
				labelColumnInfo);

		labelAttributes = new LabelAttribute[] { new LabelText("{0}",
				new String[] { PROPERTY_ADDRESS }) };
		labelColumnInfo = new LabelColumnInfo(labelAttributes);

		// Define content for "Address" column
		labelProvider.setColumnInfo(
				PeripheralsColumnPresentation.COLUMN_ADDRESS_ID,
				labelColumnInfo);

		labelAttributes = new LabelAttribute[] { new LabelText("{0}",
				new String[] { PROPERTY_DESCRIPTION }) };
		labelColumnInfo = new LabelColumnInfo(labelAttributes);

		// Define content for "Description" column
		labelProvider.setColumnInfo(
				PeripheralsColumnPresentation.COLUMN_DESCRIPTION_ID,
				labelColumnInfo);

		return labelProvider;
	}

	// ------------------------------------------------------------------------

	/**
	 * Fill-in the view node properties from a data view context.
	 * 
	 * @param update
	 *            the properties object.
	 * @param context
	 *            the data model context.
	 */
	protected void setProperties(IPropertiesUpdate update,
			IPeripheralDMContext context) {

		assert (context != null);

		update.setProperty(PROPERTY_NAME, context.getName());
		update.setProperty(PROPERTY_ADDRESS, context.getHexAddress());
		update.setProperty(PROPERTY_DESCRIPTION, context.getDescription());

		update.setProperty(PROPERTY_ISSYSTEM, new Boolean(context.isSystem()));
	}

	/**
	 * Tell if the peripheral should be displayed as checked, by testing if the
	 * peripheral is shown in the memory monitor window.
	 * 
	 * @param treePath
	 *            the peripheral path.
	 * @param presentationContext
	 *            the presentation context (unused).
	 * @return true if the peripheral should be checked.
	 * @throws CoreException
	 */
	protected boolean getChecked(TreePath treePath,
			IPresentationContext presentationContext) throws CoreException {

		Object pathSegment = treePath.getLastSegment();
		if ((pathSegment instanceof PeripheralVMContext)) {
			PeripheralVMContext peripheralVMContext = (PeripheralVMContext) pathSegment;
			PeripheralDMContext peripheralDMContext = (PeripheralDMContext) peripheralVMContext
					.getDMContext();

			// System.out.println("getChecked()="
			// + peripheralDMContext.hasMemoryMonitor() + " "
			// + peripheralDMContext);
			return peripheralDMContext.hasMemoryMonitor();
		}
		return false;
	}

	protected boolean getGrayed(TreePath treePath,
			IPresentationContext presentationContext) throws CoreException {
		return false;
	}

	// ------------------------------------------------------------------------

	public String toString() {
		return "PeripheralVMNode(" + getSession().getId() + ")";
	}

	// ------------------------------------------------------------------------
}
