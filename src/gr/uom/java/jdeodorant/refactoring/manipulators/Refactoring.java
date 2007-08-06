package gr.uom.java.jdeodorant.refactoring.manipulators;

public interface Refactoring {
	public void apply();
	public UndoRefactoring getUndoRefactoring();
}
