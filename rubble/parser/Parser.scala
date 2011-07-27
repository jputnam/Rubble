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
    
    def parse(loc: Location, tokens: ArrayBuffer[Tokens.Token]): ArrayBuffer[AST.Declaration[Unit]] = {
        val result = Parser(loc, tokens).Declaration.parseListFull
        if (result.length == 0) {
            throw new ParseError(loc, "No program text was found.")
        }
        return result
    }
}

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
            errorUnexpected(new Location(outerLoc.end), name, "ran out of tokens")
        }
        index += 1
        return tokens(index - 1)
    }
    
    
    def requireToken(name: String): Unit = {
        nextTokenExpecting(name) match {
            case t if t.actual == name => Unit
            case t => errorUnexpected(t.loc, name, t.actual)
        }
    }
    
    
    sealed abstract class PrattParser[T] {
        
        val expected: String
        val separator: String
        
        
        def errorUnexpectedToken[A](loc: Location, message: String): A =
            errorUnexpected(loc, expected, "found " + message)
        
            
        protected def inBraces(): Parser = nextToken match {
            case Tokens.Block(loc, _, bracket, subs) if (bracket == Brace || bracket == ImplicitBrace) => {
                return Parser(loc, subs)
            }
            case t => errorUnexpected(t.loc, "a statement block", "found " + t.actual)
        }
        
        
        protected def inParens: Parser = nextToken match {
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
        
        
        def parseList: ArrayBuffer[T] = {
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
        
        
        def parseListFull: ArrayBuffer[T] = {
            val result = parseList
            if (isLive) {
                val t = tokens(index)
                errorUnexpectedToken(t.loc, t.actual)
            }
            return result
        }
    }
    
    
    object Binding extends PrattParser[AST.Binding[Unit]]{
        
        val expected = "a binding"
        val separator = ","
        
        
        def leftDenotation(token: Tokens.Token): Option[(Int, AST.Binding[Unit] => AST.Binding[Unit])] = None
        
        
        def nullDenotation(token: Tokens.Token): AST.Binding[Unit] = token match {
            case Tokens.Identifier(loc, actual) => {
                index -= 1
                val names = Name.propagateTypes(Name.parseList)
                if (names.length == 0) {
                    errorUnexpected(loc, "a binding", "did not find one")
                }
                requireToken("=")
                return AST.Binding(loc, names, Expression.parseOpenTuple)
            }
            case _ => null
        }
    }
    
    
    object Declaration extends PrattParser[AST.Declaration[Unit]] {
        
        val expected = "a declaration"
        val separator = ";"
        
        
        def leftDenotation(token: Tokens.Token): Option[(Int, AST.Declaration[Unit] => AST.Declaration[Unit])] = None
        
        
        def nullDenotation(token: Tokens.Token): AST.Declaration[Unit] = token match {
            case Tokens.Reserved(loc, "def") => {
                // The function name.
                val name = nextToken match {
                    case Tokens.Identifier(_, actual) => actual
                    case t => errorUnexpected(t.loc, "a function name", t.actual)
                }
                
                // The arguments.
                val args = nextToken match {
                    case Tokens.Block(bloc, _, Paren, subtokens) => {
                        val result = Parser(bloc, subtokens).Name.parseListFull
                        for ((n,m,t) <- result) {
                            t match {
                                case Types.TypeVar(_) => throw new ParseError(bloc, "Function types must be monomorphic.")
                                case _ => Unit
                            }
                        }
                        result
                    }
                    case t => errorUnexpected(t.loc, "the parameter list", t.actual)
                }
                
                // The return type.
                val resultLoc = lookahead match {
                    case Some(t) => t.loc
                    case None    => new Location(outerLoc.end)
                }
                val resultType = Type.forceImmutable(Type.parse(0), resultLoc, "")
                
                // The body.
                val body = nextToken match {
                    case Tokens.Block(bloc, _, bracket, subtokens) if (bracket == Brace || bracket == ImplicitBrace) => {
                        Parser(bloc, subtokens).Statement(ArrayBuffer.empty[String]).parseListFull
                    }
                    case t => errorUnexpected(t.loc, "the function body", t.actual)
                }
                
                AST.Def(loc, name, args, resultType, body)
            }
            case Tokens.Reserved(_, "let") | Tokens.Reserved(_, "var") => {
                Statement(ArrayBuffer.empty[String]).parseLet(token.loc) match {
                    case AST.Let(loc, bindings) => AST.GlobalLet(loc, bindings)
                }
            }
            case _ => errorUnexpectedToken(token.loc, token.actual)
        }
    }
    
    
    object Expression extends PrattParser[AST.Expression[Unit]]{
        
        val expected = "an expression"
        val separator = ","
        
        
        private def infixExpression(prec: Int, left: AST.Expression[Unit], center: AST.Expression[Unit]): AST.Expression[Unit] = {
            val right = parse(prec)
            val (op, args) = center match {
                case AST.Apply(_, _, _op, AST.Tuple(eloc, _, extras)) => (_op, extras += left += right)
                case AST.Apply(_, _, _op, extra) => (_op, ArrayBuffer(extra, left, right))
                case _ => (center, ArrayBuffer(left, right))
            }
            return AST.Apply(left.loc, Unit, op, AST.Tuple(left.loc, Unit, args))
        }
        
        
        private def infixOperator(prec: Int, token: Tokens.Token): Option[(Int, AST.Expression[Unit] => AST.Expression[Unit])] =
            Some((prec, (ast) => infixExpression(prec, ast, AST.Variable(token.loc, Unit, token.actual))))
        
        
        def leftDenotation(token: Tokens.Token): Option[(Int, AST.Expression[Unit] => AST.Expression[Unit])] = token match {
            case Tokens.Block(loc, actual, bracket, subtokens) => bracket match {
                case BackTick      => Some((50,  (ast) => infixExpression(50, ast, Parser(loc, subtokens).Expression.parseFull)))
                case Paren         => Some((120, (ast) => AST.Apply(ast.loc, Unit, ast, parseTuple(loc, subtokens))))
                case Square        => Some((120, (ast) => AST.Index(ast.loc, Unit, ast, Parser(loc, subtokens).Expression.parseFull)))
            }
            case Tokens.Operator(loc, actual) => actual match {
                case "+" => infixOperator(50, token)
                case "-" => infixOperator(50, token)
                case "*" => infixOperator(60, token)
                case "<" => infixOperator(30, token)
                case ">" => infixOperator(30, token)
                case "<=" => infixOperator(30, token)
                case ">=" => infixOperator(30, token)
                case "==" => infixOperator(30, token)
                case "&&" => infixOperator(20, token)
                case "||" => infixOperator(20, token)
                case _ => errorUnexpectedToken(loc, "an unrecognized operator")
            }
            case Tokens.Reserved(loc, "asType") => {
                Some((0, (ast) =>  {
                    val (m, t) = Type.parse(0)
                    if (m != Types.Mode(Types.Mutability.Immutable)) {
                        throw new ParseError(loc, "Expressions cannot be declared to be mutable.")
                    }
                    AST.AsType(ast.loc, Unit, ast, t) }))
            }
            case _ => None
        }
        
        
        def nullDenotation(token: Tokens.Token): AST.Expression[Unit] = {
            return token match {
                case Tokens.Block(loc, actual, bracket, subtokens) => bracket match {
                    case Paren    => parseTuple(loc, subtokens)
                    case Square   => AST.ArrayLiteral (loc, Unit, Parser(loc, subtokens).Expression.parseListFull)
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
                            requireToken("else")
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
            val result = Expression.parseList
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
            val result = Parser(loc, subtokens).Expression.parseListFull
            return result.length match {
                case 0 => AST.Variable(loc, Unit, "()")
                case 1 => result(0)
                case _ => AST.Tuple(loc, Unit, result)
            }
        }
    }
    
    
    object Name extends PrattParser[(String, Types.Mode, Types.Type)]{
        
        val expected = "a variable name"
        val separator = ","
        
        
        def leftDenotation(token: Tokens.Token):
                Option[(Int, ((String, Types.Mode, Types.Type)) => (String, Types.Mode, Types.Type))] = None
        
        
        def nullDenotation(token: Tokens.Token): (String, Types.Mode, Types.Type) = token match {
            case (Tokens.Identifier(loc, actual)) => lookahead match {
                case Some(Tokens.Reserved(_, "asType")) => {
                    index += 1
                    val (mode, tau) = Type.parse(0)
                    return (actual, mode, tau)
                }
                case _ => (actual, Types.Mode(Types.Mutability.Immutable), null)
            }
            case _ => errorUnexpectedToken(token.loc, token.actual)
        }
        
        
        def propagateTypes(names: ArrayBuffer[(String, Types.Mode, Types.Type)]): ArrayBuffer[(String, Types.Mode, Types.Type)] = {
            if (names.length == 0) { return names }
            
            var (mode, tau) = names(names.length - 1) match {
                case (_, m, null) => ((m, Types.TypeVar(-1)))
                case (_, m, t)    => ((m, t))
            }
            for (i <- (0 until (names.length - 2)).reverse) {
                names(i) match {
                    case (n, _, null) => { names(i) = (n, mode, tau) }
                    case (n, m, t)    => { mode = m; tau = t }
                    case _ => Unit
                }
            }
            return names
        }
    }
    
    
    sealed case class Statement(
            private val scopeStack: ArrayBuffer[String])
            extends PrattParser[AST.Statement[Unit]] {
        
        val expected = "a statement"
        val separator = ";"
        
        
        private def certifyLValue(ast: AST.Expression[Unit]): AST.LValue[Unit] = ast match {
            case AST.Index(loc, _, base, offset) => AST.IndexL(Unit, certifyLValue(base), offset)
            case AST.Tuple(loc, _, ts) => AST.TupleL(Unit, ts.map(certifyLValue))
            case AST.ValueAt(loc, _, address) => AST.Indirect(Unit, address)
            case AST.Variable(loc, _, name) => AST.Direct(Unit, name)
            case _ => errorUnexpected(ast.loc, "an lvalue", "another kind of expression")
        }
        
        
        def leftDenotation(token: Tokens.Token): Option[(Int, AST.Statement[Unit] => AST.Statement[Unit])] = None
        
        
        def nullDenotation(token: Tokens.Token): AST.Statement[Unit] = token match {
            case Tokens.Block(loc, _, bracket, subs) if (bracket == Brace || bracket == ImplicitBrace) => {
                AST.Nested(loc, Parser(loc, subs).Statement(scopeStack :+ "").parseListFull)
            }
            case Tokens.Block(loc, _, Paren, subs) => {
                index -= 1
                parseCallOrAssignment
            }
            case Tokens.Identifier(loc, actual) => {
                nextToken match {
                    case Tokens.Comma(_) | Tokens.Reserved(_, "=") => {
                        AST.Assign(loc, AST.Direct(Unit, actual), Expression.parseOpenTuple)
                    }
                    case Tokens.Reserved(_, "forever") => {
                        AST.Forever(loc, actual, inBraces.Statement(scopeStack :+ actual).parseListFull)
                    }
                    case _ => {
                        index -= 2
                        parseCallOrAssignment
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
                    index -= 1
                    parseCallOrAssignment
                }
                case "if" => {
                    val cond = Expression.parse(0)
                    val trueBranch = inBraces.Statement(scopeStack).parseList
                    
                    lookahead match {
                        case Some(Tokens.Reserved(_, "else")) => {
                            index += 1
                            val falseBranch = inBraces.Statement(scopeStack).parseListFull
                            return AST.IfS(loc, cond, trueBranch, falseBranch)
                        }
                        case _ => return AST.IfS(loc, cond, trueBranch, ArrayBuffer.empty[AST.Statement[Unit]])
                    }
                }
                case "forever" => {
                    return AST.Forever(loc, "", inBraces.Statement(scopeStack :+ "").parseListFull)
                }
                case "let" => {
                    return parseLet(loc)
                }
                case "return" => {
                    return AST.Return(loc, Expression.parseOpenTuple)
                }
                case "var" => {
                    val result = parseLet(loc)
                    for (binding <- result.bindings) {
                        for (i <- 0 until binding.names.length) {
                            binding.names(i) match {
                                case (actual, Types.Mode(Types.Mutability.Immutable), tau) => {
                                    binding.names(i) = (actual, Types.Mode(Types.Mutability.Mutable), tau)
                                }
                                case _ => {
                                    throw new ParseError(loc, "Variables in a var declaration cannot be marked mutable.")
                                }
                            }
                        }
                    }
                    return result
                }
                case _ => errorUnexpectedToken(loc, token.actual)
            }
            case _ => errorUnexpectedToken(token.loc, token.actual)
        }
        
        
        def parseCallOrAssignment: AST.Statement[Unit] = {
            val ast = Expression.parseOpenTuple
            lookahead match {
                case Some(Tokens.Reserved(_, "=")) => {
                    val lValue = certifyLValue(ast)
                    requireToken("=")
                    AST.Assign(ast.loc, lValue, Expression.parseOpenTuple)
                }
                case Some(Tokens.Semicolon(_)) | None => ast match {
                    case AST.Apply(loc, _, f, x) => AST.Call(loc, f, x)
                    case _ => errorUnexpected(ast.loc, "a function call", "another kind of expression")
                }
                case Some(t) => errorUnexpected(t.loc, "a semicolon or equals sign", t.actual)
            }
        }
        
        
        def parseLet(loc: Location): AST.Let[Unit] = lookahead match {
            case Some(Tokens.Block(bloc, _, bracket, subtokens)) if (bracket == Brace || bracket == ImplicitBrace) => {
                AST.Let(bloc, Parser(bloc, subtokens).Binding.parseListFull)
            }
            case Some(Tokens.Identifier(iloc, _)) => {
                AST.Let(iloc, ArrayBuffer(Binding.parse(0)))
            }
            case Some(t) => errorUnexpectedToken(t.loc, t.actual)
            case None    => errorUnexpected(loc, "a binding", "ran out of input")
        }
    }
    
    
    object Type extends PrattParser[(Types.Mode, Types.Type)] {
        
        val expected = "a type"
        val separator = ","
        
        
        def forceImmutable(x: (Types.Mode, Types.Type), loc: Location, message: String): Types.Type = {
            val (m, t) = x
            if (m == Types.Mode(Types.Mutability.Mutable)) {
                throw new ParseError(loc, message)
            }
            return t
        }
        
        
        def immutable(t: Types.Type): (Types.Mode, Types.Type) = (Types.Mode(Types.Mutability.Immutable), t)
        
        
        def immutableGround(p: Types.Primitive.Primitive): (Types.Mode, Types.Type) = immutable(Types.Ground(p))
        
        
        def leftDenotation(token: Tokens.Token): Option[(Int, (Types.Mode, Types.Type) => (Types.Mode, Types.Type))] = token match {
            case Tokens.Identifier(_, "->") => Some((50, (mode, ast) => {
                val loc = lookahead match {
                    case Some(t) => t.loc
                    case None    => new Location(outerLoc.end)
                }
                val tau = forceImmutable(parse(49), loc, "The return type of a function cannot be mutable.")
                immutable(Types.Arrow(mode, ast, tau))
            }))
            case _ => None
        }
        
        
        def nullDenotation(token: Tokens.Token): (Types.Mode, Types.Type) = token match {
            case Tokens.Block(bloc, _, Paren, subtokens) => {
                val types = Parser(bloc, subtokens).Type.parseListFull
                types.length match {
                    case 0 => immutable(Types.Ground(Types.Primitive.Unit))
                    case 1 => types(0)
                    case _ => immutable(Types.Tuple(types))
                }
            }
            case Tokens.Identifier(loc, actual) => actual match {
                case "Boolean" => immutableGround(Types.Primitive.Boolean)
                case "Buffer"  => nextToken match {
                    case Tokens.Block(bloc, _, Square, subtokens) => {
                        val p = Parser(bloc, subtokens)
                        
                        val size = p.nextTokenExpecting("a positive integer") match {
                            case Tokens.Identifier(_, "_") => Types.SizeVar(-1)
                            case Tokens.Integer(_, _, iint) if (iint > 0) => Types.KnownSize(iint)
                            case t => errorUnexpected(t.loc, "a positive integer", t.actual)
                        }
                        p.requireToken(",")
                        val (mode, tau) = p.Type.parseFull
                        
                        immutable(Types.Buffer(size, mode, tau))
                    }
                    case t => errorUnexpectedToken(t.loc, t.actual)
                }
                case "Int8"    => immutableGround(Types.Primitive.Int8)
                case "Int16"   => immutableGround(Types.Primitive.Int16)
                case "Int32"   => immutableGround(Types.Primitive.Int32)
                case "Int64"   => immutableGround(Types.Primitive.Int64)
                case "Ptr"     => nextToken match {
                    case Tokens.Block(bloc, _, Square, subtokens) => {
                        val (mode, tau) = Parser(bloc, subtokens).Type.parseFull
                        immutable(Types.Ptr(mode, tau))
                    }
                    case t => errorUnexpectedToken(t.loc, t.actual)
                }
                case "UInt8"   => immutableGround(Types.Primitive.UInt8)
                case "UInt16"  => immutableGround(Types.Primitive.UInt16)
                case "UInt32"  => immutableGround(Types.Primitive.UInt32)
                case "UInt64"  => immutableGround(Types.Primitive.UInt64)
                case _ => errorUnexpectedToken(outerLoc, token.actual)
            }
            case Tokens.Reserved(loc, "var") => Type.parse(110) match {
                case (Types.Mode(Types.Mutability.Immutable), tau) => (Types.Mode(Types.Mutability.Mutable), tau)
                case _ => throw new ParseError(loc, "A type can only be marked mutable once.")
            }
            case _ => errorUnexpectedToken(outerLoc, token.actual)
        }
    }
}


/*
    // Infix expression prototypes.
    , leftDenotationTable = Map.fromList $
        map (\(name, (lprec, rprec, _)) -> (name, (lprec, infixExpression rprec name))) prototypes

    */
