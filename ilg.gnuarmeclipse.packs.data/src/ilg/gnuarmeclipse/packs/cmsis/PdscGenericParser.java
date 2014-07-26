package ilg.gnuarmeclipse.packs.cmsis;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import ilg.gnuarmeclipse.packs.core.tree.Leaf;
import ilg.gnuarmeclipse.packs.core.tree.Node;
import ilg.gnuarmeclipse.packs.core.tree.Property;
import ilg.gnuarmeclipse.packs.core.tree.Type;
import ilg.gnuarmeclipse.packs.data.Xml;

public class PdscGenericParser extends PdscParser {

	private Map<String, String[]> fOptimizationsMap;

	public PdscGenericParser() {

		super();

		fOptimizationsMap = new TreeMap<String, String[]>();
		fOptimizationsMap.put("package", new String[] { "description", "name",
				"vendor", "url" });
		fOptimizationsMap.put("condition", new String[] { "description" });
		fOptimizationsMap.put("example", new String[] { "description" });
		fOptimizationsMap.put("component", new String[] { "description" });
		fOptimizationsMap.put("bundle", new String[] { "description" });
		fOptimizationsMap.put("family", new String[] { "description" });
		fOptimizationsMap.put("subFamily", new String[] { "description" });
		fOptimizationsMap.put("device", new String[] { "description" });
		fOptimizationsMap.put("variant", new String[] { "description" });
		fOptimizationsMap.put("board", new String[] { "description" });
		fOptimizationsMap.put("api", new String[] { "description" });
	}

	public Node parse() {

		Element packageElement = fDocument.getDocumentElement();

		Node tree = new Node(Type.ROOT);
		parseRecusive(packageElement, tree);

		return tree;
	}

	private void parseRecusive(Element el, Node parent) {

		String type = el.getNodeName();

		if ("conditions".equals(type)) {
			System.out.println(" ");
		}
		Leaf node = null;
		List<Element> children = Xml.getChildrenElementsList(el);
		if (!children.isEmpty()) {

			node = Node.addNewChild(parent, type);

			// The element has children, some can be optimised as attributes,
			// some generate children nodes.
			String elemNames[] = fOptimizationsMap.get(type);
			for (Element child : children) {

				String childName = child.getNodeName();
				boolean isChild = true;
				if (elemNames != null) {
					for (int i = 0; i < elemNames.length; ++i) {
						if (childName.equals(elemNames[i])) {
							// Turn simple elements into properties
							String content = Xml.getElementContent(child);
							node.putNonEmptyProperty(childName, content);
							isChild = false;
							break;
						}
					}
				}
				if (isChild) {
					parseRecusive(child, (Node) node);
				}
			}
		} else {
			node = Leaf.addNewChild(parent, type);

			String content = Xml.getElementContent(el);
			node.putNonEmptyProperty(Property.XML_CONTENT, content);
			System.out.println();
		}

		NamedNodeMap attributes = el.getAttributes();
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); ++i) {
				String name = attributes.item(i).getNodeName();
				node.putProperty(name, el.getAttribute(name));
			}
		}
	}
}
