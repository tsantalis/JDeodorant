package gr.uom.java.jdeodorant.refactoring.views;

import java.util.Set;
import java.util.Map.Entry;

import gr.uom.java.history.MethodSimilarityEvolution;
import gr.uom.java.history.ProjectVersion;
import gr.uom.java.history.ProjectVersionPair;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableColumn;

public class MethodSimilarityEvolutionDialog extends Dialog {
	private MethodSimilarityEvolution methodSimilarityEvolution;
	private TableViewer methodSimilarityTableViewer;
	private MethodSimilarityRow[] methodSimilarityRows;
	
	protected MethodSimilarityEvolutionDialog(IShellProvider parentShell, MethodSimilarityEvolution methodSimilarityEvolution) {
		super(parentShell);
		this.methodSimilarityEvolution = methodSimilarityEvolution;
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite)super.createDialogArea(parent);
		parent.getShell().setText("Method Similarity Evolution");
		
		methodSimilarityTableViewer = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		methodSimilarityTableViewer.setContentProvider(new MethodSimilarityViewContentProvider());
		methodSimilarityTableViewer.setLabelProvider(new MethodSimilarityViewLabelProvider());
		
		TableLayout layout = new TableLayout();
		
		TableColumn fromVersionColumn = new TableColumn(methodSimilarityTableViewer.getTable(), SWT.CENTER);
		fromVersionColumn.setText("version i");
		fromVersionColumn.setResizable(false);
		fromVersionColumn.pack();
		
		TableColumn toVersionColumn = new TableColumn(methodSimilarityTableViewer.getTable(), SWT.CENTER);
		toVersionColumn.setText("version j");
		toVersionColumn.setResizable(false);
		toVersionColumn.pack();
		
		TableColumn similarityColumn = new TableColumn(methodSimilarityTableViewer.getTable(), SWT.CENTER);
		similarityColumn.setText("Similarity");
		similarityColumn.setResizable(false);
		similarityColumn.pack();
		
		methodSimilarityTableViewer.getTable().setLayout(layout);
		methodSimilarityTableViewer.getTable().setLinesVisible(true);
		methodSimilarityTableViewer.getTable().setHeaderVisible(true);
		
		Set<Entry<ProjectVersionPair, String>> methodSimilarityEntries = methodSimilarityEvolution.getMethodSimilarityEntries();
		methodSimilarityRows = new MethodSimilarityRow[methodSimilarityEntries.size()];
		int counter = 0;
		for(Entry<ProjectVersionPair, String> methodSimilarityEntry : methodSimilarityEntries) {
			MethodSimilarityRow row = new MethodSimilarityRow(methodSimilarityEntry.getKey(), methodSimilarityEntry.getValue());
			methodSimilarityRows[counter] = row;
			counter++;
		}
		methodSimilarityTableViewer.setInput(methodSimilarityRows);
		
		methodSimilarityTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelectionProvider().getSelection();
				MethodSimilarityRow row = (MethodSimilarityRow)selection.getFirstElement();
				if(!row.getSimilarity().equals("1.000") && !row.getSimilarity().equals("N/A")) {
					ProjectVersion fromProjectVersion = row.getProjectVersionPair().getFromVersion();
					ProjectVersion toProjectVersion = row.getProjectVersionPair().getToVersion();
					CompareConfiguration compareConfiguration = new CompareConfiguration();
					compareConfiguration.setLeftLabel(fromProjectVersion.toString());
					compareConfiguration.setRightLabel(toProjectVersion.toString());
					CompareUI.openCompareDialog(new StringCompareEditorInput(compareConfiguration,
							methodSimilarityEvolution.getMethodBody(fromProjectVersion), methodSimilarityEvolution.getMethodBody(toProjectVersion))); 
				}
			}
		});
		return composite;
	}
	
	class MethodSimilarityViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(methodSimilarityRows != null) {
				return methodSimilarityRows;
			}
			else {
				return new MethodSimilarityRow[] {};
			}
		}
	}
	
	class MethodSimilarityViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			MethodSimilarityRow entry = (MethodSimilarityRow)obj;
			switch(index){
			case 0:
				return entry.getProjectVersionPair().getFromVersion().toString();
			case 1:
				return entry.getProjectVersionPair().getToVersion().toString();
			case 2:
				return entry.getSimilarity();
			default:
				return "";
			}
			
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
		public Image getImage(Object obj) {
			return null;
		}
	}
}
