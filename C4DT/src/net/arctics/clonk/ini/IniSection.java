package net.arctics.clonk.ini;

import static net.arctics.clonk.util.Utilities.as;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTSection;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4script.ProplistDeclaration;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.PropListExpression;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.ini.IniData.IniSectionDefinition;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.IHasKeyAndValue;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.ReadOnlyIterator;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.runtime.IPath;

public class IniSection
	extends Structure
	implements IHasKeyAndValue<String, String>, IHasChildren, Iterable<IniItem>, IniItem, IASTSection
{
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected final Map<String, IniItem> map = new HashMap<>();
	protected final List<IniItem> list = new LinkedList<>();
	private IniSectionDefinition definition;
	private int indentation;
	public void clear() {
		map.clear();
		list.clear();
	}
	public IniSectionDefinition definition() { return definition; }
	public void setDefinition(final IniSectionDefinition sectionData) { this.definition = sectionData; }
	public Map<String, IniItem> map() { return map; }
	public IniItem item(final String key) { return key == null ? null : map.get(key); }
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
	public boolean subNodeOf(final ITreeNode node) { return ITreeNode.Default.subNodeOf(this, node); }
	public int indentation() { return indentation; }
	public void setIndentation(final int indentation) { this.indentation = indentation; }
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
	public void setSubElements(ASTNode[] elms) {
		list.clear();
		Arrays.stream(elms)
			.filter(e -> e instanceof Declaration)
			.map(e -> (Declaration)e)
			.forEach(i -> this.addDeclaration(i));
	}
	@Override
	public int absoluteOffset() { return sectionOffset()+start; }

	public IniSection(final String name) { this.name = name; }

	public IniSection(final SourceLocation location, final String name) {
		this(name);
		setLocation(location);
	}

	@Override
	public void addChild(final ITreeNode node) {
		if (node instanceof IniItem)
			addDeclaration((Declaration)node);
		else
			throw new IllegalArgumentException("node");
	}

	@Override
	public <T extends Declaration> T addDeclaration(final T item) {
		final IniItem ini = (IniItem) item;
		map.put(ini.key(), ini);
		list.add(ini);
		item.setParent(this);
		return item;
	}

	public void removeItem(final IniItem item) {
		map.remove(item.key());
		list.remove(item);
	}

	@Override
	public String toString() {
		final IniUnit unit = iniUnit();
		return unit != null ? unit.sectionToString(this) : name();
	}

	public void putEntry(final IniEntry entry) {
		map.put(entry.name(), entry);
		list.add(entry);
		entry.setParent(this);
	}

	@Override
	public void doPrint(final ASTNodePrinter writer, final int indentation) {
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
	public void validate(final Markers markers) throws ProblemException {
		for (final IniItem e : this)
			e.validate(markers);
	}

	public IniSection parentSection() {
		return parentDeclaration() instanceof IniSection ? (IniSection) parentDeclaration() : null;
	}

	public Stream<IniSection> sections() {
		return list.stream().map(i -> as(i, IniSection.class)).filter(s -> s != null);
	}

	public IniSection sectionAtOffset(final int offset) {
		IniSection section = null;
		for (final IniSection sec : sections().toArray(l -> new IniSection[l])) {
			final int start = sec.start();
			if (start > offset)
				break;
			section = sec;
		}
		return section != null ? section.sectionAtOffset(offset) : this;
	}

	@Override
	public String infoText(final IIndexEntity context) {
		final IniUnit unit = iniUnit();
		return String.format(Messages.IniSection_InfoTextFormat, unit.sectionToString(this), unit.infoText(context));
	}

	private static void setFromString(final Field f, final Object object, final String val) throws NumberFormatException, IllegalArgumentException, IllegalAccessException {
		if (f.getType() == Integer.TYPE)
			f.set(object, Integer.valueOf(val));
		else if (f.getType() == Long.TYPE)
			f.set(object, Long.valueOf(val));
		else if (f.getType() == java.lang.Boolean.TYPE)
			f.set(object, java.lang.Boolean.valueOf(val));
	}

	public void commit(final Object object, final boolean takeIntoAccountCategory) {
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
	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		return as(map.get(declarationName), declarationClass);
	}

	@Override
	public ASTNode proplistValue() {
		return new PropListExpression(toProplist());
	}

	public ProplistDeclaration toProplist() {
		final IniUnit unit = this instanceof IniUnit ? (IniUnit)this : parent(IniUnit.class);
		Stream<IniItem> ls = list.stream();
		if (unit != null) {
			final String n = unit.nameEntryName(this);
			final IniItem nameEntry = n != null ? this.item(n) : null;
			if (nameEntry != null)
				ls = ls.filter(i -> i != nameEntry);
		}
		return new ProplistDeclaration(
			ls.map(i -> new Variable(proplistKey(unit, i), i.proplistValue())
		).collect(Collectors.toList()));
	}

	private String proplistKey(IniUnit unit, IniItem i) {
		final IniSection s = as(i, IniSection.class);
		final IniEntry nameEntry = s != null && unit != null ? as(s.item(unit.nameEntryName(s)), IniEntry.class) : null;
		return nameEntry != null ? nameEntry.value().toString() : i.key();
	}
}