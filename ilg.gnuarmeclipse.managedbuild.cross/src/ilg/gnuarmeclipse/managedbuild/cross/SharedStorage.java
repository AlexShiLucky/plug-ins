package ilg.gnuarmeclipse.managedbuild.cross;

import org.eclipse.cdt.core.templateengine.SharedDefaults;

public class SharedStorage {

	// Note: The shared defaults keys don't have "cross" in them because we want
	// to keep
	// compatibility with defaults that were saved when it used to be a template
	static final String SHARED_CROSS_TOOLCHAIN_NAME = Activator.getIdPrefix()
			+ "." + SetCrossCommandWizardPage.CROSS_TOOLCHAIN_NAME;
	static final String SHARED_CROSS_TOOLCHAIN_PATH = Activator.getIdPrefix()
			+ "." + SetCrossCommandWizardPage.CROSS_TOOLCHAIN_PATH;

	public static String getToolchainName() {

		String toolchainName = SharedDefaults.getInstance()
				.getSharedDefaultsMap().get(SHARED_CROSS_TOOLCHAIN_NAME);

		if (toolchainName == null)
			toolchainName = "";

		return toolchainName.trim();
	}

	public static void putToolchainName(String toolchainName) {

		SharedDefaults.getInstance().getSharedDefaultsMap()
				.put(SHARED_CROSS_TOOLCHAIN_NAME, toolchainName);
	}

	public static String getToolchainPath(String toolchainName) {

		String pathKey = SHARED_CROSS_TOOLCHAIN_PATH + "."
				+ toolchainName.trim().hashCode();
		String sPath = SharedDefaults.getInstance().getSharedDefaultsMap()
				.get(pathKey);

		if (sPath == null) {
			sPath = "";
		}

		return sPath.trim();
	}

	public static void putToolchainPath(String toolchainName, String path) {

		String pathKey = SHARED_CROSS_TOOLCHAIN_PATH + "."
				+ toolchainName.trim().hashCode();
		SharedDefaults.getInstance().getSharedDefaultsMap()
				.put(pathKey, path.trim());
	}

	public static void update() {

		SharedDefaults.getInstance().updateShareDefaultsMap(
				SharedDefaults.getInstance().getSharedDefaultsMap());
	}
}
