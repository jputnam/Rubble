package rubble.parser

import rubble.data.AST
import rubble.data.Location
import rubble.data.ParseError
import rubble.data.Tokens
import rubble.data.Tokens.Bracket._
import rubble.data.Types

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer


object Parser {
    
    sealed case class Context(
            val loc: Location,
            val tokens: ArrayBuffer[Tokens.Token],
            var index: Int = 0) {
        
        def errorUnexpected[A](loc: Location, expected: String, message: String): A = {
            throw new ParseError(loc, "The compiler needed " + expected + " but " + message + ".")
        }
        
        
        def isLive: Boolean = index < tokens.length
        
        
        def lookahead: Option[Tokens.Token] =
            if (isLive) Some(tokens(index)) else None
        
        
        def nextToken(name: String): Tokens.Token = {
            if (!isLive) {
                errorUnexpected(new Location(loc.end, loc.end), name, "ran out of input")
            }
            index += 1
            return tokens(index - 1)
        }
    }
    
    
    def parse(tokens: ArrayBuffer[Tokens.Token]): Unit = {
        // FIXME: Implement this.
        // validate that the token stream is not empty
        // construct a parse context
        return
    }
    
    
    sealed abstract class PrattParser[T](val context: Context) {
        
        protected val expected: String
        
        
        /**
         * See the comment for nextToken.
         */
        def errorUnexpectedToken[A](loc: Location, message: String): A =
            context.errorUnexpected(loc, expected, "found " + message)
        
        
        protected def leftDenotation(token: Tokens.Token): Option[(Int, T => T)]
        
        
        /**
         * This shouldn't be here, but we have to let the context know what
         * the parser name is for proper error messages.  We can't write the
         * name into the context itself, because the context will be passed
         * between different parsers, each of which has a different name.
         */
        protected def nextToken: Tokens.Token = context.nextToken(expected)
        
        
        protected def nullDenotation(token: Tokens.Token): T
        
        
        def parse(rbp: Int): T = {
            val token = nextToken
            var ast = nullDenotation(token)
            
            while (context.isLive) {
                leftDenotation(context.tokens(context.index)) match {
                    case Some((lbp, f)) if rbp < lbp => {
                        context.index += 1
                        ast = f(ast)
                    }
                    case _ => return ast
                }
            }
            return ast
        }
        
        
        def parseAll(): T = {
            val result = parse(0)
            if (context.isLive) {
                val t = context.tokens(context.index)
                errorUnexpectedToken(t.loc, t.actual)
            }
            return result
        }
        
        
        def parseList(separator: String): ArrayBuffer[T] = {
            if (context.tokens.length == 0) { return ArrayBuffer.empty[T] }
            val result = ArrayBuffer.empty[T]
            while (true) {
                result += parse(0)
                if (context.isLive) {
                    context.tokens(context.index) match {
                        case t if t.actual != separator => context.errorUnexpected(t.loc, separator, t.actual)
                        case _ => context.index += 1
                    }
                } else {
                    return result
                }
            }
            throw new ParseError(context.loc.end, "ICE: parseList() should never fall off the end of its loop.")
        }
        
        
        protected def requireBlock(bracket: Bracket, name: String): (Location, ArrayBuffer[Tokens.Token]) =
            nextToken match {
                case Tokens.Block(loc, _, b, ts) if b == bracket => return (loc, ts)
                case t => errorUnexpectedToken(t.loc, t.actual)
            }
        
        
        protected def requireReserved(name: String) = {
            nextToken match {
                case Tokens.Reserved(_, s) if s == name => Unit
                case t => errorUnexpectedToken(t.loc, t.actual)
            }
        }
        
        
        protected def requireBraces(): (Location, ArrayBuffer[Tokens.Token]) = {
            nextToken match {
                case Tokens.Block(loc, _, bracket, subs) if (bracket == Brace || bracket == ImplicitBrace) => {
                    return (loc, subs)
                }
                case t => context.errorUnexpected(context.loc, "a statement block", "found " + t.actual)
            }
        }
    }
    
    
    sealed case class Expression(
            override val context: Context)
            extends PrattParser[AST.Expression[Unit]](context) {
        
        
        protected val expected = "an expression"
        
        
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
                case BackTick      => Some(5,  (ast) => { infixExpression(5, ast, Expression(Context(loc, subtokens)).parseAll) })
                case Paren         => Some(12, (ast) => { AST.Apply(loc, Unit, ast, tuple(loc, subtokens)) })
                case Square        => Some(12, (ast) => { AST.Index(loc, Unit, ast, Expression(Context(loc, subtokens)).parseList(",")) })
                case Brace         => errorUnexpectedToken(loc, "ICE: The compiler doesn't know how to parse types.") // Some(12, (ast) => { AST.HasType(loc, Unit, ast, null) })
                case ImplicitBrace => errorUnexpectedToken(loc, "ICE: The compiler doesn't know how to parse types.") // Some(12, (ast) => { AST.HasType(loc, Unit, ast, null) })
            }
            // FIXME: Fill in the operators.
            case Tokens.Operator(loc, actual) => errorUnexpectedToken(loc, "an unrecognized operator")
            case _ => None
        }
        
        
        def nullDenotation(token: Tokens.Token): AST.Expression[Unit] = {
            return token match {
                case Tokens.Block(loc, actual, bracket, subtokens) => bracket match {
                    case Paren    => tuple(loc, subtokens)
                    case Square   => AST.ArrayLiteral (loc, Unit, Expression(Context(loc, subtokens)).parseList(","))
                    case BackTick => errorUnexpectedToken(loc, "a backtick sequence")
                    case _        => errorUnexpectedToken(loc, "a code block")
                }
                case Tokens.Comma(loc)                  => errorUnexpectedToken(loc, "a comma")
                case Tokens.Identifier(loc, actual)     => AST.Variable(loc, Unit, actual)
                case Tokens.Integer(loc, actual, value) => AST.Integer(loc, Unit, actual, value)
                case Tokens.Operator(loc, actual)       => errorUnexpectedToken(loc, "an operator")
                case Tokens.Reserved(loc, actual) =>
                    actual match {
                        case "addressOf" => AST.AddressOf(loc, Unit, parse(11))
                        case "if"        => {
                            val (bloc, bsubs) = requireBlock(Paren, "(")
                            val cond = Expression(Context(bloc, bsubs)).parseAll
                            val t = parse(0)
                            requireReserved("else")
                            val f = parse(0)
                            AST.IfE(loc, Unit, cond, t, f)
                        }
                    case "negate"    => AST.Apply(loc, Unit, AST.Variable(loc, Unit, "negate"), parse(11))
                    case "valueAt"   => AST.ValueAt(loc, Unit, parse(11))
                    case _           => errorUnexpectedToken(loc, actual)
                }
                case Tokens.Semicolon(loc) => errorUnexpectedToken(loc, "a semicolon")
            }
        }
        
        
        def tuple(loc: Location, subtokens: ArrayBuffer[Tokens.Token]): AST.Expression[Unit] = {
            val result = Expression(Context(loc, subtokens)).parseList(",")
            return result.length match {
                case 0 => AST.Variable(loc, Unit, "()")
                case 1 => result(0)
                case _ => AST.Tuple(loc, Unit, result)
            }
        }
    }
    
    
    sealed case class Statement(
            override val context: Context,
            private val scopeStack: ArrayBuffer[String])
            extends PrattParser[AST.Statement[Unit]](context) {
        
        
        val expected = "a statement"
        
        
        def leftDenotation(token: Tokens.Token): Option[(Int, AST.Statement[Unit] => AST.Statement[Unit])] = None
        
        
        def nullDenotation(token: Tokens.Token): AST.Statement[Unit] = token match {
            case Tokens.Block(loc, _, bracket, subs) if (bracket == Brace || bracket == ImplicitBrace) => {
                // FIXME: Nested scope.
                errorUnexpectedToken(loc, token.actual)
            }
            case Tokens.Identifier(loc, actual) => {
                // FIXME: Handle indirect assignment, multiple assignment, and function calls.
                nextToken match {
                    case Tokens.Comma(_) => {
                        // FIXME: Multiple assignment.
                        errorUnexpectedToken(loc, token.actual)
                    }
                    case Tokens.Reserved(_, "=") => {
                        // FIXME: Assignment
                        // return AST.Assign(loc, ArrayBuffer(token), ?)
                        errorUnexpectedToken(loc, token.actual)
                    }
                    case Tokens.Reserved(_, "forever") => {
                        context.index += 1
                        val (bloc, bsubs) = requireBraces
                        return AST.Forever(loc, actual, Statement(Context(bloc, bsubs), scopeStack :+ actual).parseList(";"))
                    }
                    case _ => errorUnexpectedToken(loc, token.actual) 
                }
            }
            case Tokens.Reserved(loc, actual) => actual match {
                case "break" => {
                    context.lookahead match {
                        case Some(Tokens.Identifier(loc, actual)) => {
                            nextToken
                            scopeStack.lastIndexOf(actual) match {
                                case -1 => context.errorUnexpected(loc, expected, "the label does not correspond to any open scope blocks")
                                case i  => return AST.Break(loc, scopeStack.size - i)
                            }
                        }
                        case Some(Tokens.Semicolon(_)) | None => {
                            scopeStack.length match {
                                case 0 => context.errorUnexpected(loc, expected, "there are no open scope blocks")
                                case _ => return AST.Break(loc, 1)
                            }
                        }
                        case _    => context.errorUnexpected(loc, "the end of the statement or a label", actual)
                    }
                }
                case "if" => {
                    val (cloc, csubs) = requireBlock(Paren, "(")
                    val cond = Expression(Context(cloc, csubs)).parseAll
                    
                    val (tloc, tsubs) = requireBraces
                    val trueBranch = Statement(Context(tloc, tsubs), scopeStack).parseList(";")
                    
                    context.lookahead match {
                        case Some(Tokens.Reserved(_, "else")) => {
                            context.index += 1
                            val (floc, fsubs) = requireBraces
                            val falseBranch = Statement(Context(tloc, tsubs), scopeStack).parseList(";")
                            return AST.IfS(loc, cond, trueBranch, falseBranch)
                        }
                        case _ => return AST.IfS(loc, cond, trueBranch, ArrayBuffer.empty[AST.Statement[Unit]])
                    }
                }
                case "forever" => {
                    val (bloc, bsubs) = requireBraces
                    return AST.Forever(loc, "", Statement(Context(bloc, bsubs), scopeStack :+ "").parseList(";"))
                }
                case "let" => {
                    // FIXME: let
                    errorUnexpectedToken(loc, token.actual)
                }
                case "return" => {
                    return AST.Return(loc, Expression(context).parse(0))
                }
                case "var" => {
                    // FIXME: var
                    errorUnexpectedToken(loc, token.actual)
                }
                case _ => errorUnexpectedToken(loc, token.actual)
            }
            case _ => errorUnexpectedToken(token.loc, token.actual)
        }
    }
    
    
    sealed case class Type(
            override val context:Context)
            extends PrattParser[Types.Type](context) { // loc, tokens)) {
        
        
        val expected = "a type"
        
        
        def leftDenotation(token: Tokens.Token): Option[(Int, Types.Type => Types.Type)] = None
        
        /* FIXME: Fill in with the actual type parser */
        def nullDenotation(token: Tokens.Token): Types.Type = errorUnexpectedToken(context.loc, token.actual)
    }
    
    
    // val initialTypeParser = Type()
}

    
    /*
    statements:
        procedure call (many expressions)
        <lvalue> {, <lvalue>} = expression {, <expression>}
        let/var
    
    
    
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
