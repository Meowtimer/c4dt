package net.arctics.clonk.index;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptExtern;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.resource.c4group.C4EntryHeader;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.resource.c4group.IC4GroupVisitor;
import net.arctics.clonk.resource.c4group.InvalidDataException;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.resource.c4group.C4GroupItem.IHeaderFilter;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

public final class ExternalLibsLoader implements IC4GroupVisitor {
	private static final Pattern DESC_TXT_PATTERN = Pattern.compile("Desc(..)\\.txt", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private static final Matcher descMatcher = DESC_TXT_PATTERN.matcher(""); //$NON-NLS-1$
	private static final Matcher stringTblMatcher = StringTbl.PATTERN.matcher(""); //$NON-NLS-1$
	
	private transient ITreeNode currentExternNode;
	private transient C4Group nodeGroup;
	private ExternIndex index;
	
	public ExternalLibsLoader(ExternIndex index) {
		this.index = index;
	}
	
	public void readExternalLib(String lib, IProgressMonitor monitor) throws InvalidDataException, IOException, CoreException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		File libFile = new File(lib);
		currentExternNode = null;
		monitor.beginTask("Parse lib " + lib, 1); //$NON-NLS-1$
		if (libFile.exists()) {
			C4Group group = libFile.isDirectory()
				? C4Group.openDirectory(libFile)
				: C4Group.openFile(libFile);
			group.readIntoMemory(true, new IHeaderFilter() {
				public boolean accepts(C4EntryHeader header, C4Group context) {
					String entryName = header.getEntryName();
					// all we care about is groups, scripts, defcores and names
					return header.isGroup() ||
						entryName.endsWith(".c") || // $NON-NLS-1$ //$NON-NLS-1$
						entryName.equals("DefCore.txt") || // $NON-NLS-1$ //$NON-NLS-1$
						entryName.equals("Names.txt") || // $NON-NLS-1$ //$NON-NLS-1$
						entryName.matches(DESC_TXT_PATTERN.pattern()) ||
						// also load string tables since they are needed for getting the correct names of OC definitions
						entryName.matches(StringTbl.PATTERN.pattern());
				}

				public void processData(C4GroupItem item) throws CoreException {
				}
			});
			try {
				group.accept(this, group.getGroupType(), null);
			} finally {
				group.close();
			}
		}
		else  {
			try {
				if (!lib.equals("")) { //$NON-NLS-1$
					IMarker marker = ResourcesPlugin.getWorkspace().getRoot().createMarker(ClonkCore.MARKER_EXTERN_LIB_ERROR);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.TRANSIENT, false);
					marker.setAttribute(IMarker.MESSAGE, String.format(net.arctics.clonk.resource.Messages.ExternalLibraryDoesNotExist, lib));
					marker.setAttribute(IMarker.SOURCE_ID, ClonkCore.MARKER_EXTERN_LIB_ERROR);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		monitor.done();
		index.setDirty(true);
	}
	
	/**
	 * Starts indexing of all external libraries
	 * @throws InvalidDataException
	 * @throws IOException 
	 * @throws CoreException 
	 */
	public void readExternalLibs(IProgressMonitor monitor, String[] libs) throws InvalidDataException, IOException, CoreException {
		index.clear();
		try {
			ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(ClonkCore.MARKER_EXTERN_LIB_ERROR, false, 0);	
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		if (monitor != null) monitor.beginTask(net.arctics.clonk.resource.Messages.ParsingLibs, libs.length);
		for(String lib : libs) {
			readExternalLib(lib, new SubProgressMonitor(monitor,1));
		}
		refresh();
		if (monitor != null) monitor.done();
	}
	
	public boolean visit(C4GroupItem item, C4GroupType packageType) {
		if (item instanceof C4Group) {
			C4Group group = (C4Group) item;
			C4GroupType groupType = group.getGroupType();
			if (groupType == C4GroupType.DefinitionGroup) { // is .c4d
				C4GroupEntry defCore = null, script = null, names = null;
				Map<String, C4GroupEntry> descEntries = new HashMap<String, C4GroupEntry>();
				Map<String, C4GroupEntry> tbls = new HashMap<String, C4GroupEntry>();
				for(C4GroupItem child : group.getChildren()) {
					if (!(child instanceof C4GroupEntry)) continue;
					if (child.getName().equals("DefCore.txt")) { //$NON-NLS-1$
						defCore = (C4GroupEntry) child;
					}
					else if (child.getName().equals("Script.c")) { //$NON-NLS-1$
						script = (C4GroupEntry) child;
					}
					else if (child.getName().equals("Names.txt")) { //$NON-NLS-1$
						names = (C4GroupEntry) child;
					}
					else if (descMatcher.reset(child.getName()).matches()) {
						descEntries.put(descMatcher.group(1), (C4GroupEntry) child);
					}
					else if (stringTblMatcher.reset(child.getName()).matches()) {
						tbls.put(stringTblMatcher.group(1), (C4GroupEntry) child);
					}
				}
				if (defCore != null && script != null) {
					DefCoreUnit defCoreWrapper = new DefCoreUnit(new ByteArrayInputStream(defCore.getContentsAsArray()));
					try {
						defCoreWrapper.parse(false);
						if (currentExternNode == null) {
							// pack that is one definition: create ExternalLib wrapper with same name (FIXME: won't allow proper dragging since the file path will be off)
							createGroup(group);
						}
						C4ObjectExtern obj = new C4ObjectExtern(defCoreWrapper.getObjectID(), defCoreWrapper.getName(), script, currentExternNode);
						currentExternNode = obj; nodeGroup = group;
						
						if (names != null)
							obj.readNames(names.getContentsAsString());
						for (String descLang : descEntries.keySet()) {
							obj.addDesc(descLang, descEntries.get(descLang).getContentsAsString()); //$NON-NLS-1$ //$NON-NLS-2$
						}
						for (Entry<String, C4GroupEntry> mapEntry : tbls.entrySet()) {
							StringTbl tbl = new StringTbl();
							try {
								C4GroupEntry entry = mapEntry.getValue();
								tbl.read(entry.getContents());
							} catch (CoreException e) {
								e.printStackTrace();
							}
							obj.addStringTbl(mapEntry.getKey(), tbl);
						}
						
						C4ScriptParser parser = new C4ScriptParser((IExternalScript)obj);
						// we only need declarations
						parser.clean();
						parser.parseDeclarations();
						parser.distillAdditionalInformation(); // OC
						index.addObject(obj);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else
					createGroup(group);
			}
			else if (groupType == C4GroupType.ResourceGroup) { // System.c4g like
				createGroup(group);
				for (C4GroupItem child : group.getChildren()) {
					if (child instanceof C4GroupEntry  && child.getName().endsWith(".c")) { //$NON-NLS-1$
						try {
							C4ScriptExtern externScript = new C4ScriptExtern((C4GroupEntry) child, currentExternNode);
							C4ScriptParser parser = new C4ScriptParser(((C4GroupEntry)child).getContents(),externScript);
							parser.parseDeclarations();
							index.addScript(externScript);
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
			return true;
		}
		return false;
	}
	
	public static void readExternalLibs(ExternIndex index, IProgressMonitor monitor, String[] libs) throws InvalidDataException, IOException, CoreException {
		new ExternalLibsLoader(index).readExternalLibs(monitor, libs);
	}
	
	public static void readExternalLib(ExternIndex index, IProgressMonitor monitor, String lib) throws InvalidDataException, IOException, CoreException {
		new ExternalLibsLoader(index).readExternalLib(lib, monitor);
	}

	private void createGroup(C4Group item) {
		if (currentExternNode == null) {
			ExternalLib lib;
			currentExternNode = lib = new ExternalLib(item.getName(), index);
			lib.setFullPath(item.getOrigin().getAbsolutePath());
			index.getLibs().add(lib);
		}
		else
			currentExternNode = new C4ObjectExternGroup(item.getName(), currentExternNode);
		nodeGroup = item;
		//System.out.println(currentExternNode.getPath().toString());
	}
	
	public void groupFinished(C4Group group) {
		if (currentExternNode != null && group == nodeGroup) {
			currentExternNode = currentExternNode.getParentNode();
			nodeGroup = nodeGroup.getParentGroup();
		}
	}
	
	private void refresh() {
		index.refreshCache();
		/*for (IProject p : Utilities.getClonkProjects()) {
			ClonkProjectNature.get(p).getIndex().notifyExternalLibsSet();
		}*/
	}
	
}