package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.ClonkCore.IDocumentAction;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptOperator;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.MutableRegion;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4ScriptParser.IMarkerListener;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.BunchOfStatements;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ExprWriter;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.ast.Sequence;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.Tuple;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement;
import net.arctics.clonk.parser.c4script.ast.VarDeclarationStatement.VarInitialization;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.util.ArrayHelpers;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bis jetzt keine Funktion
 * Until now!
 * @author ZokRadonh
 *
 */
public class ClonkQuickAssistProcessor implements IQuickAssistProcessor, IMarkerListener  {
	
	/*private class SpecifiableCallFunc extends CallFunc {
		@Override
		public String getDeclarationName() {
			if (declarationName == null) {
				ClonkQuickAssistProcessor.this.
				declarationName = UI.input(shell, title, prompt, defaultValue)
			}
		}
	}*/
	
	private static final ICompletionProposal[] NO_SUGGESTIONS=  new ICompletionProposal[0];
	private static ClonkQuickAssistProcessor singleton;
	
	public static ClonkQuickAssistProcessor getSingleton() {
		return singleton;
	}
	
	public ClonkQuickAssistProcessor() {
		super();
		assert(singleton == null);
		singleton = this;
	}

	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		return true;
	}

	private static final Set<ParserErrorCode> fixableParserErrorCodes = Utilities.set(
		HashSet.class,
		ParserErrorCode.VariableCalled, ParserErrorCode.NeverReached, ParserErrorCode.NotFinished, ParserErrorCode.UndeclaredIdentifier, ParserErrorCode.IncompatibleTypes, ParserErrorCode.NoSideEffects, ParserErrorCode.NoAssignment, ParserErrorCode.NoInheritedFunction, ParserErrorCode.ReturnAsFunction, ParserErrorCode.Unused	
	);
 	
	public boolean canFix(Annotation annotation) {
		if (annotation instanceof MarkerAnnotation) {
			MarkerAnnotation ma = (MarkerAnnotation)annotation;
			String ty;
			try {
				ty = ma.getMarker().getType();
			} catch (CoreException e) {
				e.printStackTrace();
				return false;
			}
			if (ty.equals(ClonkCore.MARKER_C4SCRIPT_ERROR) || ty.equals(ClonkCore.MARKER_C4SCRIPT_ERROR_WHILE_TYPING)) {
				return fixableParserErrorCodes.contains(ParserErrorCode.getErrorCode(ma.getMarker()));
			}
		}
		return false;
	}

	private static boolean isAtPosition(int offset, Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() +  pos.getLength()));
	}
	
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
		int offset = context.getOffset();
		List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
		IAnnotationModel model = context.getSourceViewer().getAnnotationModel();
		if (model == null)
			return NO_SUGGESTIONS;
		@SuppressWarnings("rawtypes")
		Iterator iter = model.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation = (Annotation)iter.next();
			if (canFix(annotation)) {
				Position pos = model.getPosition(annotation);
				if (isAtPosition(offset, pos)) {
					collectProposals(((MarkerAnnotation) annotation).getMarker(), pos, proposals, null, ClonkTextEditor.getEditorForSourceViewer(context.getSourceViewer(), C4ScriptEditor.class));
				}
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private int semicolonAdd = 0;
	
	@Override
	public WhatToDo markerEncountered(C4ScriptParser parser, ParserErrorCode code, int markerStart, int markerEnd, boolean noThrow, int severity, Object... args) {
		if (code == ParserErrorCode.TokenExpected && args[0].equals(";")) { //$NON-NLS-1$
			semicolonAdd = 1; // replace one more char (semicolon is there, just not in the substring denoted by the expressionRegion)
			return WhatToDo.DropCharges;
		}
		return WhatToDo.PassThrough;
	}
	
	private final class ReplacementStatement extends Statement {
		private final String replacementString;
		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

		private ReplacementStatement(IRegion regionToDelete, String replacementString, IDocument document, int absoluteOffset) {
			this.replacementString = replacementString;
			possiblyAdvanceRegionToDeleteLine(document, regionToDelete, absoluteOffset);
		}
		
		private void possiblyAdvanceRegionToDeleteLine(IDocument document, IRegion expressionRegion, int absoluteOffset) {
			int exprStart = expressionRegion.getOffset()-absoluteOffset;
			int additionalExprLength = 0;
			try {
				IRegion originalLineRegion = document.getLineInformationOfOffset(expressionRegion.getOffset());
				String originalLine = document.get(originalLineRegion.getOffset(), originalLineRegion.getLength());
				boolean deleteLine = true;
				for (int i = expressionRegion.getOffset()-originalLineRegion.getOffset()-1; i >= 0; i--) {
					if (!BufferedScanner.isWhiteSpace(originalLine.charAt(i))) {
						deleteLine = false;
						break;
					}
				}
				for (int i = expressionRegion.getOffset()+expressionRegion.getLength()+1-originalLineRegion.getOffset(); i < originalLine.length(); i++) {
					if (!BufferedScanner.isWhiteSpace(originalLine.charAt(i))) {
						deleteLine = false;
						break;
					}
				}
				if (deleteLine) {
					exprStart = originalLineRegion.getOffset()-expressionRegion.getOffset();
					for (int abs = originalLineRegion.getOffset()-1; abs >= 0 && BufferedScanner.isWhiteSpace(document.getChar(abs)); abs--) {
						exprStart--;
					}
					additionalExprLength = originalLineRegion.getOffset()+originalLine.length()-expressionRegion.getOffset()-expressionRegion.getLength();
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			setExprRegion(exprStart, (exprStart < 0 ? 0 : exprStart)+expressionRegion.getLength()+additionalExprLength);
		}

		@Override
		public void doPrint(ExprWriter output, int depth) {
			output.append(replacementString);
		}
	}

	public static final class ParameterizedProposalMarkerResolution extends WorkbenchMarkerResolution {

		private ParameterizedProposal proposal;
		private IMarker originalMarker;
		
		public ParameterizedProposalMarkerResolution(ParameterizedProposal proposal, IMarker originalMarker) {
			this.proposal = proposal;
			this.originalMarker = originalMarker;
		}
		
		@Override
		public String getDescription() {
			return proposal.getDisplayString();
		}

		@Override
		public Image getImage() {
			return proposal.getImage();
		}

		@Override
		public String getLabel() {
			return proposal.getDisplayString();
		}

		@Override
		public void run(IMarker marker) {
			proposal.runOnMarker(marker);
		}

		private boolean relevant(IMarker marker) {
			if (marker.getResource().equals(originalMarker.getResource())) {
				return false;
			}
			return ParserErrorCode.getErrorCode(marker) == ParserErrorCode.getErrorCode(originalMarker);
		}
		
		@Override
		public IMarker[] findOtherMarkers(IMarker[] markers) {
			List<IMarker> result = new ArrayList<IMarker>(markers.length);
			for (IMarker m : markers) {
				if (relevant(m)) {
					result.add(m);
				}
			}
			return result.toArray(new IMarker[result.size()]);
		}
		
	}
	
	public final class ParameterizedProposal extends ClonkCompletionProposal {
		private final Replacement replacement;
		private final int tabIndentation;
		private final C4ScriptParser parser;
		private C4Function func;

		private ParameterizedProposal(C4Declaration declaration,
				String replacementString, int replacementOffset,
				int replacementLength, int cursorPosition, Image image,
				String displayString, IContextInformation contextInformation,
				String additionalProposalInfo, String postInfo,
				ClonkTextEditor editor,
				Replacement replacement, int tabIndentation,
				C4ScriptParser parser, C4Function func) {
			super(declaration, replacementString, replacementOffset,
					replacementLength, cursorPosition, image, displayString,
					contextInformation, additionalProposalInfo, postInfo,
					editor);
			this.replacement = replacement;
			this.tabIndentation = tabIndentation;
			this.parser = parser;
			this.func = func;
		}
		
		public boolean createdFrom(Replacement other) {
			return this.replacement.equals(other); 
		}

		@Override
		public void apply(IDocument document) {
			ExprElm replacementExpr = replacement.getReplacementExpression();
			if (replacementExpr != ExprElm.NULL_EXPR) {
				for (ExprElm spec : replacement.getSpecifiable()) {
					if (spec instanceof AccessDeclaration) {
						AccessDeclaration accessDec = (AccessDeclaration) spec;
						String s = UI.input(
								PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								Messages.ClonkQuickAssistProcessor_SpecifyValue,
								String.format(Messages.ClonkQuickAssistProcessor_SpecifyFormat, accessDec.getDeclarationName()), accessDec.getDeclarationName(),
								new IInputValidator() {
									@Override
									public String isValid(String newText) {
										if (!validIdentifierPattern.matcher(newText).matches()) {
											return String.format(Messages.ClonkQuickAssistProcessor_NotAValidFunctionName, newText);
										} else {
											return null;
										}
									}
								}
						);
						if (s != null) {
							accessDec.setDeclarationName(s);
						}
					}
				}
				try {
					this.replacementString = replacement.getReplacementExpression().optimize(parser).toString(tabIndentation+1);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			} else {
				// don't replace expression
				replacementString = ""; //$NON-NLS-1$
				replacementLength = 0;
			}
			cursorPosition = replacementString.length();
			super.apply(document);
			
			for (Replacement.AdditionalDeclaration dec : replacement.getAdditionalDeclarations()) {
				StringBuilder builder = new StringBuilder(50);
				dec.declaration.sourceCodeRepresentation(builder, dec.code);
				builder.append("\n"); //$NON-NLS-1$
				builder.append("\n"); //$NON-NLS-1$
				try {
					document.replace(func.getHeader().getOffset(), 0, builder.toString());
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
			
			C4ScriptEditor.TextChangeListener listener = C4ScriptEditor.TextChangeListener.getListenerFor(document);
			if (listener != null) {
				listener.scheduleReparsing(false);
			}
		}

		public void runOnMarker(IMarker marker) {
			try {
				ClonkCore.getDefault().performActionsOnFileDocument(marker.getResource(), new IDocumentAction() {
					@Override
					public void run(IDocument document) {
						System.out.println("Applying to " + document.toString());
						apply(document);
					}
				});
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		public Replacement getReplacement() {
			return replacement;
		}
	}

	private static class Replacement {
		
		public static class AdditionalDeclaration {
			public C4Declaration declaration;
			public ExprElm code;
			public AdditionalDeclaration(C4Declaration declaration, ExprElm code) {
				super();
				this.declaration = declaration;
				this.code = code;
			}
		}
		
		private String title;
		private ExprElm replacementExpression;
		private ExprElm[] specifiable;
		private List<AdditionalDeclaration> additionalDeclarations = new LinkedList<AdditionalDeclaration>();
		private boolean regionToBeReplacedSpecifiedByReplacementExpression; // yes!
		
		public Replacement(String title, ExprElm replacementExpression, ExprElm... specifiable) {
			super();
			this.title = title;
			this.replacementExpression = replacementExpression;
			this.specifiable = specifiable;
			this.regionToBeReplacedSpecifiedByReplacementExpression = !(replacementExpression instanceof Statement);
		}
		public String getTitle() {
			return title;
		}
		public ExprElm getReplacementExpression() {
			return replacementExpression;
		}
		public ExprElm[] getSpecifiable() {
			return specifiable;
		}
		public List<AdditionalDeclaration> getAdditionalDeclarations() {
			return additionalDeclarations;
		}
		@Override
		public boolean equals(Object other) {
			if (other instanceof Replacement) {
				Replacement otherR = (Replacement) other;
				return otherR.title.equals(title) && otherR.replacementExpression.equals(replacementExpression);
			} else {
				return false;
			}
		}
		@Override
		public String toString() {
			return String.format("%s: %s", title, replacementExpression.toString()); //$NON-NLS-1$
		}
	}
	
	private static final class ReplacementsList extends LinkedList<Replacement> {
		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
		private ExprElm offending;
		private List<ICompletionProposal> existingList;
		public ReplacementsList(ExprElm offending, List<ICompletionProposal> existingList) {
			super();
			this.offending = offending;
			this.existingList = existingList;
		}
		public Replacement add(String replacement, ExprElm elm, boolean alwaysStatement, ExprElm... specifiable) {
			if (alwaysStatement && !(elm instanceof Statement)) {
				elm = new SimpleStatement(elm);
			}
			if (elm.getExprEnd() == elm.getExprStart() && offending != null) {
				elm.setExprRegion(offending.getExprStart(), offending.getExprEnd());
			}
			Replacement newOne = new Replacement(replacement, elm, specifiable);
			// don't add duplicates
			for (Replacement existing : this) {
				if (existing.equals(newOne))
					return existing;
			}
			for (ICompletionProposal prop : existingList) {
				if (prop instanceof ParameterizedProposal && ((ParameterizedProposal)prop).createdFrom(newOne)) {
					return ((ParameterizedProposal)prop).getReplacement();
				}
			}
			this.add(newOne);
			return newOne;
		}
		public Replacement add(String replacement, ExprElm elm, ExprElm... specifiable) {
			return add(replacement, elm, true, specifiable);
		}
	}
	
	private static ExprElm identifierReplacement(AccessDeclaration original, String newName) {
		AccessVar result = new AccessVar(newName);
		result.setExprRegion(original.getExprStart(), original.getExprStart()+original.getIdentifierLength());
		return result;
	}
	
	private static final Pattern validIdentifierPattern = Pattern.compile("[a-zA-Z_]\\w*"); //$NON-NLS-1$
	
	private static String parmNameFromExpression(ExprElm expression, int index) {
		String exprString = expression.toString();
		Matcher m = validIdentifierPattern.matcher(exprString);
		if (m.matches()) {
			return m.group();
		} else {
			return "par"+index; //$NON-NLS-1$
		}
	}
	
	public void collectProposals(IMarker marker, Position position, List<ICompletionProposal> proposals, IDocument document, Object editorOrScript) {

		ParserErrorCode errorCode = ParserErrorCode.getErrorCode(marker);
		final IRegion expressionRegion = ParserErrorCode.getExpressionLocation(marker);
		if (expressionRegion.getOffset() == -1)
			return;
		C4ScriptEditor editor = editorOrScript instanceof C4ScriptEditor ? (C4ScriptEditor)editorOrScript : null;
		C4ScriptBase script = editorOrScript instanceof C4ScriptBase
			? (C4ScriptBase)editorOrScript
			: editor != null ? editor.scriptBeingEdited() : null;
		Object needToDisconnect = null;
		if (document == null) {
			if (editor != null) {
				document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
			} else if (script != null && script.getScriptFile() instanceof IFile) {
				needToDisconnect = script.getScriptFile();
				try {
					ClonkCore.getDefault().getTextFileDocumentProvider().connect(needToDisconnect);
				} catch (CoreException e) {
					e.printStackTrace();
					return;
				}
				document = ClonkCore.getDefault().getTextFileDocumentProvider().getDocument(needToDisconnect);
			}
		}
		try {
			if (script == null || document == null)
				return;
			C4Function func = script.funcAt(position.getOffset());
			final int tabIndentation = BufferedScanner.getTabIndentation(document.get(), expressionRegion.getOffset());
			ExpressionLocator locator = new ExpressionLocator(position.getOffset()-expressionRegion.getOffset());
			semicolonAdd = 0;
			final C4ScriptParser parser = C4ScriptParser.reportExpressionsAndStatements(document, expressionRegion, script, func, locator, this);
			ExprElm offendingExpression = locator.getExprAtRegion();
			Statement topLevel = offendingExpression != null ? offendingExpression.containingStatementOrThis() : null;
			if (offendingExpression == topLevel)
				semicolonAdd = 0;

			if (offendingExpression != null && topLevel != null) {
				ReplacementsList replacements = new ReplacementsList(offendingExpression, proposals);
				switch (errorCode) {
				case VariableCalled:
					assert(offendingExpression instanceof CallFunc);
					replacements.add(
							Messages.ClonkQuickAssistProcessor_RemoveBrackets,
							topLevel.replaceSubElement(offendingExpression, new AccessVar(((CallFunc)offendingExpression).getDeclarationName()))
					);
					break;
				case NeverReached: {
					String s = topLevel.toString();
					replacements.add(
							Messages.ClonkQuickAssistProcessor_CommentOutStatement,
							new Comment(topLevel.toString(), s.contains("\n")) //$NON-NLS-1$
					);
					break;
				}
				case NotFinished:
					replacements.add(
							Messages.ClonkQuickAssistProcessor_AddMissingSemicolon,
							topLevel // will be added by converting topLevel to string
					);
					semicolonAdd = 0; // it's really missing!
					break;
				case UndeclaredIdentifier:
					if (offendingExpression instanceof AccessVar && offendingExpression.getParent() instanceof BinaryOp) {
						AccessVar var = (AccessVar) offendingExpression;
						BinaryOp op = (BinaryOp) offendingExpression.getParent();
						if (topLevel == op.getParent() && op.getOperator() == C4ScriptOperator.Assign && op.getLeftSide() == offendingExpression) {
							replacements.add(
									Messages.ClonkQuickAssistProcessor_ConvertToVarDeclaration,
									new VarDeclarationStatement(var.getDeclarationName(), op.getRightSide(), Keywords.VarNamed.length()+1, C4VariableScope.VAR)
							);
						}
					}
					if (offendingExpression instanceof AccessDeclaration) {

						AccessDeclaration accessDec = (AccessDeclaration) offendingExpression;

						// create new variable or function
						Replacement createNewDeclarationReplacement = replacements.add(
								String.format(offendingExpression instanceof AccessVar ? Messages.ClonkQuickAssistProcessor_CreateLocalVar : Messages.ClonkQuickAssistProcessor_CreateLocalFunc, accessDec.getDeclarationName()),
								ExprElm.NULL_EXPR,
								false
						);
						List<Replacement.AdditionalDeclaration> decs = createNewDeclarationReplacement.getAdditionalDeclarations();
						if (accessDec instanceof AccessVar) {
							decs.add(new Replacement.AdditionalDeclaration(
									new C4Variable(accessDec.getDeclarationName(), C4VariableScope.LOCAL),
									ExprElm.NULL_EXPR
							));
						} else {
							CallFunc callFunc = (CallFunc) accessDec;
							C4Function function;
							decs.add(new Replacement.AdditionalDeclaration(
									function = new C4Function(accessDec.getDeclarationName(), C4FunctionScope.PUBLIC),
									ExprElm.NULL_EXPR
							));
							List<C4Variable> parms = new ArrayList<C4Variable>(callFunc.getParams().length);
							int p = 0;
							for (ExprElm parm : callFunc.getParams()) {
								parms.add(new C4Variable(parmNameFromExpression(parm, ++p), parm.getType(parser)));
							}
							function.setParameters(parms);
						}

						// gather proposals through ClonkCompletionProcessor and propose those with a similar name 
						ExprElm expr;
						if (offendingExpression.getParent() instanceof Sequence) {
							Sequence sequence = (Sequence) offendingExpression.getParent();
							expr = sequence.sequenceWithElementsRemovedFrom(offendingExpression); 
						} else {
							expr = null;
						}
						List<ICompletionProposal> possible = C4ScriptCompletionProcessor.compuateProposalsForExpression(
								expr, func, parser, document
						);
						for (ICompletionProposal p : possible) {
							if (p instanceof ClonkCompletionProposal) {
								ClonkCompletionProposal clonkProposal = (ClonkCompletionProposal) p;
								C4Declaration dec = clonkProposal.getDeclaration();
								if (dec == null || !accessDec.declarationClass().isAssignableFrom(dec.getClass()))
									continue;
								int similarity = Utilities.getSimilarity(dec.getName(), accessDec.getDeclarationName());
								if (similarity > 0) {
									// always create AccessVar and set its region such that only the identifier part of the AccessDeclaration object
									// will be replaced -> no unnecessary tidy-up of CallFunc parameters
									ExprElm repl = identifierReplacement(accessDec, dec.getName());
									replacements.add(String.format(Messages.ClonkQuickAssistProcessor_ReplaceWith, dec.getName()), repl, false);
								}
							}
						}
					}
					break;
				case IncompatibleTypes:
					if (C4Type.makeType(ParserErrorCode.getArg(marker, 0), true) == C4Type.STRING) {
						replacements.add(
								Messages.ClonkQuickAssistProcessor_QuoteExpression,
								new StringLiteral(offendingExpression.toString()),
								false
						);
					}
					break;
				case NoSideEffects:
					if (topLevel instanceof SimpleStatement) {
						SimpleStatement statement = (SimpleStatement) topLevel;
						replacements.add(
								Messages.ClonkQuickAssistProcessor_ConvertToReturn,
								new ReturnStatement((statement.getExpression()))
						);
						replacements.add(
								Messages.ClonkQuickAssistProcessor_Remove,
								new ReplacementStatement(expressionRegion, "", document, expressionRegion.getOffset())
						).regionToBeReplacedSpecifiedByReplacementExpression = true;
						CallFunc callFunc = new CallFunc(Messages.ClonkQuickAssistProcessor_FunctionToBeCalled, statement.getExpression());
						replacements.add(Messages.ClonkQuickAssistProcessor_WrapWithFunctionCall, callFunc, callFunc);
					}
					break;
				case NoAssignment:
					if (topLevel instanceof SimpleStatement) {
						SimpleStatement statement = (SimpleStatement) topLevel;

						if (statement.getExpression() instanceof BinaryOp) {
							BinaryOp binaryOp = (BinaryOp) statement.getExpression();
							if (binaryOp.getOperator() == C4ScriptOperator.Equal && binaryOp.getLeftSide().modifiable(parser)) {
								replacements.add(
										Messages.ClonkQuickAssistProcessor_ConvertComparisonToAssignment,
										// FIXME: reusing original operands so be careful when continuing to use original expression
										new BinaryOp(C4ScriptOperator.Assign, binaryOp.getLeftSide(), binaryOp.getRightSide())
								);
							}
						}
					}
					break;
				case NoInheritedFunction:
					if (offendingExpression instanceof CallFunc && ((CallFunc)offendingExpression).getDeclarationName().equals(Keywords.Inherited)) {
						replacements.add(
								String.format(Messages.ClonkQuickAssistProcessor_UseInsteadOf, Keywords.SafeInherited, Keywords.Inherited),
								identifierReplacement((AccessDeclaration) offendingExpression, Keywords.SafeInherited),
								false
						);
					}
					break;
				case ReturnAsFunction:
					if (offendingExpression instanceof Tuple) {
						Tuple tuple = (Tuple) offendingExpression;
						ExprElm[] elms = tuple.getElements();
						if (elms.length >= 2) {
							ExprElm returnExpr = elms[0];
							ExprElm[] rest = Utilities.arrayRange(elms, 1, elms.length-1, ExprElm.class);
							replacements.add(
									Messages.ClonkQuickAssistProcessor_RearrangeReturnStatement,
									new BunchOfStatements(
											ArrayHelpers.concat(SimpleStatement.wrapExpressions(rest), new ReturnStatement(returnExpr))
									)
							);
						}
					}
					break;
				case Unused:
					String varName = ParserErrorCode.getArg(marker, 0);
					if (offendingExpression instanceof VarDeclarationStatement) {
						VarDeclarationStatement s = (VarDeclarationStatement) offendingExpression;
						VarInitialization previous = null;
						final MutableRegion regionToDelete = new MutableRegion(0, expressionRegion.getLength());
						List<VarInitialization> initializations = s.getVarInitializations();
						String replacementString = "";
						for (int i = 0; i < initializations.size(); i++) {
							VarInitialization cur = initializations.get(i);
							VarInitialization next = i+1 < initializations.size() ? initializations.get(i+1) : null;
							if (cur.name.equals(varName)) {
								if (next == null) {
									if (previous != null) {
										// removing last initialization -> change ',' before it to ';'
										parser.seek(previous.getEnd());
										parser.eatWhitespace();
										if (parser.peek() == ',') {
											regionToDelete.setStartAndEnd(parser.getPosition(), cur.getEnd());
										}
										replacementString = "";
									} else {
										// already initialized with expressionRegion
									}
								} else {
									regionToDelete.setStartAndEnd(cur.getOffset(), next.getOffset());
								}
								break;
							}
							previous = cur;
						}
						final String finalReplacementString = replacementString;
						regionToDelete.incOffset(expressionRegion.getOffset());
						replacements.add(
							"Remove variable declaration",
							new ReplacementStatement(regionToDelete, finalReplacementString, document, expressionRegion.getOffset())
						).regionToBeReplacedSpecifiedByReplacementExpression = true;
					}
					break;
				}

				try {
					replacements.add(
						Messages.ClonkQuickAssistProcessor_TidyUp,
						topLevel.exhaustiveOptimize(parser)
					);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}

				for (final Replacement replacement : replacements) {
					String replacementAsString = "later"; //$NON-NLS-1$
					int offset = expressionRegion.getOffset();
					int length;
					if (replacement.regionToBeReplacedSpecifiedByReplacementExpression) {
						offset += replacement.getReplacementExpression().getExprStart();
						length = replacement.getReplacementExpression().getLength();
					} else {
						length = expressionRegion.getLength()+semicolonAdd;
					}
					proposals.add(new ParameterizedProposal(null, replacementAsString, offset, length,
							replacementAsString.length(), null, replacement.getTitle(), null, null, null, null,
							replacement, tabIndentation, parser, func));
				}
			}
		} finally {
			if (needToDisconnect != null) {
				ClonkCore.getDefault().getTextFileDocumentProvider().disconnect(needToDisconnect);
			}
		}

	}

	public String getErrorMessage() {
		return Messages.ClonkQuickAssistProcessor_FailedToFix;
	}

}
