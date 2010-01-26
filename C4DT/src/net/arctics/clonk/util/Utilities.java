package net.arctics.clonk.util;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.navigator.ClonkLabelProvider;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Contains various utility functions
 *
 */
public abstract class Utilities {
	
	private static MessageConsole clonkConsole = null;
	private static MessageConsoleStream debugConsoleStream = null;
	
	/**
	 * All Clonk projects in the current workspace
	 * @return array containing the Clonk projects
	 */
	public static IProject[] getClonkProjects() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		
		// Filter out all projects with Clonk nature
		Collection<IProject> c = new LinkedList<IProject>();
		for(IProject proj : projects)
			if (ClonkProjectNature.get(proj) != null)
				c.add(proj);
			
		return c.toArray(new IProject [c.size()]);
	}
	
	public static MessageConsole getClonkConsole() {
		if (clonkConsole == null) {
			clonkConsole = getConsole(Messages.Utilities_0);
		}
		return clonkConsole;
	}
	
	public static MessageConsole getConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		//no console found, so create a new one
		MessageConsole console = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[]{console});
		return console;
	}
	
	public static MessageConsoleStream getDebugStream() {
		if (debugConsoleStream == null) {
			debugConsoleStream = getConsole(Messages.Utilities_1).newMessageStream();
		}
		return debugConsoleStream;
	}

	public static void displayClonkConsole() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				String id = IConsoleConstants.ID_CONSOLE_VIEW;

				// show console 
				try {
					IConsoleView view = (IConsoleView) page.showView(id);
					view.display(getClonkConsole());
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public static ClonkIndex getIndex(IResource res) {
		if (res != null) {
			ClonkProjectNature nature = ClonkProjectNature.get(res);
			if (nature != null) {
				return nature.getIndex();
			}
		}
		return null;
	}
	
	public static IFile getEditingFile(IEditorPart editor) {
		if (editor.getEditorInput() instanceof FileEditorInput) {
			return ((FileEditorInput)editor.getEditorInput()).getFile();
		}
		else return null;
	}
	
	public static C4ScriptBase getScriptForResource(IResource resource) throws CoreException {
		if (resource instanceof IContainer)
			return C4ObjectIntern.objectCorrespondingTo((IContainer) resource);
		else if (resource instanceof IFile)
			return getScriptForFile((IFile) resource);
		else
			return null;
	}
	
	public static C4ScriptBase getScriptForFile(IFile scriptFile) {
		C4ScriptBase script;
		if (scriptFile == null)
			return null;
		try {
			script = C4ScriptIntern.pinnedScript(scriptFile, false);
		} catch (CoreException e) {
			script = null;
		}
		if (script == null)
			script = C4ObjectIntern.objectCorrespondingTo(scriptFile.getParent());
		// there can only be one script oO (not ScriptDE or something)
		if (script == null || script.getScriptFile() == null || !script.getScriptFile().equals(scriptFile))
			return null;
		return script;
	}

	public static C4ScriptBase getScriptForEditor(IEditorPart editor) {
		if (editor instanceof C4ScriptEditor)
			return ((C4ScriptEditor) editor).scriptBeingEdited();
		else
			return null;
	}

	public static boolean looksLikeID(String word) {
		if (word == null || word.length() != 4)
			return false;
		int digits = 0;
		for(int i = 0; i < 4;i++) {
			int readChar = word.charAt(i);
			if ('0' <= readChar && readChar <= '9')
				digits++;
			if (('A' <= readChar && readChar <= 'Z') ||
			    ('0' <= readChar && readChar <= '9') ||
			    (readChar == '_')) {
				continue;
			}
			else {
				return false;
			}
		}
		return digits != 4; // rather interpret 1000 as int
	}

	/**
	 * Shorthand for comparing resources
	 * @param a the first resource
	 * @param b the second resource
	 * @return true if resources are both null or denote the same resource, false if not
	 */
	public static boolean objectsEqual(Object a, Object b) {
		return (a == null && b == null) || (a != null && b != null && a.equals(b));
	}
	
	/**
	 * Returns whether resource somewhere below container in the file hierarchy
	 * @param resource the resource
	 * @param container the container
	 * @return true if resource is below container, false if not
	 */
	public static boolean resourceInside(IResource resource, IContainer container) {
		for (IContainer c = resource instanceof IContainer ? (IContainer)resource : resource.getParent(); c != null; c = c.getParent())
			if (c.equals(container))
				return true;
		return false;
	}
	
	/**
	 * Return the hops needed to get to a parent folder of a that also contains b
	 * @param a
	 * @param b
	 * @return The distance to a container that both a and b are contained in
	 */
	public static int distanceToCommonContainer(IResource a, IResource b) {
		IContainer c;
		int dist = 0;
		for (c = a instanceof IContainer ? (IContainer)a : a.getParent(); c != null; c = c.getParent()) {
			if (resourceInside(b, c))
				break;
			dist++;
		}
		return dist;
	}
	
	public static <T extends IHasRelatedResource> T pickNearest(IResource resource, Collection<T> fromList, IPredicate<T> filter) {
		int bestDist = 1000;
		T best = null;
		if (fromList != null) {
			for (T o : fromList) {
				if (filter != null && !filter.test(o))
					continue;
				IResource res = o.getResource();
				int newDist = res != null
					? distanceToCommonContainer(resource, res)
					: 100;
				if (best == null || newDist < bestDist) {
					best = o;
					bestDist = newDist;
				}
			}
		}
		return best;
	}
	
	// nowhere to be found oO
	/**
	 * Return the index of an item in an array
	 */
	public static <T> int indexOf(T[] items, T item) {
		for (int i = 0; i < items.length; i++)
			if (items[i].equals(item))
				return i;
		return -1;
	}
	
	/**
	 * Helper for creating a map with one assignment
	 * @param <KeyType> key type for resulting map
	 * @param <ValueType> value type for resulting map
	 * @param mapClass class the method is to instantiate
	 * @param keysAndValues array containing keys and values. keys are at even indices while values are at uneven ones
	 * @return the map
	 */
	@SuppressWarnings("unchecked")
	public static <KeyType, ValueType> Map<KeyType, ValueType> mapOfType(Class<? extends Map<KeyType, ValueType>> mapClass, Object... keysAndValues) {
		try {
			Map<KeyType, ValueType> map = mapClass.newInstance();
			for (int i = 0; i < keysAndValues.length-1; i += 2) {
				map.put((KeyType)keysAndValues[i], (ValueType)keysAndValues[i+1]);
			}
			return Collections.unmodifiableMap(map);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * like mapOfType, but called with HashMap.class
	 * @param <KeyType>
	 * @param <ValueType>
	 * @param keysAndValues
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	public static <KeyType, ValueType> Map<KeyType, ValueType> map(Object... keysAndValues) {
		return mapOfType((Class<? extends Map<KeyType, ValueType>>) HashMap.class, keysAndValues);
	}
	
	public static boolean allInstanceOf(Object[] objects, Class<?> cls) {
		for (Object item : objects)
			if (!(cls.isAssignableFrom(item.getClass())))
				return false;
		return true;
	} 

	public static Class<?> baseClass(Class<?> a, Class<?> b) {
		Class<?> result = a;
		while (!result.isAssignableFrom(b))
			result = result.getSuperclass();
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] concat(T[] a, T... b) {
		final int alen = a != null ? a.length : 0;
		final int blen = b != null ? b.length : 0;
		if (alen == 0) {
			return b != null ? b : (T[])new Object[0];
		}
		if (blen == 0) {
			return a != null ? a : (T[])new Object[0];
		}
		final T[] result = (T[]) Array.newInstance(baseClass(a.getClass().getComponentType(), b.getClass().getComponentType()), alen+blen);
		System.arraycopy(a, 0, result, 0, alen);
		System.arraycopy(b, 0, result, alen, blen);
		return result;
	}

	public static IRegion wordRegionAt(CharSequence line, int relativeOffset) {
		int start, end;
		relativeOffset = clamp(relativeOffset, 0, line.length()-1);
		start = end = relativeOffset;
		for (int s = relativeOffset; s >= 0 && BufferedScanner.isWordPart(line.charAt(s)); s--)
			start = s;
		for (int e = relativeOffset+1; e < line.length() && BufferedScanner.isWordPart(line.charAt(e)); e++)
			end = e;
		return new Region(start, end-start+1);
	}
	
	public static int clamp(int value, int min, int max) {
		if (value < min)
			return min;
		else if (value > max)
			return max;
		else
			return value;
	}

	public static void refreshClonkProjects(IProgressMonitor monitor) throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (monitor != null)
			monitor.beginTask(Messages.Utilities_9, projects.length);
		int work = 0;
		for (IProject p : projects) {
			if (ClonkProjectNature.get(p) != null)
				p.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			if (monitor != null)
				monitor.worked(work++);
		}
		if (monitor != null)
			monitor.done();
	}
	
	public static <T> T itemMatching(IPredicate<T> predicate, List<T> sectionsList) {
		for (T item : sectionsList)
			if (predicate.test(item))
				return item;
		return null;
	}
	
	public static Enum<?>[] valuesOfEnum(Class<?> enumClass) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return (Enum<?>[]) enumClass.getMethod("values").invoke(null); //$NON-NLS-1$
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] convertArray(Object[] baseArray, Class<T> newElementClass) {
		T[] result = (T[]) Array.newInstance(newElementClass, baseArray.length);
		System.arraycopy(baseArray, 0, result, 0, baseArray.length);
		return result;
	}
	
	public static <E, T extends Collection<E>> T collectionFromArray(Class<T> cls, E[] array) {
		try {
			T result = cls.newInstance();
			for (E e : array)
				result.add(e);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static <S, T extends S> boolean isAnyOf(S something, T... things) {
		if (something != null) {
			for (Object o : things) {
				if (something.equals(o))
					return true;
			}
		}
		return false;
	}
	
	public static String stringFromInputStream(InputStream stream, String encoding) throws IOException {
		InputStreamReader inputStreamReader = new InputStreamReader(stream, encoding);
		StringBuilder stringBuilder;
		try {
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			try {
				stringBuilder = new StringBuilder();
				char[] buffer = new char[1024];
				int read;
				while ((read = bufferedReader.read(buffer)) > 0) {
					stringBuilder.append(buffer, 0, read);
				}

			} finally {
				bufferedReader.close();
			}
		} finally {
			inputStreamReader.close();
		}
		return stringBuilder.toString();
	}
	
	public static String stringFromInputStream(InputStream stream) throws IOException {
		return stringFromInputStream(stream, "UTF-8"); //$NON-NLS-1$
	}

	public static String stringFromFile(IFile file) throws IOException, CoreException {
		TextFileDocumentProvider provider = ClonkCore.getDefault().getTextFileDocumentProvider();
		provider.connect(file);
		try {
			return provider.getDocument(file).get();
		} finally {
			provider.disconnect(file);
		}
	}

	public static CommonNavigator getProjectExplorer() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench != null) {
			return getProjectExplorer(workbench.getActiveWorkbenchWindow());
		}
		return null;
	}

	public static CommonNavigator getProjectExplorer(IWorkbenchWindow window) {
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IViewPart viewPart = page.findView(IPageLayout.ID_PROJECT_EXPLORER);
				if (viewPart instanceof CommonNavigator) {
					return (CommonNavigator) viewPart;
				}
			}
		}
		return null;
	}
	
	public static <T> boolean collectionContains(Collection<T> list, T elm) {
		for (T e : list)
			if (e.equals(elm))
				return true;
		return false;
	}
	
	public static <T> List<T> filter(Iterable<T> iterable, IPredicate<T> filter) {
		List<T> result = new LinkedList<T>();
		for (T elm : iterable)
			if (filter.test(elm))
				result.add(elm);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] filter(T[] array, IPredicate<T> filter) {
		try {
			List<T> list = filter(arrayIterable(array), filter);
			return list.toArray((T[]) Array.newInstance(array.getClass().getComponentType(), list.size()));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static <T> Iterable<T> arrayIterable(final T[] items) {
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private int index = -1;
					public boolean hasNext() {
						return index+1<items.length;
					}

					public T next() throws NoSuchElementException {
						try {
							return items[++index];
						} catch (ArrayIndexOutOfBoundsException e) {
							throw new NoSuchElementException("Array iterator fail");
						}
					}

					public void remove() {
					}
				};
			}
		};
	}
	
	public static IProject getDependenciesProject() {
		IViewPart projExp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IPageLayout.ID_PROJECT_EXPLORER);
		CommonViewer commonViewer = (CommonViewer) projExp.getAdapter(CommonViewer.class);
		if (commonViewer != null) {
			ISelection sel = commonViewer.getSelection();
			if (sel instanceof ITreeSelection) {
				ITreeSelection treeSel = (ITreeSelection) sel;
				if (treeSel.getFirstElement() != null) {
					TreePath[] treePaths = treeSel.getPathsFor(treeSel.getFirstElement());
					if (treePaths.length > 0) {
						for (int i = 0; i < treePaths[0].getSegmentCount(); i++) {
							if (treePaths[0].getSegment(i) instanceof IProject) {
								try {
									if (((IProject)treePaths[0].getSegment(i)).hasNature(ClonkCore.CLONK_DEPS_NATURE_ID)) {
										return (IProject) treePaths[0].getSegment(i);
									}
								} catch (CoreException e) {}
								break;
							}
						}
					}
				}
			}
		}
		for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			try {
	            if (p.isOpen() && p.hasNature(ClonkCore.CLONK_DEPS_NATURE_ID))
	            	return p;
            } catch (CoreException e) {	        
	            e.printStackTrace();
	            return null;
            }
		}
		return null;
	}
	
	public static void setShowsDependencies(IProject clonkProj, boolean has) throws CoreException {
		if (has != clonkProj.hasNature(ClonkCore.CLONK_DEPS_NATURE_ID)) {
			String[] oldNatures = clonkProj.getDescription().getNatureIds();
			String[] newNatures;
			if (has)
				newNatures = concat(oldNatures, ClonkCore.CLONK_DEPS_NATURE_ID);
			else
				newNatures = filter(oldNatures, new IPredicate<String>() {
					@Override
					public boolean test(String item) {
						return !item.equals(ClonkCore.CLONK_DEPS_NATURE_ID);
					}
				});
			IProjectDescription desc = clonkProj.getDescription();
			desc.setNatureIds(newNatures);
			clonkProj.setDescription(desc, null);
		}
	}
	
	public static void errorMessage(Throwable error, final String title) {
		String message = error.getClass().getSimpleName();
		if (error.getLocalizedMessage() != null)
			message += ": " + error.getLocalizedMessage(); //$NON-NLS-1$
		errorMessage(message, title);
	}
	
	public static void errorMessage(final String message, final String title) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog messageDialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					title == null ? Messages.Utilities_12 : title, null,
					message, MessageDialog.ERROR,
					new String[] { Messages.Utilities_13 }, 1
				);
				messageDialog.open();
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public static <From, To> To[] map(From[] elms, Class<To> toClass, IConverter<From, To> converter) {
		To[] result = (To[]) Array.newInstance(toClass, elms.length);
		for (int i = 0; i < result.length; i++)
			result[i] = converter.convert(elms[i]);
		return result;
	}
	
	public static IResource findMemberCaseInsensitively(IContainer container, String name) {
		try {
	        for (IResource child : container.members()) {
	        	if (child.getName().equalsIgnoreCase(name))
	        		return child;
	        }
        } catch (CoreException e) {
	        e.printStackTrace();
        }
		return null;
	}

	@SuppressWarnings("unchecked")
    public static <T> T[] concat(T first, T... rest) {
		T[] result = (T[]) Array.newInstance(rest.getClass().getComponentType(), 1+rest.length);
		result[0] = first;
		for (int i = 0; i < rest.length; i++)
			result[1+i] = rest[i];
		return result;
    }

	/**
	 * Returns true if there is anything in items that matches the predicate
	 * @param <T>
	 * @param items items
	 * @param predicate predicate
	 * @return see above
	 */
	public static <T> boolean any(Iterable<T> items, IPredicate<T> predicate) {
		for (T item : items)
			if (predicate.test(item))
				return true;
		return false;
	}

	@SuppressWarnings("rawtypes")
	public static final IConverter<Object, Class> INSTANCE_TO_CLASS_CONVERTER = new IConverter<Object, Class>() {

		@Override
		public Class convert(Object from) {
			return from.getClass();
		}
	};
	
	public static Class<?>[] getParameterTypes(Object[] constructorArgs) {
		return map(constructorArgs, Class.class, INSTANCE_TO_CLASS_CONVERTER);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Set<T> arrayToSet(T[] arr, Class<? extends Set> setClass) {
		try {
			Set<T> result = setClass.newInstance();
			for (T elm : arr)
				result.add(elm);
			return result;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}	
	}

	public static IProject clonkProjectSelectionDialog(IProject initialSelection) {
		// Create dialog listing all Clonk projects
		ElementListSelectionDialog dialog
			= new ElementListSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), new ClonkLabelProvider());
		dialog.setTitle("Choose Clonk Project");
		dialog.setMessage("Please choose a Clonk Project");
		dialog.setElements(Utilities.getClonkProjects());

		// Set selection
		dialog.setInitialSelections(new Object [] { initialSelection });

		// Show
		if(dialog.open() == Window.OK) {
			return (IProject) dialog.getFirstResult();
		}
		else
			return null;
	}
	
}