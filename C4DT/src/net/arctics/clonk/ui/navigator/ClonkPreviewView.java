package net.arctics.clonk.ui.navigator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.rtf.RTFEditorKit;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.FileExtension;
import net.arctics.clonk.debug.ClonkLaunchConfigurationDelegate;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.preferences.ClonkPreferences;
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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

/**
 * View to show preview of files
 * @author madeen
 *
 */
public class ClonkPreviewView extends ViewPart implements ISelectionListener, ControlListener {

	public static final String ID = Core.id("views.ClonkPreviewView"); //$NON-NLS-1$
	private static final float LANDSCAPE_PREVIEW_SCALE = 0.5f;

	private final class PreviewUpdaterJob extends Job {

		private WeakReference<ISelection> selection;

		public synchronized void setSelection(final ISelection selection) {
			this.selection = new WeakReference<ISelection>(selection);
		}

		private PreviewUpdaterJob(final String name) {
			super(name);
		}

		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			final ISelection sel = selection.get();
			if (sel != null)
				synchronizedSelectionChanged(sel);
			return Status.OK_STATUS;
		}

		public synchronized void reschedule(final int delay, final ISelection selection) {
			if (selection == null)
				this.cancel();
			else if (this.selection == null || getState() != WAITING || !selection.equals(this.selection.get())) {
				this.setSelection(selection);
				this.schedule(delay);
			}
		}
	}

	private final class ImageCanvas extends Canvas {
		private ImageCanvas(final Composite parent, final int style) {
			super(parent, style);
			this.addPaintListener(e -> {
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

	private static FormData createFormData(final FormAttachment left, final FormAttachment right, final FormAttachment top, final FormAttachment bottom) {
		final FormData result = new FormData();
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
		sash.addListener(SWT.Selection, event -> {
			sashData.top = new FormAttachment(0, event.y);
			parent.layout();
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
	public void init(final IViewSite site) throws PartInitException {
		super.init(site);
		final ISelectionService selService = site.getWorkbenchWindow().getSelectionService();
		selService.addSelectionListener(IPageLayout.ID_PROJECT_EXPLORER, this);
	}

	@Override
	public void setFocus() {
	}

	private File tempLandscapeRenderFile = null;

	private static String getMaterialsFolderPath(final Engine engine, final IFile resource) {
		final String materialFolderBaseName = "Material."+engine.settings().canonicalToConcreteExtension().get(FileExtension.ResourceGroup);
		for (IContainer container = resource.getParent(); container != null; container = container.getParent()) {
			final IResource matsRes = container.findMember(materialFolderBaseName);
			if (matsRes != null)
				return ClonkLaunchConfigurationDelegate.resFilePath(matsRes);
		}
		return engine.settings().gamePath+"/"+materialFolderBaseName;
	}

	private synchronized void synchronizedSelectionChanged(final ISelection selection) {
		Image newImage = null;
		String newHtml = ""; //$NON-NLS-1$
		String newDefText = ""; //$NON-NLS-1$
		final boolean newDoNotDispose = false;
		if (selection instanceof IStructuredSelection) try {
			final IStructuredSelection structSel = (IStructuredSelection) selection;
			final Object sel = structSel.getFirstElement();
			/*if (sel instanceof IFile && (C4Structure.pinned((IFile)sel, false, false) != null || C4ScriptBase.get((IFile) sel, true) != null))
				sel = ((IFile)sel).getParent(); */
			if (sel instanceof IFile) {
				final IFile file = (IFile) sel;
				final String fileName = file.getName().toLowerCase();
				if (fileName.endsWith(".png") || fileName.endsWith(".bmp") || fileName.endsWith(".jpeg") || fileName.endsWith("jpg")) { //$NON-NLS-1$ //$NON-NLS-2$
					final InputStream contents = file.getContents();
					try {
						newImage = new Image(canvas.getDisplay(), contents);
					} finally {
						contents.close();
					}
				}
				else if (fileName.equalsIgnoreCase("Landscape.txt")) {
					// render landscape.txt using utility embedded into OpenClonk
					final ClonkProjectNature nature = ClonkProjectNature.get(file);
					final Engine engine = nature != null ? nature.index().engine() : null;
					if (engine != null && engine.settings().supportsEmbeddedUtilities) try {
						if (tempLandscapeRenderFile == null) {
							tempLandscapeRenderFile = File.createTempFile("c4dt", "landscaperender");
							tempLandscapeRenderFile.deleteOnExit();
						}
						final Process drawLandscape = engine.executeEmbeddedUtility("drawlandscape",
							"-f"+ClonkLaunchConfigurationDelegate.resFilePath(file),
							"-o"+tempLandscapeRenderFile.getAbsolutePath(),
							"-m"+getMaterialsFolderPath(engine, file),
							"-w"+Math.round(canvasSize.x*LANDSCAPE_PREVIEW_SCALE),
							"-h"+Math.round(canvasSize.y*LANDSCAPE_PREVIEW_SCALE)
						);
						if (drawLandscape != null) {
							drawLandscape.waitFor();
							final FileInputStream stream = new FileInputStream(tempLandscapeRenderFile);
							try {
								newImage = new Image(canvas.getDisplay(), stream);
								if (LANDSCAPE_PREVIEW_SCALE != 1) {
									final Image biggerImage = new Image(canvas.getDisplay(), canvasSize.x, canvasSize.y);
									final GC gc = new GC(biggerImage);
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
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
				else if (fileName.endsWith(".rtf"))
					newHtml = rtfToHtml(StreamUtil.stringFromFileDocument(file));
				else if (fileName.endsWith(".txt"))
					newHtml = StreamUtil.stringFromFileDocument(file);
			}
			else if (sel instanceof IContainer && ((IContainer)sel).getProject().isOpen()) {
				final IContainer container = (IContainer) sel;

				final Definition obj = Definition.at(container);
				if (obj != null)
					newDefText = obj.infoTextIncludingIDAndName();

				IResource descFile = Utilities.findMemberCaseInsensitively(container, "Desc"+ClonkPreferences.languagePref()+".rtf"); //$NON-NLS-1$ //$NON-NLS-2$
				if (descFile instanceof IFile)
					newHtml = rtfToHtml(StreamUtil.stringFromFileDocument((IFile) descFile));
				else {
					descFile = Utilities.findMemberCaseInsensitively(container, "Desc"+ClonkPreferences.languagePref()+".txt"); //$NON-NLS-1$ //$NON-NLS-2$
					if (descFile instanceof IFile)
						newHtml = StreamUtil.stringFromFileDocument((IFile) descFile);
				}

				{
					// Title.png
					if (newImage == null) {
						final IResource graphicsFile = container.findMember("Title.png"); //$NON-NLS-1$
						if (graphicsFile instanceof IFile)
							newImage = new Image(canvas.getDisplay(), ((IFile)graphicsFile).getContents());
					}

					// part of Graphics.png as specified by DefCore.Picture
					if (newImage == null)
						newImage = UI.imageForContainer(container);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		if (image != null && !doNotDisposeImage)
			image.dispose();
		doNotDisposeImage = newDoNotDispose;
		image = newImage;
		final String finalNewHtml = newHtml;
		final String finalNewDefText = newDefText;
		Display.getDefault().asyncExec(() -> {
			canvas.redraw();
			if (!browser.getText().equals(finalNewHtml))
				browser.setText(finalNewHtml);
			if (!defInfo.getText().equals(finalNewDefText))
				defInfo.setText(finalNewDefText);
		});
	}

	public String rtfToHtml(final String rtf) throws IOException, BadLocationException {
		final BufferedReader input = new BufferedReader(new StringReader(rtf));

		final RTFEditorKit rtfKit = new RTFEditorKit();
		final StyledDocument doc = (StyledDocument) rtfKit.createDefaultDocument();
		rtfKit.read( input, doc, 0 );
		input.close();

		final HTMLEditorKit htmlKit = new HTMLEditorKit();
		final StringWriter output = new StringWriter();
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

	private void scheduleJob(final ISelection selection) {
		previewUpdaterJob.reschedule(300, selection);
	}

	@Override
	public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
		scheduleJob(selection);
	}

	@Override
	public void controlMoved(final ControlEvent e) {
	}

	@Override
	public void controlResized(final ControlEvent e) {
		if (e.getSource() == canvas) {
			canvasSize = canvas.getSize();
			scheduleJob(getSelectionOfInterest());
		}
	}

	public void schedulePreviewUpdaterJob() {
		scheduleJob(getSelectionOfInterest());
	}

}