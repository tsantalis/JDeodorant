package gr.uom.java.jdeodorant.refactoring.manipulators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.UndoEdit;

public class UndoRefactoring {
	private Map<IDocument, List<UndoEdit>> undoEditMap;
	private Map<IFile, IDocument> documentMap;
	private List<IFile> newlyCreatedFiles;
	
	public UndoRefactoring() {
		this.undoEditMap = new LinkedHashMap<IDocument, List<UndoEdit>>();
		this.documentMap = new LinkedHashMap<IFile, IDocument>();
		this.newlyCreatedFiles = new ArrayList<IFile>();
	}

	public Set<IFile> getFileKeySet() {
		return documentMap.keySet();
	}

	public Set<IDocument> getDocumentKeySet() {
		return undoEditMap.keySet();
	}

	public List<IFile> getNewlyCreatedFiles() {
		return newlyCreatedFiles;
	}

	public void put(IFile file, IDocument document, UndoEdit undoEdit) {
		if(undoEditMap.containsKey(document)) {
			List<UndoEdit> undoEditList = undoEditMap.get(document);
			undoEditList.add(undoEdit);
		}
		else {
			List<UndoEdit> undoEditList = new ArrayList<UndoEdit>();
			undoEditList.add(undoEdit);
			undoEditMap.put(document, undoEditList);
		}
		if(!documentMap.containsKey(file)) {
			documentMap.put(file, document);
		}
	}

	public void merge(UndoRefactoring undoRefactoring) {
		Map<IDocument, List<UndoEdit>> undoEditMap = undoRefactoring.undoEditMap;
		Set<IDocument> documentKeySet = undoEditMap.keySet();
		for(IDocument key : documentKeySet) {
			if(this.undoEditMap.containsKey(key)) {
				List<UndoEdit> undoEditList = this.undoEditMap.get(key);
				undoEditList.addAll(undoEditMap.get(key));
			}
			else {
				this.undoEditMap.put(key, undoEditMap.get(key));
			}
		}
		Map<IFile, IDocument> documentMap = undoRefactoring.documentMap;
		Set<IFile> fileKeySet = documentMap.keySet();
		for(IFile key : fileKeySet) {
			if(!this.documentMap.containsKey(key)) {
				this.documentMap.put(key, documentMap.get(key));
			}
		}
		for(IFile file : undoRefactoring.getNewlyCreatedFiles()) {
			if(!newlyCreatedFiles.contains(file))
				newlyCreatedFiles.add(file);
		}
	}

	public void addNewlyCreatedFile(IFile file) {
		newlyCreatedFiles.add(file);
	}

	public void apply() {
		Set<IDocument> keySet = this.undoEditMap.keySet();
		for(IDocument key : keySet) {
			List<UndoEdit> undoEditList = undoEditMap.get(key);
			for(int i=undoEditList.size()-1; i>=0; i--) {
				UndoEdit undoEdit = undoEditList.get(i);
				try {
					undoEdit.apply(key);
				} catch (MalformedTreeException e) {
					e.printStackTrace();
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
