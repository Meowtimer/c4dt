package net.arctics.clonk.ini;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTSection;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.ini.IniData.IniSectionDefinition;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.IHasKeyAndValue;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.ReadOnlyIterator;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IRegion;

public class IniSection
	extends Declaration
	implements IHasKeyAndValue<String, String>, IHasChildren, Iterable<IniItem>, IniItem, IASTSection
{
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final Map<String, IniItem> map = new HashMap<>();
	private final List<IniItem> list = new LinkedList<>();
	private IniSectionDefinition definition;
	private int indentation;

	public IniSectionDefinition definition() { return definition; }
	public void setDefinition(IniSectionDefinition sectionData) { this.definition = sectionData; }
	public Map<String, IniItem> map() { return map; }
	public IniItem itemByKey(String key) { return map.get(key); }
	public List<IniItem> items() { return list; }
	@Override
	public String key() { return name(); }
	@Override
	public String stringValue() { return ""; } //$NON-NLS-1$
	@Override
	public Object[] children() { return items().toArray(new Object[items().size()]); }
	@Override
	public boolean hasChildren() { return !map.isEmpty(); }
	@Override
	public Collection<? extends IniItem> childCollection() { return list; }
	@Override
	public String nodeName() { return name(); }
	@Override
	public ITreeNode parentNode() { return null; }
	@Override
	public IPath path() { return ITreeNode.Default.path(this); }
	@Override
	public boolean subNodeOf(ITreeNode node) { return ITreeNode.Default.subNodeOf(this, node); }
	public int indentation() { return indentation; }
	public void setIndentation(int indentation) { this.indentation = indentation; }
	@Override
	public int sortCategory() { return 1; }
	public IniUnit iniUnit() { return parent(IniUnit.class); }
	@Override
	public boolean isTransient() { return false; }
	@Override
	public Object[] subDeclarationsForOutline() { return hasChildren() ? this.children() : null; }
	@Override
	public Iterator<IniItem> iterator() { return new ReadOnlyIterator<IniItem>(this.map.values().iterator()); }
	@Override
	public ASTNode[] subElements() { return list.toArray(new ASTNode[list.size()]); }
	@Override
	public int absoluteOffset() { return sectionOffset()+start; }
	@Override
	public IRegion selectionRegion() { return absolute(); }

	public IniSection(String name) { this.name = name; }

	public IniSection(SourceLocation location, String name) {
		this(name);
		setLocation(location);
	}

	@Override
	public void addChild(ITreeNode node) {
		if (node instanceof IniItem)
			addDeclaration((Declaration)node);
		else
			throw new IllegalArgumentException("node");
	}

	@Override
	public <T extends Declaration> T addDeclaration(T item) {
		final IniItem ini = (IniItem) item;
		map.put(ini.key(), ini);
		list.add(ini);
		item.setParent(this);
		return item;
	}

	public void removeItem(IniItem item) {
		map.remove(item.key());
		list.remove(item);
	}

	@Override
	public String toString() {
		final IniUnit unit = iniUnit();
		return unit != null ? unit.sectionToString(this) : name();
	}

	public void putEntry(IniEntry entry) {
		map.put(entry.name(), entry);
		list.add(entry);
		entry.setParent(this);
	}

	@Override
	public void doPrint(ASTNodePrinter writer, int indentation) {
		if (indentation >= 0)
			writer.append(StringUtil.multiply("\t", indentation+1));
		writer.append('[');
		writer.append(name());
		writer.append(']');
		writer.append('\n');

		for (final IniItem item : items()) {
			if (item.isTransient())
				continue;
			item.print(writer, indentation + 1);
			writer.append('\n');
		}
	}

	public boolean hasPersistentItems() {
		for (final IniItem item : items())
			if (!item.isTransient())
				return true;
		return false;
	}

	@Override
	public void validate(Markers markers) throws ProblemException {
		for (final IniItem e : this)
			e.validate(markers);
	}

	public Iterable<IniSection> sections() {
		// unable to make this work generically ;c
		final List<IniSection> sections = new LinkedList<IniSection>();
		for (final ITreeNode node : childCollection())
			if (node instanceof IniSection)
				sections.add((IniSection) node);
		return sections;
	}

	public IniSection parentSection() {
		return parentDeclaration() instanceof IniSection ? (IniSection) parentDeclaration()
				: null;
	}

	@Override
	public String infoText(IIndexEntity context) {
		final IniUnit unit = iniUnit();
		return String.format(Messages.IniSection_InfoTextFormat, unit.sectionToString(this), unit.infoText(context));
	}

	private static void setFromString(Field f, Object object, String val) throws NumberFormatException, IllegalArgumentException, IllegalAccessException {
		if (f.getType() == Integer.TYPE)
			f.set(object, Integer.valueOf(val));
		else if (f.getType() == Long.TYPE)
			f.set(object, Long.valueOf(val));
		else if (f.getType() == java.lang.Boolean.TYPE)
			f.set(object, java.lang.Boolean.valueOf(val));
	}

	public void commit(Object object, boolean takeIntoAccountCategory) {
		for (final IniItem item : map().values())
			if (item instanceof IniSection)
				((IniSection)item).commit(object, takeIntoAccountCategory);
			else if (item instanceof IniEntry) {
				final IniEntry entry = (IniEntry) item;
				Field f;
				try {
					f = object.getClass().getField(entry.name());
				} catch (final Exception e) {
					// don't panic - probably unknown field
					//e.printStackTrace();
					continue;
				}
				IniField annot;
				if (f != null && (annot = f.getAnnotation(IniField.class)) != null && (!takeIntoAccountCategory || IniData.category(annot, object.getClass()).equals(name()))) {
					Object val = entry.value();
					if (val instanceof IConvertibleToPrimitive)
						val = ((IConvertibleToPrimitive)val).convertToPrimitive();
					try {
						if (f.getType() != String.class && val instanceof String)
							setFromString(f, object, (String)val);
						else try {
							f.set(object, val);
						} catch (final IllegalArgumentException e) {
							if (val instanceof Long && f.getType() == Integer.TYPE) {
								f.set(object, (int)(long)(Long)val);
								continue;
							} else if (val instanceof Long && f.getType() == java.lang.Boolean.TYPE) {
								f.set(object, (Long)val != 0);
								continue;
							} else if (val instanceof java.lang.Boolean && f.getType() == Integer.TYPE) {
								f.set(object, ((java.lang.Boolean)val) ? 1 : 0);
								continue;
							}
							// unboxing failed
							try {
								final Constructor<?> ctor = f.getType().getConstructor(val.getClass());
								f.set(object, ctor.newInstance(val));
							} catch (final NoSuchMethodException nsm) {
								System.out.println(f.getName());
								nsm.printStackTrace();
							}
						}
					} catch (final Exception e) {
						e.printStackTrace();
						continue;
					}
				}
			}
	}
}