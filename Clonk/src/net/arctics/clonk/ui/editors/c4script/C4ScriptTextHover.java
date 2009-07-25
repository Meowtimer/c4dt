/**
 * 
 */
package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.widgets.Shell;

@SuppressWarnings("restriction")
public class C4ScriptTextHover implements ITextHover, ITextHoverExtension {

		/**
		 * 
		 */
		private final C4ScriptSourceViewerConfiguration configuration;
		private DeclarationLocator declLocator;
//		private IInformationControlCreator informationControlCreator;
		
		public C4ScriptTextHover(C4ScriptSourceViewerConfiguration clonkSourceViewerConfiguration) {
			super();
			//informationControlCreator = new C4ScriptTextHoverCreator();
			configuration = clonkSourceViewerConfiguration;
		}
		
		public String getHoverInfo(ITextViewer viewer, IRegion region) {
			return declLocator != null && declLocator.getDeclaration() != null
				? declLocator.getDeclaration().getInfoText()
				: null;
		}

		public IRegion getHoverRegion(ITextViewer viewer, int offset) {
			try {
				declLocator = new DeclarationLocator(configuration.getEditor(), viewer.getDocument(), new Region(offset, 0));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return declLocator.getIdentRegion();
		}

		public IInformationControlCreator getHoverControlCreator() {
			return new IInformationControlCreator() {
				public IInformationControl createInformationControl(Shell parent) {
					return new DefaultInformationControl(parent, new HTMLTextPresenter(true));
				}
			};
		}
		
	}