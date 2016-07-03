package net.arctics.clonk.ui.editors.c4script;

import java.util.Arrays;
import java.util.stream.Collectors;

import net.arctics.clonk.Core;
import net.arctics.clonk.DefinitionInfo;
import net.arctics.clonk.TestBase;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.typing.dabble.DabbleInferenceTest.Setup;
import net.arctics.clonk.ui.editors.DeclarationProposal;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEventConsumer;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class ScriptCompletionProcessorTest extends TestBase {
	@Test
	public void testProposalsBasedOnRevisiting() {
		final String baseSource = StringUtil.join("\n",
			"local count;",
			"public func SetStackCount(int amount)",
			"{",
			"	count = amount;",
			"}",
			"public func TakeObject()",
			"{",
			"	if (count == 1)",
			"	{",
			"		Exit();",
			"		return this;",
			"	}",
			"	else if (count > 1)",
			"	{",
			"		var take = CreateObject(GetID(), 0, 0, GetOwner());",
			"		take->SetStackCount(1);",
			"		return take;",
			"	}",
			"}"
		);
		final String derivedSourcePart1 = StringUtil.join("\n",
			"#include Base",
			"func Test()",
			"{",
			"	return TakeObject()->"
		);
		final String derivedSourcePart2 = "}";
		final String derivedSource = derivedSourcePart1 + derivedSourcePart2;
		final Setup setup = new Setup(
			new DefinitionInfo(baseSource, "Base"),
			new DefinitionInfo(derivedSource, "Derived")
		);
		
		final IDocument derivedDocument = documentMock(derivedSource);
		final ITextViewer viewer = textViewerMock(derivedSourcePart1, derivedDocument);
		System.out.println(viewer.getSelectedRange().x);
		
		setup.parsers.forEach(Runnable::run);
		setup.index.refresh();
		setup.scripts.forEach(Script::deriveInformation);
		setup.performInference();
		
		final Script base = setup.scripts.get(0);
		final Script derived = setup.scripts.get(1);
		
		final ScriptEditingState state = new ScriptEditingState(Core.instance().getPreferenceStore());
		state.set(null, derived, derivedDocument);
		final ScriptCompletionProcessor processor = new ScriptCompletionProcessor(state);
		final ICompletionProposal[] props = processor.computeCompletionProposals(viewer, derivedSourcePart1.length());
		System.out.println(Arrays.stream(props).map(p -> p.toString()).collect(Collectors.joining("\n")));
		Assert.assertNotNull(props);
		Assert.assertTrue(props.length >= 3);
		Assert.assertEquals(derived.findLocalFunction("Test", false), ((DeclarationProposal)props[0]).declaration());
		Assert.assertEquals(base.findLocalFunction("SetStackCount", false), ((DeclarationProposal)props[1]).declaration());
		Assert.assertEquals(base.findLocalFunction("TakeObject", false), ((DeclarationProposal)props[2]).declaration());
	}

	public ITextViewer textViewerMock(final String derivedSourcePart1, final IDocument derivedDocument) {
		return new ITextViewer() {

			@Override
			public StyledText getTextWidget() {
				return null;
			}

			@Override
			public void setUndoManager(IUndoManager undoManager) {
			}

			@Override
			public void setTextDoubleClickStrategy(ITextDoubleClickStrategy strategy, String contentType) {
			}

			@Override
			public void setAutoIndentStrategy(IAutoIndentStrategy strategy, String contentType) {
			}

			@Override
			public void setTextHover(ITextHover textViewerHover, String contentType) {
			}

			@Override
			public void activatePlugins() {
			}

			@Override
			public void resetPlugins() {
			}

			@Override
			public void addViewportListener(IViewportListener listener) {
			}

			@Override
			public void removeViewportListener(IViewportListener listener) {
			}

			@Override
			public void addTextListener(ITextListener listener) {
			}

			@Override
			public void removeTextListener(ITextListener listener) {
			}

			@Override
			public void addTextInputListener(ITextInputListener listener) {
			}

			@Override
			public void removeTextInputListener(ITextInputListener listener) {
			}

			@Override
			public void setDocument(IDocument document) {
			}

			@Override
			public IDocument getDocument() {
				return derivedDocument;
			}

			@Override
			public void setEventConsumer(IEventConsumer consumer) {
			}

			@Override
			public void setEditable(boolean editable) {
			}

			@Override
			public boolean isEditable() {
				return false;
			}

			@Override
			public void setDocument(IDocument document, int modelRangeOffset, int modelRangeLength) {
			}

			@Override
			public void setVisibleRegion(int offset, int length) {
			}

			@Override
			public void resetVisibleRegion() {
			}

			@Override
			public IRegion getVisibleRegion() {
				return null;
			}

			@Override
			public boolean overlapsWithVisibleRegion(int offset, int length) {
				return false;
			}

			@Override
			public void changeTextPresentation(TextPresentation presentation, boolean controlRedraw) {
			}

			@Override
			public void invalidateTextPresentation() {
			}

			@Override
			public void setTextColor(Color color) {
			}

			@Override
			public void setTextColor(Color color, int offset, int length, boolean controlRedraw) {
			}

			@Override
			public ITextOperationTarget getTextOperationTarget() {
				return null;
			}

			@Override
			public IFindReplaceTarget getFindReplaceTarget() {
				return null;
			}

			@Override
			public void setDefaultPrefixes(String[] defaultPrefixes, String contentType) {
			}

			@Override
			public void setIndentPrefixes(String[] indentPrefixes, String contentType) {
			}

			@Override
			public void setSelectedRange(int offset, int length) {
			}

			@Override
			public Point getSelectedRange() {
				return new Point(derivedSourcePart1.length(), 0);
			}

			@Override
			public ISelectionProvider getSelectionProvider() {
				return null;
			}

			@Override
			public void revealRange(int offset, int length) {
			}

			@Override
			public void setTopIndex(int index) {
			}

			@Override
			public int getTopIndex() {
				return 0;
			}

			@Override
			public int getTopIndexStartOffset() {
				return 0;
			}

			@Override
			public int getBottomIndex() {
				return 0;
			}

			@Override
			public int getBottomIndexEndOffset() {
				return 0;
			}

			@Override
			public int getTopInset() {
				return 0;
			}
			
		};
	}
}
