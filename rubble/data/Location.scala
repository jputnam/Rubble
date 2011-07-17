package rubble.data;


sealed case class Location
    ( val startRow   : Int
    , val startColumn: Int
    , val endRow     : Int
    , val endColumn  : Int
    ) {
    
    def this(row: Int, endColumn: Int, length: Int) = this(row, endColumn - length, row, endColumn)
    
    def this(start: Location, end: Location) = this(start.startRow, start.startColumn, end.endRow, end.endColumn)
    
    def before(): Location = {
        return new Location(startRow, startColumn, startRow, startColumn)
    }
    
    override def toString(): String = {
        return "(new Location(" + startRow + "," + startColumn +
            "," + endRow + "," + endColumn + "))"
    }
}
