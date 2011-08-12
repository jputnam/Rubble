package rubble.data;

/**
 * The location of something in the source file.  The end location is recorded
 * at one row past its actual end so that automatically inserted semicolons
 * have zero width.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class Location {
	
	public final int startRow;
	public final int startColumn;
	public final int endRow;
	public final int endColumn;
	
	public Location(int sr, int sc, int er, int ec) {
		startRow = sr;
		startColumn = sc;
		endRow = er;
		endColumn = ec;
	}
	
	public Location(int row, int column) {
		startRow = row;
		startColumn = column;
		endRow = row;
		endColumn = column;
	}
	
	public Location(int row, int startColumn, int endColumn) {
		startRow = row;
		this.startColumn = startColumn;
		endRow = row;
		this.endColumn = endColumn;
	}
	
	public Location(Location start, Location end) {
	    this(start.startRow, start.startColumn, end.endRow, end.endColumn);
	}
	
	public Location atEnd() {
	    return new Location(endRow, endColumn - 1, endRow, endColumn);
	}
	
	public Location before() {
		return new Location(startRow, startColumn, startColumn);
	}
	
	public String pretty() {
		return "(" + startRow + "," + startColumn + ")-(" + endRow + "," + endColumn + ")";
	}
	
    public String toString() {
        return "@" + startRow + "," + startColumn +
            "," + endRow + "," + endColumn;
    }

}
