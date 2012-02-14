package net.arctics.clonk.ui.navigator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.rtf.RTFEditorKit;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.debug.ClonkLaunchConfigurationDelegate;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IntegerArray;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.UI;
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
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
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
public class ClonkPreviewView extends ViewPart implements ISelectionListener, ControlListener {

	public static final String ID = ClonkCore.id("views.ClonkPreviewView"); //$NON-NLS-1$
	private static final float LANDSCAPE_PREVIEW_SCALE = 0.5f;
	
	private final class PreviewUpdaterJob extends Job {
		
		private WeakReference<ISelection> selection;
		
		public synchronized void setSelection(ISelection selection) {
			this.selection = new WeakReference<ISelection>(selection);
		}

		private PreviewUpdaterJob(String name) {
			super(name);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ISelection sel = selection.get();
			if (sel != null)
				synchronizedSelectionChanged(sel);
			return Status.OK_STATUS;
		}

		public synchronized void reschedule(int delay, ISelection selection) {
			if (selection == null) {
				this.cancel();
			} else if (this.selection == null || getState() != WAITING || !selection.equals(this.selection.get())) {
				this.setSelection(selection);
				this.schedule(delay);
			}
		}
	}

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
	private Point canvasSize = new Point(100, 100);

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
		canvas.addControlListener(this);
		canvasSize = canvas.getSize();
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
		synchronizedSelectionChanged(getSelectionOfInterest());
	}

	public ISelection getSelectionOfInterest() {
		return UI.projectExplorerSelection(getSite());
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
		if (pictureEntry != null && pictureEntry.value() instanceof IntegerArray) {
			IntegerArray values = (IntegerArray) pictureEntry.value();
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
	
	private File tempLandscapeRenderFile = null;
	
	private static String getMaterialsFolderPath(Engine engine, IFile resource) {
		String materialFolderBaseName = "Material."+engine.currentSettings().groupTypeToFileExtensionMapping().get(GroupType.ResourceGroup);
		for (IContainer container = resource.getParent(); container != null; container = container.getParent()) {
			IResource matsRes = container.findMember(materialFolderBaseName);
			if (matsRes != null) {
				return ClonkLaunchConfigurationDelegate.resFilePath(matsRes);
			}
		}
		return engine.currentSettings().gamePath+"/"+materialFolderBaseName; 
	}

	private synchronized void synchronizedSelectionChanged(ISelection selection) {
		Image newImage = null;
		String newHtml = ""; //$NON-NLS-1$
		String newDefText = ""; //$NON-NLS-1$
		boolean newDoNotDispose = false;
		if (selection instanceof IStructuredSelection) try {
			IStructuredSelection structSel = (IStructuredSelection) selection;
			Object sel = structSel.getFirstElement();
			/*if (sel instanceof IFile && (C4Structure.pinned((IFile)sel, false, false) != null || C4ScriptBase.get((IFile) sel, true) != null))
				sel = ((IFile)sel).getParent(); */
			if (sel instanceof IFile) {
				IFile file = (IFile) sel;
				String fileName = file.getName().toLowerCase();
				if (fileName.endsWith(".png") || fileName.endsWith(".bmp") || fileName.endsWith(".jpeg") || fileName.endsWith("jpg")) { //$NON-NLS-1$ //$NON-NLS-2$
					newImage = new Image(canvas.getDisplay(), file.getContents());
				}
				else if (fileName.equalsIgnoreCase("Landscape.txt")) {
					// render landscape.txt using utility embedded into OpenClonk
					ClonkProjectNature nature = ClonkProjectNature.get(file);
					Engine engine = nature != null ? nature.index().engine() : null;
					if (engine != null && engine.currentSettings().supportsEmbeddedUtilities) try {
						if (tempLandscapeRenderFile == null) {
							tempLandscapeRenderFile = File.createTempFile("c4dt", "landscaperender");
							tempLandscapeRenderFile.deleteOnExit();
						}
						Process drawLandscape = engine.executeEmbeddedUtility("drawlandscape",
							"-f"+ClonkLaunchConfigurationDelegate.resFilePath(file),
							"-o"+tempLandscapeRenderFile.getAbsolutePath(),
							"-m"+getMaterialsFolderPath(engine, file),
							"-w"+Math.round(canvasSize.x*LANDSCAPE_PREVIEW_SCALE),
							"-h"+Math.round(canvasSize.y*LANDSCAPE_PREVIEW_SCALE)
						);
						if (drawLandscape != null) {
							drawLandscape.waitFor();
							FileInputStream stream = new FileInputStream(tempLandscapeRenderFile);
							try {
								newImage = new Image(canvas.getDisplay(), stream);
								if (LANDSCAPE_PREVIEW_SCALE != 1) {
									Image biggerImage = new Image(canvas.getDisplay(), canvasSize.x, canvasSize.y);
									GC gc = new GC(biggerImage);
									gc.setAntialias(SWT.ON);
									gc.setInterpolation(SWT.HIGH);
									gc.drawImage(newImage,
										0, 0, newImage.getBounds().width, newImage.getBounds().height,
										0, 0, canvasSize.x, canvasSize.y
									);
									gc.dispose();
									newImage.dispose();
									newImage = biggerImage;
								}
							} finally {
								stream.close();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else if (fileName.endsWith(".rtf")) { //$NON-NLS-1$
					newHtml = rtfToHtml(StreamUtil.stringFromFileDocument(file));
				}
				else if (fileName.endsWith(".txt")) { //$NON-NLS-1$
					newHtml = StreamUtil.stringFromFileDocument(file);
				}
			}
			else if (sel instanceof IContainer && ((IContainer)sel).getProject().isOpen()) {
				IContainer container = (IContainer) sel;
				
				Definition obj = Definition.definitionCorrespondingToFolder(container);
				if (obj != null)
					newDefText = obj.idWithName();

				IResource descFile = Utilities.findMemberCaseInsensitively(container, "Desc"+ClonkPreferences.languagePref()+".rtf"); //$NON-NLS-1$ //$NON-NLS-2$
				if (descFile instanceof IFile) {
					newHtml = rtfToHtml(StreamUtil.stringFromFileDocument((IFile) descFile));
				}
				else {
					descFile = Utilities.findMemberCaseInsensitively(container, "Desc"+ClonkPreferences.languagePref()+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
					if (descFile instanceof IFile) {
						newHtml = StreamUtil.stringFromFileDocument((IFile) descFile);
					}
				}

				if (obj != null && obj.cachedPicture() != null) {
					newImage = obj.cachedPicture();
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
									DefCoreUnit defCore = (DefCoreUnit) Structure.pinned(defCoreFile, true, false);
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
				if (!browser.getText().equals(finalNewHtml))
					browser.setText(finalNewHtml);
				if (!defInfo.getText().equals(finalNewDefText))
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

	private final PreviewUpdaterJob previewUpdaterJob = new PreviewUpdaterJob(Messages.ClonkPreviewView_Updater);
	
	private void scheduleJob(ISelection selection) {
		previewUpdaterJob.reschedule(300, selection);
	}
	
	@Override
	public void selectionChanged(IWorkbenchPart part, final ISelection selection) {
		scheduleJob(selection);
	}

	@Override
	public void controlMoved(ControlEvent e) {
	}

	@Override
	public void controlResized(ControlEvent e) {
		if (e.getSource() == canvas) {
			canvasSize = canvas.getSize();
			scheduleJob(getSelectionOfInterest());
		}
	}
	
	public void schedulePreviewUpdaterJob() {
		scheduleJob(getSelectionOfInterest());
	}

}