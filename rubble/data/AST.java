package rubble.data;

import java.util.ArrayList;

import rubble.data.Names.ResolvedName;


public final class AST {
    
    public static final class Binding<Phase> {
        
        public final Location loc;
        public final ArrayList<Reference<Phase>> references;
        public final Expression<Phase> value;
        
        public Binding(Location loc, ArrayList<Reference<Phase>> references, Expression<Phase> value) {
            this.loc = loc;
            this.references = references;
            this.value = value;
        }
        
        public void resolveNames(NamingContext context, boolean isLocal) throws CompilerError {
            value.resolveNames(context);
            for (Reference<Phase> r: references) {
                r.resolveNames(context);
                if (isLocal) {
                    context.observeLocal(loc, r.name);
                } else {
                    context.observeGlobal(loc, r.name);
                }
            }
        }
        
        public String toString() {
            String refs = "";
            for (Reference<Phase> r: references) {
                refs += r.toString();
            }
            return "(Binding " + loc.toString() + " " + refs + value.toString() + ")";
        }
    }
    
    public static final class Reference<Phase> {
        
        public final String name;
        public final Types.Type<Phase> type;
        
        public Reference(String name, Types.Type<Phase> type) {
            this.name = name;
            this.type = type;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            type.resolveNames(context);
        }
        
        public String toString() {
            return "{" + name + " " + type.toString() + "}";
        }
    }
    
    
    public static enum DeclarationTag { Def, GlobalLet }
    
    public abstract static class Declaration<Phase> {
        
        public Location loc;
        public DeclarationTag tag;
        
        public Declaration(Location loc, DeclarationTag tag) {
            this.loc = loc;
            this.tag = tag;
        }
        
        public abstract void resolveNames(NamingContext context) throws CompilerError;
    }
    
    public static final class Def<Phase> extends Declaration<Phase> {
        
        public final String name;
        public final ArrayList<Reference<Phase>> arguments;
        public final Types.Type<Phase> returnType;
        public final ArrayList<Statement<Phase>> body;
        
        public Def(Location loc, String name, ArrayList<Reference<Phase>> arguments, Types.Type<Phase> returnType, ArrayList<Statement<Phase>> body) {
            super(loc, DeclarationTag.Def);
            this.name = name;
            this.arguments = arguments;
            this.returnType = returnType;
            this.body = body;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            for (Reference<Phase> r: arguments) {
                r.resolveNames(context);
                context.observeArgument(loc, r.name);
            }
            returnType.resolveNames(context);
            for (Statement<Phase> s: body) {
                s.resolveNames(context);
            }
        }
        
        public String toString() {
            String args = "";
            for (Reference<Phase> r: arguments) {
                args += r.toString();
            }
            String bodyString = "";
            for (Statement<Phase> s: body) {
                bodyString += s.toString();
            }
            return "(Def " + loc.toString() + " " + name + " " + args + " : " + returnType.toString() + "{" + bodyString + "})";
        }
    }
    
    public static final class GlobalLet<Phase> extends Declaration<Phase> {
        
        public final ArrayList<Binding<Phase>> bindings;
        
        public GlobalLet(Location loc, ArrayList<Binding<Phase>> bindings) {
            super(loc, DeclarationTag.GlobalLet);
            this.bindings = bindings;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            for (Binding<Phase> binding: bindings) {
                binding.resolveNames(context, false);
            }
        }
        
        public String toString() {
            String bs = "";
            for (Binding<Phase> b: bindings) {
                bs += b.toString();
            }
            return "(GlobalLet " + loc.toString() + " " + bs + ")";
        }
    }
    
    
    public static enum ExpressionTag { AddressOf, Apply, AsType, BufferLiteral, IfE, Index, Number, Tuple, ValueAt, Variable }
    
    public abstract static class Expression<Phase> {
        
        public final Location loc;
        public final Types.Type<Phase> type;
        public final ExpressionTag tag;
        
        public Expression(Location loc, Types.Type<Phase> type, ExpressionTag tag) {
            this.loc = loc;
            this.type = type;
            this.tag = tag;
        }
        
        public abstract void resolveNames(NamingContext context) throws CompilerError;
    }

    public static final class AddressOf<Phase> extends Expression<Phase> {
        
        public final Expression<Phase> value;
        
        public AddressOf(Location loc, Types.Type<Phase> type, Expression<Phase> value) {
            super(loc, type, ExpressionTag.AddressOf);
            this.value = value;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            value.resolveNames(context);
        }
        
        public String toString() {
            return "(& " + loc.toString() + " " + value.toString() + ")";
        }
    }
    
    public static final class Apply<Phase> extends Expression<Phase> {
        
        public final Expression<Phase> function;
        public final Expression<Phase> argument;
        
        public Apply(Location loc, Types.Type<Phase> type, Expression<Phase> function, Expression<Phase> argument) {
            super(loc, type, ExpressionTag.Apply);
            this.function = function;
            this.argument = argument;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            function.resolveNames(context);
            argument.resolveNames(context);
        }
        
        public String toString() {
            return "(A " + loc.toString() + " " + function.toString() + " $ " + argument.toString() + ")";
        }
    }
    
    public static final class AsType<Phase> extends Expression<Phase> {
        
        public final Expression<Phase> value;
        
        public AsType(Location loc, Types.Type<Phase> type, Expression<Phase> value) {
            super(loc, type, ExpressionTag.AsType);
            this.value = value;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            value.resolveNames(context);
        }
        
        public String toString() {
            return "(AsType " + loc.toString() + " " + value.toString() + " : " + type.toString() + ")";
        }
    }
    
    public static final class BufferLiteral<Phase> extends Expression<Phase> {
        
        public final ArrayList<Expression<Phase>> es;
        
        public BufferLiteral(Location loc, Types.Type<Phase> type, ArrayList<Expression<Phase>> es) {
            super(loc, type, ExpressionTag.BufferLiteral);
            this.es = es;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            for (Expression<Phase> e: es) {
                e.resolveNames(context);
            }
        }
        
        public String toString() {
            String ess = "";
            for (Expression<Phase> e: es) {
                ess += e.toString();
            }
            return "[" + loc.toString() + " " + ess + "]";
        }
    }
    
    public static final class IfE<Phase> extends Expression<Phase> {
        
        public final Expression<Phase> cond;
        public final Expression<Phase> trueBranch;
        public final Expression<Phase> falseBranch;
        
        public IfE(Location loc, Types.Type<Phase> type, Expression<Phase> cond, Expression<Phase> trueBranch, Expression<Phase> falseBranch) {
            super(loc, type, ExpressionTag.IfE);
            this.cond = cond;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            cond.resolveNames(context);
            trueBranch.resolveNames(context);
            falseBranch.resolveNames(context);
        }
        
        public String toString() {
            return "(IfE " + loc.toString() + " " + cond.toString() + " " + trueBranch.toString() + " " + falseBranch.toString() + ")";
        }
    }
    
    public static final class Index<Phase> extends Expression<Phase> {
        
        public final Expression<Phase> base;
        public final Expression<Phase> offset;
        
        public Index(Location loc, Types.Type<Phase> type, Expression<Phase> base, Expression<Phase> offset) {
            super(loc, type, ExpressionTag.Index);
            this.base = base;
            this.offset = offset;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            base.resolveNames(context);
            offset.resolveNames(context);
        }
        
        public String toString() {
            return "(Index " + loc.toString() + " " + base.toString() + " " + offset.toString() + ")";
        }
    }
    
    public static final class Number<Phase> extends Expression<Phase> {
        
        public final String number;
        
        public Number(Location loc, Types.Type<Phase> type, String number) {
            super(loc, type, ExpressionTag.Number);
            this.number = number;
        }
        
        public boolean isPositive() {
            return number.charAt(0) != '-' && !(number.equals("0"));
        }
        
        public void resolveNames(NamingContext context) throws CompilerError { }
        
        public String toString() {
            return "(" + loc.toString() + " {" + number + "})";
        }
    }
    
    public static final class Tuple<Phase> extends Expression<Phase> {
        
        public final ArrayList<Expression<Phase>> es;
        
        public Tuple(Location loc, Types.Type<Phase> type, ArrayList<Expression<Phase>> es) {
            super(loc, type, ExpressionTag.Tuple);
            this.es = es;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            for (Expression<Phase> e: es) {
                e.resolveNames(context);
            }
        }
        
        public String toString() {
            String ess = "";
            for (Expression<Phase> e: es) {
                ess += e.toString();
            }
            return "(Tuple " + loc.toString() + " " + ess + ")";
        }
    }
    
    public static final class ValueAt<Phase> extends Expression<Phase> {
        
        public final Expression<Phase> value;
        
        public ValueAt(Location loc, Types.Type<Phase> type, Expression<Phase> value) {
            super(loc, type, ExpressionTag.ValueAt);
            this.value = value;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            value.resolveNames(context);
        }
        
        public String toString() {
            return "(* " + loc.toString() + " " + value.toString() + ")";
        }
    }
    
    public static final class Variable<Phase> extends Expression<Phase> {
        
        public final String source;
        public ResolvedName name;
        
        public Variable(Location loc, Types.Type<Phase> type, String source) {
            super(loc, type, ExpressionTag.Variable);
            this.source = source;
            this.name = null;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            name = context.resolve(loc, source);
        }
        
        public String toString() {
            String nameString = name == null ? source : name.toString();
            return "(Var " + loc.toString() + " {" + nameString + "})";
        }
    }
    
    
    public static enum LValueTag { Direct, IndexL, Indirect, TupleL };
    
    public abstract static class LValue<Phase>{
        
        public final Location loc;
        public final Types.Type<Phase> type;
        public final LValueTag tag;
        
        public LValue(Location loc, Types.Type<Phase> type, LValueTag tag) {
            this.loc = loc;
            this.type = type;
            this.tag = tag;
        }
        
        public abstract void resolveNames(NamingContext context) throws CompilerError;
    }
    
    public static final class Direct<Phase> extends LValue<Phase> {
        
        public final String source;
        public ResolvedName name;
        
        public Direct(Location loc, Types.Type<Phase> type, String source) {
            super(loc, type, LValueTag.Direct);
            this.source = source;
            this.name = null;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            name = context.resolve(loc, source);
        }
        
        public String toString() {
            return "(Direct " + loc.toString() + " {" + name + "})";
        }
    }
    
    public static final class IndexL<Phase> extends LValue<Phase>{
        
        public final LValue<Phase> base;
        public final Expression<Phase> offset;
        
        public IndexL(Location loc, Types.Type<Phase> type, LValue<Phase> base, Expression<Phase> offset) {
            super(loc, type, LValueTag.IndexL);
            this.base = base;
            this.offset = offset;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            base.resolveNames(context);
            offset.resolveNames(context);
        }
        
        public String toString() {
            return "(IndexL " + loc.toString() + " " + base.toString() + "[" + offset.toString() + "])";
        }
    }
    
    public static final class Indirect<Phase> extends LValue<Phase> {
        
        public final Expression<Phase> address;
        
        public Indirect(Location loc, Types.Type<Phase> type, Expression<Phase> address) {
            super(loc, type, LValueTag.Indirect);
            this.address = address;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            address.resolveNames(context);
        }
        
        public String toString() {
            return "(Indirect " + loc.toString() + " " + address.toString() + ")";
        }
    }
    
    public static final class TupleL<Phase> extends LValue<Phase> {
        
        public final ArrayList<LValue<Phase>> lValues;
        
        public TupleL(Location loc, Types.Type<Phase> type, ArrayList<LValue<Phase>> lValues) {
            super(loc, type, LValueTag.TupleL);
            this.lValues = lValues;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            for (LValue<Phase> lValue: lValues) {
                lValue.resolveNames(context);
            }
        }
        
        public String toString() {
            String lvs = "";
            for (LValue<Phase> l: lValues) {
                lvs += l.toString();
            }
            return "(TupleL " + loc.toString() + " " + lvs + ")";
        }
    }
    
    
    public static enum StatementTag { Assign, Break, Call, Forever, IfS, Let, Nested, Return }
    
    public abstract static class Statement<Phase> {
        
        public final Location loc;
        public final StatementTag tag;
        
        public Statement(Location loc, StatementTag tag) {
            this.loc = loc;
            this.tag = tag;
        }
        
        public abstract void resolveNames(NamingContext context) throws CompilerError;
    }
    
    public static final class Assign<Phase> extends Statement<Phase> {
        
        public LValue<Phase> lValue;
        public Expression<Phase> value;
        
        public Assign(Location loc, LValue<Phase> lValue, Expression<Phase> value) {
            super(loc, StatementTag.Assign);
            this.lValue = lValue;
            this.value = value;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            lValue.resolveNames(context);
            value.resolveNames(context);
        }
        
        public String toString() {
            return "(Assign " + loc.toString() + " " + lValue.toString() + " " + value.toString() + ")";
        }
    }
    
    public static final class Break<Phase> extends Statement<Phase> {
        
        public final int depth;
        
        public Break(Location loc, int depth) {
            super(loc, StatementTag.Break);
            this.depth = depth;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError { }
        
        public String toString() {
            return "(Break " + loc.toString() + " " + depth + ")";
        }
    }
    
    public static final class Call<Phase> extends Statement<Phase> {
        
        public final Expression<Phase> function;
        public final Expression<Phase> argument;
        
        public Call(Location loc, Expression<Phase> function, Expression<Phase> argument) {
            super(loc, StatementTag.Call);
            this.function = function;
            this.argument = argument;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            function.resolveNames(context);
            argument.resolveNames(context);
        }
        
        public String toString() {
            return "(Call " + loc.toString() + " " + function.toString() + " " + argument.toString() + ")";
        }
    }
    
    public static final class Forever<Phase> extends Statement<Phase> {
        
        public final String label;
        public final ArrayList<Statement<Phase>> body;
        
        public Forever(Location loc, String label, ArrayList<Statement<Phase>> block) {
            super(loc, StatementTag.Forever);
            this.label = label;
            this.body = block;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            context.newScope();
            for (Statement<Phase> s: body) {
                s.resolveNames(context);
            }
        }
        
        public String toString() {
            String bs = "";
            for (Statement<Phase> b: body) {
                bs += b.toString();
            }
            return "(Forever " + loc.toString() + " {" + label + "} "  + bs + ")";
        }
    }
    
    public static final class IfS<Phase> extends Statement<Phase> {
        
        public final Expression<Phase> cond;
        public final ArrayList<Statement<Phase>> trueBranch;
        public final ArrayList<Statement<Phase>> falseBranch;
        
        public IfS(Location loc, Expression<Phase> cond, ArrayList<Statement<Phase>> trueBranch, ArrayList<Statement<Phase>> falseBranch) {
            super(loc, StatementTag.IfS);
            this.cond = cond;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            cond.resolveNames(context);
            for (Statement<Phase> s: trueBranch) {
                s.resolveNames(context);
            }
            for (Statement<Phase> s: falseBranch) {
                s.resolveNames(context);
            }
        }
        
        public String toString() {
            String ts = "";
            for (Statement<Phase> t: trueBranch) {
                ts += t.toString();
            }
            String fs = "";
            for (Statement<Phase> f: falseBranch) {
                fs += f.toString();
            }
            return "(IfS " + loc.toString() + " " + cond.toString() + " " + ts + " " + fs + ")";
        }
    }
    
    public static final class Let<Phase> extends Statement<Phase> {
        
        public final ArrayList<Binding<Phase>> bindings;
        
        public Let(Location loc, ArrayList<Binding<Phase>> bindings) {
            super(loc, StatementTag.Let);
            this.bindings = bindings;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            for (Binding<Phase> b: bindings) {
                b.resolveNames(context, true);
            }
        }
        
        public String toString() {
            String bs = "";
            for (Binding<Phase> b: bindings) {
                bs += b.toString();
            }
            return "(Let " + loc.toString() + " " + bs + ")";
        }
    }
    
    public static final class Nested<Phase> extends Statement<Phase> {
        
        public final ArrayList<Statement<Phase>> body;
        
        public Nested(Location loc, ArrayList<Statement<Phase>> statements) {
            super(loc, StatementTag.Nested);
            this.body = statements;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            context.newScope();
            for (Statement<Phase> s: body) {
                s.resolveNames(context);
            }
        }
        
        public String toString() {
            String ss = "";
            for (Statement<Phase> s: body) {
                ss += s.toString();
            }
            return "(Nested " + loc.toString() + " " + ss + ")";
        }
    }
    
    public static final class Return<Phase> extends Statement<Phase> {
        
        public Expression<Phase> value;
        
        public Return(Location loc, Expression<Phase> value) {
            super(loc, StatementTag.Return);
            this.value = value;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            value.resolveNames(context);
        }
        
        public String toString() {
            return "(Return " + loc.toString() + " " + value.toString() + ")";
        }
    }
}
