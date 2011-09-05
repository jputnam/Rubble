package rubble.data;

import java.util.ArrayList;

import rubble.data.Names.ResolvedName;
import rubble.data.Types.Poly;

/**
 * A container class for the abstract term-level syntax.  Types are elsewhere,
 * as are modes.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class AST {
    
    public static final class Binding<Name, Phase> {
        
        public final Location loc;
        public final ArrayList<Variable<Name, Phase>> references;
        public final Expression<Name, Phase> value;
        
        public Binding(Location loc, ArrayList<Variable<Name, Phase>> references, Expression<Name, Phase> value) {
            this.loc = loc;
            this.references = references;
            this.value = value;
        }
        
        public Binding<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            ArrayList<Variable<ResolvedName, Poly>> newReferences = new ArrayList<Variable<ResolvedName, Poly>>();
            for (Variable<Name, Phase> r: references) {
                newReferences.add(r.resolveNames(context));
            }
            return new Binding<ResolvedName, Poly>(loc, newReferences, value.resolveNames(context));
        }
        
        public String toString() {
            String refs = "";
            for (Variable<Name, Phase> r: references) {
                refs += r.toString();
            }
            return "(Binding " + loc.toString() + " " + refs + value.toString() + ")";
        }
    }
    
    
    public static enum DeclarationTag { Def, GlobalLet }
    
    public abstract static class Declaration<Name, Phase> {
        
        public final Location loc;
        public final DeclarationTag tag;
        
        public Declaration(Location loc, DeclarationTag tag) {
            this.loc = loc;
            this.tag = tag;
        }
    }
    
    public static final class Def<Name, Phase> extends Declaration<Name, Phase> {
        
        public final String name;
        public final ArrayList<Variable<Name, Phase>> arguments;
        public final Types.Type<Name, Phase> returnType;
        public final ArrayList<Statement<Name, Phase>> body;
        
        public Def(Location loc, String name, ArrayList<Variable<Name, Phase>> arguments, Types.Type<Name, Phase> returnType, ArrayList<Statement<Name, Phase>> body) {
            super(loc, DeclarationTag.Def);
            this.name = name;
            this.arguments = arguments;
            this.returnType = returnType;
            this.body = body;
        }
        /* 
        public void resolveNames(NamingContext context) throws CompilerError {
            context.observeGlobal(loc, name);
            for (Reference<Phase> r: arguments) {
                r.resolveNames(context);
                context.observeArgument(loc, r.name);
            }
            returnType.resolveNames(context);
            for (Statement<Name, Phase> s: body) {
                s.resolveNames(context);
            }
        }
        */
        public String toString() {
            String args = "";
            for (Variable<Name, Phase> r: arguments) {
                args += r.toString();
            }
            String bodyString = "";
            for (Statement<Name, Phase> s: body) {
                bodyString += s.toString();
            }
            return "(Def " + loc.toString() + " " + name + " " + args + " : " + returnType.toString() + "{" + bodyString + "})";
        }
    }
    
    public static final class GlobalLet<Name, Phase> extends Declaration<Name, Phase> {
        
        public final ArrayList<Binding<Name, Phase>> bindings;
        
        public GlobalLet(Location loc, ArrayList<Binding<Name, Phase>> bindings) {
            super(loc, DeclarationTag.GlobalLet);
            this.bindings = bindings;
        }
        /*
         * Don't forget that resolving the bindings doesn't observe the names.
        public void resolveNames(NamingContext context) throws CompilerError {
            for (Binding<Phase> binding: bindings) {
                binding.resolveNames(context, false);
            }
        }
        */
        public String toString() {
            String bs = "";
            for (Binding<Name, Phase> b: bindings) {
                bs += b.toString();
            }
            return "(GlobalLet " + loc.toString() + " " + bs + ")";
        }
    }
    
    
    public static enum ExpressionTag { AddressOf, Apply, AsType, BufferLiteral, IfE, Index, Number, Tuple, ValueAt, Variable }
    
    public abstract static class Expression<Name, Phase> {
        
        public final Location loc;
        public final Types.Type<Name, Phase> type;
        public final ExpressionTag tag;
        
        public Expression(Location loc, Types.Type<Name, Phase> type, ExpressionTag tag) {
            this.loc = loc;
            this.type = type;
            this.tag = tag;
        }
        
        public abstract Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError;
    }

    public static final class AddressOf<Name, Phase> extends Expression<Name, Phase> {
        
        public final Expression<Name, Phase> value;
        
        public AddressOf(Location loc, Types.Type<Name, Phase> type, Expression<Name, Phase> value) {
            super(loc, type, ExpressionTag.AddressOf);
            this.value = value;
        }
        
        public Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new AddressOf<ResolvedName, Poly>(loc, type.resolveNames(context), value.resolveNames(context));
        }
        
        public String toString() {
            return "(& " + loc.toString() + " " + value.toString() + ")";
        }
    }
    
    public static final class Apply<Name, Phase> extends Expression<Name, Phase> {
        
        public final Expression<Name, Phase> function;
        public final Expression<Name, Phase> argument;
        
        public Apply(Location loc, Types.Type<Name, Phase> type, Expression<Name, Phase> function, Expression<Name, Phase> argument) {
            super(loc, type, ExpressionTag.Apply);
            this.function = function;
            this.argument = argument;
        }
        
        public Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new Apply<ResolvedName, Poly>(loc, type.resolveNames(context), function.resolveNames(context), argument.resolveNames(context));
        }
        
        public String toString() {
            return "(A " + loc.toString() + " " + function.toString() + " $ " + argument.toString() + ")";
        }
    }
    
    public static final class AsType<Name, Phase> extends Expression<Name, Phase> {
        
        public final Expression<Name, Phase> value;
        
        public AsType(Location loc, Types.Type<Name, Phase> type, Expression<Name, Phase> value) {
            super(loc, type, ExpressionTag.AsType);
            this.value = value;
        }
        
        public Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new AsType<ResolvedName, Poly>(loc, type.resolveNames(context), value.resolveNames(context));
        }
        
        public String toString() {
            return "(AsType " + loc.toString() + " " + value.toString() + " : " + type.toString() + ")";
        }
    }
    
    public static final class BufferLiteral<Name, Phase> extends Expression<Name, Phase> {
        
        public final ArrayList<Expression<Name, Phase>> es;
        
        public BufferLiteral(Location loc, Types.Type<Name, Phase> type, ArrayList<Expression<Name, Phase>> es) {
            super(loc, type, ExpressionTag.BufferLiteral);
            this.es = es;
        }
        
        public Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            ArrayList<Expression<ResolvedName, Poly>> newEs = new ArrayList<Expression<ResolvedName, Poly>>();
            for (Expression<Name, Phase> e: es) {
                newEs.add(e.resolveNames(context));
            }
            return new BufferLiteral<ResolvedName, Poly>(loc, type.resolveNames(context), newEs);
        }
        
        public String toString() {
            String ess = "";
            for (Expression<Name, Phase> e: es) {
                ess += e.toString();
            }
            return "[" + loc.toString() + " " + ess + "]";
        }
    }
    
    public static final class IfE<Name, Phase> extends Expression<Name, Phase> {
        
        public final Expression<Name, Phase> cond;
        public final Expression<Name, Phase> trueBranch;
        public final Expression<Name, Phase> falseBranch;
        
        public IfE(Location loc, Types.Type<Name, Phase> type, Expression<Name, Phase> cond, Expression<Name, Phase> trueBranch, Expression<Name, Phase> falseBranch) {
            super(loc, type, ExpressionTag.IfE);
            this.cond = cond;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }
        
        public Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new IfE<ResolvedName, Poly>(loc, type.resolveNames(context), cond.resolveNames(context), trueBranch.resolveNames(context), falseBranch.resolveNames(context));
        }
        
        public String toString() {
            return "(IfE " + loc.toString() + " " + cond.toString() + " " + trueBranch.toString() + " " + falseBranch.toString() + ")";
        }
    }
    
    public static final class Index<Name, Phase> extends Expression<Name, Phase> {
        
        public final Expression<Name, Phase> base;
        public final Expression<Name, Phase> offset;
        
        public Index(Location loc, Types.Type<Name, Phase> type, Expression<Name, Phase> base, Expression<Name, Phase> offset) {
            super(loc, type, ExpressionTag.Index);
            this.base = base;
            this.offset = offset;
        }
        
        public Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new Index<ResolvedName, Poly>(loc, type.resolveNames(context), base.resolveNames(context), offset.resolveNames(context));
        }
        
        public String toString() {
            return "(Index " + loc.toString() + " " + base.toString() + " " + offset.toString() + ")";
        }
    }
    
    public static final class Number<Name, Phase> extends Expression<Name, Phase> {
        
        public final String number;
        
        public Number(Location loc, Types.Type<Name, Phase> type, String number) {
            super(loc, type, ExpressionTag.Number);
            this.number = number;
        }
        
        public boolean isPositive() {
            return number.charAt(0) != '-' && !(number.equals("0"));
        }
        
        public Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new Number<ResolvedName, Poly>(loc, type.resolveNames(context), number);
        }
        
        public String toString() {
            return "(" + loc.toString() + " {" + number + "})";
        }
    }
    
    public static final class Reference<Name, Phase> extends Expression<Name, Phase> {
        
        public final Name name;
        
        public Reference(Location loc, Types.Type<Name, Phase> type, Name name) {
            super(loc, type, ExpressionTag.Variable);
            this.name = name;
        }
        
        public Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new Reference<ResolvedName, Poly>(loc, type.resolveNames(context), context.resolve(loc, name.toString()));
        }
        
        public String toString() {
            return "(Var " + loc.toString() + " {" + name.toString() + "})";
        }
    }
    
    public static final class Tuple<Name, Phase> extends Expression<Name, Phase> {
        
        public final ArrayList<Expression<Name, Phase>> es;
        
        public Tuple(Location loc, Types.Type<Name, Phase> type, ArrayList<Expression<Name, Phase>> es) {
            super(loc, type, ExpressionTag.Tuple);
            this.es = es;
        }
        
        public Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            ArrayList<Expression<ResolvedName, Poly>> newEs = new ArrayList<Expression<ResolvedName, Poly>>();
            for (Expression<Name, Phase> e: es) {
                newEs.add(e.resolveNames(context));
            }
            return new Tuple<ResolvedName, Poly>(loc, type.resolveNames(context), newEs);
        }
        
        public String toString() {
            String ess = "";
            for (Expression<Name, Phase> e: es) {
                ess += e.toString();
            }
            return "(Tuple " + loc.toString() + " " + ess + ")";
        }
    }
    
    public static final class ValueAt<Name, Phase> extends Expression<Name, Phase> {
        
        public final Expression<Name, Phase> value;
        
        public ValueAt(Location loc, Types.Type<Name, Phase> type, Expression<Name, Phase> value) {
            super(loc, type, ExpressionTag.ValueAt);
            this.value = value;
        }
        
        public Expression<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new ValueAt<ResolvedName, Poly>(loc, type.resolveNames(context), value.resolveNames(context));
        }
        
        public String toString() {
            return "(* " + loc.toString() + " " + value.toString() + ")";
        }
    }
    
    
    public static enum LValueTag { Direct, IndexL, Indirect, TupleL };
    
    public abstract static class LValue<Name, Phase>{
        
        public final Location loc;
        public final Types.Type<Name, Phase> type;
        public final LValueTag tag;
        
        public LValue(Location loc, Types.Type<Name, Phase> type, LValueTag tag) {
            this.loc = loc;
            this.type = type;
            this.tag = tag;
        }
        
        public abstract LValue<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError;
    }
    
    public static final class Direct<Name, Phase> extends LValue<Name, Phase> {
        
        public final Name name;
        
        public Direct(Location loc, Types.Type<Name, Phase> type, Name name) {
            super(loc, type, LValueTag.Direct);
            this.name = name;
        }
        
        public LValue<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new Direct<ResolvedName, Poly>(loc, type.resolveNames(context), context.resolve(loc, name.toString())); 
        }
        
        public String toString() {
            return "(Direct " + loc.toString() + " {" + name + "})";
        }
    }
    
    public static final class IndexL<Name, Phase> extends LValue<Name, Phase>{
        
        public final LValue<Name, Phase> base;
        public final Expression<Name, Phase> offset;
        
        public IndexL(Location loc, Types.Type<Name, Phase> type, LValue<Name, Phase> base, Expression<Name, Phase> offset) {
            super(loc, type, LValueTag.IndexL);
            this.base = base;
            this.offset = offset;
        }
        
        public LValue<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new IndexL<ResolvedName, Poly>(loc, type.resolveNames(context), base.resolveNames(context), offset.resolveNames(context));
        }
        
        public String toString() {
            return "(IndexL " + loc.toString() + " " + base.toString() + "[" + offset.toString() + "])";
        }
    }
    
    public static final class Indirect<Name, Phase> extends LValue<Name, Phase> {
        
        public final Expression<Name, Phase> address;
        
        public Indirect(Location loc, Types.Type<Name, Phase> type, Expression<Name, Phase> address) {
            super(loc, type, LValueTag.Indirect);
            this.address = address;
        }
        
        public LValue<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new Indirect<ResolvedName, Poly>(loc, type.resolveNames(context), address.resolveNames(context));
        }
        
        public String toString() {
            return "(Indirect " + loc.toString() + " " + address.toString() + ")";
        }
    }
    
    public static final class TupleL<Name, Phase> extends LValue<Name, Phase> {
        
        public final ArrayList<LValue<Name, Phase>> lValues;
        
        public TupleL(Location loc, Types.Type<Name, Phase> type, ArrayList<LValue<Name, Phase>> lValues) {
            super(loc, type, LValueTag.TupleL);
            this.lValues = lValues;
        }
        
        public LValue<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            ArrayList<LValue<ResolvedName, Poly>> newLValues = new ArrayList<LValue<ResolvedName, Poly>>();
            for (LValue<Name, Phase> lValue: lValues) {
                newLValues.add(lValue.resolveNames(context));
            }
            return new TupleL<ResolvedName, Poly>(loc, type.resolveNames(context), newLValues);
        }
        
        public String toString() {
            String lvs = "";
            for (LValue<Name, Phase> l: lValues) {
                lvs += l.toString();
            }
            return "(TupleL " + loc.toString() + " " + lvs + ")";
        }
    }
    
    
    public static enum StatementTag { Assign, Break, Call, Forever, IfS, Let, Nested, Return }
    
    public abstract static class Statement<Name, Phase> {
        
        public final Location loc;
        public final StatementTag tag;
        
        public Statement(Location loc, StatementTag tag) {
            this.loc = loc;
            this.tag = tag;
        }
        
        // public abstract void resolveNames(NamingContext context) throws CompilerError;
    }
    
    public static final class Assign<Name, Phase> extends Statement<Name, Phase> {
        
        public final LValue<Name, Phase> lValue;
        public final Expression<Name, Phase> value;
        
        public Assign(Location loc, LValue<Name, Phase> lValue, Expression<Name, Phase> value) {
            super(loc, StatementTag.Assign);
            this.lValue = lValue;
            this.value = value;
        }
        /*
        public void resolveNames(NamingContext context) throws CompilerError {
            lValue.resolveNames(context);
            value.resolveNames(context);
        }
        */
        public String toString() {
            return "(Assign " + loc.toString() + " " + lValue.toString() + " " + value.toString() + ")";
        }
    }
    
    public static final class Break<Name, Phase> extends Statement<Name, Phase> {
        
        public final int depth;
        
        public Break(Location loc, int depth) {
            super(loc, StatementTag.Break);
            this.depth = depth;
        }
        
        // public void resolveNames(NamingContext context) throws CompilerError { }
        
        public String toString() {
            return "(Break " + loc.toString() + " " + depth + ")";
        }
    }
    
    public static final class Call<Name, Phase> extends Statement<Name, Phase> {
        
        public final Expression<Name, Phase> function;
        public final Expression<Name, Phase> argument;
        
        public Call(Location loc, Expression<Name, Phase> function, Expression<Name, Phase> argument) {
            super(loc, StatementTag.Call);
            this.function = function;
            this.argument = argument;
        }
        /*
        public void resolveNames(NamingContext context) throws CompilerError {
            function.resolveNames(context);
            argument.resolveNames(context);
        }
        */
        public String toString() {
            return "(Call " + loc.toString() + " " + function.toString() + " " + argument.toString() + ")";
        }
    }
    
    public static final class Forever<Name, Phase> extends Statement<Name, Phase> {
        
        public final String label;
        public final ArrayList<Statement<Name, Phase>> body;
        
        public Forever(Location loc, String label, ArrayList<Statement<Name, Phase>> block) {
            super(loc, StatementTag.Forever);
            this.label = label;
            this.body = block;
        }
        /*
        public void resolveNames(NamingContext context) throws CompilerError {
            context.newScope();
            for (Statement<Name, Phase> s: body) {
                s.resolveNames(context);
            }
        }
        */
        public String toString() {
            String bs = "";
            for (Statement<Name, Phase> b: body) {
                bs += b.toString();
            }
            return "(Forever " + loc.toString() + " {" + label + "} "  + bs + ")";
        }
    }
    
    public static final class IfS<Name, Phase> extends Statement<Name, Phase> {
        
        public final Expression<Name, Phase> cond;
        public final ArrayList<Statement<Name, Phase>> trueBranch;
        public final ArrayList<Statement<Name, Phase>> falseBranch;
        
        public IfS(Location loc, Expression<Name, Phase> cond, ArrayList<Statement<Name, Phase>> trueBranch, ArrayList<Statement<Name, Phase>> falseBranch) {
            super(loc, StatementTag.IfS);
            this.cond = cond;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }
        /*
        public void resolveNames(NamingContext context) throws CompilerError {
            cond.resolveNames(context);
            for (Statement<Name, Phase> s: trueBranch) {
                s.resolveNames(context);
            }
            for (Statement<Name, Phase> s: falseBranch) {
                s.resolveNames(context);
            }
        }
        */
        public String toString() {
            String ts = "";
            for (Statement<Name, Phase> t: trueBranch) {
                ts += t.toString();
            }
            String fs = "";
            for (Statement<Name, Phase> f: falseBranch) {
                fs += f.toString();
            }
            return "(IfS " + loc.toString() + " " + cond.toString() + " " + ts + " " + fs + ")";
        }
    }
    
    public static final class Let<Name, Phase> extends Statement<Name, Phase> {
        
        public final ArrayList<Binding<Name, Phase>> bindings;
        
        public Let(Location loc, ArrayList<Binding<Name, Phase>> bindings) {
            super(loc, StatementTag.Let);
            this.bindings = bindings;
        }
        /*
         * Don't forget that binding resolution doesn't observe the names.
        public void resolveNames(NamingContext context) throws CompilerError {
            for (Binding<Phase> b: bindings) {
                b.resolveNames(context, true);
            }
        }
        */
        public String toString() {
            String bs = "";
            for (Binding<Name, Phase> b: bindings) {
                bs += b.toString();
            }
            return "(Let " + loc.toString() + " " + bs + ")";
        }
    }
    
    public static final class Nested<Name, Phase> extends Statement<Name, Phase> {
        
        public final ArrayList<Statement<Name, Phase>> body;
        
        public Nested(Location loc, ArrayList<Statement<Name, Phase>> statements) {
            super(loc, StatementTag.Nested);
            this.body = statements;
        }
        /*
        public void resolveNames(NamingContext context) throws CompilerError {
            context.newScope();
            for (Statement<Name, Phase> s: body) {
                s.resolveNames(context);
            }
        }
        */
        public String toString() {
            String ss = "";
            for (Statement<Name, Phase> s: body) {
                ss += s.toString();
            }
            return "(Nested " + loc.toString() + " " + ss + ")";
        }
    }
    
    public static final class Return<Name, Phase> extends Statement<Name, Phase> {
        
        public final Expression<Name, Phase> value;
        
        public Return(Location loc, Expression<Name, Phase> value) {
            super(loc, StatementTag.Return);
            this.value = value;
        }
        /*
        public void resolveNames(NamingContext context) throws CompilerError {
            value.resolveNames(context);
        }
        */
        public String toString() {
            return "(Return " + loc.toString() + " " + value.toString() + ")";
        }
    }
}
