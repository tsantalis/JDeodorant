package gr.uom.java.jdeodorant.refactoring.views;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Image;

public class StringCompareEditorInput extends CompareEditorInput {
	private String input1;
	private String input2;
	
	public StringCompareEditorInput(CompareConfiguration configuration, String input1, String input2) {
		super(configuration);
		this.input1 = input1;
		this.input2 = input2;
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		Differencer d = new Differencer();
		Object diff = d.findDifferences(false, new NullProgressMonitor(), null, null, new Input(input1), new Input(input2));
		return diff; 
	}
	
	private class Input implements ITypedElement, IStreamContentAccessor {
		String fContent;
		public Input(String s) {
			fContent = s;
		}

		public String getName() {
			return "name";
		}

		public Image getImage() {
			return null;
		}

		public String getType() {
			return "txt";
		}

		public InputStream getContents() throws CoreException {
			return new ByteArrayInputStream(fContent.getBytes());
		}
	}
}
