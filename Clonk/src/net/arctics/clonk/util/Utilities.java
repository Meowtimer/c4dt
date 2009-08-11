package net.arctics.clonk.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4ObjectExternGroup;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.inireader.ActMapUnit;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.inireader.MaterialUnit;
import net.arctics.clonk.parser.inireader.ParticleUnit;
import net.arctics.clonk.parser.inireader.ScenarioUnit;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Contains various utility functions
 *
 */
public abstract class Utilities {
	
	private static MessageConsole clonkConsole = null;
	private static MessageConsoleStream debugConsoleStream = null;
	
	/**
	 * Returns the clonk project nature of the project the file that is being edited using the supplied editor belongs to
	 * @param editor the editor
	 * @return the nature
	 */
	public static ClonkProjectNature getClonkNature(ITextEditor editor) {
		if (editor.getEditorInput() instanceof FileEditorInput) {
			return getClonkNature(((FileEditorInput)editor.getEditorInput()).getFile());
		}
		return null;
	}
	
	/**
	 * Returns the clonk project nature associated with the project of res
	 * @param res the resource
	 * @return the nature
	 */
	public static ClonkProjectNature getClonkNature(IResource res) {
		if (res == null) return null;
		IProject project = res.getProject();
		if (project == null || !project.isOpen()) return null;
		try {
			IProjectNature clonkProj = project.getNature(ClonkCore.id("clonknature"));
			return (ClonkProjectNature) clonkProj;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 
	 * @param script
	 * @return
	 */
	public static ClonkProjectNature getProject(C4ScriptBase script) {
		if (script == null)
			return null;
		if (script instanceof C4ObjectIntern)
			return getClonkNature(((C4ObjectIntern)script).getObjectFolder());
		if (script instanceof C4ScriptIntern)
			return getClonkNature(((C4ScriptIntern)script).getScriptFile());
		else
			return null;
	}
	
	/** @return All Clonk projects in the current workspace */
	public static IProject[] getClonkProjects() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		
		// Filter out all projects with Clonk nature
		Collection<IProject> c = new LinkedList<IProject>();
		for(IProject proj : projects)
			try {
				proj.getNature(ClonkCore.CLONK_NATURE_ID);
				c.add(proj);
			}
			catch(CoreException e) { }
			
		return c.toArray(new IProject [c.size()]);
	}
	
	public static MessageConsole getClonkConsole() {
		if (clonkConsole == null) {
			clonkConsole = getConsole("Clonk");
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
			debugConsoleStream = getConsole("Clonk Debug").newMessageStream();
		}
		return debugConsoleStream;
	}
	
	public static void displayClonkConsole() {
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
	
	public static ClonkIndex getIndex(IResource res) {
		if (res != null) {
			ClonkProjectNature nature = getClonkNature(res);
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
	
	public static Image getIconForFunction(C4Function function) {
		String iconName = function.getVisibility().name().toLowerCase();
		return ClonkCore.getDefault().getIconImage(iconName);
	}
	
	public static Image getIconForVariable(C4Variable variable) {
		String iconName = variable.getScope().toString().toLowerCase();
		return ClonkCore.getDefault().getIconImage(iconName);
	}

	public static Image getIconForObject(Object element) {
		if (element instanceof C4Function)
			return getIconForFunction((C4Function)element);
		if (element instanceof C4Variable)
			return getIconForVariable((C4Variable)element);
		if (element instanceof C4Object)
			return Icons.GENERAL_OBJECT_ICON;
		if (element instanceof C4ObjectExternGroup) {
			C4ObjectExternGroup group = (C4ObjectExternGroup) element;
			if (group.getNodeName().endsWith(".c4g"))
				return Icons.GROUP_ICON;
			return Icons.GENERAL_OBJECT_ICON;
		}
		if (element instanceof C4ScriptBase)
			return Icons.SCRIPT_ICON;
		return null;
	}
	
	public static Image getIconForC4Object(C4Object element) {
		return Icons.GENERAL_OBJECT_ICON;
//		Image base = new Image(PlatformUI.getWorkbench().getDisplay(),FileLocator.find(null, null, null).openStream());
//		ImageData data = base.getImageData();
//		org.eclipse.swt.graphics.
//		ImageData newData = data.scaledTo(16, 16);
//		return new Image(PlatformUI.getWorkbench().getDisplay(), newData);
	}

	public static ImageDescriptor getIconDescriptor(String path) {
		return ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path(path), null));
	}
	
	public static Image getIconImage(String registryKey, String iconPath) {
		ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
		Image img = reg.get(registryKey);
		if (img == null) {
			reg.put(registryKey, Utilities.getIconDescriptor(iconPath));
			img = reg.get(registryKey);
		}
//			if (element.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE).length > 0) {
//				return decorateImage(reg.getDescriptor(registryKey), element).createImage();
//			}
		return img;
	}
	
	public static C4ScriptBase getScriptForFile(IFile scriptFile) {
		C4ScriptBase script;
		try {
			script = C4ScriptIntern.pinnedScript(scriptFile);
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
		if (editor.getEditorInput() instanceof ScriptWithStorageEditorInput) {
			return ((ScriptWithStorageEditorInput)editor.getEditorInput()).getScript();
		}
		return getScriptForFile(getEditingFile(editor));
	}
	
	public static C4GroupType groupTypeFromFolderName(String name) {
		C4GroupType result = C4Group.extensionToGroupTypeMap.get(name.substring(name.lastIndexOf(".")+1));
		if (result != null)
			return result;
		return C4GroupType.OtherGroup;
	}
	
	public static boolean c4FilenameExtensionIs(String filename, String ext) {
		return filename.endsWith(ext);
	}

	public static boolean looksLikeID(String word) {
		if (word == null || word.length() < 4)
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
	public static boolean resourceEqual(IResource a, IResource b) {
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
	@SuppressWarnings("unchecked")
	public static <KeyType, ValueType> Map<KeyType, ValueType> map(Object... keysAndValues) {
		return mapOfType((Class<? extends Map<KeyType, ValueType>>) HashMap.class, keysAndValues);
	}
	
	private static Map<String, Class<? extends IniUnit>> INIREADER_CLASSES = map(new Object[] {
		ClonkCore.id("scenariocfg"), ScenarioUnit.class,
		ClonkCore.id("actmap")     , ActMapUnit.class,
		ClonkCore.id("defcore")    , DefCoreUnit.class,
		ClonkCore.id("particle")   , ParticleUnit.class,
		ClonkCore.id("material")   , MaterialUnit.class
	});

	/**
	 * Returns the IniUnit class that is best suited to parsing the given ini file
	 * @param file the ini file to return an IniUnit class for
	 * @return the IniUnit class or null if no suitable one could be found
	 */
	public static Class<? extends IniUnit> getIniUnitClass(IFile file) {
		IContentType contentType = IDE.getContentType(file);
		if (contentType == null)
			return null;
		return INIREADER_CLASSES.get(contentType.getId());
	}
	
	public static boolean allInstanceOf(Object[] objects, Class<?> cls) {
		for (Object item : objects)
			if (!(cls.isAssignableFrom(item.getClass())))
				return false;
		return true;
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
		final T[] result = (T[]) new Object[alen + blen];
		System.arraycopy(a, 0, result, 0, alen);
		System.arraycopy(b, 0, result, alen, blen);
		return result;
	}
	
	public static IniUnit createAdequateIniUnit(IFile file) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		return createAdequateIniUnit(file, file);
	}

	public static IniUnit createAdequateIniUnit(IFile file, Object arg) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<? extends IniUnit> cls = getIniUnitClass(file);
		if (cls == null)
			return null;
		Class<?> neededArgType =
			arg instanceof String
				? String.class
				: arg instanceof IFile
					? IFile.class
					: InputStream.class;
		Constructor<? extends IniUnit> ctor = cls.getConstructor(neededArgType);
		IniUnit result = ctor.newInstance(arg);
		result.setIniFile(file);
		return result;
	}

	public static IRegion wordRegionAt(CharSequence line, int relativeOffset) {
		int start, end;
		start = end = relativeOffset;
		for (int s = relativeOffset; s >= 0 && BufferedScanner.isWordPart(line.charAt(s)); s--)
			start = s;
		for (int e = relativeOffset+1; e < line.length() && BufferedScanner.isWordPart(line.charAt(e)); e++)
			end = e;
		return new Region(start, end-start+1);
	}
	
	public static void refreshClonkProjects(IProgressMonitor monitor) throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (monitor != null)
			monitor.beginTask("Refreshing Clonk projects", projects.length);
		int work = 0;
		for (IProject p : projects) {
			if (Utilities.getClonkNature(p) != null)
				p.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			monitor.worked(work++);
		}
		monitor.done();
	}
	
	public static <T> T itemMatching(Predicate<T> predicate, Iterable<T> iterable) {
		for (T item : iterable)
			if (predicate.test(item))
				return item;
		return null;
	}
	
	public static Enum<?>[] valuesOfEnum(Class<?> enumClass) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return (Enum<?>[]) enumClass.getMethod("values").invoke(null);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] convertArray(Object[] baseArray, Class<T> newElementClass) {
		T[] result = (T[]) Array.newInstance(newElementClass, baseArray.length);
		System.arraycopy(baseArray, 0, result, 0, baseArray.length);
		return result;
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
	
	public static String stringFromInputStream(InputStream stream) throws IOException {
		InputStreamReader inputStreamReader = new InputStreamReader(stream);
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
	
	public static String stringFromFile(IFile file) throws IOException, CoreException {
		InputStream stream = file.getContents();
		try {
			return stringFromInputStream(stream);
		} finally {
			stream.close();
		}
	}

	public static CommonNavigator getProjectExplorer() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench != null) {
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if (window != null) {
				IWorkbenchPage page = window.getActivePage();
				if (page != null) {
					IViewPart viewPart = page.findView(IPageLayout.ID_PROJECT_EXPLORER);
					if (viewPart instanceof CommonNavigator) {
						return (CommonNavigator) viewPart;
					}
				}
			}
		}
		return null;
	}
}