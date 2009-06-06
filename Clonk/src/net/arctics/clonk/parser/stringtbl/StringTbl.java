package net.arctics.clonk.parser.stringtbl;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.ReadOnlyIterator;

public class StringTbl extends C4Structure implements ITreeNode {
	
	private static final long serialVersionUID = 1L;
	
	private Map<String, NameValueAssignment> map = new HashMap<String, NameValueAssignment>();
	private IFile file;

	public IFile getFile() {
		return file;
	}

	public void setFile(IFile file) {
		this.file = file;
	}
	
	@Override
	public IResource getResource() {
		return file;
	}

	public Map<String, NameValueAssignment> getMap() {
		return map;
	}
	
	public void add(String key, String value, int start, int end) {
		NameValueAssignment nv = new NameValueAssignment(start, end, key, value);
		nv.setParentDeclaration(this);
		map.put(key, nv);
	}

	@Override
	public C4Declaration findDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		if (declarationClass == NameValueAssignment.class)
			return map.get(declarationName);
		return null;
	}
	
	@Override
	public C4Declaration findDeclaration(String declarationName) {
		return map.get(declarationName);
	}
	
	public void read(InputStream stream) {
		BufferedScanner scanner = new BufferedScanner(stream);
		while (!scanner.reachedEOF()) {
			scanner.eatWhitespace();
			if (scanner.read() == '#')
				scanner.readStringUntil(BufferedScanner.NEWLINE_DELIMITERS);
			else {
				scanner.unread();
				int start = scanner.getPosition();
				String key = scanner.readStringUntil('=');
				if (scanner.read() == '=') {
					String value = scanner.readStringUntil(BufferedScanner.NEWLINE_DELIMITERS);
					add(key, value, start, scanner.getPosition());
				}
				else
					scanner.unread();
			}
		}
	}
	
	@Override
	public Object[] getSubDeclarationsForOutline() {
		return map.values().toArray(new Object[map.values().size()]);
	}
	
	public Iterator<NameValueAssignment> iterator() {
		return new ReadOnlyIterator<NameValueAssignment>(map.values().iterator());
	}
	
	public static void register() {
		registerStructureFactory(new IStructureFactory() {
			public C4Structure create(IFile file) {
				if (file.getName().matches("StringTbl..\\.txt")) {
					StringTbl tbl = new StringTbl();
					tbl.setFile(file);
					try {
						tbl.read(file.getContents());
					} catch (CoreException e) {
						e.printStackTrace();
						return null;
					}
					return tbl;
				}
				return null;
			}
		});
	}

	public void addChild(ITreeNode node) {
	}

	public Collection<? extends ITreeNode> getChildCollection() {
		return map.values();
	}

	public String getNodeName() {
		return "StringTbl"; 
	}

	public ITreeNode getParentNode() {
		return null;
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

}