package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.ArrayUtil.concat;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IStorage;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.util.SelfcontainedStorage;

public class TempScript extends Script {

	private final String text;

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public TempScript(final String text, final Engine engine, final Index... referencedIndices) {
		super(new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Engine engine() { return engine; }
			@Override
			public List<Index> relevantIndexes() {
				return Arrays.asList(concat(this, referencedIndices));
			}
		});
		this.text = text;
	}

	@Override
	public IStorage source() { return new SelfcontainedStorage(text, text); }

}