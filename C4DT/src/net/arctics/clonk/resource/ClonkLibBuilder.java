package net.arctics.clonk.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.ExternalLibsLoader;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.c4group.InvalidDataException;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;

public class ClonkLibBuilder implements IPropertyChangeListener {
	
	private boolean buildNeeded = false;
	
	public ClonkLibBuilder() {
		ClonkCore.getDefault().getPreferenceStore().addPropertyChangeListener(this);
//		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_BUILD);
	}
	
	public void build(IProgressMonitor monitor) throws InvalidDataException, IOException, CoreException {
		ExternalLibsLoader.readExternalLibs(ClonkCore.getDefault().getExternIndex(), monitor, ClonkPreferences.getExternalLibNames());
		buildNeeded = false;
	}
	
	public void clean() {
		ClonkCore.getDefault().getExternIndex().clear();
		buildNeeded = true;
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(ClonkPreferences.STANDARD_EXT_LIBS)) {
			String oldValue = (String) event.getOldValue();
			String newValue = (String) event.getNewValue();
			final String[] oldLibs = oldValue.split("<>"); //$NON-NLS-1$
			final String[] newLibs = newValue.split("<>"); //$NON-NLS-1$
			final ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
			try {
				progressDialog.run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException, InterruptedException {
						for(String lib : oldLibs) {
							if (!isIn(lib, newLibs)) { 
								// lib deselected
								try {
									build(monitor);
									return;
								} catch (InvalidDataException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								} catch (CoreException e) {
									e.printStackTrace();
								}
							}
						}
						ExternalLibsLoader loader = new ExternalLibsLoader(ClonkCore.getDefault().getExternIndex());
						for(String lib : newLibs) {
							if (!isIn(lib, oldLibs)) {
								// new lib selected
								try {
									loader.readExternalLib(lib, monitor);
								} catch (InvalidDataException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								} catch (CoreException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
						refresh();
						try {
							ClonkCore.getDefault().saveExternIndex(monitor);
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
						}
						try {
							Utilities.refreshClonkProjects(monitor);
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				});
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		else if (event.getProperty().equals(ClonkPreferences.PREFERRED_LANGID)) {
			for (C4Object o : ClonkCore.getDefault().getExternIndex()) {
				o.chooseLocalizedName();
			}
			for (IProject proj : Utilities.getClonkProjects()) {
				for (C4Object obj : ClonkProjectNature.get(proj).getIndex()) {
					obj.chooseLocalizedName();
				}
			}
		}
	}
	
	private void refresh() {
		ClonkCore.getDefault().getExternIndex().refreshCache();
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

	public boolean isBuildNeeded() {
		return buildNeeded;
	}
	
}
