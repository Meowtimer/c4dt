package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.lang.ref.WeakReference;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.C4ID;

public class ObjectType implements Serializable {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private C4ID id;
	private transient WeakReference<C4Object> object;
	
	public void setObject(C4Object object) {
		this.object = object != null ? new WeakReference<C4Object>(object) : null;
		this.id = object != null ? object.getId() : null;
	}
	
	public C4ID getId() {
		return id;
	}
	
	public C4Object getObject() {
		return object != null ? object.get() : null;
	}
	
	public void restoreType(C4ScriptBase context) {
		if (context.getResource() != null && context.getIndex() != null) {
			setObject(context.getIndex().getObjectNearestTo(context.getResource(), id));
		}
	}
}
