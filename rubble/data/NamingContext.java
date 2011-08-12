package rubble.data;

import java.util.Hashtable;

import rubble.data.Names.*;

/**
 * The naming contexts used to perform name resolution.  The context is also
 * used to initialize type checking.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class NamingContext {
    
    private final static class Locals {
        
        public final static Locals NIL = new Locals(null, 0);
        
        private final Hashtable<String, Local> locals;
        private int level;
        public final Locals next;
        
        private Locals(Locals next, int level) {
            this.locals = new Hashtable<String, Local>();
            this.level = level;
            this.next = next;
        }
        
        public Local find(String name) {
            Local l = locals.get(name);
            if (l != null) { return l; }
            if (next != null) { return next.find(name); }
            return null;
        }
        
        public Locals nestScope() {
            return new Locals(this, level);
        }
        
        public void observe(Location loc, String name, Mode mode) throws CompilerError {
            if (locals.containsKey(name)) {
                throw CompilerError.check(loc, "The name " + name + " is already defined in this scope.");
            }
            locals.put(name, new Local(name, level, mode));
            level++;
        }
    }
    
    
    private final Hashtable<String, Global> globals;
    private Hashtable<String, Argument> arguments;
    private int argumentLevel;
    private Locals locals;
    
    public NamingContext() {
        globals = new Hashtable<String, Global>();
        arguments = new Hashtable<String, Argument>();
        argumentLevel = 0;
        locals = Locals.NIL.nestScope();
    }
    
    public void discardNonGlobals() {
        arguments = new Hashtable<String, Argument>();
        argumentLevel = 0;
        locals = Locals.NIL.nestScope();
    }
    
    public void newScope() {
        locals.nestScope();
    }
    
    public void observeArgument(Location loc, Mode mode, String name) throws CompilerError {
        if (arguments.containsKey(name)) {
            throw CompilerError.check(loc, "The name " + name + " is already defined in this scope.");
        }
        arguments.put(name, new Argument(name, argumentLevel, mode));
        argumentLevel++;
    }
    
    public void observeGlobal(Location loc, Mode mode, String name) throws CompilerError {
        if (globals.contains(name)) {
            throw CompilerError.check(loc, "The global name " + name + " has already been defined.");
        }
        globals.put(name, new Global(name, mode));
    }
    
    public void observeLocal(Location loc, Mode mode, String name) throws CompilerError {
        locals.observe(loc, name, mode);
    }
    
    public ResolvedName resolve(Location loc, String source) throws CompilerError {
        ResolvedName name = locals.find(source);
        if (name != null) { return name; }
        
        name = arguments.get(source);
        if (name != null) { return name; }
        
        name = globals.get(source);
        if (name != null) { return name; }
        
        throw CompilerError.check(loc, "The variable " + source + " is not in scope.");
    }
    
}
