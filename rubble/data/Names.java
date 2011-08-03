package rubble.data;


public final class Names {
    
    public static enum Tag { Argument, Global, Local }
    
    
    public static abstract class ResolvedName {
        
        public final String source;
        public final Tag tag;
        
        public ResolvedName(Tag tag, String source) {
            this.tag = tag;
            this.source = source;
        }
    }
    
    public static final class Argument extends ResolvedName {
        
        public final int index;
        
        public Argument(String source, int index) {
            super(Tag.Argument, source);
            this.index = index;
        }
        
        public String toString() {
            return "A " + source + " " + index;
        }
    }
    
    public static final class Global extends ResolvedName {
        
        public Global(String source) {
            super(Tag.Global, source);
        }
        
        public String toString() {
            return "G " + source;
        }
    }
    
    public static final class Local extends ResolvedName {
        
        public final int index;
        
        public Local(String source, int index) {
            super(Tag.Local, source);
            this.index = index;
        }
        
        public String toString() {
            return "L " + source + " " + index;
        }
    }
    
}
