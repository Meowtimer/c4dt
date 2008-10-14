package net.arctics.clonk.resource;

import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4DefCoreWrapper;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ObjectIntern;
import net.arctics.clonk.parser.C4ObjectParser;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.ui.editors.C4ScriptEditor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
public class ClonkBuilder extends IncrementalProjectBuilder implements IResourceDeltaVisitor, IResourceVisitor {
	
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
					
					// parse code bodies
					buildPhase = 1;
					proj.accept(this);
				}
				proj.touch(monitor);
			}
			
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					IWorkbench w = PlatformUI.getWorkbench();
					if (w == null)
						return;
					IWorkbenchPage page = w.getActiveWorkbenchWindow().getActivePage();
					for (IEditorReference ref : page.getEditorReferences()) {
						IEditorPart part = ref.getEditor(false);
						if (part != null && part instanceof C4ScriptEditor) {
							C4ScriptEditor scriptEd = (C4ScriptEditor)part;
							scriptEd.getOutlinePage().refresh();
						}
					}
				}
			});
			
			// saves all objects persistent
			Utilities.getProject(proj).getIndexedData().saveIndexData();

			monitor.done();
			return new IProject[] { proj };
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
						new C4ScriptParser((IFile)obj.getScript(), obj).parseFunctionCode();
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

}
