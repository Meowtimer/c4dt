package net.arctics.clonk.parser.c4script.quickfix;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ExprElm;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.VisitCodeFlavour;
import net.arctics.clonk.ui.editors.c4script.ExpressionLocator;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public class C4ScriptMarkerResolution implements IMarkerResolution, IMarkerResolution2 {

	private String label;
	private String description;
	//private Image image;
	private IRegion region;

	public C4ScriptMarkerResolution(IMarker marker) {
		this.label = String.format(Messages.Fix, marker.getAttribute(IMarker.MESSAGE, Messages.MarkerResolutionDefaultMessage));
		this.description = Messages.MarkerResolutionDescription;
		int charStart = marker.getAttribute(IMarker.CHAR_START, 0), charEnd = marker.getAttribute(IMarker.CHAR_END, 0);
		this.region = new Region(charStart, charEnd-charStart);
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		Script script = Script.get(marker.getResource(), true);
		Function func = script.funcAt(region.getOffset()); 
		ExpressionLocator locator = new ExpressionLocator(region.getOffset()-func.bodyLocation().getOffset());
		TextFileDocumentProvider provider = Core.instance().textFileDocumentProvider();
		IDocument doc = null;
		try {
			provider.connect(marker.getResource());
		} catch (CoreException e1) {
			e1.printStackTrace();
			return;
		}
		try {
			doc = provider.getDocument(marker.getResource());
			C4ScriptParser parser;
			try {
				parser = C4ScriptParser.visitCode(doc, script, func, locator, null, VisitCodeFlavour.AlsoStatements, true);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			ExprElm expr = locator.getTopLevelInRegion();
			if (expr != null) {
				try {
					doc.replace(expr.getOffset()+func.bodyLocation().getOffset(), expr.getLength(), expr.exhaustiveOptimize(parser).toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			provider.disconnect(doc);
		}
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Image getImage() {
		return null;
	}

}
