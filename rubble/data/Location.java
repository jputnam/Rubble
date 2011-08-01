package rubble.data;

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
