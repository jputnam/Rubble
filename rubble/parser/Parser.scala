package rubble.parser

import rubble.data.AST
import rubble.data.Location
import rubble.data.ParseError
import rubble.data.Tokens
import rubble.data.Tokens.Bracket._

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer


object Parser {
    
    // Denotation a = Location -> GetParseToken -> a
    
    abstract class PrattParser[T] {
        
        def leftDenotation(t: T, token: Tokens.Token): T
        
        def nullDenotation(token: Tokens.Token): T
    }
    
    
    def unexpectedToken[A](loc: Location, expected: String, found: String): A = {
      throw new ParseError(loc, "The compiler needed " + expected + " but found " + found + ".")
    }
    
    
    def expression(loc: Location) = {
    	throw new ParseError(loc, "ICE: Don't know how to parse expressions")
    }
    
    
    def expressionList(loc: Location, ts: ArrayBuffer[Tokens.Token]): ArrayBuffer[AST.Expression] = {
        throw new ParseError(loc, "ICE: Don't know how to parse expression lists")
    }
    
    
    object Expression extends PrattParser[AST.Expression] {
        
        def leftDenotation(t: AST.Expression, token: Tokens.Token): AST.Expression = {
            return null
        }
        
        def nullDenotation(token: Tokens.Token): AST.Expression =
    		token match {
              case Tokens.Block(loc, actual, bracket, subTokens) =>
                bracket match {
                  case Paren    => AST.Parenthesized(loc, null, expressionList(loc, subTokens))
                  case Square   => AST.ArrayLiteral(loc, null, expressionList(loc, subTokens))
                  case BackTick => unexpectedToken(loc, "an expression", "a backtick sequence")
                  case default  => unexpectedToken(loc, "an expression", "a code block")
                }
              case Tokens.Comma(loc) => unexpectedToken(loc, "an expression", "a comma")
              case Tokens.Identifier(loc, actual) =>
                AST.Variable(loc, null, actual)
              case Tokens.Integer(loc, actual, value) =>
                AST.Integer(loc, null, actual, value)
              case Tokens.Operator(loc, actual) => unexpectedToken(loc, "an expression", "an operator")
              case Tokens.Reserved(loc, actual) =>
                actual match {
                  case "addressOf" => AST.AddressOf(loc, null, expression(loc))
                  case "if"        => {
                    val cond = throw new ParseError(loc, "ICE: Don't know how to parse conditionals.")
                    val t = throw new ParseError(loc, "ICE: Don't know how to parse conditionals.")
                    // grab the else keyword
                    val f = throw new ParseError(loc, "ICE: Don't know how to parse conditionals.")
                    AST.Conditional(loc, null, cond, t, f)
                  }
                  case "negate"    => AST.Apply(loc, null, null, null)
                  case "valueAt"   => AST.ValueAt(loc, null, expression(loc))
                  case default     => unexpectedToken(loc, "an expression", actual)
                }
              case Tokens.Semicolon(loc) => unexpectedToken(loc, "an expression", "a semicolon")
    		}
    }
}

sealed class Parser[T]
{ }
    
    
    /*
    , nullDenotationFunction = \table loc tok ->
        case (tok, Map.lookup (ugly tok) (nullDenotationTable table)) of
            (_, Just d)                       -> d loc tok
            (In3_1 (Identifier a), _)         -> return (Expr loc () $ Variable a)
            (In3_1 (Token.Integer a), _)      -> return (Expr loc () $ AST.Integer a)
            (In3_2 (TokenBlock Paren ts), _)  -> subexpression ")" ts
            (In3_2 (TokenBlock Square ts), _) ->
                expressionList ts >>= (return . Expr loc () . ArrayLiteral)
            _                                 -> errorUnexpected loc "an expression" ("found " ++ pretty tok)
    */
    
    
    /*
    statements:
        procedure call (many expressions)
        if/then/else
        <identifier> forever
        break <identifier>
        <lvalue> {, <lvalue>} = expression {, <expression>}
        return <expression>
        let/var
    
    expressions:
        <expression> <operator> <expression>
        function call
        (<expression>)
        <expression>[[<expression>]]
        [[ [<expression> {, <expression> }] ]]
        <identifier>
        <number>
        if/then/else
        *<expression>
        &<expression>
    
    
    
data ParseContext = ParseContext
    { keywords :: HashMap T.Text ()
    , expressionTable  :: Table (Expr Parsed)
    , declarationTable :: Table (Decl Parsed)
    , statementTable   :: Table (Stmt Parsed)
    }

data Table a = Table
    { leftDenotationFunction :: Table a -> Denotation (Maybe (Word, a -> ParseMonad a a))
    , leftDenotationTable    :: HashMap T.Text (Word, Denotation (a -> ParseMonad a a))
    , nullDenotationFunction :: Table a -> Denotation (ParseMonad a a)
    , nullDenotationTable    :: HashMap T.Text (Denotation (ParseMonad a a))
    }


ugly tok = case tok of
    In3_1 Comma             -> "#comma"
    In3_1 (Identifier a)    -> a
    In3_1 (Token.Integer a) -> "#integer"
    In3_1 Semicolon         -> "#semicolon"
    In3_2 (TokenBlock Brace _)  -> "#{"
    In3_2 (TokenBlock Paren _)  -> "#("
    In3_2 (TokenBlock Square _) -> "#["
    In3_3 a                 -> a


initialContext = ParseContext
    { keywords = Map.fromList [("=", ()), ("break", ()), ("def", ()), ("if", ()), ("forever", ()), ("var", ())]
    , expressionTable  = initialExpressionTable
    , declarationTable = initialDeclarationTable
    , statementTable   = initialStatementTable
    }


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


initialExpressionTable = Table
    { leftDenotationFunction = \table loc tok ->
        case (tok, Map.lookup (ugly tok) (leftDenotationTable table)) of
            (_, Just (prec, d))               -> Just (prec, d loc tok)
            (In3_1 (Identifier a), _)         -> Just (50, infixExpression 50 a loc tok)
            (In3_2 (TokenBlock Brace ts), _)  -> Just (10, \ast -> do
                tau <- parseOn ts (once "a type" parseType)
                return (Expr loc () (HasType (DeclaredType 0 [] (KnownType tau)) ast)))
            (In3_2 (TokenBlock Paren ts), _)  -> Just (110, \ast -> do
                args <- expressionList ts
                return (Expr loc () $ Apply ast args))
            (In3_2 (TokenBlock Square ts), _) -> Just (110, \ast -> do
                index <- subexpression "]" ts
                return (Expr loc () $ Index ast index))
            _                                 -> Nothing
    
    , leftDenotationTable = Map.fromList $
        map (\(name, (lprec, rprec, _)) -> (name, (lprec, infixExpression rprec name))) prototypes
    
    , nullDenotationFunction = \table loc tok ->
        case (tok, Map.lookup (ugly tok) (nullDenotationTable table)) of
            (_, Just d)                       -> d loc tok
            (In3_1 (Identifier a), _)         -> return (Expr loc () $ Variable a)
            (In3_1 (Token.Integer a), _)      -> return (Expr loc () $ AST.Integer a)
            (In3_2 (TokenBlock Paren ts), _)  -> subexpression ")" ts
            (In3_2 (TokenBlock Square ts), _) ->
                expressionList ts >>= (return . Expr loc () . ArrayLiteral)
            _                                 -> errorUnexpected loc "an expression" ("found " ++ pretty tok)
    
    , nullDenotationTable = Map.fromList []
    }


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
