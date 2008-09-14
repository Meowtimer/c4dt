package net.arctics.clonk.ui;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;

public class ClonkProjectProperties extends PropertyPage {

	private IEclipsePreferences prefs;
	private Text text;
	
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
		
		Composite comp = new Composite(parent,SWT.NONE);
		
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		layout.numColumns= 2;
		comp.setLayout(layout);
		
//		Table table = new Table(comp, SWT.SINGLE | SWT.FULL_SELECTION);
//		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		table.setLinesVisible(true);
//		table.setHeaderVisible(true);
		
		Label lab = new Label(comp, SWT.LEFT);
		lab.setText("Clonk path:");
		
		text = new Text(comp, SWT.SINGLE | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(path);
//		text.setLayoutData(new org.eclipse.swt.layout.RowData(300,50));
		text.setSize(200, 25);
//		text.addModifyListener(new ModifyListener() {
//
//			@Override
//			public void modifyText(ModifyEvent e) {
//				prefs.put("clonkpath", text.getText());
//			}
//			
//		});
		
//		for (int i = 0; i < table.getColumnCount(); i++) {
//			table.getColumn(i).pack();
//		}
		return comp;
	}

	@Override
	public boolean performOk() {
		prefs.put("clonkpath", text.getText());
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		return super.performOk();
	}

}
