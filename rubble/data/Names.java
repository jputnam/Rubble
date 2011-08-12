package rubble.data;

/**
 * Name resolution.  Resolved names indicate the variable mode; its type in
 * encoded in the term in which it appears.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class Names {
    
    public static enum Tag { Argument, Global, Local }
    
    public static abstract class ResolvedName {
        
        public final String source;
        public final Tag tag;
        public final Mode mode;
        
        public ResolvedName(Tag tag, String source, Mode mode) {
            this.tag = tag;
            this.source = source;
            this.mode = mode;
        }
    }
    
    public static final class Argument extends ResolvedName {
        
        public final int index;
        
        public Argument(String source, int index, Mode mode) {
            super(Tag.Argument, source, mode);
            this.index = index;
        }
        
        public String toString() {
            return "A " + source + " " + index;
        }
    }
    
    public static final class Global extends ResolvedName {
        
        public Global(String source, Mode mode) {
            super(Tag.Global, source, mode);
        }
        
        public String toString() {
            return "G " + source;
        }
    }
    
    public static final class Local extends ResolvedName {
        
        public final int index;
        
        public Local(String source, int index, Mode mode) {
            super(Tag.Local, source, mode);
            this.index = index;
        }
        
        public String toString() {
            return "L " + source + " " + index;
        }
    }
    
}
