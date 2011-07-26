package rubble.parser

import rubble.data.AST
import rubble.data.Location
import rubble.data.ParseError
import rubble.data.Tokens
import rubble.data.Tokens.Bracket._
import rubble.data.Types

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer


sealed case class Parser(
        val outerLoc: Location,
        val tokens: ArrayBuffer[Tokens.Token]) {
    
    var index: Int = 0
    
    
    def errorUnexpected[A](loc: Location, expected: String, message: String): A = {
        throw new ParseError(loc, "The compiler needed " + expected + " but " + message + ".")
    }
    
    
    def isLive: Boolean = index < tokens.length
    
    
    def lookahead: Option[Tokens.Token] = if (isLive) Some(tokens(index)) else None
    
    
    def nextTokenExpecting(name: String): Tokens.Token = {
        if (!isLive) {
            errorUnexpected(new Location(outerLoc.end), name, "ran out of input")
        }
        index += 1
        return tokens(index - 1)
    }
    
    /*
    def parse(tokens: ArrayBuffer[Tokens.Token]): Unit = {
        // FIXME: Implement this.
        // validate that the token stream is not empty
        // construct a parse context
        return
    }
    */
    
    // FIXME: Move ParseContext back in.
    
    sealed abstract class PrattParser[T] {
        
        val expected: String
        
        
        def errorUnexpectedToken[A](loc: Location, message: String): A =
            errorUnexpected(loc, expected, "found " + message)
        
        
        protected def inBraces(): Parser =
            nextToken match {
                case Tokens.Block(loc, _, bracket, subs) if (bracket == Brace || bracket == ImplicitBrace) => {
                    return Parser(loc, subs)
                }
                case t => errorUnexpected(t.loc, "a statement block", "found " + t.actual)
            }
        
        
        protected def inParens: Parser =
            nextToken match {
                case Tokens.Block(loc, _, Paren, ts) => return Parser(loc, ts)
                case t => errorUnexpectedToken(t.loc, t.actual)
            }
        
        
        protected def leftDenotation(token: Tokens.Token): Option[(Int, T => T)]
        
        
        protected def nextToken: Tokens.Token = nextTokenExpecting(expected)
        
        
        protected def nullDenotation(token: Tokens.Token): T
        
        
        def parse(rbp: Int): T = {
            val token = nextToken
            var ast = nullDenotation(token)
            
            while (isLive) {
                leftDenotation(tokens(index)) match {
                    case Some((lbp, f)) if rbp < lbp => {
                        index += 1
                        ast = f(ast)
                    }
                    case _ => return ast
                }
            }
            return ast
        }
        
        
        def parseFull: T = {
            val result = parse(0)
            if (isLive) {
                val t = tokens(index)
                errorUnexpectedToken(t.loc, t.actual)
            }
            return result
        }
        
        
        def parseList(separator: String): ArrayBuffer[T] = {
            if (tokens.length == 0) { return ArrayBuffer.empty[T] }
            val result = ArrayBuffer.empty[T]
            while (true) {
                result += parse(0)
                if (isLive) {
                    nextToken match {
                        case t if t.actual == separator => Unit
                        case t => errorUnexpected(t.loc, separator, t.actual)
                    }
                } else {
                    return result
                }
            }
            throw new ParseError(outerLoc.end, "ICE: parseList() should never fall off the end of its loop.")
        }
        
        
        def parseListFull(separator: String): ArrayBuffer[T] = {
            val result = parseList(separator)
            if (isLive) {
                val t = tokens(index)
                errorUnexpectedToken(t.loc, t.actual)
            }
            return result
        }
        
        
        protected def requireReserved(name: String) = {
            nextToken match {
                case Tokens.Reserved(_, s) if s == name => Unit
                case t => errorUnexpectedToken(t.loc, t.actual)
            }
        }
    }
    
    
    object Expression extends PrattParser[AST.Expression[Unit]]{
        
        val expected = "an expression"
        
        
        private def infixExpression(prec: Int, left: AST.Expression[Unit], center: AST.Expression[Unit]): AST.Expression[Unit] = {
            val right = parse(prec)
            val (op, args) = center match {
                case AST.Apply(_, _, _op, AST.Tuple(eloc, _, extras)) => (_op, extras += left += right)
                case AST.Apply(_, _, _op, extra) => (_op, ArrayBuffer(extra, left, right))
                case _ => (center, ArrayBuffer(left, right))
            }
            return AST.Apply(left.loc, Unit, op, AST.Tuple(left.loc, Unit, args))
        }
        
        
        def leftDenotation(token: Tokens.Token): Option[(Int, AST.Expression[Unit] => AST.Expression[Unit])] = token match {
            case Tokens.Block(loc, actual, bracket, subtokens) => bracket match {
                case BackTick      => Some((50,  (ast) => { infixExpression(50, ast, Parser(loc, subtokens).Expression.parseFull) }))
                case Paren         => Some((120, (ast) => { AST.Apply(loc, Unit, ast, parseTuple(loc, subtokens)) }))
                case Square        => Some((120, (ast) => { AST.Index(loc, Unit, ast, Parser(loc, subtokens).Expression.parseListFull(",")) }))
                case Brace         => errorUnexpectedToken(loc, "ICE: The compiler doesn't know how to parse types.") // FIXME: Some(12, (ast) => { AST.HasType(loc, Unit, ast, null) })
                case ImplicitBrace => errorUnexpectedToken(loc, "ICE: The compiler doesn't know how to parse types.") // FIXME: Some(12, (ast) => { AST.HasType(loc, Unit, ast, null) })
            }
            // FIXME: Fill in the operators.
            case Tokens.Operator(loc, actual) => errorUnexpectedToken(loc, "an unrecognized operator")
            case _ => None
        }
        
        
        def nullDenotation(token: Tokens.Token): AST.Expression[Unit] = {
            return token match {
                case Tokens.Block(loc, actual, bracket, subtokens) => bracket match {
                    case Paren    => parseTuple(loc, subtokens)
                    case Square   => AST.ArrayLiteral (loc, Unit, Parser(loc, subtokens).Expression.parseListFull(","))
                    case BackTick => errorUnexpectedToken(loc, "a backtick sequence")
                    case _        => errorUnexpectedToken(loc, "a code block")
                }
                case Tokens.Comma(loc)                  => errorUnexpectedToken(loc, "a comma")
                case Tokens.Identifier(loc, actual)     => AST.Variable(loc, Unit, actual)
                case Tokens.Integer(loc, actual, value) => AST.Integer(loc, Unit, actual, value)
                case Tokens.Operator(loc, actual)       => errorUnexpectedToken(loc, "an operator")
                case Tokens.Reserved(loc, actual) =>
                    actual match {
                        case "addressOf" => AST.AddressOf(loc, Unit, parse(110))
                        case "if"        => {
                            val cond = inParens.Expression.parseFull
                            val t = parse(0)
                            requireReserved("else")
                            val f = parse(0)
                            AST.IfE(loc, Unit, cond, t, f)
                        }
                    case "negate"  => AST.Apply(loc, Unit, AST.Variable(loc, Unit, "negate"), parse(110))
                    case "valueAt" => AST.ValueAt(loc, Unit, parse(110))
                    case _         => errorUnexpectedToken(loc, actual)
                }
                case Tokens.Semicolon(loc) => errorUnexpectedToken(loc, "a semicolon")
            }
        }
        
        
        def parseOpenTuple: AST.Expression[Unit] = {
            val result = Expression.parseList(",")
            return result.length match {
                case 0 => {
                    val loc = if (isLive) tokens(index).loc else new Location(outerLoc.end, outerLoc.end)
                    errorUnexpected(loc, "an expression", "nothing")
                }
                case 1 => result(0)
                case _ => AST.Tuple(result(0).loc, Unit, result)
            }
        }
        
        
        def parseTuple(loc: Location, subtokens: ArrayBuffer[Tokens.Token]): AST.Expression[Unit] = {
            val result = Parser(loc, subtokens).Expression.parseListFull(",")
            return result.length match {
                case 0 => AST.Variable(loc, Unit, "()")
                case 1 => result(0)
                case _ => AST.Tuple(loc, Unit, result)
            }
        }
    }
    
    
    sealed case class Statement(
            private val scopeStack: ArrayBuffer[String])
            extends PrattParser[AST.Statement[Unit]] {
        
        val expected = "a statement"
        
        def leftDenotation(token: Tokens.Token): Option[(Int, AST.Statement[Unit] => AST.Statement[Unit])] = None
        
        
        def nullDenotation(token: Tokens.Token): AST.Statement[Unit] = token match {
            case Tokens.Block(loc, _, bracket, subs) if (bracket == Brace || bracket == ImplicitBrace) => {
            	AST.Nested(loc, Parser(loc, subs).Statement(scopeStack :+ "").parseListFull(";"))
            }
            case Tokens.Block(loc, _, Paren, subs) => {
                parseCallOrAssignment(index-1)
            }
            case Tokens.Identifier(loc, actual) => {
                nextToken match {
                    case Tokens.Block(bloc, _, Square, subs) => {
                        parseCallOrAssignment(index - 2)
                    }
                    case Tokens.Comma(_) | Tokens.Reserved(_, "=") => {
                        index -= 2
                        return parseAssignment
                    }
                    case Tokens.Reserved(_, "forever") => {
                        null
                        // FIXME: AST.Forever(loc, actual, Statement(requireBraces, scopeStack :+ actual).parseList(";"))
                    }
                    case _ => {
                        index -= 2
                        parseCall
                    }
                }
            }
            case Tokens.Reserved(loc, actual) => actual match {
                case "break" => {
                    lookahead match {
                        case Some(Tokens.Identifier(loc, actual)) => {
                            nextToken
                            scopeStack.lastIndexOf(actual) match {
                                case -1 => errorUnexpected(loc, expected, "the label does not correspond to any open scope blocks")
                                case i  => return AST.Break(loc, scopeStack.size - i)
                            }
                        }
                        case Some(Tokens.Semicolon(_)) | None => {
                            scopeStack.length match {
                                case 0 => errorUnexpected(loc, expected, "there are no open scope blocks")
                                case _ => return AST.Break(loc, 1)
                            }
                        }
                        case _    => errorUnexpected(loc, "the end of the statement or a label", actual)
                    }
                }
                case "valueAt" => {
                    parseCallOrAssignment(index - 1)
                }
                case "if" => {
                    val cond = Expression.parse(0)
                    
                    val trueBranch = inBraces.Statement(scopeStack).parseList(";")
                    
                    lookahead match {
                        case Some(Tokens.Reserved(_, "else")) => {
                            index += 1
                            val falseBranch = inBraces.Statement(scopeStack).parseListFull(";")
                            return AST.IfS(loc, cond, trueBranch, falseBranch)
                        }
                        case _ => return AST.IfS(loc, cond, trueBranch, ArrayBuffer.empty[AST.Statement[Unit]])
                    }
                }
                case "forever" => {
                    return AST.Forever(loc, "", inBraces.Statement(scopeStack :+ "").parseList(";"))
                }
                case "let" => {
                    nextToken match {
                        case Tokens.Block(_, _, bracket, _) if (bracket == Brace || bracket == ImplicitBrace) => {
                            errorUnexpected(new Location(1,1,1), "a let block", "ICE: don't know how to parse let blocks") // FIXME
                        }
                        case Tokens.Identifier(_, _) => {
                            index -= 1
                            parseLet
                        }
                        case t => errorUnexpected(t.loc, "a variable name", t.actual)
                    }
                    // FIXME: let
                    // let a: Type, b: var Type, c, d: Type = foo, bar, baz
                    // let: a = foo
                    //      b = bar
                    // let { a = foo
                    //       b = bar
                    // }
                    // So we're looking at a parameter list.
                    errorUnexpectedToken(loc, token.actual)
                }
                case "return" => {
                    return AST.Return(loc, Expression.parseOpenTuple)
                }
                case "var" => {
                    // FIXME: var
                    nextToken match {
                        case Tokens.Block(_, _, bracket, _) if (bracket == Brace || bracket == ImplicitBrace) => {
                            errorUnexpected(new Location(1,1,1), "a var block", "ICE: don't know how to parse var blocks") // FIXME
                        }
                        case Tokens.Identifier(_, _) => {
                            index -= 1
                            val result = parseLet
                            return null
                            // FIXME: need to change the modes to Mutable and make sure they aren't already mutable
                        }
                        case t => errorUnexpected(t.loc, "a variable name", t.actual)
                    }
                    errorUnexpectedToken(loc, token.actual)
                }
                case _ => errorUnexpectedToken(loc, token.actual)
            }
            case _ => errorUnexpectedToken(token.loc, token.actual)
        }
        
        
        def parseAssignment: AST.Statement[Unit] = {
            // FIXME: multiple indirect assignment
            // FIXME: Do I allow f(1)[5] = foo
            // I do need *(f(1)) = foo
            // I eventually need (*f).foo = bar
            
            val loc = tokens(index).loc
            val lValues = ArrayBuffer.empty[AST.LValue[Unit]]
            
            var equalsFound = false;
            while (!equalsFound) {
                errorUnexpectedToken(null, "")
            }
            return AST.Assign(loc, lValues, Expression.parseOpenTuple)
        }
        
        
        def parseCallOrAssignment(index_0: Int): AST.Statement[Unit] = {
            while (isLive) {
                val token = nextToken
                token match {
                    case Tokens.Block(_, _, Brace, _) | Tokens.Block(_, _, ImplicitBrace, _) => {
                        errorUnexpectedToken(token.loc, token.actual)
                    }
                    case Tokens.Block(_, _, Paren, _) | Tokens.Block(_, _, Square, _) |
                        Tokens.Identifier(_,_) | Tokens.Reserved(_, "valueAt") => Unit
                    case Tokens.Comma(_) | Tokens.Reserved(_, "=") => {
                        index = index_0
                        return parseAssignment
                    }
                    case _ => {
                        index = index_0
                        return parseCall
                    }
                }
            }
            index = index_0
            return parseCall
        }
        
        
        def parseCall: AST.Statement[Unit] = {
            val index_0 = index
            Expression.parse(0) match {
                case AST.Apply(aloc, _, f, arg) => return AST.Call(aloc, f, arg)
                case _ => {
                    val token = tokens(index_0)
                    errorUnexpectedToken(token.loc, token.actual)
                }
            }
        }
        
        
        def parseLet: AST.Statement[Unit] = {
            errorUnexpectedToken(new Location(1,1,1), "ICE: Don't know how to parse let statements.")
        }
    }
    
    
    object Type extends PrattParser[Types.Type] {
        
        
        val expected = "a type"
        
        
        def leftDenotation(token: Tokens.Token): Option[(Int, Types.Type => Types.Type)] = None
        
        /* FIXME: Fill in with the actual type parser */
        def nullDenotation(token: Tokens.Token): Types.Type = errorUnexpectedToken(outerLoc, token.actual)
    }
    
    
    // val initialTypeParser = Type()
}

    
    /*
    
initialDeclarationTable = Table
    { leftDenotationFunction = \table loc tok -> Nothing
    , leftDenotationTable = Map.fromList []
    , nullDenotationFunction = \table loc tok ->
        case (tok, Map.lookup (ugly tok) (nullDenotationTable table)) of
            (_, Just d) -> d loc tok
            _ -> errorUnexpected loc "a declaration" ("found " ++ pretty tok)
    , nullDenotationTable = Map.fromList
        [ ("def", \loc _ -> do
            (nloc, nameTok) <- nextToken
            name <- case nameTok of
                Just (In3_1 (Identifier a)) -> return a
                a -> errorGeneric nloc "a name" a
            (ploc, paramsTok) <- nextToken
            params <- case paramsTok of
                Just (In3_2 (TokenBlock Paren ts)) -> do
                    -- format:
                    -- a, b, c Int8, d Boolean
                    let getNames = do
                            (loc, t) <- nextToken
                            case t of
                                Just (In3_1 (Identifier a)) -> fmap (a:) loop
                                a -> errorGeneric loc "a parameter name" a
                          where loop = do
                                    (loc, t) <- lookahead
                                    case t of
                                        Just (In3_1 Comma) -> advanceToken >> getNames
                                        _ -> return []
                        getTypedNames = do
                            names <- getNames
                            tau <- parseType
                            return (names, tau)
                    typedNames <- parseOn ts (eosList Comma getTypedNames)
                    return [(param, tau) | (ps, tau) <- typedNames, param <- ps]
                a -> errorGeneric ploc "a list of parameters" a
            returnType <- parseType
            (bloc, bodyTok) <- nextToken
            (body, endLoc) <- case bodyTok of
                Just (In3_2 (TokenBlock Brace ts)) -> do
                    body <- statementList ts
                    let getEndLoc (Cons _ _ ts) = getEndLoc ts
                        getEndLoc (Nil loc)     = loc
                    return (body, getEndLoc ts)
                a -> errorGeneric bloc "the function body" a
            return (Def loc endLoc name params returnType body))
        , ("var", \loc _ -> do
            (nloc, nameTok) <- nextToken
            name <- case nameTok of
                Just (In3_1 (Identifier a)) -> return a
                a -> errorGeneric nloc "a name" a
            tau <- parseType
            return (GlobalVar loc name tau))
        ]
    }


    // Infix expression prototypes.
    , leftDenotationTable = Map.fromList $
        map (\(name, (lprec, rprec, _)) -> (name, (lprec, infixExpression rprec name))) prototypes


initialStatementTable = Table
    { leftDenotationFunction = \table loc tok ->
        case (tok, Map.lookup (ugly tok) (leftDenotationTable table)) of
            (_, Just (prec, d)) -> Just (prec, d loc tok)
            _ -> Nothing
    , leftDenotationTable = Map.fromList
        [ ("=", (1, \loc _ ast -> do
            (lvLoc, lValue) <- toLValue loc ast
            expr <- expression
            return (Stmt lvLoc (Assign lValue expr))))
        , ("forever", (1, \loc _ ast -> do
            case ast of
                Stmt sloc (Perform (Expr _ () (Variable name))) -> do
                    (bloc, b) <- nextToken
                    block <- case b of
                        Just (In3_2 (TokenBlock Brace ts)) -> statementList ts
                        a -> errorGeneric bloc "{" a
                    return (Stmt sloc (Forever name block))
                _ -> errorUnexpected loc "a semicolon" "found forever"))
        ]
    , nullDenotationFunction = \table loc tok ->
        let perform e = do
                expr <- inContext expressionTable (parseLeft 0 e)
                return (Stmt loc (Perform expr))
        in case (tok, Map.lookup (ugly tok) (nullDenotationTable table)) of
            (_, Just d)                       -> d loc tok
            (In3_1 (Identifier a), _)         -> perform (Expr loc () $ Variable a)
            (In3_1 (Token.Integer a), _)      -> perform (Expr loc () $ AST.Integer a)
            (In3_2 (TokenBlock Paren ts), _)  -> subexpression ")" ts >>= perform
            (In3_2 (TokenBlock Square ts), _) -> do
                expressionList ts >>= (perform . Expr loc () . ArrayLiteral)
            _                               -> errorUnexpected loc "a statement" ("found " ++ pretty tok)
    , nullDenotationTable = Map.fromList
        [ ("break", \loc _ -> do
            (nloc, n) <- lookahead
            case n of
                (Just (In3_1 (Identifier a))) -> do
                    nextToken
                    return (Stmt loc (Break a))
                _ -> return (Stmt loc (Break "")))
        , ("if", \loc _ -> do
            (cloc, c) <- nextToken
            cond <- case c of
                Just (In3_2 (TokenBlock Paren ts)) -> subexpression ")" ts
                a -> errorGeneric cloc "(" a
            (tloc, t) <- nextToken
            trueBranch <- case t of
                Just (In3_2 (TokenBlock Brace ts)) -> statementList ts
                a -> errorGeneric tloc "{" a
            (elseLoc, elseToken) <- nextToken
            falseBranch <- case elseToken of
                Nothing                -> return []
                Just (In3_1 Semicolon) -> return []
                Just (In3_1 (Identifier "else")) -> do
                    (floc, f) <- nextToken
                    case f of
                        Just (In3_2 (TokenBlock Brace ts)) -> statementList ts
                        a -> errorGeneric floc "{" a
                Just a -> errorUnexpected elseLoc "else or a semicolon" ("found " ++ pretty a)
            return (Stmt loc (If cond trueBranch falseBranch)))
        , ("forever", \loc _ -> do
            (bloc, b) <- nextToken
            block <- case b of
                Just (In3_2 (TokenBlock Brace ts)) -> statementList ts
                a -> errorGeneric bloc "{" a
            return (Stmt loc (Forever "" block)))
        , ("return", \loc _ -> do
            expr <- expression
            return (Stmt loc (Return expr)))
        , ("var", \loc _ -> do
            (nloc, n) <- nextToken
            name <- case n of
                Just (In3_1 (Identifier a)) -> return a
                _ -> errorGeneric nloc "a variable name" n
            tau <- parseType
            (eqloc, eq) <- lookahead
            init <- case eq of
                Nothing                -> return Nothing
                Just (In3_1 Semicolon) -> return Nothing
                Just (In3_3 "=") -> nextToken >> fmap Just expression
                Just a -> errorUnexpected eqloc "= or a semicolon" ("found " ++ pretty a)
            return (Stmt loc (Var name tau init)))
        ]
    }

    */
