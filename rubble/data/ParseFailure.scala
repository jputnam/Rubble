package rubble.data


class ParseFailure(val loc: SourceLocation, val payload: String) extends Exception {
    
    def this(row: Int, startColumn: Int, length: Int, payload: String) =
        this(new SourceLocation(row, startColumn-length, length), payload)

}
