package rubble.data;


sealed class Location
    ( val startRow   : Int
    , val startColumn: Int
    , val endRow     : Int
    , val endColumn  : Int
    ) {
    
    def this(row: Int, endColumn: Int, length: Int) = this(row, endColumn - length, row, endColumn)
    
    override def toString(): String = {
        return "(new Location(" + startRow + "," + startColumn +
            "," + endRow + "," + endColumn + "))"
    }
}
