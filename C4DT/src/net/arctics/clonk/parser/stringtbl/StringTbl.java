package net.arctics.clonk.parser.stringtbl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.ReadOnlyIterator;
import net.arctics.clonk.util.Utilities;

public class StringTbl extends C4Structure implements ITreeNode, ITableEntryInformationSink {
	
	public static final Pattern PATTERN = Pattern.compile("StringTbl(..)\\.txt", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	private static final long serialVersionUID = 1L;
	
	private Map<String, NameValueAssignment> map = new HashMap<String, NameValueAssignment>();
	private transient IFile file;

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
	
	public void addTblEntry(String key, String value, int start, int end) {
		NameValueAssignment nv = new NameValueAssignment(start, end, key, value);
		nv.setParentDeclaration(this);
		map.put(key, nv);
	}

	@Override
	public C4Declaration findLocalDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		if (declarationClass == NameValueAssignment.class)
			return map.get(declarationName);
		return null;
	}
	
	@Override
	public C4Declaration findDeclaration(String declarationName) {
		return map.get(declarationName);
	}
	
	public static void readStringTbl(Reader reader, ITableEntryInformationSink sink) {
		BufferedScanner scanner;
		try {
			scanner = new BufferedScanner(reader);
		} catch (IOException e) {
			scanner = new BufferedScanner("");
			e.printStackTrace();
		}
		while (!scanner.reachedEOF()) {
			scanner.eatWhitespace();
			if (scanner.read() == '#')
				scanner.readStringUntil(BufferedScanner.NEWLINE_CHARS);
			else {
				scanner.unread();
				int start = scanner.getPosition();
				String key = scanner.readStringUntil('=');
				if (scanner.read() == '=') {
					String value = scanner.readStringUntil(BufferedScanner.NEWLINE_CHARS);
					sink.addTblEntry(key, value, start, scanner.getPosition());
				}
				else
					scanner.unread();
			}
		}
	}
	
	public void read(Reader reader) {
		readStringTbl(reader, this);
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
			private final Matcher stringTblFileMatcher = PATTERN.matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
			public C4Structure create(IResource resource, boolean duringBuild) {
				if (resource instanceof IFile && stringTblFileMatcher.reset(resource.getName()).matches()) {
					IFile file = (IFile) resource;
					StringTbl tbl = new StringTbl();
					tbl.setFile(file);
					String fileContents;
					try {
						fileContents = Utilities.stringFromFile(file);
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
					StringReader reader = new StringReader(fileContents);
					tbl.read(reader);
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
		return "StringTbl";  //$NON-NLS-1$
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