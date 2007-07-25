package gr.uom.java.ast;

public enum Access {
    NONE, PUBLIC, PRIVATE, PROTECTED;

    public String toString() {
        switch(this) {
            case NONE: return "";
            case PUBLIC: return "public";
            case PRIVATE: return "private";
            case PROTECTED: return "protected";
            default: return "";
        }
    }
}
