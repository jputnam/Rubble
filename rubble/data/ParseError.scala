package rubble.data


class ParseError(val loc: Location, val payload: String) extends Exception {
    
    def this(row: Int, startColumn: Int, length: Int, payload: String) =
        this(new Location(row, startColumn-length, length), payload)
    
    def this(p: Point, payload: String) = this(new Location(p, p), payload)
}
