package net.arctics.clonk.ui.editors.mapcreator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import net.arctics.clonk.ui.editors.c4script.ClonkContentOutlinePage;
import net.arctics.clonk.util.Icons;

public class MapCreatorOutlinePage extends ClonkContentOutlinePage {
	
	private class MapCanvas extends Canvas {

		public MapCanvas(Composite parent, int style) {
			super(parent, style);
		}
		
		@Override
		public void drawBackground(GC gc, int x, int y, int width, int height) {
			super.drawBackground(gc, x, y, width, height);
			gc.drawImage(Icons.FOLDER_ICON, x, y);
		}
		
	}
	
	private MapCanvas mapCanvas;
	
	@Override
	public Control getControl() {
		if (mapCanvas == null)
			return null;
		return mapCanvas.getParent();
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite subComposite = new Composite(parent, SWT.DEFAULT);
		subComposite.setLayout(new GridLayout());
		subComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		super.createControl(subComposite);
		mapCanvas = new MapCanvas(subComposite, SWT.DEFAULT);
	}
}
