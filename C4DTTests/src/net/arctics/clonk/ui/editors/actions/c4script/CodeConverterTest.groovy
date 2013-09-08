package net.arctics.clonk.ui.editors.actions.c4script;

import static org.junit.Assert.*
import net.arctics.clonk.TestBase
import net.arctics.clonk.ast.ASTNode
import net.arctics.clonk.ast.Declaration
import net.arctics.clonk.c4script.ScriptParserTest.Setup
import net.arctics.clonk.c4script.ast.Tidy
import net.arctics.clonk.ui.editors.actions.c4script.CodeConverter.ICodeConverterContext

import org.eclipse.jface.text.IDocument
import org.junit.Test

public class CodeConverterTest extends TestBase {
	@Test
	public void testOldStyleToNewStyleConversion() {
		String source =
"""
Initialize:
	return 1;
"""
		def setup = new Setup(source)
		setup.parsers.each { it.run() }
		setup.index.refresh()
		setup.scripts.each { it.deriveInformation() }

		def converter = new CodeConverter() {
			@Override
			protected ASTNode performConversion(ASTNode node, Declaration owner, ICodeConverterContext context) {
				return new Tidy(null, 2).tidyExhaustive(node);
			}
		}

		converter.runOnDocument(setup.script, documentMock(source))
	}
}
