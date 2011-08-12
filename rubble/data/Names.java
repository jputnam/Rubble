package rubble.data;

import rubble.data.Types.*;

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
        
        public final Tag tag;
        public final Mode mode;
        public final String source;
        public final Type<ResolvedName, Poly> type;
        
        public ResolvedName(Tag tag, Mode mode, String source, Type<ResolvedName, Poly> type) {
            this.tag = tag;
            this.mode = mode;
            this.source = source;
            this.type = type;
        }
    }
    
    public static final class Argument extends ResolvedName {
        
        public final int index;
        
        public Argument(Mode mode, String source, Type<ResolvedName, Poly> type, int index) {
            super(Tag.Argument, mode, source, type);
            this.index = index;
        }
        
        public String toString() {
            return "A " + source + " " + index;
        }
    }
    
    public static final class Global extends ResolvedName {
        
        public Global(Mode mode, String source, Type<ResolvedName, Poly> type) {
            super(Tag.Global, mode, source, type);
        }
        
        public String toString() {
            return "G " + source;
        }
    }
    
    public static final class Local extends ResolvedName {
        
        public final int index;
        
        public Local(Mode mode, String source, Type<ResolvedName, Poly> type, int index) {
            super(Tag.Local, mode, source, type);
            this.index = index;
        }
        
        public String toString() {
            return "L " + source + " " + index;
        }
    }
    
}
