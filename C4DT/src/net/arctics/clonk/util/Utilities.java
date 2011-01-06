package net.arctics.clonk.util;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
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
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Contains various utility functions
 *
 */
public abstract class Utilities {
	
	private static MessageConsole clonkConsole = null;
	private static MessageConsoleStream debugConsoleStream = null;
	
	public static MessageConsole getClonkConsole() {
		if (clonkConsole == null) {
			clonkConsole = getConsole(Messages.Utilities_ClonkConsole);
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
			debugConsoleStream = getConsole(Messages.Utilities_DebugConsole).newMessageStream();
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
			return C4ScriptBase.get(resource, true);
		else
			return null;
	}

	public static C4ScriptBase getScriptForEditor(IEditorPart editor) {
		if (editor instanceof C4ScriptEditor)
			return ((C4ScriptEditor) editor).scriptBeingEdited();
		else
			return null;
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
	public static <KeyType, ValueType> Map<KeyType, ValueType> mapOfType(Map<KeyType, ValueType> resultMap, Object... keysAndValues) {
		try {
			for (int i = 0; i < keysAndValues.length-1; i += 2) {
				resultMap.put((KeyType)keysAndValues[i], (ValueType)keysAndValues[i+1]);
			}
			return Collections.unmodifiableMap(resultMap);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static <KeyType, ValueType> Map<ValueType, KeyType> reverseMap(Map<KeyType, ValueType> originalMap, Map<ValueType, KeyType> resultMap) {
		try {
			for (Map.Entry<KeyType, ValueType> entry : originalMap.entrySet()) {
				resultMap.put(entry.getValue(), entry.getKey());
			}
			return resultMap;
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
	public static <KeyType, ValueType> Map<KeyType, ValueType> map(Object... keysAndValues) {
		return mapOfType(new HashMap<KeyType, ValueType>(), keysAndValues);
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
	
	public static <T> T itemMatching(IPredicate<T> predicate, List<T> sectionsList) {
		for (T item : sectionsList)
			if (predicate.test(item))
				return item;
		return null;
	}
	
	public static Enum<?>[] valuesOfEnum(Class<?> enumClass) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return (Enum<?>[]) enumClass.getMethod("values").invoke(null); //$NON-NLS-1$
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

	public static CommonNavigator getProjectExplorer() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench != null) {
			return getProjectExplorer(workbench.getActiveWorkbenchWindow());
		}
		return null;
	}
	
	public static ISelection getProjectExplorerSelection(IWorkbenchSite site) {
		return site.getWorkbenchWindow().getSelectionService().getSelection(IPageLayout.ID_PROJECT_EXPLORER);
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
	
	public static <T> List<T> filter(Iterable<? extends T> iterable, IPredicate<T> filter) {
		List<T> result = new LinkedList<T>();
		for (T elm : iterable)
			if (filter.test(elm))
				result.add(elm);
		return result;
	}
	
	public static @SuppressWarnings("unchecked")
	<T, U extends T> Iterable<U> filter(Iterable<T> iterable, IPredicate<U> filter, Class<? extends T> cls) {
		List<U> result = new LinkedList<U>();
		for (T elm : iterable) {
			if (cls.isAssignableFrom(elm.getClass()) && filter.test((U)elm)) {
				result.add((U) elm);
			}
		}
		return result;
	}
	
	public static <T> IPredicate<T> classMembershipPredicate(final Class<? extends T> cls) {
		return new IPredicate<T>() {
			@Override
			public boolean test(T item) {
				return cls.isAssignableFrom(item.getClass());
			}
		};
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
					title == null ? Messages.Utilities_InternalError : title, null,
					message, MessageDialog.ERROR,
					new String[] { Messages.Utilities_InternalErrorButton }, 1
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
	        //e.printStackTrace(); just return null, will just be some case of having a referenced container that does not exist anymore
        }
		return null;
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
	
	public static <T> Set<T> set(@SuppressWarnings("rawtypes") Class<? extends Set> cls, T... elements) {
		return arrayToSet(elements, cls);
	}
	
	public static String multiply(String s, int times) {
		StringBuilder builder = new StringBuilder(s.length()*times);
		for (int i = 0; i < times; i++)
			builder.append(s);
		return builder.toString();
	}
	
// Copy-Pasta org.eclipse.jdt.internal.ui.text.correction.NameMatcher
	
	private static boolean isSimilarChar(char ch1, char ch2) {
		return Character.toLowerCase(ch1) == Character.toLowerCase(ch2);
	}
	
	/**
	 * Returns a similarity value of the two names.
	 * The range of is from 0 to 256. no similarity is negative
	 * @param name1 the first name
	 * @param name2 the second name
	 * @return the similarity valuer
	 */
	public static int getSimilarity(String name1, String name2) {
		if (name1.length() > name2.length()) {
			String tmp= name1;
			name1= name2;
			name2= tmp;
		}
		int name1len= name1.length();
		int name2len= name2.length();

		int nMatched= 0;

		int i= 0;
		while (i < name1len && isSimilarChar(name1.charAt(i), name2.charAt(i))) {
			i++;
			nMatched++;
		}

		int k= name1len;
		int diff= name2len - name1len;
		while (k > i && isSimilarChar(name1.charAt(k - 1), name2.charAt(k + diff - 1))) {
			k--;
			nMatched++;
		}

		if (nMatched == name2len) {
			return 200;
		}

		if (name2len - nMatched > nMatched) {
			return -1;
		}

		int tolerance= name2len / 4 + 1;
		return (tolerance - (k - i)) * 256 / tolerance;
	}
	
	// --------
	
	public static String htmlerize(String text) {
		return text.
			replace("<", "&lt;"). //$NON-NLS-1$ //$NON-NLS-2$
			replace(">", "&gt;"). //$NON-NLS-1$ //$NON-NLS-2$
			replace("\n", " "). //$NON-NLS-1$ //$NON-NLS-2$
			replace("\t", " "); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}