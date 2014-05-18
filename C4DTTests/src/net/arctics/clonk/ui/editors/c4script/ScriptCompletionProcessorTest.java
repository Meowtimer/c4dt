package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.DefinitionInfo;
import net.arctics.clonk.TestBase;
import net.arctics.clonk.c4script.typing.dabble.DabbleInferenceTest.Setup;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.graphics.Point;
import org.junit.Assert;
import org.junit.Test;

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
			new DefinitionInfo(name: "Base", source: baseSource),
			new DefinitionInfo(name: "Derived", source: derivedSource)
		)
		
		final IDocument derivedDocument = documentMock(derivedSource)
		final ITextViewer viewer = [
			getDocument: {derivedDocument},
			getSelectedRange: { new Point(derivedSourcePart1.length(), 0) }
		] final as ITextViewer
		System.out.println(viewer.getSelectedRange().x)
		
		final setup.parsers.each { it.run() }
		setup.index.refresh()
		final setup.scripts.each { it.deriveInformation() }
		setup.inference.perform()
		
		def (base, derived) = setup.scripts
		
		final ScriptCompletionProcessor processor = new ScriptCompletionProcessor(derived)
		processor.computeCompletionProposals(viewer, derivedSourcePart1.length())
	}
}
