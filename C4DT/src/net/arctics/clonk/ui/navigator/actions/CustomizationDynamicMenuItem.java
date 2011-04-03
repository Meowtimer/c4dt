package net.arctics.clonk.ui.navigator.actions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.resource.CustomizationNature;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

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
		
		public SelListener(Menu menu) {
			ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
			if (sel instanceof IStructuredSelection && ((IStructuredSelection)sel).getFirstElement() instanceof IContainer) {
				IContainer container = (IContainer) ((IStructuredSelection)sel).getFirstElement();
				CustomizationNature nat = CustomizationNature.get(container.getProject());
				if (nat != null) {
					Iterable<URL> urls = possibleFiles(container);
					if (urls != null) {
						Outer: for (URL url : urls) {
							IPath path = engineSpecificPathForURL(url);
							// ignore any '.'-files
							for (String s : path.segments())
								if (s.startsWith(".")) //$NON-NLS-1$
									continue Outer;
							MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
							menuItem.setText(path.toOSString());
							menuItem.addSelectionListener(this);
							menuItem.setData(URL_PROP, url);
							menuItem.setData(PATH_PROP, path);
						}
					} else {
						MenuItem failItem = new MenuItem(menu, SWT.RADIO);
						failItem.setEnabled(false);
						failItem.setText(Messages.CustomizationDynamicMenuItem_SelectTopLevelEngineFolder);
					}
				}
			}
		}

		private IPath engineSpecificPathForURL(URL url) {
			IPath path = new Path(url.getPath());
			for (int i = 0; i < path.segmentCount(); i++)
				if (path.segment(i).equals(engine.getName())) {
					path = path.removeFirstSegments(i);
					break;
				}
			return path.makeRelativeTo(container.getProjectRelativePath());
		}
		
		@Override
		public void widgetSelected(SelectionEvent event) {
			URL url = (URL)event.widget.getData(URL_PROP);
			IPath path = (IPath) event.widget.getData(PATH_PROP);
			String fileName = path.toOSString();
			OutputStream outputStream = engine.outputStreamForStorageLocationEntry(resPath.append(fileName).toString());
			if (outputStream != null) try {
				try {
					InputStream inputStream = url.openStream();
					if (inputStream != null) try {
						StreamUtil.transfer(inputStream, outputStream);
					} finally {
						inputStream.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} finally {
				try {
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				container.refreshLocal(IResource.DEPTH_INFINITE, null);
				Utilities.getProjectExplorer().selectReveal(new StructuredSelection(container.getFile(resPath.append(path))));
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		private Iterable<URL> possibleFiles(IContainer container) {
			this.container = container;
			resPath = container.getProjectRelativePath();
			String engineName = resPath.segment(0);
			resPath = resPath.removeFirstSegments(1);
			engine = ClonkCore.getDefault().loadEngine(engineName);
			if (engine != null) {
				Iterable<URL> filesToReplicate = engine.getURLsOfStorageLocationPath(resPath.toString(), true);
				return filesToReplicate;
			} else
				return null;
		}
	}

	public CustomizationDynamicMenuItem() {
		
	}
	
	public CustomizationDynamicMenuItem(String id) {
		super(id);
	}
	
	@Override
	public void fill(Menu menu, int index) {	
		new SelListener(menu);
	}

}
