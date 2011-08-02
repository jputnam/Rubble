package rubble.data;

public final class CompilerError extends Exception {
    
	public final static long serialVersionUID = 0;
	public final Location loc;
	public final String message;
	public final String phase;
	
	public CompilerError(Location loc, String phase, String message) {
		this.loc = loc;
		this.phase = phase;
		this.message = message;
	}
	
	public static CompilerError check(Location loc, String message) {
	    return new CompilerError(loc, "Well-formedness check", message);
	}
	
	public static CompilerError ice(Location loc, String message) {
		return new CompilerError(loc, "Internal compiler", message);
	}
	
	public static CompilerError lexical(Location loc, String message) {
		return new CompilerError(loc, "Lexical", message);
	}
	
    public static CompilerError parse(Location loc, String message) {
        return new CompilerError(loc, "Parse", message);
    }
    
    public static CompilerError type(Location loc, String message) {
        return new CompilerError(loc, "Type", message);
    }
	
	public String toString() {
		return phase + " error at " + loc.pretty() + ".  " + message;
	}
}
