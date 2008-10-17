package net.arctics.clonk.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Stack;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;

public class ClonkProjectProperties extends PropertyPage {

	private IEclipsePreferences prefs;
	private Text text;
	private Tree tree;
	private Stack<File> fileNames;
	
	private Composite comp;
	
	@Override
	protected Control createContents(Composite parent) {
		IProject nature = (IProject) getElement().getAdapter(IProject.class);
		if (nature == null) {
			Label lab = new Label(parent, SWT.LEFT);
			lab.setText("Returned null oO;");
			return lab;
		}
		
		ProjectScope scope = new ProjectScope(nature);
		prefs = scope.getNode(ClonkCore.PLUGIN_ID);
		String path = prefs.get("clonkpath", "D:\\");
		
		comp = new Composite(parent,SWT.NONE);
		
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		layout.numColumns= 2;
		comp.setLayout(layout);
		
		Label lab = new Label(comp, SWT.LEFT);
		lab.setText("THIS PROPERTY PAGE IS DEPRECATED");
		
		text = new Text(comp, SWT.SINGLE | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(path);
		text.setSize(200, 25);
		text.addListener(SWT.Verify, new Listener() {
			public void handleEvent(Event event) {
				File tmp = new File(((Text)event.widget).getText());
				if (tmp.exists()) {
					appendLibraryChoser(tmp);
				}
			}
			
		});
		
		File tmp = new File(text.getText());
		if (tmp.exists()) {
			appendLibraryChoser(tmp);
		}
		
		return comp;
	}
	
	private void generateFileStack(File dir) {
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(".c4d") || name.endsWith(".c4g"));
			}
		});
		fileNames = new Stack<File>();
		for(File file : files) fileNames.add(file);
	}
	
	private void appendLibraryChoser(File dir) {
		generateFileStack(dir);
		if (fileNames.size() == 0) {
			if (tree != null) tree.dispose();
			tree = null;
			return;
		}
		
		final org.osgi.service.prefs.Preferences extLibs = prefs.node("extLibs");
		
		if (tree != null) {
			tree.clearAll(true);
			tree.setItemCount(fileNames.size());
		}
		else {
			tree = new Tree(comp, SWT.CHECK | SWT.VIRTUAL | SWT.BORDER);
			tree.setItemCount(fileNames.size());
			tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,true,2,1));
			tree.addListener(SWT.SetData, new Listener() {
				public void handleEvent(Event event) {
					TreeItem item = (TreeItem) event.item;
					item.setText(fileNames.pop().getName());
					item.setChecked((extLibs.get(item.getText(), null) != null));
				}

			});
		}
		
		comp.layout(true);
	}
	
	@Override
	public boolean performOk() {
		prefs.put("clonkpath", text.getText());
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		TreeItem[] items = tree.getItems();
		org.osgi.service.prefs.Preferences extLibs = prefs.node("extLibs");
		try {
			extLibs.clear();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		for(TreeItem item : items) {
			if (item.getChecked()) {
				extLibs.put(item.getText(), item.getText());
			}
		}
		
		return super.performOk();
	}

}
