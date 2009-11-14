package net.arctics.clonk.resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4ObjectExtern;
import net.arctics.clonk.index.C4ObjectExternGroup;
import net.arctics.clonk.index.IExternalScript;
import net.arctics.clonk.parser.c4script.C4ScriptExtern;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4EntryHeader;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.resource.c4group.IC4GroupVisitor;
import net.arctics.clonk.resource.c4group.InvalidDataException;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.resource.c4group.C4GroupItem.IHeaderFilter;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;

public class ClonkLibBuilder implements IC4GroupVisitor, IPropertyChangeListener {
	
	private static final Pattern DESC_TXT_PATTERN = Pattern.compile("Desc(..)\\.txt", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private boolean buildNeeded = false;
	private transient ITreeNode currentExternNode;
	private transient C4Group nodeGroup;
	
	public ClonkLibBuilder() {
		ClonkCore.getDefault().getPreferenceStore().addPropertyChangeListener(this);
//		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_BUILD);
	}
	
	public void build(IProgressMonitor monitor) throws InvalidDataException, IOException, CoreException {
		readExternalLibs(monitor);
		buildNeeded = false;
	}
	
	public void clean() {
		ClonkCore.getDefault().getExternIndex().clear();
		buildNeeded = true;
	}
	
	private String[] getExternalLibNames() {
		String optionString = ClonkCore.getDefault().getPreferenceStore().getString(ClonkPreferences.STANDARD_EXT_LIBS);
		return optionString.split("<>"); //$NON-NLS-1$
	}
	
	private void readExternalLib(String lib, IProgressMonitor monitor) throws InvalidDataException, IOException, CoreException {
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
						// also load string tables since they are needed for getting the correct names OC definitions
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
					marker.setAttribute(IMarker.MESSAGE, String.format(Messages.ExternalLibraryDoesNotExist, lib));
					marker.setAttribute(IMarker.SOURCE_ID, ClonkCore.MARKER_EXTERN_LIB_ERROR);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		monitor.done();
	}
	
	/**
	 * Starts indexing of all external libraries
	 * @throws InvalidDataException
	 * @throws IOException 
	 * @throws CoreException 
	 */
	private void readExternalLibs(IProgressMonitor monitor) throws InvalidDataException, IOException, CoreException {
		String[] libs = getExternalLibNames();
		ClonkCore.getDefault().getExternIndex().clear();
		try {
			ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(ClonkCore.MARKER_EXTERN_LIB_ERROR, false, 0);	
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		if (monitor != null) monitor.beginTask(Messages.ParsingLibs, libs.length);
		for(String lib : libs) {
			readExternalLib(lib, new SubProgressMonitor(monitor,1));
		}
		refresh();
		if (monitor != null) monitor.done();
	}
	
	private final Matcher descMatcher = DESC_TXT_PATTERN.matcher(""); //$NON-NLS-1$
	private final Matcher stringTblMatcher = StringTbl.PATTERN.matcher(""); //$NON-NLS-1$
	
	public boolean visit(C4GroupItem item, C4GroupType packageType) {
		if (item instanceof C4Group) {
			C4Group group = (C4Group) item;
			C4GroupType groupType = group.getGroupType();
			if (groupType == C4GroupType.DefinitionGroup) { // is .c4d
				C4GroupEntry defCore = null, script = null, names = null;
				Map<String, C4GroupEntry> descEntries = new HashMap<String, C4GroupEntry>();
				Map<String, C4GroupEntry> tbls = new HashMap<String, C4GroupEntry>();
				for(C4GroupItem child : group.getChildEntries()) {
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
						C4ObjectExtern obj = new C4ObjectExtern(defCoreWrapper.getObjectID(), defCoreWrapper.getName(), script, currentExternNode);
						currentExternNode = obj; nodeGroup = group;
						
						if (names != null)
							obj.readNames(names.getContentsAsString());
						for (String descLang : descEntries.keySet()) {
							obj.addDesc(descLang, descEntries.get(descLang).getContentsAsString()); //$NON-NLS-1$ //$NON-NLS-2$
						}
						for (String stringTblLang : tbls.keySet()) {
							StringTbl tbl = new StringTbl();
							try {
								C4GroupEntry entry = tbls.get(stringTblLang);
								tbl.read(entry.getContents());
							} catch (CoreException e) {
								e.printStackTrace();
							}
							obj.addStringTbl(stringTblLang, tbl);
						}
						
						C4ScriptParser parser = new C4ScriptParser((IExternalScript)obj);
						// we only need declarations
						parser.clean();
						parser.parseDeclarations();
						parser.distillAdditionalInformation(); // OC
						ClonkCore.getDefault().getExternIndex().addObject(obj);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else
					createGroup(group);
			}
			else if (groupType == C4GroupType.ResourceGroup) { // System.c4g like
				createGroup(group);
				for (C4GroupItem child : group.getChildEntries()) {
					if (child.getName().endsWith(".c")) { //$NON-NLS-1$
						try {
							C4ScriptExtern externScript = new C4ScriptExtern(child, currentExternNode);
							C4ScriptParser parser = new C4ScriptParser(((C4GroupEntry)child).getContents(),externScript);
							parser.parseDeclarations();
							ClonkCore.getDefault().getExternIndex().addScript(externScript);
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

	private void createGroup(C4Group item) {
		if (currentExternNode == null) {
			ExternalLib lib;
			currentExternNode = lib = new ExternalLib(item.getName(), null);
			lib.setFullPath(item.getOrigin());
			ClonkCore.getDefault().getExternIndex().getLibs().add(lib);
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

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(ClonkPreferences.STANDARD_EXT_LIBS)) {
			String oldValue = (String) event.getOldValue();
			String newValue = (String) event.getNewValue();
			final String[] oldLibs = oldValue.split("<>"); //$NON-NLS-1$
			final String[] newLibs = newValue.split("<>"); //$NON-NLS-1$
			final ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
			try {
				progressDialog.run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException, InterruptedException {
						for(String lib : oldLibs) {
							if (!isIn(lib, newLibs)) { 
								// lib deselected
								try {
									build(monitor);
									return;
								} catch (InvalidDataException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								} catch (CoreException e) {
									e.printStackTrace();
								}
							}
						}
						for(String lib : newLibs) {
							if (!isIn(lib, oldLibs)) {
								// new lib selected
								try {
									readExternalLib(lib, monitor);
								} catch (InvalidDataException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								} catch (CoreException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
						refresh();
						try {
							ClonkCore.getDefault().saveExternIndex(monitor);
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
						}
						try {
							Utilities.refreshClonkProjects(monitor);
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				});
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		else if (event.getProperty().equals(ClonkPreferences.PREFERRED_LANGID)) {
			for (C4Object o : ClonkCore.getDefault().getExternIndex()) {
				o.chooseLocalizedName();
			}
			for (IProject proj : Utilities.getClonkProjects()) {
				for (C4Object obj : ClonkProjectNature.getClonkNature(proj).getIndex()) {
					obj.chooseLocalizedName();
				}
			}
		}
	}
	
	private void refresh() {
		ClonkCore.getDefault().getExternIndex().refreshCache();
		for (IProject p : Utilities.getClonkProjects()) {
			ClonkProjectNature.getClonkNature(p).getIndex().notifyExternalLibsSet();
		}
	}

	/**
	 * Simple method to check whether the String <tt>item</tt> is in <tt>array</tt>. 
	 * @param item
	 * @param array
	 * @return <code>true</code> when item exists in <tt>array</tt>, false if not
	 */
	private boolean isIn(String item, String[] array) {
		for(String newLib : array) {
			if (newLib.equals(item)) {
				return true;
			}
		}
		return false;
	}

	public boolean isBuildNeeded() {
		return buildNeeded;
	}
	
}
