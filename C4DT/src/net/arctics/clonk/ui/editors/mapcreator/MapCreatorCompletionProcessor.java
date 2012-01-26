package net.arctics.clonk.ui.editors.mapcreator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.parser.mapcreator.MapOverlayBase;
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

	private static final Pattern startedOverlay = Pattern.compile(".*\\s+overlay\\s+([A-Za-z_0-9]*)"); //$NON-NLS-1$
	private static final Pattern startedMap     = Pattern.compile(".*\\s+map\\s+([A-Za-z_0-9]*)"); //$NON-NLS-1$
	private static final Pattern startedAttr    = Pattern.compile(".*\\s+([A-Za-z_0-9]*).*"); //$NON-NLS-1$
	private static final Pattern startedAttrVal = Pattern.compile(".*\\s+([A-Za-z_0-9]*)\\s*=\\s*([A-Za-z_0-9]*).*"); //$NON-NLS-1$
	
	public MapCreatorCompletionProcessor(MapCreatorEditor editor) {
		super(editor);
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		
		try {
			editor().silentReparse();
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
			String seps = ";{}"; //$NON-NLS-1$
			for (int i = seps.length()-1; i>=0; i--) {
				int sepIndex = line.lastIndexOf(seps.charAt(i), offset-lineStart-1);
				if (sepIndex != -1) {
					line = line.substring(sepIndex+1);
					lineStart += sepIndex+1;
					break;
				}
			}
		} catch (BadLocationException e) {
			line = ""; //$NON-NLS-1$
			lineStart = offset;
		}
		
		List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
		Matcher m;
		MapOverlayBase overlay = editor().getMapCreator().overlayAt(offset);
		if (overlay == editor().getMapCreator())
			overlay = null;
		if ((m = startedOverlay.matcher(line)).matches() || (m = startedMap.matcher(line)).matches()) {

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
		else if ((m = startedAttr.matcher(line)).matches() && offset-lineStart >= m.start(1) && offset-lineStart <= m.end(1)) {
			String prefix = m.group(1).toLowerCase();
			if (overlay != null) {
				Field[] fields = overlay.getClass().getFields();
				for (Field f : fields) {
					if (Modifier.isPublic(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())) {
						if (f.getName().toLowerCase().startsWith(prefix)) {
							proposals.add(new CompletionProposal(f.getName(), lineStart+m.start(1), prefix.length(), f.getName().length()));
						}
					}
				}
			}
			
			for (String keyword : MapOverlayBase.DEFAULT_CLASS.keySet()) {
				if (keyword.toLowerCase().startsWith(prefix))
					proposals.add(new CompletionProposal(keyword, lineStart+m.start(1), prefix.length(), keyword.length()));
			}
		}
		
		return sortProposals(proposals);
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
