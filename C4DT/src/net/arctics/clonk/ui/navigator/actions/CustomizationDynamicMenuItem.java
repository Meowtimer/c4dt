package net.arctics.clonk.ui.navigator.actions;

import static java.util.Arrays.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.stream.StreamSupport;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.CustomizationNature;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;

public class CustomizationDynamicMenuItem extends ContributionItem {

	private final class SelListener implements SelectionListener {

		private static final String URL_PROP = "_url"; //$NON-NLS-1$
		private static final String PATH_PROP = "_path"; //$NON-NLS-1$

		private Engine engine;
		private IPath resPath;
		private IContainer container;

		public SelListener(final Menu menu) {
			final ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
			if (sel instanceof IStructuredSelection && ((IStructuredSelection)sel).getFirstElement() instanceof IContainer) {
				final IContainer container = (IContainer) ((IStructuredSelection)sel).getFirstElement();
				final CustomizationNature nat = CustomizationNature.get(container.getProject());
				if (nat != null) {
					final Iterable<URL> urls = possibleFiles(container);
					if (urls != null)
						StreamSupport.stream(urls.spliterator(), false).forEach(url -> {
							final IPath path = engineSpecificPathForURL(url);
							// ignore any '.'-files
							if (stream(path.segments()).anyMatch(s -> s.startsWith(".")))
								return;
							final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
							menuItem.setText(path.toOSString());
							menuItem.addSelectionListener(this);
							menuItem.setData(URL_PROP, url);
							menuItem.setData(PATH_PROP, path);
						});
					else {
						final MenuItem failItem = new MenuItem(menu, SWT.RADIO);
						failItem.setEnabled(false);
						failItem.setText(Messages.CustomizationDynamicMenuItem_SelectTopLevelEngineFolder);
					}
				}
			}
		}

		private IPath engineSpecificPathForURL(final URL url) {
			IPath path = new Path(url.getPath());
			for (int i = 0; i < path.segmentCount(); i++)
				if (path.segment(i).equals(engine.name())) {
					path = path.removeFirstSegments(i);
					break;
				}
			return path.makeRelativeTo(container.getProjectRelativePath());
		}

		@Override
		public void widgetSelected(final SelectionEvent event) {
			final URL url = (URL)event.widget.getData(URL_PROP);
			final IPath path = (IPath) event.widget.getData(PATH_PROP);
			final String fileName = path.toOSString();
			try (
				final OutputStream outputStream = engine.outputStreamForStorageLocationEntry
					(resPath.append(fileName).toString());
				final InputStream inputStream = url.openStream()
			) {
				StreamUtil.transfer(inputStream, outputStream);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			try {
				container.refreshLocal(IResource.DEPTH_INFINITE, null);
				UI.projectExplorer().selectReveal(new StructuredSelection(container.getFile(resPath.append(path))));
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void widgetDefaultSelected(final SelectionEvent e) {}

		private Iterable<URL> possibleFiles(final IContainer container) {
			this.container = container;
			resPath = container.getProjectRelativePath();
			final String engineName = resPath.segment(0);
			resPath = resPath.removeFirstSegments(1);
			engine = Core.instance().loadEngine(engineName);
			return engine != null ? engine.getURLsOfStorageLocationPath(resPath.toString(), true) : null;
		}
	}

	public CustomizationDynamicMenuItem() {

	}

	public CustomizationDynamicMenuItem(final String id) {
		super(id);
	}

	@Override
	public void fill(final Menu menu, final int index) {
		new SelListener(menu);
	}

}
