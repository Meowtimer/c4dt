package net.arctics.clonk.ui.navigator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;

public class ColorTagging extends ContributionItem {

	public static final QualifiedName COLOR_TAG = new QualifiedName(Core.PLUGIN_ID, "colorTag"); //$NON-NLS-1$
	public static final QualifiedName COLOR_RGB = new QualifiedName(Core.PLUGIN_ID, "colorRGB"); //$NON-NLS-1$

	private static final Map<String, RGB> existingTags = new HashMap<String, RGB>();

	public static Map<String, RGB> tags() {
		return existingTags;
	}

	public static RGB rgbForResource(final IResource resource) {
		try {
			final String tag = resource.getPersistentProperty(COLOR_TAG);
			final String rgb = resource.getPersistentProperty(COLOR_RGB);
			if (tag != null && rgb != null) {
				RGB result = existingTags.get(tag);
				if (result == null)
					existingTags.put(tag, result = StringConverter.asRGB(rgb));
				return result;
			} else
				return null;
		} catch (final Exception e) {
			return null;
		}
	}

	public ColorTagging() {
		super();
	}

	public ColorTagging(final String id) {
		super(id);
	}

	@Override
	public void fill(final Menu menu, final int index) {
		final SelectionListener menuItemListener = new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				@SuppressWarnings("unchecked")
				Map.Entry<String, RGB> tagInfo = (Map.Entry<String, RGB>) ((MenuItem)e.widget).getData("tagInfo"); //$NON-NLS-1$
				if (tagInfo == null) {
					final String tagName = UI.input(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.ColorTagging_ChooseColorDialogTitle, Messages.ColorTagging_ChooseColorDialogPrompt, null);
					if (tagName == null)
						return;
					final ColorDialog colorDialog = new ColorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
					colorDialog.setText(Messages.ColorTagging_ChooseColorColorDialogTitle);
					final RGB rgb = colorDialog.open();
					if (rgb == null)
						return;
					existingTags.put(tagName, rgb);
					tagInfo = new Map.Entry<String, RGB>() {
						@Override
						public String getKey() {
							return tagName;
						};
						@Override
						public RGB getValue() {
							return rgb;
						}
						@Override
						public RGB setValue(final RGB value) {
							// yes
							return rgb;
						};
					};
				}
				final CommonNavigator projectExplorer = UI.projectExplorer(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
				final IStructuredSelection sel = (IStructuredSelection)projectExplorer.getCommonViewer().getSelection();
				final List<IContainer> clonkProjectFolders = new ArrayList<IContainer>(sel.size());
				for (final Object obj : sel.toArray())
					if (obj instanceof IContainer && ClonkProjectNature.get((IContainer)obj) != null && ((IContainer) obj).getParent() instanceof IProject)
						clonkProjectFolders.add((IContainer)obj);

				if (tagInfo != null) {
					final String rgbString = StringConverter.asString(tagInfo.getValue());
					final Set<IProject> projects = new HashSet<IProject>();
					for (final IContainer c : clonkProjectFolders) {
						try {
							c.setPersistentProperty(COLOR_TAG, tagInfo.getKey());
							c.setPersistentProperty(COLOR_RGB, rgbString);
						} catch (final CoreException ex) {
							ex.printStackTrace();
						}
						projects.add(c.getProject());
					}
					for (final IProject p : projects)
						UI.refreshAllProjectExplorers(p);
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {}
		};
		// predefined/existing colors
		for (final Map.Entry<String, RGB> defaultColor : existingTags.entrySet()) {
			final MenuItem m = new MenuItem(menu, SWT.RADIO);
			m.setData("tagInfo", defaultColor); //$NON-NLS-1$
			m.setText(defaultColor.getKey());
			m.addSelectionListener(menuItemListener);
		}
		if (existingTags.size() > 0)
			new MenuItem(menu, SWT.SEPARATOR);
		// new color
		MenuItem m = new MenuItem(menu, SWT.RADIO);
		m.setText(Messages.ColorTagging_ChooseColorMenuItemTitle);
		m.setData(null);
		m.addSelectionListener(menuItemListener);
		m = new MenuItem(menu, SWT.RADIO);
		m.setText(Messages.ColorTagging_RemoveTag);
		m.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final CommonNavigator projectExplorer = UI.projectExplorer(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
				final IStructuredSelection sel = (IStructuredSelection)projectExplorer.getCommonViewer().getSelection();
				final Set<IProject> projects = new HashSet<IProject>();
				for (final Object obj : sel.toArray())
					if (obj instanceof IResource) {
						final IResource r = (IResource)obj;
						try {
							r.setPersistentProperty(COLOR_RGB, null);
							r.setPersistentProperty(COLOR_TAG, null);
						} catch (final Exception x) {
							x.printStackTrace();
						}
						projects.add(r.getProject());
					}
				for (final IProject p : projects)
					UI.refreshAllProjectExplorers(p);
			}
			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {}
		});
	}

}
