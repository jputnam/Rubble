package rubble.data;


sealed class SourceLocation
    ( val startRow   : Int
    , val startColumn: Int
    , val endRow     : Int
    , val endColumn  : Int
    ) {
    
    def this(row: Int, endColumn: Int, length: Int) = this(row, endColumn - length, row, endColumn)
    
    override def toString(): String = {
        return "(new SourceLocation(" + startRow + "," + startColumn +
            "," + endRow + "," + endColumn + "))"
    }
}
