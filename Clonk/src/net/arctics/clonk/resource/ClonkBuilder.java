package net.arctics.clonk.resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ObjectExtern;
import net.arctics.clonk.parser.C4ObjectIntern;
import net.arctics.clonk.parser.C4ObjectParser;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.C4SystemScript;
import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.defcore.C4DefCoreWrapper;
import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.resource.c4group.C4Entry;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.resource.c4group.IC4GroupVisitor;
import net.arctics.clonk.resource.c4group.InvalidDataException;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.ui.editors.C4ScriptEditor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * An incremental builder for all project data.
 * This builder launches the parser that indexes all c4objects and highlights syntax errors.
 * @author ZokRadonh
 */
public class ClonkBuilder extends IncrementalProjectBuilder implements IResourceDeltaVisitor, IResourceVisitor, IC4GroupVisitor, IPropertyChangeListener {
	
	private int buildPhase;
	private IProgressMonitor monitor;
	
	public ClonkBuilder() {
		super();
	}
	
	public void worked(int count) {
		monitor.worked(count);
	}
	
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		if (monitor != null) monitor.beginTask("Cleaning up", 1);
		IProject proj = this.getProject();
		if (proj != null) {
			Utilities.getProject(proj).getIndexedData().clear();
			proj.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					if (resource.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID) != null) {
						resource.setSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID, null);
					}
					if (resource instanceof IContainer) return true;
					else return false;
				}
			});
		}
		if (monitor != null) {
			monitor.worked(1);
			monitor.done();
		}
		System.gc();
	}
	
	@SuppressWarnings("unchecked")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		try {
			IProject proj = getProject();
			this.monitor = monitor;
			switch(kind) {
			case AUTO_BUILD:
			case INCREMENTAL_BUILD:
				IResourceDelta delta = getDelta(proj);
				if (delta != null) {
					
					// count num of resources to build
					ResourceCounter counter = new ResourceCounter(ResourceCounter.COUNT_CONTAINER);
					delta.accept(counter);
					
					// initialize progress monitor
					monitor.beginTask("Build project " + proj.getName(), counter.getCount());
					// parse
					buildPhase = 0;
					delta.accept(this);
					Utilities.getProject(proj).getIndexedData().refreshCache();
					buildPhase = 1;
					delta.accept(this);
					
					// fire change event
					delta.getResource().touch(monitor);
					
					// refresh global func and static var cache
					Utilities.getProject(proj).getIndexedData().refreshCache();
				}
				break;
			
			case FULL_BUILD:
				int[] operations = new int[4];
				if (proj != null) {
					// count num of resources to build
					ResourceCounter counter = new ResourceCounter(ResourceCounter.COUNT_CONTAINER);
					proj.accept(counter);
					operations[0] = counter.getCount() * 2;
					operations[1] = counter.getCount();
				}
				else {
					operations[0] = 0;
					operations[1] = 0;
				}
				
				operations[2] = 0;
				operations[3] = 0;
				String[] externalLibs = getExternalLibNames();
				for(String lib : externalLibs) {
					File file = new File(lib);
					if (file.exists()) {
						operations[2] += (int) (file.length() / 7000); // approximate time
					}
				}
				operations[3] = operations[2] / 2; // approximate save time
				
				int workSum = 0;
				for(int work : operations) workSum += work;
				
				// initialize progress monitor
				monitor.beginTask("Build project " + proj.getName(), workSum);

				monitor.subTask("Parsing libraries");
				readExternalLibs(new SubProgressMonitor(monitor,operations[2]));
				monitor.subTask("Saving libraries");
				ClonkCore.saveExternIndex(new SubProgressMonitor(monitor,operations[3]));
				
				
				if (proj != null) {
					monitor.subTask("Index project " + proj.getName());
					// parse declarations
					buildPhase = 0;
					proj.accept(this);
					Utilities.getProject(proj).getIndexedData().refreshCache();
					if (monitor.isCanceled()) {
						monitor.done();
						return null;
					}
					monitor.subTask("Parse project " + proj.getName());
					// parse code bodies
					buildPhase = 1;
					proj.accept(this);
					
					// fire update event
					proj.touch(monitor);
				}
				
			}
			
			if (monitor.isCanceled()) {
				monitor.done();
				return null;
			}
			monitor.subTask("Save data");
			// saves all objects persistent
			Utilities.getProject(proj).saveIndexData();

			monitor.done();
			
			// refresh outlines
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					IWorkbench w = PlatformUI.getWorkbench();
					if (w == null)
						return;
					IWorkbenchPage page = w.getActiveWorkbenchWindow().getActivePage();
					for (IEditorReference ref : page.getEditorReferences()) {
						IEditorPart part = ref.getEditor(false);
						if (part != null && part instanceof C4ScriptEditor) {
							((C4ScriptEditor)part).refreshOutline();
						}
					}
				}
			});
			
			return new IProject[] { proj };
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private String[] getExternalLibNames() {
		String optionString = ClonkCore.getDefault().getPreferenceStore().getString(PreferenceConstants.STANDARD_EXT_LIBS);
		return optionString.split("<>");
	}
	
	/**
	 * Starts indexing of all external libraries
	 * @throws InvalidDataException
	 * @throws FileNotFoundException
	 */
	private void readExternalLibs(IProgressMonitor monitor) throws InvalidDataException, FileNotFoundException {
		String[] libs = getExternalLibNames();
		ClonkCore.EXTERN_INDEX.clear();
		try {
			ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(ClonkCore.MARKER_EXTERN_LIB_ERROR, false, 0);	
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		if (monitor != null) monitor.beginTask("Parsing libs", 3);
		for(String lib : libs) {
			if (new File(lib).exists()) {
				C4Group group = C4Group.OpenFile(new File(lib));
				group.open(true);
				try {
					group.accept(this, group.getGroupType(), null);
				} finally {
					group.close();
				}
			}
			else  {
				try {
					IMarker marker = ResourcesPlugin.getWorkspace().getRoot().createMarker(ClonkCore.MARKER_EXTERN_LIB_ERROR);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.TRANSIENT, false);
					marker.setAttribute(IMarker.MESSAGE, "Clonk extern library does not exist: '" + lib + "'");
					marker.setAttribute(IMarker.SOURCE_ID, "net.arctics.clonk.externliberror");
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			if (monitor != null) monitor.worked(1);
		}
		ClonkCore.EXTERN_INDEX.refreshCache();
		if (monitor != null) monitor.done();
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta == null) 
			return false;
		
//		for (Field f : IResourceDelta.class.getFields()) {
//			try {
//				if (delta.getKind() ==  f.getInt(null)) {
//					System.out.println(f.getName() + ": " + delta.getResource().getName());
//					break;
//				}
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

		if (delta.getResource() instanceof IFile) {
			if (delta.getKind() == IResourceDelta.CHANGED || delta.getKind() == IResourceDelta.ADDED) {
				C4ScriptBase script = Utilities.getScriptForFile((IFile) delta.getResource());
				if (script == null && buildPhase == 0) {
					// create if new file
					IContainer folder = delta.getResource().getParent();
					C4ObjectParser parser;
					if (delta.getResource().getName().endsWith(".c") && folder.getName().endsWith(".c4g")) {
						script = new C4SystemScript(delta.getResource());
					} else if ((parser = C4ObjectParser.create(folder)) != null) {
						try {
							script = parser.parse();
						} catch (CompilerException e) {
							e.printStackTrace();
						}
					}
				}
				try {
					if (script != null && delta.getResource().equals(script.getScriptFile())) {
						if (script != null) {
							C4ScriptParser parser = new C4ScriptParser((IFile) delta.getResource(), script);
							switch (buildPhase) {
							case 0:
								parser.clean();
								parser.parseDeclarations();
								break;
							case 1:
								parser.parseCodeOfFunctions();
							}
						}
					}
					else if (buildPhase == 0 && delta.getResource().getName().equals("DefCore.txt")) {
						C4DefCoreWrapper defCore = new C4DefCoreWrapper((IFile) delta.getResource());
						defCore.parse();
						if (script instanceof C4Object)
							((C4Object)script).setId(defCore.getObjectID());
					}
				} catch (CompilerException e) {
					// TODO display CompilerException messages
					e.printStackTrace();
				}
			}
			else if (delta.getKind() == IResourceDelta.REMOVED && delta.getResource().getParent().exists()) {
				if (buildPhase == 0) {
					C4ScriptBase script = Utilities.getScriptForFile((IFile) delta.getResource());
					if (script != null && delta.getResource().equals(script.getScriptFile()))
						script.clearFields();
				}
			}
			if (monitor != null) monitor.worked(1);
			return true;
		}
		else if (delta.getResource() instanceof IContainer) {
			// make sure the object has a reference to its folder (not to some obsolete deleted one)
			if (delta.getKind() == IResourceDelta.ADDED) {
				C4ObjectIntern object = C4ObjectIntern.objectCorrespondingTo((IContainer)delta.getResource());
				if (object != null)
					object.setObjectFolder((IContainer) delta.getResource());
			}
			else if (delta.getKind() == IResourceDelta.REMOVED) {
				// removed object when folder is removed
				C4ObjectIntern object = C4ObjectIntern.objectCorrespondingTo((IContainer)delta.getResource());
				if (object != null)
					object.getIndex().removeObject(object);
			}
			return true;
		}
		return false;
	}

	public boolean visit(IResource resource) throws CoreException {
		if (resource == null)
			return false;
		if (resource instanceof IContainer) {
			switch (buildPhase) {
			case 0:
				// first phase: just gather declarations
				C4ObjectParser parser = C4ObjectParser.create((IContainer) resource);
				if (parser != null) { // is complete c4d (with DefCore.txt Script.c and Graphics)
					try {
						parser.parse();
					} catch (CompilerException e) {
						// TODO display CompilerException messages
						e.printStackTrace();
					}
				}
				if (monitor != null) monitor.worked(2);
				return true;
			case 1:
				// check correctness of function code
				ClonkIndex index = Utilities.getProject(resource).getIndexedData();
				C4Object obj = index.getObject((IContainer)resource);
				IFile scriptFile = (IFile) ((obj != null) ? obj.getScriptFile() : null);
				if (scriptFile != null) {
					try {
						new C4ScriptParser(scriptFile, obj).parseCodeOfFunctions();
					} catch (CompilerException e) {
						e.printStackTrace();
					} 
				}
				if (monitor != null) monitor.worked(1);
				return true;
			default:
				assert(false); // soft exception
				return false; // :C ?
			}
		}
		else if (resource.getName().endsWith(".c") && resource.getParent().getName().endsWith(".c4g")) {
			C4ScriptBase script = C4SystemScript.scriptCorrespondingTo(resource);
			switch (buildPhase) {
			case 0:
				if (script == null) {
					script = new C4SystemScript(resource);
				}
				Utilities.getProject(resource).getIndexedData().addScript(script);
				try {
					C4ScriptParser parser = new C4ScriptParser((IFile)resource, script);
					parser.clean();
					parser.parseDeclarations();
				} catch (CompilerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			case 1:
				if (script != null) try {
					new C4ScriptParser((IFile)resource, script).parseCodeOfFunctions();
				} catch (CompilerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			default:
				return false;
			}
		}
		else return false;
	}

	public boolean visit(C4GroupItem item, C4GroupType packageType) {
		if (item instanceof C4Group) {
			C4Group group = (C4Group) item;
			if (group.getGroupType() == C4GroupType.DefinitionGroup) { // is .c4d
				C4Entry defCore = null, script = null;
				for(C4GroupItem child : group.getChildEntries()) {
					if (!(child instanceof C4Entry)) continue;
					if (child.getName().equals("DefCore.txt")) {
						defCore = (C4Entry) child;
					}
					else if (child.getName().equals("Script.c")) {
						script = (C4Entry) child;
					}
				}
				if (defCore != null && script != null) {
					C4DefCoreWrapper defCoreWrapper = new C4DefCoreWrapper(new ByteArrayInputStream(defCore.getContentsAsArray()));
					try {
						defCoreWrapper.parse();
						C4ObjectExtern obj = new C4ObjectExtern(defCoreWrapper.getObjectID(),item.getName(),group);
						C4ScriptParser parser = new C4ScriptParser(new ByteArrayInputStream(script.getContentsAsArray()),script.computeSize(),obj);
						// we only need declarations
						parser.clean();
						parser.parseDeclarations();
						ClonkCore.EXTERN_INDEX.addObject(obj);
					} catch (CompilerException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else if (item instanceof C4Entry) {
			if (packageType == C4GroupType.ResourceGroup) { // System.c4g like
				if (item.getName().endsWith(".c")) {
					byte[] content = ((C4Entry)item).getContentsAsArray();
					try {
						C4ObjectExtern externObj = new C4ObjectExtern(C4ID.getSpecialID("System"),"System",item.getParentGroup());
						C4ScriptParser parser = new C4ScriptParser(new ByteArrayInputStream(content),((C4Entry)item).computeSize(),externObj);
						parser.parseDeclarations();
						ClonkCore.EXTERN_INDEX.addObject(externObj);
//						Utilities.getProject(getProject()).getIndexedData().addObject(externSystemc4g);
					} catch (CompilerException e) {
						e.printStackTrace();
					}
				}				
			}
		}
		if (item instanceof C4Group)
			return true;
		else
			return false;
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

	public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
		if (event.getProperty().equals(PreferenceConstants.STANDARD_EXT_LIBS)) {
			String oldValue = (String) event.getOldValue();
			String newValue = (String) event.getNewValue();
			String[] oldLibs = oldValue.split("<>");
			String[] newLibs = newValue.split("<>");
			for(String lib : oldLibs) {
				if (!isIn(lib, newLibs)) { 
					// lib deselected
					// TODO: remove all objects in lib
				}
			}
			for(String lib : newLibs) {
				if (!isIn(lib, oldLibs)) {
					// new lib selected
					// TODO: create new externobjects and add to index
					File libFile = new File(lib);
					try {
						C4Group group = C4Group.OpenFile(libFile);
						group.open(true);
						try {
							group.accept(this);
						} finally {
							group.close();
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (InvalidDataException e) {
						e.printStackTrace();
					}
					
				}
			}
		}
	}

}
