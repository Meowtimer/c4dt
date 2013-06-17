package net.arctics.clonk.index.serialization.replacements;

import net.arctics.clonk.index.IReplacedWhenSaved;

public interface IDeferredDeclaration extends IReplacedWhenSaved { Object resolve(); }