package net.arctics.clonk.ui.editors.mapcreator;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.parser.mapcreator.C4MapOverlay;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class MapCreatorCompletionProcessor extends ClonkCompletionProcessor<MapCreatorEditor> {

	private static final Pattern startedOverlay = Pattern.compile(".*\\s+overlay\\s+([A-Za-z_0-9]*)");
	private static final Pattern startedMap     = Pattern.compile(".*\\s+map\\s+([A-Za-z_0-9]*)");
	private static final Pattern startedAttr    = Pattern.compile(".*\\s+([A-Za-z_0-9]*)");
	private static final Pattern startedAttrVal = Pattern.compile(".*\\s+([A-Za-z_0-9]*)\\s*=\\s*([A-Za-z_0-9]*).*");
	
	public MapCreatorCompletionProcessor(MapCreatorEditor editor) {
		super(editor);
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		
		try {
			getEditor().silentReparse();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		IDocument doc = viewer.getDocument();
		String line;
		int lineStart;
		try {
			IRegion lineRegion = doc.getLineInformationOfOffset(offset);
			line = doc.get(lineRegion.getOffset(), lineRegion.getLength());
			lineStart = lineRegion.getOffset();
		} catch (BadLocationException e) {
			line = "";
			lineStart = offset;
		}
		
		List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
		Matcher m;
		C4MapOverlay overlay = getEditor().getMapCreator().overlayAt(offset);
		if ((m = startedOverlay.matcher(line)).matches() || (m = startedMap.matcher(line)).matches()) {

		}
		else if (overlay != null && (m = startedAttr.matcher(line)).matches()) {
			String prefix = m.group(1).toLowerCase();
			Field[] fields = overlay.getClass().getFields();
			for (Field f : fields) {
				if (f.getName().toLowerCase().startsWith(prefix)) {
					proposals.add(new CompletionProposal(f.getName(), lineStart+m.start(1), prefix.length(), f.getName().length()));
				}
			}
		}
		else if (overlay != null && (m = startedAttrVal.matcher(line)).matches()) {
			String attrName = m.group(1);
			String attrValStart = m.group(2);
			try {
				Field attr = overlay.getClass().getField(attrName);
				// enum recommendations
				if (attr.getType().getSuperclass() == Enum.class) {
					Enum<?>[] values = Utilities.valuesOfEnum(attr.getType());
					for (Enum<?> v : values) {
						if (v.name().toLowerCase().startsWith(attrValStart)) {
							proposals.add(new CompletionProposal(v.name(), lineStart+m.start(2), attrValStart.length(), v.name().length()));
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
