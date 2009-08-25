package net.arctics.clonk.ui.editors;

import net.arctics.clonk.parser.C4Declaration;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.widgets.Shell;

@SuppressWarnings("restriction")
public class ClonkTextHover<EditorType extends ClonkTextEditor> implements ITextHover, ITextHoverExtension {

		protected final ClonkSourceViewerConfiguration<EditorType> configuration;
		private transient IHyperlink hyperlink;
		
		public ClonkTextHover(ClonkSourceViewerConfiguration<EditorType> clonkSourceViewerConfiguration) {
			super();
			//informationControlCreator = new C4ScriptTextHoverCreator();
			configuration = clonkSourceViewerConfiguration;
		}
		
		public String getHoverInfo(ITextViewer viewer, IRegion region) {
			if (hyperlink instanceof ClonkHyperlink) {
				ClonkHyperlink clonkHyperlink = (ClonkHyperlink) hyperlink;
				C4Declaration dec = clonkHyperlink.getTarget();
				hyperlink = null;
				return dec.getInfoText();
			}
			return null;
		}

		public IRegion getHoverRegion(ITextViewer viewer, int offset) {
			hyperlink = configuration.getEditor().hyperlinkAtOffset(offset);
			if (hyperlink != null)
				return hyperlink.getHyperlinkRegion();
			return null;
		}

		public IInformationControlCreator getHoverControlCreator() {
			return new IInformationControlCreator() {
				public IInformationControl createInformationControl(Shell parent) {
					return new DefaultInformationControl(parent, new HTMLTextPresenter(true));
				}
			};
		}
		
	}