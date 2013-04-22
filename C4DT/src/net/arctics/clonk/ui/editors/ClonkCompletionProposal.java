package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;

import java.util.regex.Pattern;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IDocumentedDeclaration;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class ClonkCompletionProposal implements ICompletionProposal, ICompletionProposalExtension6, ICompletionProposalExtension2 {

	/** Associated declaration */
	private final Declaration declaration;

	/** The string to be displayed in the completion proposal popup. */
	private String displayString;
	/** The string to be displayed after the display string. */
	private final String postInfo;
	/** The replacement string. */
	protected String replacementString;

	/** The replacement offset. */
	protected int replacementOffset;
	/** The replacement length. */
	protected int replacementLength;
	/** The cursor position after this proposal has been applied. */
	protected int cursorPosition;
	/** The image to be displayed in the completion proposal popup. */
	private final Image image;
	/** The context information of this proposal. */
	private final IContextInformation contextInformation;
	/** The additional info of this proposal. */
	private String additionalProposalInfo;

	/** Editor the proposal was created for */
	private ClonkTextEditor editor;

	/** Category for sorting */
	private int category;

	private boolean displayStringRecomputationNecessary;

	public void setEditor(ClonkTextEditor editor) {
		this.editor = editor;
	}

	public String replacementString() { return replacementString; }
	public int replacementOffset() { return replacementOffset; }
	public int replacementLength() { return replacementLength; }
	public final int category() { return category;}
	public void setCategory(int category) { this.category = category; }
	public int cursorPosition() { return cursorPosition; }

	/**
	 * Creates a new completion proposal based on the provided information. The replacement string is
	 * considered being the display string too. All remaining fields are set to <code>null</code>.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
	 */
	public ClonkCompletionProposal(Declaration declaration, String replacementString, int replacementOffset, int replacementLength, int cursorPosition) {
		this(declaration, replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null, null, null);
	}

	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided information.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
	 * @param image the image to display for this proposal
	 * @param displayString the string to be displayed for the proposal
	 * @param contextInformation the context information associated with this proposal
	 * @param additionalProposalInfo the additional information associated with this proposal
	 * @param postInfo information that is appended to displayString
	 */
	public ClonkCompletionProposal(
		Declaration declaration,
		String replacementString,
		int replacementOffset, int replacementLength, int cursorPosition,
		Image image,
		String displayString,
		IContextInformation contextInformation,
		String additionalProposalInfo, String postInfo,
		ClonkTextEditor editor
	) {
		this.declaration = declaration;
		this.replacementString= replacementString;
		this.replacementOffset= replacementOffset;
		this.replacementLength= replacementLength;
		this.cursorPosition= cursorPosition;
		this.image= image;
		this.displayString= displayString;
		this.contextInformation= contextInformation;
		this.additionalProposalInfo= additionalProposalInfo;
		this.postInfo = postInfo;
		this.setEditor(editor);
	}

	public ClonkCompletionProposal(
		Declaration declaration,
		String replacementString,
		int replacementOffset, int replacementLength,
		Image image,
		IContextInformation contextInformation,
		String additionalProposalInfo, String postInfo,
		ClonkTextEditor editor
	) {
		this(
			declaration, replacementString, replacementOffset, replacementLength,
			declaration.name().length(), image, null, contextInformation, additionalProposalInfo, postInfo, editor
		);
		displayStringRecomputationNecessary = true;
	}

	public Declaration declaration() {
		return declaration;
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	@Override
	public void apply(IDocument document) {
		try {
			if (replacementString != null)
				document.replace(replacementOffset, replacementLength, replacementString);
			if (editor != null)
				editor.completionProposalApplied(this);
		} catch (BadLocationException x) {
			// ignore
		}
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	@Override
	public Point getSelection(IDocument document) {
		getDisplayString();
		return new Point(replacementOffset + cursorPosition, 0);
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	@Override
	public IContextInformation getContextInformation() {
		return contextInformation;
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	@Override
	public Image getImage() {
		return image;
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	@Override
	public String getDisplayString() {
		if (displayStringRecomputationNecessary) {
			displayStringRecomputationNecessary = false;
			if (declaration instanceof IDocumentedDeclaration)
				((IDocumentedDeclaration)declaration).fetchDocumentation();
			displayString = declaration.displayString(declaration);
			Function func = as(declaration, Function.class);
			if (func != null)
				// adjust cursor position to jump over brackets if zero parameters, but only when not just inserting the plain function name
				// for more than zero parameters, jump into brackets to let user type her parameters
				if (replacementString.length() > declaration.name().length())
					cursorPosition += func.numParameters() == 0 ? 2 : 1;
		}
		return displayString();
	}

	public String displayString() {
		if (displayString != null)
			return displayString;
		return replacementString;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	@Override
	public String getAdditionalProposalInfo() {
		if (additionalProposalInfo == null && declaration != null)
			additionalProposalInfo = declaration.infoText(context());
		return additionalProposalInfo;
	}

	private Declaration context() {
		return editor != null ? editor.structure() : declaration;
	}

	@Override
	public StyledString getStyledDisplayString() {
		getDisplayString();
		if (displayString == null)
			return new StyledString("<Error>", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
		StyledString result = new StyledString(displayString);
		result.append(postInfo, StyledString.QUALIFIER_STYLER);
		return result;
	}

	public static final Pattern VALID_PREFIX_PATTERN = Pattern.compile("\\w+");

	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		try {
			int replaceOffset = replacementOffset();
			if (offset >= replaceOffset) {
				String prefix = document.get(replaceOffset, offset - replaceOffset).toLowerCase();
				if (!VALID_PREFIX_PATTERN.matcher(prefix).matches())
					return false;
				for (String s : identifiers())
					if (s.toLowerCase().contains(prefix))
						return true;
				String content = document.get(replaceOffset, offset - replaceOffset).toLowerCase();
				if (declaration != null) {
					if (declaration.name().toLowerCase().contains(content))
						return true;
					if (declaration instanceof Definition && ((Definition)declaration).id().stringValue().toLowerCase().contains(content))
						return true;
				}
			}
		} catch (BadLocationException e) {
			// concurrent modification - ignore
		}
		return false;
	}

	@Override
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		replacementLength = offset - replacementOffset;
		apply(viewer.getDocument());
	}

	@Override
	public void selected(ITextViewer viewer, boolean smartToggle) {
	}

	@Override
	public void unselected(ITextViewer viewer) {
	}

	public String[] identifiers() {
		if (declaration != null) {
			String decName = declaration.name();
			if (declaration instanceof Function)
				decName += "()";
			if (declaration instanceof Definition)
				return new String[] {replacementString(), decName, ((Definition)declaration).id().stringValue()};
			else
				return new String[] {replacementString(), decName};
		}
		return new String[] {replacementString()};
	}

	public String primaryComparisonIdentifier() {
		if (declaration instanceof Definition)
			return ((Definition)declaration).id().stringValue();
		if (displayString != null)
			return displayString;
		return replacementString;
	}

	public boolean requiresDocumentReparse() { return false; }

}
