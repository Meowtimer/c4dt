package net.arctics.clonk.ui.editors;

import net.arctics.clonk.parser.C4Declaration;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class ClonkCompletionProposal implements ICompletionProposal, ICompletionProposalExtension6 {
	
	private C4Declaration declaration;
	
	/** The string to be displayed in the completion proposal popup. */
	private String displayString;
	/** The string to be displayed after the display string. */
	private String postInfo;
	/** The replacement string. */
	protected String replacementString;

	/** The replacement offset. */
	protected int replacementOffset;
	/** The replacement length. */
	protected int replacementLength;
	/** The cursor position after this proposal has been applied. */
	private int cursorPosition;
	/** The image to be displayed in the completion proposal popup. */
	private Image image;
	/** The context information of this proposal. */
	private IContextInformation contextInformation;
	/** The additional info of this proposal. */
	private String additionalProposalInfo;
	
	private ClonkTextEditor editor;

	public void setEditor(ClonkTextEditor editor) {
		this.editor = editor;
	}
	
	public String getReplacementString() {
		return replacementString;
	}

	public int getReplacementOffset() {
		return replacementOffset;
	}

	public int getReplacementLength() {
		return replacementLength;
	}

	/**
	 * Creates a new completion proposal based on the provided information. The replacement string is
	 * considered being the display string too. All remaining fields are set to <code>null</code>.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
	 */
	public ClonkCompletionProposal(C4Declaration declaration, String replacementString, int replacementOffset, int replacementLength, int cursorPosition) {
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
			C4Declaration declaration,
			String replacementString,
			int replacementOffset, int replacementLength, int cursorPosition,
			Image image,
			String displayString,
			IContextInformation contextInformation,
			String additionalProposalInfo, String postInfo,
			ClonkTextEditor editor
	) {
//		Assert.isNotNull(replacementString);
//		Assert.isTrue(replacementOffset >= 0);
//		Assert.isTrue(replacementLength >= 0);
//		Assert.isTrue(cursorPosition >= 0);

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
	
	public C4Declaration getDeclaration() {
		return declaration;
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
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
	public Point getSelection(IDocument document) {
		return new Point(replacementOffset + cursorPosition, 0);
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return contextInformation;
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return image;
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		if (displayString != null)
			return displayString;
		return replacementString;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		if (additionalProposalInfo == null && declaration != null) {
			additionalProposalInfo = declaration.getInfoText();
		}
		return additionalProposalInfo;
	}

	public StyledString getStyledDisplayString() {
		if (displayString == null)
			return new StyledString("<Error>", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
		StyledString result = new StyledString(displayString);
		result.append(postInfo, StyledString.QUALIFIER_STYLER);
//		result.setStyle(fDisplayString.length(), fPostInfo.length(), StyledString.createColorRegistryStyler(JFacePreferences.DECORATIONS_COLOR,JFacePreferences.CONTENT_ASSIST_BACKGROUND_COLOR) );
		
		return result;
	}
}
