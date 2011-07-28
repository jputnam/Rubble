package rubble.data


sealed case class Location
    ( val start: Point
    , val end: Point
    ) {
    
    def this(startRow: Int, startColumn: Int, endRow: Int, endColumn: Int) = this(Point(startRow, startColumn), Point(endRow, endColumn))
    
    def this(row: Int, endColumn: Int, length: Int) = this(Point(row, endColumn - length), Point(row, endColumn))
    
    def this(p: Point) = this(p, p)
    
    def before(): Location = {
        return new Location(start, start)
    }
    
    override def toString(): String = {
        return "(new Location(" + start.row + "," + start.column +
            "," + end.row + "," + end.column + "))"
    }
}
