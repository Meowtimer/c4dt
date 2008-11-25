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
import net.arctics.clonk.parser.C4ScriptParser;
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
					delta.accept(this);
					
					// fire change event
					delta.getResource().touch(monitor);
					
					// refresh global func and static var cache
					Utilities.getProject(proj).getIndexedData().refreshCache();
				}
				break;
			case FULL_BUILD:
			case CLEAN_BUILD:
				readExternalLibs();
				ClonkCore.saveExternIndex();
				if (proj != null) {
					// count num of resources to build
					ResourceCounter counter = new ResourceCounter(ResourceCounter.COUNT_CONTAINER);
					proj.accept(counter);
					
					// initialize progress monitor
					monitor.beginTask("Build project " + proj.getName(), counter.getCount() * 2);
					
					// parse declarations
					buildPhase = 0;
					proj.accept(this);
					Utilities.getProject(proj).getIndexedData().refreshCache();
					
					if (monitor.isCanceled()) {
						monitor.done();
						return null;
					}
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
	
	/**
	 * Starts indexing of all external libraries
	 * @throws InvalidDataException
	 * @throws FileNotFoundException
	 */
	private void readExternalLibs() throws InvalidDataException, FileNotFoundException {
		String optionString = ClonkCore.getDefault().getPreferenceStore().getString(PreferenceConstants.STANDARD_EXT_LIBS);
		String[] libs = optionString.split("<>");
		ClonkCore.EXTERN_INDEX.clear();
		try {
			ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(ClonkCore.MARKER_EXTERN_LIB_ERROR, false, 0);	
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		for(String lib : libs) {
			if (new File(lib).exists()) {
				C4Group group = C4Group.OpenFile(new File(lib));
				group.open(true);
				try {
					group.accept(this);
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
		}
		ClonkCore.EXTERN_INDEX.refreshCache();
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta == null) 
			return false;

		if (delta.getResource() instanceof IFile) {
			if (delta.getKind() != IResourceDelta.REMOVED) {
				C4Object container = C4Object.objectCorrespondingTo(delta.getResource().getParent());
				if (container != null) {
					try {
						if (delta.getResource().getName().endsWith(".c")) {
							// remove and add object to refresh globalFunctions/staticVariables
							ClonkIndex index = Utilities.getProject(delta.getResource()).getIndexedData();
							index.removeObject(container);
							new C4ScriptParser((IFile) delta.getResource(), container).parse();
							index.addObject(container);
						}
						else if (delta.getResource().getName().equals("DefCore.txt")) {
							C4DefCoreWrapper defCore = new C4DefCoreWrapper((IFile) delta.getResource());
							defCore.parse();
							container.setId(defCore.getObjectID());
							if (container instanceof C4ObjectIntern) {
								((C4ObjectIntern)container).getObjectFolder().setPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID, defCore.getObjectID().getName());
							}
						}
					} catch (CompilerException e) {
						// TODO display CompilerException messages
						e.printStackTrace();
					}
				} else {
					/// ???
				}
			}
			if (monitor != null) monitor.worked(1);
		}
		if (delta.getResource() instanceof IContainer)
			return true;
		else
			return false;
	}

	public boolean visit(IResource resource) throws CoreException {
		if (resource == null)
			return false;
//		if (resource instanceof IFile) {
//			C4Object container = Utilities.getProject(resource).getIndexedData()
//				.getObject(C4ID.getID(resource.getParent().getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID)));
//			try {
//				if (resource.getName().endsWith(".c")) {
//					new C4ScriptParser((IFile) resource, container).parse();
//				}
//				else if (resource.getName().equals("DefCore.txt")) {
//					new C4DefCoreParser((IFile) resource).parse();
//				}
//			} catch (CompilerException e) {
//				e.printStackTrace();
//			}
//			return false;
//		}
		// TODO i think we should delete the complete index here, since we regenerate it now completely
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
				if (monitor != null) monitor.worked(1);
				return true;
			case 1:
				// check correctness of function code (or compile into bytecodes ;D)
				ClonkIndex index = Utilities.getProject(resource).getIndexedData();
				C4Object obj = index.getObject((IContainer)resource);
				if (obj != null && obj.getScript() != null) {
					try {
						new C4ScriptParser((IFile)obj.getScript(), obj).parseCodeOfFunctions();
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
