package net.arctics.clonk.builder;

import java.util.Map;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.ITransformer;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.Tidy;
import net.arctics.clonk.index.ProjectConversionConfiguration;
import net.arctics.clonk.index.ProjectConversionConfiguration.CodeTransformation;
import net.arctics.clonk.ui.editors.actions.c4script.CodeConverter;

public final class TransformationsBasedCodeConverter extends CodeConverter {
	private final ProjectConversionConfiguration configuration;
	public TransformationsBasedCodeConverter(ProjectConversionConfiguration configuration) {
		this.configuration = configuration;
	}
	@Override
	public ASTNode performConversion(final ASTNode expression, final Declaration declaration, final ICodeConverterContext context) {
		if (configuration == null)
			return expression;
		ASTNode node = (ASTNode)(new ITransformer() {
			@Override
			public Object transform(final ASTNode prev, final Object prevT, ASTNode expression) {
				if (expression == null)
					return null;
				if (expression instanceof IDLiteral || (expression instanceof AccessVar && (((AccessVar)expression).proxiedDefinition()) != null)) {
					final String mapped = configuration.idMap().get(expression.toString());
					if (mapped != null)
						return new AccessVar(mapped);
				}
				expression = expression.transformSubElements(this);
				for (final ProjectConversionConfiguration.CodeTransformation ct : configuration.transformations()) {
					boolean success = false;
					for (CodeTransformation c = ct; c != null; c = c.chain()) {
						final Map<String, Object> matched = c.template().match(expression);
						if (matched != null) {
							expression = c.transformation().transform(matched, context);
							success = true;
						}
					}
					if (success)
						break;
				}
				return expression;
			}
		}).transform(null, null, expression);
		if (node != null)
			try {
				node = new Tidy(declaration.topLevelStructure(), 2).tidyExhaustive(node);
			} catch (final CloneNotSupportedException e) {}
		return node;
	}
}