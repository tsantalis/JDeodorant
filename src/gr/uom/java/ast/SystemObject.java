package gr.uom.java.ast;

import java.util.*;

public class SystemObject {

    private List<ClassObject> classList;
    //Map that has as key the classname and as value
    //the position of className in the classNameList
    private HashMap<String,Integer> classNameMap;

    public SystemObject() {
        this.classList = new ArrayList<ClassObject>();
        this.classNameMap = new HashMap<String,Integer>();
    }

    public void addClass(ClassObject c) {
        classNameMap.put(c.getName(),classList.size());
        classList.add(c);
    }
    
    public ClassObject getClassObject(String className) {
        Integer pos = classNameMap.get(className);
        if(pos != null)
            return getClassObject(pos);
        else
            return null;
    }

    public ClassObject getClassObject(int pos) {
        return classList.get(pos);
    }

    public ListIterator<ClassObject> getClassListIterator() {
        return classList.listIterator();
    }

    public int getClassNumber() {
        return classList.size();
    }

    public int getPositionInClassList(String className) {
        Integer pos = classNameMap.get(className);
        if(pos != null)
            return pos;
        else
            return -1;
    }

    public List<String> getClassNames() {
        List<String> names = new ArrayList<String>();

        for(int i=0; i<classList.size(); i++) {
            names.add(getClassObject(i).getName());
        }
        return names;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(ClassObject classObject : classList) {
            sb.append(classObject.toString());
            sb.append("\n--------------------------------------------------------------------------------\n");
        }
        return sb.toString();
    }
}