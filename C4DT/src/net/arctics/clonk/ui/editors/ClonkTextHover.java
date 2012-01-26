package net.arctics.clonk.ui.editors;

import net.arctics.clonk.parser.Declaration;

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
		
		@Override
		public String getHoverInfo(ITextViewer viewer, IRegion region) {
			if (hyperlink instanceof ClonkHyperlink) {
				ClonkHyperlink clonkHyperlink = (ClonkHyperlink) hyperlink;
				Declaration dec = clonkHyperlink.getTarget();
				hyperlink = null;
				if (dec != null)
					return dec.infoText();
			}
			return null;
		}

		@Override
		public IRegion getHoverRegion(ITextViewer viewer, int offset) {
			hyperlink = configuration.editor().hyperlinkAtOffset(offset);
			if (hyperlink != null)
				return hyperlink.getHyperlinkRegion();
			return null;
		}

		@Override
		public IInformationControlCreator getHoverControlCreator() {
			return new IInformationControlCreator() {
				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new DefaultInformationControl(parent, new HTMLTextPresenter(true));
				}
			};
		}
		
	}