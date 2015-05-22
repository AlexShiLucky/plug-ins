package ilg.gnuarmeclipse.managedbuild.cross.properties;

import ilg.gnuarmeclipse.core.EclipseUtils;
import ilg.gnuarmeclipse.core.ScopedPreferenceStoreWithoutDefaults;
import ilg.gnuarmeclipse.core.preferences.LabelFakeFieldEditor;
import ilg.gnuarmeclipse.core.ui.FieldEditorPropertyPage;
import ilg.gnuarmeclipse.managedbuild.cross.Activator;
import ilg.gnuarmeclipse.managedbuild.cross.Option;
import ilg.gnuarmeclipse.managedbuild.cross.ui.Messages;
import ilg.gnuarmeclipse.managedbuild.cross.ui.PersistentPreferences;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;

public class ProjectToolsPathPropertyPage extends FieldEditorPropertyPage {

	// ------------------------------------------------------------------------

	public static final String ID = "ilg.gnuarmeclipse.managedbuild.cross.properties.toolsPage";

	// ------------------------------------------------------------------------

	public ProjectToolsPathPropertyPage() {
		super(GRID);

		setDescription(Messages.ProjectToolsPathsPropertyPage_description);
	}

	// ------------------------------------------------------------------------

	protected IPreferenceStore doGetPreferenceStore() {

		Object element = getElement();
		if (element instanceof IProject) {
			return new ScopedPreferenceStoreWithoutDefaults(new ProjectScope(
					(IProject) element), Activator.PLUGIN_ID);
		}
		return null;
	}

	@Override
	protected void createFieldEditors() {

		FieldEditor buildToolsPathField = new DirectoryFieldEditor(
				PersistentPreferences.BUILD_TOOLS_PATH_KEY,
				Messages.ToolsPaths_label, getFieldEditorParent());
		addField(buildToolsPathField);

		Set<String> toolchainNames = new HashSet<String>();

		Object element = getElement();
		if (element instanceof IProject) {
			// TODO: get project toolchain name. How?
			IProject project = (IProject) element;
			IConfiguration[] configs = EclipseUtils
					.getConfigurationsForProject(project);
			if (configs != null) {
				for (int i = 0; i < configs.length; ++i) {
					IToolChain toolchain = configs[i].getToolChain();
					if (toolchain == null) {
						continue;
					}
					IOption option = toolchain
							.getOptionBySuperClassId(Option.OPTION_TOOLCHAIN_NAME);
					if (option == null) {
						continue;
					}
					try {
						String name = option.getStringValue();
						if (!name.isEmpty()){
							toolchainNames.add(name);
						}
					} catch (BuildException e) {
						;
					}
				}
			}
		}
		if (toolchainNames.isEmpty()) {
			toolchainNames.add(PersistentPreferences.getToolchainName());
		}

		for (String toolchainName : toolchainNames) {

			FieldEditor labelField = new LabelFakeFieldEditor(toolchainName,
					Messages.ToolsPaths_ToolchainName_label,
					getFieldEditorParent());
			addField(labelField);

			String key = PersistentPreferences.getToolchainKey(toolchainName);
			FieldEditor toolchainPathField = new DirectoryFieldEditor(key,
					Messages.ToolchainPaths_label, getFieldEditorParent());
			addField(toolchainPathField);
		}
	}

	// ------------------------------------------------------------------------
}
