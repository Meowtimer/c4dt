package net.arctics.clonk.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Contains various utility functions
 *
 */
public abstract class Utilities {

	public static IFile fileEditedBy(final IEditorPart editor) {
		final IEditorInput editorInput = editor.getEditorInput();
		return fileFromEditorInput(editorInput);
	}

	public static IFile fileFromEditorInput(final IEditorInput editorInput) {
		if (editorInput instanceof FileEditorInput)
			return ((FileEditorInput)editorInput).getFile();
		else
			return null;
	}

	public static Script scriptForResource(final IResource resource) {
		if (resource instanceof IContainer)
			return Definition.at((IContainer) resource);
		else if (resource instanceof IFile)
			return Script.get(resource, true);
		else
			return null;
	}

	public static Script scriptForEditor(final IEditorPart editor) {
		if (editor instanceof C4ScriptEditor)
			return ((C4ScriptEditor) editor).script();
		else
			return null;
	}

	/**
	 * Shorthand for comparing resources
	 * @param a the first resource
	 * @param b the second resource
	 * @return true if resources are both null or denote the same resource, false if not
	 */
	public static boolean eq(final Object a, final Object b) {
		return (a == null && b == null) || (a != null && b != null && (a.equals(b)||b.equals(a)));
	}

	public static boolean objectsNonNullEqual(final Object a, final Object b) {
		return a != null && b != null && a.equals(b);
	}

	/**
	 * Returns whether resource somewhere below container in the file hierarchy
	 * @param resource the resource
	 * @param container the container
	 * @return true if resource is below container, false if not
	 */
	public static boolean resourceInside(final IResource resource, final IContainer container) {
		for (IContainer c = resource instanceof IContainer ? (IContainer)resource : resource.getParent(); c != null; c = c.getParent())
			if (c.equals(container))
				return true;
		return false;
	}

	private static int distanceToCommonContainer(final IResource a, final IResource b, Scenario aScenario, Scenario bScenario) {
		IContainer c;
		int dist = 0;
		for (c = a instanceof IContainer ? (IContainer)a : a.getParent(); c != null; c = c.getParent()) {
			if (resourceInside(b, c))
				break;
			dist++;
		}
		if (aScenario == null)
			aScenario = Scenario.containingScenario(a);
		if (bScenario == null)
			bScenario = Scenario.containingScenario(b);
		if (aScenario != bScenario) {
			dist += 500; // penalty for scenario boundary
			if (aScenario != null && bScenario != null)
				dist += 500; // double penalty for different scenarios
		}
		return dist;
	}

	/**
	 * From some list containing {@link IHasRelatedResource} thingies, pick the one with the least amount of hops between its related {@link IResource} ({@link IHasRelatedResource#resource()}) and the specified {@link IResource}
	 * @param <T> The type of elements in the passed list, constrained to extend {@link IHasRelatedResource}
	 * @param fromList The list to pick the result from
	 * @param resource The pivot dictating the perspective of the call.
	 * @param filter A filter to exclude some of the items contained in the list
	 * @return The item 'nearest' to resource
	 */
	public static <T extends IHasRelatedResource> T pickNearest(final List<? extends T> fromList, final IResource resource, final Predicate<T> filter) {
		int bestDist = Integer.MAX_VALUE;
		T best = null;
		if (fromList != null) {
			if (fromList.size() == 1) {
				final T soleItem = fromList.get(0);
				return filter == null || filter.test(soleItem) ? soleItem : null;
			}
			final Scenario scen = Scenario.containingScenario(resource);
			for (final T o : fromList) {
				if (filter != null && !filter.test(o))
					continue;
				final IResource res = o.resource();
				final int newDist = res != null
					? distanceToCommonContainer(resource, res, scen, null)
					: Integer.MAX_VALUE;
				if (best == null || newDist < bestDist) {
					best = o;
					bestDist = newDist;
				}
			}
		}
		return best;
	}

	public static boolean allInstanceOf(final Object[] objects, final Class<?> cls) {
		for (final Object item : objects)
			if (!(cls.isAssignableFrom(item.getClass())))
				return false;
		return true;
	}

	public static Class<?> baseClass(final Class<?> a, final Class<?> b) {
		Class<?> result = a;
		while (!result.isAssignableFrom(b))
			result = result.getSuperclass();
		return result;
	}

	public static IRegion wordRegionAt(final CharSequence line, int relativeOffset) {
		int start, end;
		relativeOffset = clamp(relativeOffset, 0, line.length()-1);
		start = end = relativeOffset;
		for (int s = relativeOffset; s >= 0 && BufferedScanner.isWordPart(line.charAt(s)); s--)
			start = s;
		for (int e = relativeOffset+1; e < line.length() && BufferedScanner.isWordPart(line.charAt(e)); e++)
			end = e;
		return new Region(start, end-start+1);
	}

	public static boolean regionContainsOffset(final IRegion region, final int offset) {
		return offset >= region.getOffset() && offset < region.getOffset() + region.getLength();
	}

	public static boolean regionContainsOtherRegion(final IRegion region, final IRegion otherRegion) {
		return otherRegion.getOffset() >= region.getOffset() && otherRegion.getOffset()+otherRegion.getLength() < region.getOffset()+region.getLength();
	}

	public static int clamp(final int value, final int min, final int max) {
		if (value < min)
			return min;
		else if (value > max)
			return max;
		else
			return value;
	}

	public static <T> T itemMatching(final Predicate<T> predicate, final List<T> sectionsList) {
		for (final T item : sectionsList)
			if (predicate.test(item))
				return item;
		return null;
	}

	public static Enum<?>[] enumValues(final Class<?> enumClass) {
		try {
			return (Enum<?>[]) enumClass.getMethod("values").invoke(null); //$NON-NLS-1$
		} catch (IllegalAccessException | IllegalArgumentException
			| InvocationTargetException | NoSuchMethodException
			| SecurityException e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T enumValueFromString(final Class<T> enumClass, final String value) {
		try {
			return (T) Enum.valueOf((Class<Enum>)enumClass, value);
		} catch (final IllegalArgumentException e) {
			try {
				return (T) Enum.valueOf((Class<Enum>)enumClass, makeJavaConstantString(value));
			} catch (final IllegalArgumentException e2) {
				try {
					final Field f = enumClass.getField(value);
					return (T) f.get(null);
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException e1) {
					return (T) enumValues(enumClass)[0];
				}
			}
		}
	}

	private static String makeJavaConstantString(final String value) {
		final StringBuilder builder = new StringBuilder(value.length()+5);
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			if (Character.isUpperCase(c)) {
				if (i > 0)
					builder.append('_');
				builder.append(c);
			} else
				builder.append(Character.toUpperCase(c));
		}
		return builder.toString();
	}

	public static <E, T extends Collection<E>> T collectionFromArray(final Class<T> cls, final E[] array) {
		try {
			final T result = cls.newInstance();
			for (final E e : array)
				result.add(e);
			return result;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static <S, T extends S> boolean isAnyOf(final S something, @SuppressWarnings("unchecked") final T... things) {
		if (something != null)
			for (final Object o : things)
				if (something.equals(o))
					return true;
		return false;
	}

	public static <T> boolean collectionContains(final Collection<T> list, final T elm) {
		for (final T e : list)
			if (e.equals(elm))
				return true;
		return false;
	}

	public static void errorMessage(final Throwable error, final String title) {
		String message = error.getClass().getSimpleName();
		if (error.getLocalizedMessage() != null)
			message += ": " + error.getLocalizedMessage(); //$NON-NLS-1$
		errorMessage(message, title);
	}

	public static void errorMessage(final String message, final String title) {
		Display.getDefault().asyncExec(() -> {
			final MessageDialog messageDialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				title == null ? Messages.Utilities_InternalError : title, null,
				message, MessageDialog.ERROR,
				new String[] { Messages.Utilities_InternalErrorButton }, 1
			);
			messageDialog.open();
		});
	}

	public static IResource findMemberCaseInsensitively(final IContainer container, final String name) {
		if (container == null)
			return null;
		try {
	        for (final IResource child : container.members())
				if (child.getName().equalsIgnoreCase(name))
	        		return child;
        } catch (final CoreException e) {
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
	public static <T> boolean any(final Iterable<? extends T> items, final Predicate<T> predicate) {
		for (final T item : items)
			if (predicate.test(item))
				return true;
		return false;
	}

	@SuppressWarnings("rawtypes")
	public static final IConverter<Object, Class> INSTANCE_TO_CLASS_CONVERTER = from -> from.getClass();

	public static <A, B> B as(final A obj, final Class<B> type) {
		return type.isInstance(obj) ? type.cast(obj) : null;
	}

	public static <A> A defaulting(final A firstChoice, final A defaultChoice) {
		return firstChoice != null ? firstChoice : defaultChoice;
	}

	public static <A> A or(final A a, final A b) {
		if (a != null)
			return a;
		else
			return b;
	}

	public static Object token(final String token) {
		return new Object() {
			@Override
			public String toString() {
				return token;
			}
		};
	}

	private static Object autoBuildDisablingLock = new Object();

	public static void runWithoutAutoBuild(final Runnable runnable) {
		synchronized (autoBuildDisablingLock) {
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			final IWorkspaceDescription workspaceDescription = workspace.getDescription();
			final boolean autoBuilding = workspaceDescription.isAutoBuilding();
			workspaceDescription.setAutoBuilding(false);
			try {
				workspace.setDescription(workspaceDescription);
			} catch (final CoreException e2) {
				e2.printStackTrace();
			}
			try {
				runnable.run();
			} finally {
				workspaceDescription.setAutoBuilding(autoBuilding);
				try {
					workspace.setDescription(workspaceDescription);
				} catch (final CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void removeRecursively(final File f) {
		if (f.isDirectory())
			for (final File fi : f.listFiles())
				removeRecursively(fi);
		f.delete();
	}

	public static void abort(final int severity, final String message, final Throwable nested) throws CoreException {
		throw new CoreException(new Status(severity, Core.PLUGIN_ID, message, nested));
	}

	/** Helper for throwing CoreException objects */
	public static void abort(final int severity, final String message) throws CoreException {
		throw new CoreException(new Status(severity, Core.PLUGIN_ID, message));
	}

}