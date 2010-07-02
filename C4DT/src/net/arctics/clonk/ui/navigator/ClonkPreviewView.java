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
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IntegerArray;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.*;


/**
 * View to show preview of files
 * @author madeen
 *
 */
public class ClonkPreviewView extends ViewPart implements ISelectionListener {

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
	private Sash sash;
	private Image image;
	private boolean doNotDisposeImage;
	private Text defInfo;

	public ClonkPreviewView() {
	}
	
	private static FormData createFormData(FormAttachment left, FormAttachment right, FormAttachment top, FormAttachment bottom) {
		FormData result = new FormData();
		result.left   = left;
		result.top    = top;
		result.right  = right;
		result.bottom = bottom;
		return result;
	}
	
	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(new FormLayout());
		
		canvas = new ImageCanvas(parent, SWT.NO_SCROLL);
		sash = new Sash(parent, SWT.HORIZONTAL);
		browser = new Browser(parent, SWT.NONE);
		defInfo = new Text(parent, SWT.NONE);
		
		canvas.setLayoutData(createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(0, 0),
			new FormAttachment(sash, 0)
		));
		
		final FormData sashData;
		sash.setLayoutData(sashData = createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(40, 0),
			null
		));
		sash.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				sashData.top = new FormAttachment(0, event.y);
				parent.layout();
			}
		});
		
		browser.setLayoutData(createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(sash, 0),
			new FormAttachment(100, -defInfo.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)
		));
		
		defInfo.setLayoutData(createFormData(
			new FormAttachment(0, 0),
			new FormAttachment(100, 0),
			new FormAttachment(browser, 0),
			new FormAttachment(100, 0)
		));
		
		parent.layout();
		synchronizedSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection(IPageLayout.ID_PROJECT_EXPLORER));
	}
	
	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		ISelectionService selService = site.getWorkbenchWindow().getSelectionService();
		selService.addSelectionListener(IPageLayout.ID_PROJECT_EXPLORER, this);
	}
	
	@Override
	public void setFocus() {
	}
	
	public Image getPicture(DefCoreUnit defCore, Image graphics) {
		Image result = null;
		IniEntry pictureEntry = defCore.entryInSection("DefCore", "Picture"); //$NON-NLS-1$ //$NON-NLS-2$
		if (pictureEntry != null && pictureEntry.getValueObject() instanceof IntegerArray) {
			IntegerArray values = (IntegerArray) pictureEntry.getValueObject();
			result = new Image(canvas.getDisplay(), values.get(2), values.get(3));
			GC gc = new GC(result);
			try {
				gc.drawImage(graphics, values.get(0), values.get(1), values.get(2), values.get(3), 0, 0, result.getBounds().width, result.getBounds().height);
			} finally {
				gc.dispose();
			}
		}
		return result;
	}

	private synchronized void synchronizedSelectionChanged(ISelection selection) {
		Image newImage = null;
		String newHtml = ""; //$NON-NLS-1$
		String newDefText = ""; //$NON-NLS-1$
		boolean newDoNotDispose = false;
		if (selection instanceof IStructuredSelection) try {
			IStructuredSelection structSel = (IStructuredSelection) selection;
			Object sel = structSel.getFirstElement();
			if (sel instanceof IFile && (C4Structure.pinned((IFile)sel, false, false) != null || C4ScriptBase.get((IFile) sel, true) != null))
				sel = ((IFile)sel).getParent();
			if (sel instanceof IFile) {
				IFile file = (IFile) sel;
				String fileName = file.getName().toLowerCase();
				if (fileName.endsWith(".png") || fileName.endsWith(".bmp")) { //$NON-NLS-1$ //$NON-NLS-2$
					newImage = new Image(canvas.getDisplay(), file.getContents());
				}
				else if (fileName.endsWith(".rtf")) { //$NON-NLS-1$
					newHtml = rtfToHtml(Utilities.stringFromFileDocument(file));
				}
				else if (fileName.endsWith(".txt")) { //$NON-NLS-1$
					newHtml = Utilities.stringFromFileDocument(file);
				}
			}
			else if (sel instanceof IContainer && ((IContainer)sel).getProject().isOpen()) {
				IContainer container = (IContainer) sel;
				
				C4ObjectIntern obj = C4ObjectIntern.objectCorrespondingTo(container);
				if (obj != null)
					newDefText = obj.idWithName();

				IResource descFile = Utilities.findMemberCaseInsensitively(container, "Desc"+ClonkPreferences.getLanguagePref()+".rtf"); //$NON-NLS-1$ //$NON-NLS-2$
				if (descFile instanceof IFile) {
					newHtml = rtfToHtml(Utilities.stringFromFileDocument((IFile) descFile));
				}
				else {
					descFile = Utilities.findMemberCaseInsensitively(container, "Desc"+ClonkPreferences.getLanguagePref()+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
					if (descFile instanceof IFile) {
						newHtml = Utilities.stringFromFileDocument((IFile) descFile);
					}
				}

				if (obj != null && obj.getCachedPicture() != null) {
					newImage = obj.getCachedPicture();
					newDoNotDispose = true;
				}
				else {

					// Title.png
					if (newImage == null) {
						IResource graphicsFile = container.findMember("Title.png"); //$NON-NLS-1$
						if (graphicsFile instanceof IFile) {
							newImage = new Image(canvas.getDisplay(), ((IFile)graphicsFile).getContents());
						}
					}

					// part of Graphics.png as specified by DefCore.Picture
					if (newImage == null) {
						IResource graphicsFile = container.findMember("Graphics.png"); //$NON-NLS-1$
						if (graphicsFile == null)
							graphicsFile = container.findMember("Graphics.bmp"); //$NON-NLS-1$
						if (graphicsFile instanceof IFile) {
							Image fullGraphics = new Image(canvas.getDisplay(), ((IFile)graphicsFile).getContents());
							try {
								IResource defCoreFile = container.findMember("DefCore.txt"); //$NON-NLS-1$
								if (defCoreFile instanceof IFile) {
									DefCoreUnit defCore = (DefCoreUnit) DefCoreUnit.pinned((IFile) defCoreFile, true, false);
									newImage = getPicture(defCore, fullGraphics);
								}
							} finally {
								if (newImage == null)
									newImage = fullGraphics;
								else
									fullGraphics.dispose();
								if (obj != null) {
									obj.setCachedPicture(newImage);
									newDoNotDispose = true;
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (image != null && !doNotDisposeImage)
			image.dispose();
		doNotDisposeImage = newDoNotDispose;
		image = newImage;
		final String finalNewHtml = newHtml;
		final String finalNewDefText = newDefText;
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				canvas.redraw();
				browser.setText(finalNewHtml);
				defInfo.setText(finalNewDefText);	
			}
		});
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
			if (!doNotDisposeImage)
				image.dispose();
			image = null;
		}
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(IPageLayout.ID_PROJECT_EXPLORER, this);
		super.dispose();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, final ISelection selection) {
		new Job(Messages.ClonkPreviewView_Updater) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				synchronizedSelectionChanged(selection);
				return Status.OK_STATUS;
			}
		}.schedule();
	}

}