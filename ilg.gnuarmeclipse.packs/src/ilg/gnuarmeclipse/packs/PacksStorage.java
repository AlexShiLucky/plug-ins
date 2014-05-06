/*******************************************************************************
 * Copyright (c) 2014 Liviu ionescu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Liviu Ionescu - initial implementation.
 *******************************************************************************/

package ilg.gnuarmeclipse.packs;

import ilg.gnuarmeclipse.packs.ui.preferences.FolderConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PacksStorage {

	public static final String SITES_FILE_NAME = "sites.xml";
	public static final String CACHE_FILE_NAME = "cache.xml";

	public static final String CMSIS_PACK_TYPE = "CMSIS Pack";

	public static List<String[]> getDefaultSites() {

		List<String[]> sitesList;
		sitesList = new ArrayList<String[]>();

		sitesList.add(new String[] { CMSIS_PACK_TYPE,
				"http://www.keil.com/pack/index.idx" });
		sitesList.add(new String[] { CMSIS_PACK_TYPE,
				"http://gnuarmeclipse.sourceforge.net/packages/index.xml" });

		return sitesList;
	}

	public static List<String[]> getSites() {

		List<String[]> sites = parseSites();
		if (sites != null) {
			return sites;
		}
		return getDefaultSites();
	}

	private static String getFolderPath() {

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String sitesFolderPath = store.getString(FolderConstants.P_FOLDER_PATH)
				.trim();

		return sitesFolderPath;
	}

	private static File getFile(String name) {

		String folderPath = getFolderPath();
		if (folderPath.length() == 0)
			return null;

		IPath path = (new Path(folderPath)).append(name);
		File file = path.toFile();
		return file;
	}

	private static List<String[]> parseSites() {

		File file = getFile(SITES_FILE_NAME);
		if (file == null)
			return null;
		if (!file.exists())
			return null;

		try {
			InputSource inputSource = new InputSource(new FileInputStream(file));

			DocumentBuilder parser = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document document = parser.parse(inputSource);

			Element el = document.getDocumentElement();
			if (!"sites".equals(el.getNodeName())) {
				return null;
			}

			List<String[]> sitesList;
			sitesList = new ArrayList<String[]>();

			NodeList siteList = el.getElementsByTagName("site");
			for (int i = 0; i < siteList.getLength(); ++i) {
				Node siteNode = siteList.item(i);

				String type = "(unknown)";
				String url = "(unknown)";

				NodeList siteNodeChildren = siteNode.getChildNodes();
				for (int j = 0; j < siteNodeChildren.getLength(); ++j) {
					Node child = siteNodeChildren.item(j);
					short nodeType = child.getNodeType();
					if (nodeType != Node.ELEMENT_NODE) {
						continue;
					}
					String childName = child.getNodeName();
					if ("type".equals(childName)) {
						type = child.getTextContent().trim();
					} else if ("url".equals(childName)) {
						url = child.getTextContent().trim();
					}
				}

				sitesList.add(new String[] { type, url });
			}
			// System.out.println(SITES_FILE_NAME+" parsed");
			return sitesList;
		} catch (ParserConfigurationException e) {
			Activator.log(e);
		} catch (SAXException e) {
			Activator.log(e);
		} catch (IOException e) {
			Activator.log(e);
		}

		return null;
	}

	public static void putSites(List<String[]> sitesList) {

		File file = getFile(SITES_FILE_NAME);
		if (file == null)
			return;

		// The xml structure is simple, write it as strings
		try {
			if (!file.exists())
				file.createNewFile();
			if (file.exists()) {
				PrintWriter writer = new PrintWriter(new BufferedWriter(
						new FileWriter(file)));
				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
				writer.println("<sites>"); // $NON-NLS-1$

				for (String[] site : sitesList) {
					writer.println("\t<site>"); // $NON-NLS-1$
					// Warning: the strings should not contain xml special chars
					writer.println("\t\t<type>" + site[0] + "</type>"); // $NON-NLS-1$
					writer.println("\t\t<url>" + site[1] + "</url>"); // $NON-NLS-1$
					writer.println("\t</site>"); // $NON-NLS-1$
				}
				writer.println("</sites>");
				writer.close();

				// System.out.println(SITES_FILE_NAME+" saved");
			}
		} catch (IOException e) {
			Activator.log(e);
		}
	}

	public static void putCache(TreeNode tree) {

		File file = getFile(CACHE_FILE_NAME);
		if (file == null)
			return;

		// The xml structure is simple, write it as strings
		try {
			if (!file.exists())
				file.createNewFile();
			if (file.exists()) {
				PrintWriter writer = new PrintWriter(new BufferedWriter(
						new FileWriter(file)));
				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				writer.println("<root version=\"1.0\">");

				putCacheNodeRecursive(tree, 0, writer);

				writer.println("</root>");
				writer.close();

				// System.out.println(SITES_FILE_NAME+" saved");
			}
		} catch (IOException e) {
			Activator.log(e);
			;
		}
	}

	private static void putCacheNodeRecursive(TreeNode node, int depth,
			PrintWriter writer) {

		putIndentation(depth * 2, writer);
		writer.println("<node type=\"" + node.getType() + "\" name=\""
				+ node.getName() + "\">");

		String description = node.getDescription();
		if (description != null && description.length() > 0) {
			putIndentation(depth * 2 + 1, writer);
			writer.println("<description>"
					+ Utils.xmlEscape(node.getDescription()) + "</description>");
		}

		List<TreeNode> children = node.getChildren();
		if (children != null && !children.isEmpty()) {
			putIndentation(depth * 2 + 1, writer);
			writer.println("<nodes>");

			for (TreeNode child : children) {
				putCacheNodeRecursive(child, depth + 1, writer);
			}

			putIndentation(depth * 2 + 1, writer);
			writer.println("</nodes>");
		}
		putIndentation(depth * 2, writer);
		writer.println("</node>");
	}

	private static void putIndentation(int depth, PrintWriter writer) {
		depth++;
		for (int i = 0; i < depth; ++i) {
			writer.print("  ");
		}
	}

	public static TreeNode getCache() {

		File file = getFile(CACHE_FILE_NAME);
		if (file == null)
			return null;
		if (!file.exists())
			return null;

		try {
			InputSource inputSource = new InputSource(new FileInputStream(file));

			DocumentBuilder parser = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document document = parser.parse(inputSource);

			Element el = document.getDocumentElement();
			if (!"root".equals(el.getNodeName())) {
				return null;
			}

			Element nodeElement = Utils.getChildElement(el, "node");
			if (nodeElement != null) {
				return getCacheRecursive(nodeElement);
			}
		} catch (ParserConfigurationException e) {
			Activator.log(e);
		} catch (SAXException e) {
			Activator.log(e);
		} catch (IOException e) {
			Activator.log(e);
		}

		return null;
	}

	private static TreeNode getCacheRecursive(Element el) {

		String type = el.getAttribute("type");
		TreeNode treeNode = new TreeNode(type);

		String name = el.getAttribute("name");
		treeNode.setName(name);

		Element descriptionElement = Utils.getChildElement(el, "description");
		if (descriptionElement != null ) {
			String description = descriptionElement.getTextContent();
			treeNode.setDescription(description);
		}

		Element nodesElement = Utils.getChildElement(el, "nodes");
		if (nodesElement != null) {
			List<Element> nodeElements = Utils.getChildElementList(nodesElement, "node");
			for (Element nodeElement: nodeElements) {

				TreeNode childTreeNode = getCacheRecursive((Element) nodeElement);
				treeNode.addChild(childTreeNode);
			}
		}

		return treeNode;
	}
}
