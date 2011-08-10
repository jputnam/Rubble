package rubble.data;

import java.util.HashSet;
import java.util.Hashtable;

import rubble.data.Names.*;


public final class NamingContext {
    
    private final static class Locals {
        
        public final static Locals NIL = new Locals(null, 0);
        
        private final Hashtable<String, Integer> locals;
        private int level;
        public final Locals next;
        
        private Locals(Locals next, int level) {
            this.locals = new Hashtable<String, Integer>();
            this.level = level;
            this.next = next;
        }
        
        public Integer find(String name) {
            Integer i = locals.get(name);
            if (i != null) { return i; }
            if (next != null) { return next.find(name); }
            return null;
        }
        
        public Locals nestScope() {
            return new Locals(this, level);
        }
        
        public void observe(Location loc, String name) throws CompilerError {
            if (locals.containsKey(name)) {
                throw CompilerError.check(loc, "The name " + name + " is already defined in this scope.");
            }
            locals.put(name, level);
            level++;
        }
    }
    
    
    private final HashSet<String> globals;
    private Hashtable<String, Integer> arguments;
    private int argumentLevel;
    private Locals locals;
    
    public NamingContext() {
        globals = new HashSet<String>();
        arguments = new Hashtable<String, Integer>();
        argumentLevel = 0;
        locals = Locals.NIL;
    }
    
    public void discardNonGlobals() {
        arguments = new Hashtable<String, Integer>();
        argumentLevel = 0;
        locals = Locals.NIL;
    }
    
    public void newScope() {
        locals.nestScope();
    }
    
    public void observeArgument(Location loc, String name) throws CompilerError {
        if (arguments.containsKey(name)) {
            throw CompilerError.check(loc, "The name " + name + " is already defined in this scope.");
        }
        arguments.put(name, argumentLevel);
        argumentLevel++;
    }
    
    public void observeGlobal(Location loc, String name) throws CompilerError {
        if (globals.contains(name)) {
            throw CompilerError.check(loc, "The global name " + name + " has already been defined.");
        }
        globals.add(name);
    }
    
    public void observeLocal(Location loc, String name) throws CompilerError {
        locals.observe(loc, name);
    }
    
    public ResolvedName resolve(Location loc, String source) throws CompilerError {
        Integer index = locals.find(source);
        if (index != null) return new Names.Local(source, index);
        
        index = arguments.get(source);
        if (index != null) return new Names.Argument(source, index);
        
        if (globals.contains(source)) return new Names.Global(source);
        
        throw CompilerError.check(loc, "The variable " + source + " is not in scope.");
    }
    
}
