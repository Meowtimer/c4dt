package net.arctics.clonk.ui.editors.c4script;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.Statement;

import static net.arctics.clonk.util.StreamUtil.ofType;

final class ReplacementsList extends LinkedList<Replacement> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public final ASTNode offending;
	public final List<ICompletionProposal> existingList;
	public ReplacementsList(final ASTNode offending, final List<ICompletionProposal> existingList) {
		super();
		this.offending = offending;
		this.existingList = existingList;
	}
	public Replacement add(final String replacement, ASTNode elm, final boolean alwaysStatement, final Boolean regionSpecified, final ASTNode... specifiable) {
		if (alwaysStatement && !(elm instanceof Statement))
			elm = new SimpleStatement(elm);
		if (elm.end() == elm.start() && offending != null)
			elm.setLocation(offending.start(), offending.end());
		final Replacement newOne = new Replacement(replacement, elm, specifiable);
		if (regionSpecified != null)
			newOne.regionToBeReplacedSpecifiedByReplacementExpression = regionSpecified;
		// don't add duplicates
		return this.stream()
			.filter(newOne::equals)
			.findFirst()
			.orElseGet(() -> ofType(existingList.stream(), ParameterizedProposal.class)
				.filter(prop -> prop.createdFrom(newOne))
				.map(ParameterizedProposal::replacement)
				.findFirst().orElseGet(() -> {
					this.add(newOne);
					return newOne;
				})
			);
	}
	public Replacement add(final String replacement, final ASTNode elm, final ASTNode... specifiable) {
		return add(replacement, elm, true, null, specifiable);
	}
}
