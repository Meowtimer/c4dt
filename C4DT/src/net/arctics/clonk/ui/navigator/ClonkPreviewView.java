package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IntegerArray;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.*;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */
public class ClonkPreviewView extends ViewPart implements ISelectionChangedListener {

	private final class ImageCanvas extends Canvas {
		private ImageCanvas(Composite parent, int style) {
			super(parent, style);
			this.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					if (image != null) {
						int hgt = getBounds().height;
						float ratio = (float)hgt/(float)image.getBounds().height;
						int wdt = (int) (image.getBounds().width*ratio);
						if (wdt > getBounds().width) {
							wdt = getBounds().width;
							ratio = (float)wdt/(float)image.getBounds().width;
							hgt = (int) (image.getBounds().height*ratio);
						}
						e.gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, (getBounds().width-wdt)/2, (getBounds().height-hgt)/2, wdt, hgt);
					}
				}
			});
		}
	}

	public static final String ID = "net.arctics.clonk.views.ClonkPreviewView";
	
	private Canvas canvas;
	private Image image;

	public ClonkPreviewView() {
	}
	
	@Override
	public void createPartControl(Composite parent) {
		Utilities.getProjectExplorer().getCommonViewer().addSelectionChangedListener(this);
		canvas = new ImageCanvas(parent, SWT.NO_SCROLL);
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Image newImage = null;
		if (event.getSelection() instanceof IStructuredSelection) try {
			IStructuredSelection structSel = (IStructuredSelection) event.getSelection();
			Object sel = structSel.getFirstElement();
			if (sel instanceof IContainer) {
				IContainer container = (IContainer) sel;

				// Title.png
				if (newImage == null) {
					IResource graphicsFile = container.findMember("Title.png");
					if (graphicsFile != null) {
						newImage = new Image(canvas.getDisplay(), graphicsFile.getLocation().toOSString());
					}
				}
				
				// part of Graphics.png as specified by DefCore.Picture
				if (newImage == null) {
					IResource graphicsFile = container.findMember("Graphics.png");
					if (graphicsFile == null)
						graphicsFile = container.findMember("Graphics.bmp");
					if (graphicsFile != null) {
						Image fullGraphics = new Image(canvas.getDisplay(), graphicsFile.getLocation().toOSString());
						try {
							IResource defCoreFile = container.findMember("DefCore.txt");
							if (defCoreFile instanceof IFile) {
								DefCoreUnit defCore = (DefCoreUnit) DefCoreUnit.pinned((IFile) defCoreFile, true, false);
								IniEntry pictureEntry = defCore.entryInSection("DefCore", "Picture");
								if (pictureEntry != null && pictureEntry.getValueObject() instanceof IntegerArray) {
									IntegerArray values = (IntegerArray) pictureEntry.getValueObject();
									newImage = new Image(canvas.getDisplay(), values.get(2), values.get(3));
									GC gc = new GC(newImage);
									try {
										gc.drawImage(fullGraphics, values.get(0), values.get(1), values.get(2), values.get(3), 0, 0, newImage.getBounds().width, newImage.getBounds().height);
									} finally {
										gc.dispose();
									}
								}
							}
						} finally {
							if (newImage == null)
								newImage = fullGraphics;
							else
								fullGraphics.dispose();
						}
					}
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (image != null)
			image.dispose();
		image = newImage;
		canvas.redraw();
		
	}
	
	@Override
	public void dispose() {
		if (image != null)
			image.dispose();
		Utilities.getProjectExplorer().getCommonViewer().removeSelectionChangedListener(this);
		super.dispose();
	}

}