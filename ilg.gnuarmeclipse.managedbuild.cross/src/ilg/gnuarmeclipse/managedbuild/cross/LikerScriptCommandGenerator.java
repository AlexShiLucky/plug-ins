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

package ilg.gnuarmeclipse.managedbuild.cross;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IOptionCommandGenerator;
import org.eclipse.cdt.managedbuilder.makegen.gnu.GnuMakefileGenerator;
import org.eclipse.cdt.utils.cdtvariables.IVariableSubstitutor;

public class LikerScriptCommandGenerator implements IOptionCommandGenerator {

	@Override
	public String generateCommand(IOption option,
			IVariableSubstitutor macroSubstitutor) {

		String command = null;
		try {
			int valueType = option.getValueType();

			if (valueType == IOption.STRING) {
				// for compatibility with projects created with previous
				// versions, keep accepting single strings
				command = "-T "
						+ GnuMakefileGenerator
								.escapeWhitespaces(GnuMakefileGenerator
										.ensureUnquoted(option.getStringValue()));
			} else if (valueType == IOption.STRING_LIST) {
				for (String value : option.getStringListValue()) {

					if (value != null) {
						value = value.trim();
					}

					if (value.length() > 0) {
						if (command == null)
							command = "";

						command += "-T "
								+ GnuMakefileGenerator
										.escapeWhitespaces(GnuMakefileGenerator
												.ensureUnquoted(value)) + " ";
					}
				}
			}			
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (command != null)
			command = command.trim();
		
		return command;
	}

}
