package gr.uom.java.jdeodorant.refactoring.views;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import gr.uom.java.ast.decomposition.cfg.BasicBlock;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.GraphNode;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.PDGObjectSliceUnion;
import gr.uom.java.ast.decomposition.cfg.PDGSliceUnion;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

public class SliceProfileDialog extends Dialog {

	private PDG pdg;
	private Map<PlainVariable, Set<PDGNode>> sliceProfileMap;
	private Map<PlainVariable, Boolean> enabledVariableMap;
	private Map<PlainVariable, Integer> columnWidthMap;
	private Map<Integer, PlainVariable> columnIndexMap;
	private SliceProfileRow[] sliceProfileRows;
	private TableViewer sliceProfileTableViewer;
	private CheckboxTableViewer enabledVariableTableViewer;
	private List<Integer> sliceProfileIntersectionIndices;
	private Text overlapText;
	private Text tightnessText;
	private Text coverageText;
	private final DecimalFormat decimalFormat = new DecimalFormat("0.000");

	protected SliceProfileDialog(IShellProvider parentShell, PDG pdg) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE  | SWT.MAX);
		this.pdg = pdg;
		this.sliceProfileMap = new LinkedHashMap<PlainVariable, Set<PDGNode>>();
		this.enabledVariableMap = new LinkedHashMap<PlainVariable, Boolean>();
		this.columnWidthMap = new LinkedHashMap<PlainVariable, Integer>();
		this.columnIndexMap = new LinkedHashMap<Integer, PlainVariable>();
		this.sliceProfileIntersectionIndices = new ArrayList<Integer>();
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite)super.createDialogArea(parent);
		parent.getShell().setText("Slice-based Cohesion Metrics");
	    composite.setLayout(new GridLayout(2, false));
	    
		sliceProfileTableViewer = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		sliceProfileTableViewer.setContentProvider(new SliceProfileViewContentProvider());
		sliceProfileTableViewer.setLabelProvider(new SliceProfileViewLabelProvider());
		
		Composite resultComposite = new Composite(composite, SWT.NONE);
		resultComposite.setLayout(new GridLayout(1, false));
		
		Group metricsGroup = new Group(resultComposite, SWT.SHADOW_ETCHED_IN);
		metricsGroup.setLayout(new GridLayout(2, false));
		metricsGroup.setText("Metrics");
		
		Group enableScopeVariablesGroup = new Group(resultComposite, SWT.SHADOW_ETCHED_IN);
		enableScopeVariablesGroup.setLayout(new GridLayout(1, false));
		Button enableButton = null;
		if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
			enableButton = new MultilineButton(enableScopeVariablesGroup, SWT.WRAP | SWT.CHECK);
		}
		else {
			enableButton = new Button(enableScopeVariablesGroup, SWT.WRAP | SWT.CHECK);
		}
		enableButton.setText("Enable only variables\nwith method scope");
		enableButton.addSelectionListener(new MySelectionListener(enableButton));
		
		enabledVariableTableViewer = CheckboxTableViewer.newCheckList(resultComposite, SWT.BORDER);
		enabledVariableTableViewer.setContentProvider(new EnabledVariableViewContentProvider());
		enabledVariableTableViewer.addCheckStateListener(new EnabledVariableCheckStateListener());
		
		Label overlapLabel = new Label(metricsGroup, SWT.NONE);
		overlapLabel.setText("Overlap:");
		overlapLabel.pack();
		this.overlapText = new Text(metricsGroup, SWT.NONE);
		overlapText.setEditable(false);
		
		Label tightnessLabel = new Label(metricsGroup, SWT.NONE);
		tightnessLabel.setText("Tightness:");
		tightnessLabel.pack();
		this.tightnessText = new Text(metricsGroup, SWT.NONE);
		tightnessText.setEditable(false);
		
		Label coverageLabel = new Label(metricsGroup, SWT.NONE);
		coverageLabel.setText("Coverage:");
		coverageLabel.pack();
		this.coverageText = new Text(metricsGroup, SWT.NONE);
		coverageText.setEditable(false);
		
		metricsGroup.pack();
		
		TableColumn statementIDColumn = new TableColumn(sliceProfileTableViewer.getTable(), SWT.CENTER);
		statementIDColumn.setText("id");
		statementIDColumn.setResizable(false);
		statementIDColumn.pack();
		
		int columnIndex = 1;
		for(PlainVariable plainVariable : pdg.getAllDeclaredVariables()) {
			PDGNode firstDefNode = pdg.getFirstDef(plainVariable);
			PDGNode lastUseNode = pdg.getLastUse(plainVariable);
			if(firstDefNode != null && lastUseNode != null) {
				BasicBlock boundaryBlock = pdg.getBasicBlocks().get(0);
				Map<CompositeVariable, LinkedHashSet<PDGNode>> definedAttributeNodeCriteriaMap = pdg.getDefinedAttributesOfReference(plainVariable);
				TreeSet<PDGNode> sliceProfile = new TreeSet<PDGNode>();
				if(definedAttributeNodeCriteriaMap.isEmpty()) {
					Set<PDGNode> nodeCriteria = pdg.getAssignmentNodesOfVariableCriterionIncludingDeclaration(plainVariable);
					if(!nodeCriteria.isEmpty()) {
						PDGSliceUnion sliceUnion = new PDGSliceUnion(pdg, boundaryBlock, nodeCriteria, plainVariable);
						sliceProfile.addAll(sliceUnion.getSliceNodes());
					}
				}
				else {
					Set<PDGNode> allNodeCriteria = new LinkedHashSet<PDGNode>();
					for(CompositeVariable compositeVariable : definedAttributeNodeCriteriaMap.keySet()) {
						Set<PDGNode> nodeCriteria = definedAttributeNodeCriteriaMap.get(compositeVariable);
						allNodeCriteria.addAll(nodeCriteria);
					}
					PDGObjectSliceUnion sliceUnion = new PDGObjectSliceUnion(pdg, boundaryBlock, allNodeCriteria, plainVariable);
					sliceProfile.addAll(sliceUnion.getSliceNodes());
				}
				sliceProfile.add(lastUseNode);
				sliceProfileMap.put(plainVariable, sliceProfile);
				columnIndexMap.put(columnIndex, plainVariable);
				enabledVariableMap.put(plainVariable, true);
				TableColumn column = new TableColumn(sliceProfileTableViewer.getTable(), SWT.CENTER);
				column.setText(plainVariable.getVariableName());
				column.setResizable(false);
				column.pack();
				columnWidthMap.put(plainVariable, column.getWidth());
				columnIndex++;
			}
		}
		GridData layoutData = new GridData(GridData.GRAB_VERTICAL | GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
		sliceProfileTableViewer.getTable().setLayoutData(layoutData);
		sliceProfileTableViewer.getTable().setLinesVisible(true);
		sliceProfileTableViewer.getTable().setHeaderVisible(true);
		
		sliceProfileRows = new SliceProfileRow[pdg.getTotalNumberOfStatements()];
		
		Iterator<GraphNode> nodeIterator = pdg.getNodeIterator();
		while(nodeIterator.hasNext()) {
			PDGNode node = (PDGNode)nodeIterator.next();
			SliceProfileRow row = new SliceProfileRow(node.getId());
			for(PlainVariable variable : sliceProfileMap.keySet()) {
				Set<PDGNode> slice = sliceProfileMap.get(variable);
				if(slice.contains(node))
					row.put(variable, true);
				else
					row.put(variable, false);
			}
			sliceProfileRows[node.getId() - 1] = row;
		}
		sliceProfileTableViewer.setInput(sliceProfileRows);
		enabledVariableTableViewer.setInput(sliceProfileMap.keySet().toArray());
		enabledVariableTableViewer.setAllChecked(true);
		sliceProfileIntersectionIndices = computeSliceProfileIntersectionStatements(sliceProfileMap.keySet());
		sliceProfileTableViewer.refresh();
		overlapText.setText(decimalFormat.format(overlap(sliceProfileMap.keySet())));
		tightnessText.setText(decimalFormat.format(tightness()));
		coverageText.setText(decimalFormat.format(coverage(sliceProfileMap.keySet())));
		return composite;
	}

	class SliceProfileViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(sliceProfileRows != null) {
				return sliceProfileRows;
			}
			else {
				return new SliceProfileRow[] {};
			}
		}
	}

	class SliceProfileViewLabelProvider extends StyledCellLabelProvider {
		private Color highlightColor = Display.getCurrent().getSystemColor(SWT.COLOR_GREEN);
		
		public void update(ViewerCell cell) {
			SliceProfileRow element = (SliceProfileRow)cell.getElement();
			int index = cell.getColumnIndex();
			String columnText = getColumnText(element, index);
			cell.setText(columnText);
			cell.setImage(getColumnImage(element, index));
			if(sliceProfileIntersectionIndices.contains(element.getStatementID()-1)) {
				cell.setBackground(highlightColor);
			}
			else
				cell.setBackground(null);
			super.update(cell);
		}
		private String getColumnText(Object obj, int index) {
			SliceProfileRow entry = (SliceProfileRow)obj;
			int statementID = entry.getStatementID();
			switch(index) {
			case 0:
				return String.valueOf(statementID);
			default:
				PlainVariable variable = columnIndexMap.get(index);
				if(entry.getValue(variable))
					return "|";
				else
					return "";
			}
		}
		private Image getColumnImage(Object obj, int index) {
			return null;
		}
		public Image getImage(Object obj) {
			return null;
		}
	}

	class EnabledVariableViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			return sliceProfileMap.keySet().toArray();
		}
	}

	class MySelectionListener extends SelectionAdapter {
		private Button button;
		public MySelectionListener(Button button) {
			this.button = button;
		}
		public void widgetSelected(SelectionEvent e) {
			Set<PlainVariable> variablesWithMethodScope = pdg.getVariablesWithMethodBodyScope();
			if(button.getSelection()) {
				for(PlainVariable variable : enabledVariableMap.keySet()) {
					if(!variablesWithMethodScope.contains(variable)) {
						enabledVariableMap.put(variable, false);
						int index = getColumnIndex(variable);
						sliceProfileTableViewer.getTable().getColumn(index).setWidth(0);
						enabledVariableTableViewer.setChecked(variable, false);
					}
					else {
						enabledVariableMap.put(variable, true);
						int index = getColumnIndex(variable);
						sliceProfileTableViewer.getTable().getColumn(index).setWidth(columnWidthMap.get(variable));
						enabledVariableTableViewer.setChecked(variable, true);
					}
				}
			}
			else {
				for(PlainVariable variable : enabledVariableMap.keySet()) {
					enabledVariableMap.put(variable, true);
					int index = getColumnIndex(variable);
					sliceProfileTableViewer.getTable().getColumn(index).setWidth(columnWidthMap.get(variable));
					enabledVariableTableViewer.setChecked(variable, true);
				}
			}
			updateMetrics();
		}
	}

	class EnabledVariableCheckStateListener implements ICheckStateListener {
		public void checkStateChanged(CheckStateChangedEvent event) {
			PlainVariable element = (PlainVariable)event.getElement();
			enabledVariableMap.put(element, event.getChecked());
			int index = getColumnIndex(element);
			if(event.getChecked())
				sliceProfileTableViewer.getTable().getColumn(index).setWidth(columnWidthMap.get(element));
			else
				sliceProfileTableViewer.getTable().getColumn(index).setWidth(0);
			updateMetrics();
		}
	}

	private List<Integer> computeSliceProfileIntersectionStatements(Set<PlainVariable> enabledVariables) {
		List<Integer> indicesList = new ArrayList<Integer>();
		for(SliceProfileRow row : sliceProfileRows) {
			if(row.statementBelongsToAllSlices(enabledVariables))
				indicesList.add(row.getStatementID()-1);
		}
		return indicesList;
	}

	private double tightness() {
		int SLint = sliceProfileIntersectionIndices.size();
		return (double)SLint/(double)sliceProfileRows.length;
	}

	private double coverage(Set<PlainVariable> enabledVariables) {
		if(enabledVariables.size() == 0)
			return 0;
		double sliceRatioSum = 0;
		for(PlainVariable variable : enabledVariables) {
			Set<PDGNode> slice = sliceProfileMap.get(variable);
			int sliceSize = slice.size();
			sliceRatioSum += (double)sliceSize/(double)sliceProfileRows.length;
		}
		return sliceRatioSum/(double)enabledVariables.size();
	}

	private double overlap(Set<PlainVariable> enabledVariables) {
		if(enabledVariables.size() == 0)
			return 0;
		int SLint = sliceProfileIntersectionIndices.size();
		double sliceRatioSum = 0;
		for(PlainVariable variable : enabledVariables) {
			Set<PDGNode> slice = sliceProfileMap.get(variable);
			int sliceSize = slice.size();
			sliceRatioSum += (double)SLint/(double)sliceSize;
		}
		return sliceRatioSum/(double)enabledVariables.size();
	}

	private int getColumnIndex(PlainVariable variable) {
		int index = 0;
		for(Integer key : columnIndexMap.keySet()) {
			if(columnIndexMap.get(key).equals(variable)) {
				index = key;
				break;
			}
		}
		return index;
	}

	private void updateMetrics() {
		Set<PlainVariable> enabledVariables = new LinkedHashSet<PlainVariable>();
		for(PlainVariable variable : enabledVariableMap.keySet()) {
			if(enabledVariableMap.get(variable) == true)
				enabledVariables.add(variable);
		}
		sliceProfileIntersectionIndices = computeSliceProfileIntersectionStatements(enabledVariables);
		sliceProfileTableViewer.refresh();
		overlapText.setText(decimalFormat.format(overlap(enabledVariables)));
		tightnessText.setText(decimalFormat.format(tightness()));
		coverageText.setText(decimalFormat.format(coverage(enabledVariables)));
	}
}
