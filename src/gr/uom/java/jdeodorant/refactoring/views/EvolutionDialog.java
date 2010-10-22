package gr.uom.java.jdeodorant.refactoring.views;

import java.text.DecimalFormat;
import java.util.Set;
import java.util.Map.Entry;

import gr.uom.java.history.Evolution;
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

public class EvolutionDialog extends Dialog {
	private Evolution evolution;
	private String dialogTitle;
	private boolean displaySimilarity;
	private TableViewer tableViewer;
	private EvolutionRow[] evolutionRows;
	private final DecimalFormat decimalFormat = new DecimalFormat("0.000");
	
	protected EvolutionDialog(IShellProvider parentShell, Evolution evolution, String dialogTitle, boolean displaySimilarity) {
		super(parentShell);
		this.evolution = evolution;
		this.dialogTitle = dialogTitle;
		this.displaySimilarity = displaySimilarity;
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite)super.createDialogArea(parent);
		parent.getShell().setText(dialogTitle);
		
		tableViewer = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.setContentProvider(new MethodSimilarityViewContentProvider());
		tableViewer.setLabelProvider(new MethodSimilarityViewLabelProvider());
		
		TableLayout layout = new TableLayout();
		
		TableColumn fromVersionColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER);
		fromVersionColumn.setText("version i");
		fromVersionColumn.setResizable(false);
		fromVersionColumn.pack();
		
		TableColumn toVersionColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER);
		toVersionColumn.setText("version i+1");
		toVersionColumn.setResizable(false);
		toVersionColumn.pack();
		
		TableColumn percentageColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER);
		if(displaySimilarity)
			percentageColumn.setText("Similarity");
		else
			percentageColumn.setText("Change");
		percentageColumn.setResizable(false);
		percentageColumn.pack();
		
		tableViewer.getTable().setLayout(layout);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		
		Set<Entry<ProjectVersionPair, Double>> entries = null;
		if(displaySimilarity)
			entries = evolution.getSimilarityEntries();
		else
			entries = evolution.getChangeEntries();
		
		evolutionRows = new EvolutionRow[entries.size()];
		int counter = 0;
		for(Entry<ProjectVersionPair, Double> entry : entries) {
			String value = null;
			if(entry.getValue() != null)
				value = decimalFormat.format(entry.getValue());
			else
				value = "N/A";
			EvolutionRow row = new EvolutionRow(entry.getKey(), value);
			evolutionRows[counter] = row;
			counter++;
		}
		tableViewer.setInput(evolutionRows);
		
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelectionProvider().getSelection();
				EvolutionRow row = (EvolutionRow)selection.getFirstElement();
				if(!row.getPercentage().equals("N/A")) {
					ProjectVersion fromProjectVersion = row.getProjectVersionPair().getFromVersion();
					ProjectVersion toProjectVersion = row.getProjectVersionPair().getToVersion();
					CompareConfiguration compareConfiguration = new CompareConfiguration();
					compareConfiguration.setLeftLabel(fromProjectVersion.toString());
					compareConfiguration.setRightLabel(toProjectVersion.toString());
					CompareUI.openCompareDialog(new StringCompareEditorInput(compareConfiguration,
							evolution.getCode(fromProjectVersion), evolution.getCode(toProjectVersion))); 
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
			if(evolutionRows != null) {
				return evolutionRows;
			}
			else {
				return new EvolutionRow[] {};
			}
		}
	}
	
	class MethodSimilarityViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			EvolutionRow entry = (EvolutionRow)obj;
			switch(index){
			case 0:
				return entry.getProjectVersionPair().getFromVersion().toString();
			case 1:
				return entry.getProjectVersionPair().getToVersion().toString();
			case 2:
				return entry.getPercentage();
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
