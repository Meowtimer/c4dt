package net.arctics.clonk.ui.navigator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.rtf.RTFEditorKit;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IntegerArray;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.*;


/**
 * View to show preview of files
 * @author madeen
 *
 */
public class ClonkPreviewView extends ViewPart implements ISelectionChangedListener {

	public static final String ID = ClonkCore.id("views.ClonkPreviewView"); //$NON-NLS-1$
	
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
	
	private Canvas canvas;
	private Browser browser;
	private Image image;

	public ClonkPreviewView() {
	}
	
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout(SWT.VERTICAL));
		canvas = new ImageCanvas(parent, SWT.NO_SCROLL);
		browser = new Browser(parent, SWT.NONE);
	}
	
	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		Utilities.getProjectExplorer().getCommonViewer().addSelectionChangedListener(this);
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Image newImage = null;
		String newHtml = ""; //$NON-NLS-1$
		if (event.getSelection() instanceof IStructuredSelection) try {
			IStructuredSelection structSel = (IStructuredSelection) event.getSelection();
			Object sel = structSel.getFirstElement();
			if (sel instanceof IFile && (C4Structure.pinned((IFile)sel, false, false) != null || Utilities.getScriptForFile((IFile) sel) != null))
				sel = ((IFile)sel).getParent();
			if (sel instanceof IFile) {
				IFile file = (IFile) sel;
				String fileName = file.getName().toLowerCase();
				if (fileName.endsWith(".png") || fileName.endsWith(".bmp")) { //$NON-NLS-1$ //$NON-NLS-2$
					newImage = new Image(canvas.getDisplay(), file.getLocation().toOSString());
				}
				else if (fileName.endsWith(".rtf")) { //$NON-NLS-1$
					newHtml = rtfToHtml(Utilities.stringFromFile(file));
				}
				else if (fileName.endsWith(".txt")) { //$NON-NLS-1$
					newHtml = Utilities.stringFromFile(file);
				}
			}
			else if (sel instanceof IContainer) {
				IContainer container = (IContainer) sel;

				IResource descFile = Utilities.findMemberCaseInsensitively(container, "Desc"+ClonkPreferences.getLanguagePref()+".rtf"); //$NON-NLS-1$ //$NON-NLS-2$
				if (descFile instanceof IFile) {
					newHtml = rtfToHtml(Utilities.stringFromFile((IFile) descFile));
				}
				else {
					descFile = Utilities.findMemberCaseInsensitively(container, "Desc"+ClonkPreferences.getLanguagePref()+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
					if (descFile instanceof IFile) {
						newHtml = Utilities.stringFromFile((IFile) descFile);
					}
				}
				
				// Title.png
				if (newImage == null) {
					IResource graphicsFile = container.findMember("Title.png"); //$NON-NLS-1$
					if (graphicsFile != null) {
						newImage = new Image(canvas.getDisplay(), graphicsFile.getLocation().toOSString());
					}
				}
				
				// part of Graphics.png as specified by DefCore.Picture
				if (newImage == null) {
					IResource graphicsFile = container.findMember("Graphics.png"); //$NON-NLS-1$
					if (graphicsFile == null)
						graphicsFile = container.findMember("Graphics.bmp"); //$NON-NLS-1$
					if (graphicsFile != null) {
						Image fullGraphics = new Image(canvas.getDisplay(), graphicsFile.getLocation().toOSString());
						try {
							IResource defCoreFile = container.findMember("DefCore.txt"); //$NON-NLS-1$
							if (defCoreFile instanceof IFile) {
								DefCoreUnit defCore = (DefCoreUnit) DefCoreUnit.pinned((IFile) defCoreFile, true, false);
								IniEntry pictureEntry = defCore.entryInSection("DefCore", "Picture"); //$NON-NLS-1$ //$NON-NLS-2$
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
		browser.setText(newHtml);

	}

	public String rtfToHtml(String rtf) throws IOException, BadLocationException {
		BufferedReader input = new BufferedReader(new StringReader(rtf));

		RTFEditorKit rtfKit = new RTFEditorKit();
		StyledDocument doc = (StyledDocument) rtfKit.createDefaultDocument();
		rtfKit.read( input, doc, 0 );
		input.close();

		HTMLEditorKit htmlKit = new HTMLEditorKit();       
		StringWriter output = new StringWriter();
		htmlKit.write(output, doc, 0, doc.getLength());

		return output.toString();
	}

	
	@Override
	public void dispose() {
		if (image != null) {
			image.dispose();
			image = null;
		}
		CommonNavigator nav = Utilities.getProjectExplorer();
		if (nav != null)
			nav.getCommonViewer().removeSelectionChangedListener(this);
		super.dispose();
	}

}