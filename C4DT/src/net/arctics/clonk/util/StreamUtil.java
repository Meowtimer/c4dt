package net.arctics.clonk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public class StreamUtil {
	public static String stringFromReader(Reader reader) {
		char[] buffer = new char[1024];
		int read;
		StringBuilder builder = new StringBuilder(1024);
		try {
			while ((read = reader.read(buffer)) > 0) {
				builder.append(buffer, 0, read);
			}
		} catch (IOException e) {
			return "";
		}
		return builder.toString();
	}
	
	public static String stringFromInputStream(InputStream stream, String encoding) throws IOException {
		InputStreamReader inputStreamReader = new InputStreamReader(stream, encoding);
		try {
			return stringFromReader(inputStreamReader);
		} finally {
			inputStreamReader.close();
		}
	}
	
	public static String stringFromInputStream(InputStream stream) {
		try {
			return stringFromInputStream(stream, "UTF-8"); //$NON-NLS-1$
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static String stringFromFile(IFile file) {
		InputStream stream;
		try {
			stream = file.getContents();
		} catch (CoreException e1) {
			e1.printStackTrace();
			return null;
		}
		try {
			return stringFromInputStream(stream);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String stringFromFile(File file) {
		InputStream stream;
		try {
			stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			return "";
		}
		try {
			return stringFromInputStream(stream);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String stringFromFileDocument(IFile file) {
		TextFileDocumentProvider provider = ClonkCore.getDefault().getTextFileDocumentProvider();
		try {
			provider.connect(file);
		} catch (CoreException e) {
			return "";
		}
		try {
			return provider.getDocument(file).get();
		} finally {
			provider.disconnect(file);
		}
	}
	public interface StreamWriteRunnable {
		void run(File file, OutputStream stream, OutputStreamWriter writer) throws IOException;
	}
	public static void writeToFile(File file, StreamWriteRunnable runnable) throws IOException {
		FileOutputStream s = new FileOutputStream(file);
		try {
			OutputStreamWriter writer = new OutputStreamWriter(s);
			try {
				runnable.run(file, s, writer);
			} finally {
				writer.close();
			}
		} finally {
			s.close();
		}
	}
	public static void transfer(InputStream source, OutputStream dest) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = source.read(buffer)) != -1) {
			dest.write(buffer, 0, read);
		}
	}
}
