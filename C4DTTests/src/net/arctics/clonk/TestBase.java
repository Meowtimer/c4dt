package net.arctics.clonk;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.junit.Before;

public abstract class TestBase {
	public static final String ENGINE = "OpenClonk";
	@Before
	public void headlessSetup() {
		Core.headlessInitialize(System.getenv("HOME")+"/Projects/Clonk/C4DT/C4DT/res/engines", ENGINE);
		Flags.DEBUG = true;
	}
	protected IDocument documentMock(final String source) {
		return new IDocument() {
			@Override
			public char getChar(int offset) throws BadLocationException {
				return source.charAt(offset);
			}

			@Override
			public int getLength() {
				return source.length();
			}

			@Override
			public String get() {
				return source;
			}

			@Override
			public String get(int offset, int length) throws BadLocationException {
				return source.substring(offset, offset+length);
			}

			@Override
			public void set(String text) {
			}

			@Override
			public void replace(int offset, int length, String text) throws BadLocationException {
			}

			@Override
			public void addDocumentListener(IDocumentListener listener) {
			}

			@Override
			public void removeDocumentListener(IDocumentListener listener) {
			}

			@Override
			public void addPrenotifiedDocumentListener(IDocumentListener documentAdapter) {
			}

			@Override
			public void removePrenotifiedDocumentListener(IDocumentListener documentAdapter) {
			}

			@Override
			public void addPositionCategory(String category) {
			}

			@Override
			public void removePositionCategory(String category) throws BadPositionCategoryException {
			}

			@Override
			public String[] getPositionCategories() {
				return null;
			}

			@Override
			public boolean containsPositionCategory(String category) {
				return false;
			}

			@Override
			public void addPosition(Position position) throws BadLocationException {
			}

			@Override
			public void removePosition(Position position) {
			}

			@Override
			public void addPosition(String category, Position position) throws BadLocationException, BadPositionCategoryException {
			}

			@Override
			public void removePosition(String category, Position position) throws BadPositionCategoryException {
			}

			@Override
			public Position[] getPositions(String category) throws BadPositionCategoryException {
				return null;
			}

			@Override
			public boolean containsPosition(String category, int offset, int length) {
				return false;
			}

			@Override
			public int computeIndexInCategory(String category, int offset) throws BadLocationException, BadPositionCategoryException {
				return 0;
			}

			@Override
			public void addPositionUpdater(IPositionUpdater updater) {
			}

			@Override
			public void removePositionUpdater(IPositionUpdater updater) {
			}

			@Override
			public void insertPositionUpdater(IPositionUpdater updater, int index) {
			}

			@Override
			public IPositionUpdater[] getPositionUpdaters() {
				return null;
			}

			@Override
			public String[] getLegalContentTypes() {
				return null;
			}

			@Override
			public String getContentType(int offset) throws BadLocationException {
				return null;
			}

			@Override
			public ITypedRegion getPartition(int offset) throws BadLocationException {
				return null;
			}

			@Override
			public ITypedRegion[] computePartitioning(int offset, int length) throws BadLocationException {
				return null;
			}

			@Override
			public void addDocumentPartitioningListener(IDocumentPartitioningListener listener) {
			}

			@Override
			public void removeDocumentPartitioningListener(IDocumentPartitioningListener listener) {
			}

			@Override
			public void setDocumentPartitioner(IDocumentPartitioner partitioner) {
			}

			@Override
			public IDocumentPartitioner getDocumentPartitioner() {
				return null;
			}

			@Override
			public int getLineLength(int line) throws BadLocationException {
				return 0;
			}

			@Override
			public int getLineOfOffset(int offset) throws BadLocationException {
				return 0;
			}

			@Override
			public int getLineOffset(int line) throws BadLocationException {
				return 0;
			}

			@Override
			public IRegion getLineInformation(int line) throws BadLocationException {
				return null;
			}

			@Override
			public IRegion getLineInformationOfOffset(int offset) throws BadLocationException {
				return null;
			}

			@Override
			public int getNumberOfLines() {
				return 0;
			}

			@Override
			public int getNumberOfLines(int offset, int length) throws BadLocationException {
				return 0;
			}

			@Override
			public int computeNumberOfLines(String text) {
				return 0;
			}

			@Override
			public String[] getLegalLineDelimiters() {
				return null;
			}

			@Override
			public String getLineDelimiter(int line) throws BadLocationException {
				return null;
			}

			@Override
			public int search(int startOffset, String findString, boolean forwardSearch, boolean caseSensitive, boolean wholeWord) throws BadLocationException {
				return 0;
			}
		};
	}
}
