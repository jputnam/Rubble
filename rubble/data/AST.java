package rubble.data;

import java.util.ArrayList;

public final class AST {
    
    public static enum ExpressionTag { AddressOf, Apply, AsType, BufferLiteral, IfE, Index, Number, Tuple, ValueAt, Variable }
    
    public static final class Binding<Type, Phase, Name> {
        
        public final Location loc;
        public final ArrayList<Reference<Phase, Name>> references;
        public final Expression<Type, Phase, Name> value;
        
        public Binding(Location loc, ArrayList<Reference<Phase, Name>> references, Expression<Type, Phase, Name> value) {
            this.loc = loc;
            this.references = references;
            this.value = value;
        }
        
        public String toString() {
            String refs = "";
            for (Reference<Phase, Name> r: references) {
                refs += r.toString();
            }
            return "(Binding " + loc.toString() + " " + refs + value.toString() + ")";
        }
    }
    
    public static final class Reference<Phase, Name> {
        
        public final Name name;
        public final Types.Type<Phase> type;
        
        public Reference(Name name, Types.Type<Phase> type) {
            this.name = name;
            this.type = type;
        }
        
        public String toString() {
            return "{" + name.toString() + " " + type.toString() + "}";
        }
    }
    
    
    public static enum DeclarationTag { Def, GlobalLet }
    
    public abstract static class Declaration<Type, Phase, Name> {
        
        public Location loc;
        public DeclarationTag tag;
        
        public Declaration(Location loc, DeclarationTag tag) {
            this.loc = loc;
            this.tag = tag;
        }
    }
    
    public static final class Def<Type, Phase, Name> extends Declaration<Type, Phase, Name> {
        
        public final Name name;
        public final ArrayList<Reference<Phase, Name>> arguments;
        public final Types.Type<Phase> returnType;
        public final ArrayList<Statement<Type, Phase, Name>> body;
        
        public Def(Location loc, Name name, ArrayList<Reference<Phase, Name>> arguments, Types.Type<Phase> returnType, ArrayList<Statement<Type, Phase, Name>> body) {
            super(loc, DeclarationTag.Def);
            this.name = name;
            this.arguments = arguments;
            this.returnType = returnType;
            this.body = body;
        }
        
        public String toString() {
            String args = "";
            for (Reference<Phase, Name> r: arguments) {
                args += r.toString();
            }
            String bodyString = "";
            for (Statement<Type, Phase, Name> s: body) {
                bodyString += s.toString();
            }
            return "(Def " + loc.toString() + " " + name + " " + args + " : " + returnType.toString() + "{" + bodyString + "})";
        }
    }
    
    public static final class GlobalLet<Type, Phase, Name> extends Declaration<Type, Phase, Name> {
        
        public final ArrayList<Binding<Type, Phase, Name>> bindings;
        
        public GlobalLet(Location loc, ArrayList<Binding<Type, Phase, Name>> bindings) {
            super(loc, DeclarationTag.GlobalLet);
            this.bindings = bindings;
        }
        
        public String toString() {
            String bs = "";
            for (Binding<Type, Phase, Name> b: bindings) {
                bs += b.toString();
            }
            return "(GlobalLet " + loc.toString() + " " + bs + ")";
        }
    }
    
    
    public abstract static class Expression<Type, Phase, Name> {
        
        public final Location loc;
        public final Type type;
        public final ExpressionTag tag;
        
        public Expression(Location loc, Type type, ExpressionTag tag) {
            this.loc = loc;
            this.type = type;
            this.tag = tag;
        }
    }

    public static final class AddressOf<Type, Phase, Name> extends Expression<Type, Phase, Name> {
        
        public final Expression<Type, Phase, Name> value;
        
        public AddressOf(Location loc, Type type, Expression<Type, Phase, Name> value) {
            super(loc, type, ExpressionTag.AddressOf);
            this.value = value;
        }
        
        public String toString() {
            return "(& " + loc.toString() + " " + value.toString() + ")";
        }
    }
    
    public static final class Apply<Type, Phase, Name> extends Expression<Type, Phase, Name> {
        
        public final Expression<Type, Phase, Name> function;
        public final Expression<Type, Phase, Name> argument;
        
        public Apply(Location loc, Type type, Expression<Type, Phase, Name> function, Expression<Type, Phase, Name> argument) {
            super(loc, type, ExpressionTag.Apply);
            this.function = function;
            this.argument = argument;
        }
        
        public String toString() {
            return "(A " + loc.toString() + " " + function.toString() + " $ " + argument.toString() + ")";
        }
    }
    
    public static final class AsType<Type, Phase, Name> extends Expression<Type, Phase, Name> {
        
        public final Expression<Type, Phase, Name> value;
        public final Types.Type<Phase> asType;
        
        public AsType(Location loc, Type type, Expression<Type, Phase, Name> value, Types.Type<Phase> asType) {
            super(loc, type, ExpressionTag.AsType);
            this.value = value;
            this.asType = asType;
        }
        
        public String toString() {
            return "(AsType " + loc.toString() + " " + value.toString() + " : " + asType.toString() + ")";
        }
    }
    
    public static final class BufferLiteral<Type, Phase, Name> extends Expression<Type, Phase, Name> {
        
        public final ArrayList<Expression<Type, Phase, Name>> es;
        
        public BufferLiteral(Location loc, Type type, ArrayList<Expression<Type, Phase, Name>> es) {
            super(loc, type, ExpressionTag.BufferLiteral);
            this.es = es;
        }
        
        public String toString() {
            String ess = "";
            for (Expression<Type, Phase, Name> e: es) {
                ess += e.toString();
            }
            return "[" + loc.toString() + " " + ess + "]";
        }
    }
    
    public static final class IfE<Type, Phase, Name> extends Expression<Type, Phase, Name> {
        
        public final Expression<Type, Phase, Name> cond;
        public final Expression<Type, Phase, Name> trueBranch;
        public final Expression<Type, Phase, Name> falseBranch;
        
        public IfE(Location loc, Type type, Expression<Type, Phase, Name> cond, Expression<Type, Phase, Name> trueBranch, Expression<Type, Phase, Name> falseBranch) {
            super(loc, type, ExpressionTag.IfE);
            this.cond = cond;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }
        
        public String toString() {
            return "(IfE " + loc.toString() + " " + cond.toString() + " " + trueBranch.toString() + " " + falseBranch.toString() + ")";
        }
    }
    
    public static final class Index<Type, Phase, Name> extends Expression<Type, Phase, Name> {
        
        public final Expression<Type, Phase, Name> base;
        public final Expression<Type, Phase, Name> offset;
        
        public Index(Location loc, Type type, Expression<Type, Phase, Name> base, Expression<Type, Phase, Name> offset) {
            super(loc, type, ExpressionTag.Index);
            this.base = base;
            this.offset = offset;
        }
        
        public String toString() {
            return "(Index " + loc.toString() + " " + base.toString() + " " + offset.toString() + ")";
        }
    }
    
    public static final class Number<Type, Phase, Name> extends Expression<Type, Phase, Name> {
        
        public final String number;
        
        public Number(Location loc, Type type, String number) {
            super(loc, type, ExpressionTag.Number);
            this.number = number;
        }
        
        public boolean isPositive() {
            return number.charAt(0) != '-' && !(number.equals("0"));
        }
        
        public String toString() {
            return "(" + loc.toString() + " {" + number + "})";
        }
    }
    
    public static final class Tuple<Type, Phase, Name> extends Expression<Type, Phase, Name> {
        
        public final ArrayList<Expression<Type, Phase, Name>> es;
        
        public Tuple(Location loc, Type type, ArrayList<Expression<Type, Phase, Name>> es) {
            super(loc, type, ExpressionTag.Tuple);
            this.es = es;
        }
        
        public String toString() {
            String ess = "";
            for (Expression<Type, Phase, Name> e: es) {
                ess += e.toString();
            }
            return "(Tuple " + loc.toString() + " " + ess + ")";
        }
    }
    
    public static final class ValueAt<Type, Phase, Name> extends Expression<Type, Phase, Name> {
        
        public final Expression<Type, Phase, Name> value;
        
        public ValueAt(Location loc, Type type, Expression<Type, Phase, Name> value) {
            super(loc, type, ExpressionTag.ValueAt);
            this.value = value;
        }
        
        public String toString() {
            return "(* " + loc.toString() + " " + value.toString() + ")";
        }
    }
    
    public static final class Variable<Type, Phase, Name> extends Expression<Type, Phase, Name> {
        
        public final Name name;
        
        public Variable(Location loc, Type type, Name name) {
            super(loc, type, ExpressionTag.Variable);
            this.name = name;
        }
        
        public String toString() {
            return "(Var " + loc.toString() + " {" + name + "})";
        }
    }
    
    
    public static enum LValueTag { Direct, IndexL, Indirect, TupleL };
    
    public abstract static class LValue<Type, Phase, Name>{
        
        public final Location loc;
        public final Type type;
        public final LValueTag tag;
        
        public LValue(Location loc, Type type, LValueTag tag) {
            this.loc = loc;
            this.type = type;
            this.tag = tag;
        }
    }
    
    public static final class Direct<Type, Phase, Name> extends LValue<Type, Phase, Name> {
        
        public final Name name;
        
        public Direct(Location loc, Type type, Name name) {
            super(loc, type, LValueTag.Direct);
            this.name = name;
        }
        
        public String toString() {
            return "(Direct " + loc.toString() + " {" + name + "})";
        }
    }
    
    public static final class IndexL<Type, Phase, N> extends LValue<Type, Phase, N>{
        
        public final LValue<Type, Phase, N> base;
        public final Expression<Type, Phase, N> offset;
        
        public IndexL(Location loc, Type type, LValue<Type, Phase, N> base, Expression<Type, Phase, N> offset) {
            super(loc, type, LValueTag.IndexL);
            this.base = base;
            this.offset = offset;
        }
        
        public String toString() {
            return "(IndexL " + loc.toString() + " " + base.toString() + "[" + offset.toString() + "])";
        }
    }
    
    public static final class Indirect<Type, Phase, Name> extends LValue<Type, Phase, Name> {
        
        public final Expression<Type, Phase, Name> address;
        
        public Indirect(Location loc, Type type, Expression<Type, Phase, Name> address) {
            super(loc, type, LValueTag.Indirect);
            this.address = address;
        }
        
        public String toString() {
            return "(Indirect " + loc.toString() + " " + address.toString() + ")";
        }
    }
    
    public static final class TupleL<Type, Phase, Name> extends LValue<Type, Phase, Name> {
        
        public final ArrayList<LValue<Type, Phase, Name>> lValues;
        
        public TupleL(Location loc, Type type, ArrayList<LValue<Type, Phase, Name>> lValues) {
            super(loc, type, LValueTag.TupleL);
            this.lValues = lValues;
        }
        
        public String toString() {
            String lvs = "";
            for (LValue<Type, Phase, Name> l: lValues) {
                lvs += l.toString();
            }
            return "(TupleL " + loc.toString() + " " + lvs + ")";
        }
    }
    
    
    public static enum StatementTag { Assign, Break, Call, Forever, IfS, Let, Nested, Return }
    
    public abstract static class Statement<Type, Phase, Name> {
        
        public final Location loc;
        public final StatementTag tag;
        
        public Statement(Location loc, StatementTag tag) {
            this.loc = loc;
            this.tag = tag;
        }
    }
    
    public static final class Assign<Type, Phase, Name> extends Statement<Type, Phase, Name> {
        
        public LValue<Type, Phase, Name> lValue;
        public Expression<Type, Phase, Name> value;
        
        public Assign(Location loc, LValue<Type, Phase, Name> lValue, Expression<Type, Phase, Name> value) {
            super(loc, StatementTag.Assign);
            this.lValue = lValue;
            this.value = value;
        }
        
        public String toString() {
            return "(Assign " + loc.toString() + " " + lValue.toString() + " " + value.toString() + ")";
        }
    }
    
    public static final class Break<Type, Phase, Name> extends Statement<Type, Phase, Name> {
        
        public final int depth;
        
        public Break(Location loc, int depth) {
            super(loc, StatementTag.Break);
            this.depth = depth;
        }
        
        public String toString() {
            return "(Break " + loc.toString() + " " + depth + ")";
        }
    }
    
    public static final class Call<Type, Phase, Name> extends Statement<Type, Phase, Name> {
        
        public final Expression<Type, Phase, Name> function;
        public final Expression<Type, Phase, Name> argument;
        
        public Call(Location loc, Expression<Type, Phase, Name> function, Expression<Type, Phase, Name> argument) {
            super(loc, StatementTag.Call);
            this.function = function;
            this.argument = argument;
        }
        
        public String toString() {
            return "(Call " + loc.toString() + " " + function.toString() + " " + argument.toString() + ")";
        }
    }
    
    public static final class Forever<Type, Phase, Name> extends Statement<Type, Phase, Name> {
        
        public final String label;
        public final ArrayList<Statement<Type, Phase, Name>> body;
        
        public Forever(Location loc, String label, ArrayList<Statement<Type, Phase, Name>> block) {
            super(loc, StatementTag.Forever);
            this.label = label;
            this.body = block;
        }
        
        public String toString() {
            String bs = "";
            for (Statement<Type, Phase, Name> b: body) {
                bs += b.toString();
            }
            return "(Forever " + loc.toString() + " {" + label + "} "  + bs + ")";
        }
    }
    
    public static final class IfS<Type, Phase, Name> extends Statement<Type, Phase, Name> {
        
        public final Expression<Type, Phase, Name> cond;
        public final ArrayList<Statement<Type, Phase, Name>> trueBranch;
        public final ArrayList<Statement<Type, Phase, Name>> falseBranch;
        
        public IfS(Location loc, Expression<Type, Phase, Name> cond, ArrayList<Statement<Type, Phase, Name>> trueBranch, ArrayList<Statement<Type, Phase, Name>> falseBranch) {
            super(loc, StatementTag.IfS);
            this.cond = cond;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }
        
        public String toString() {
            String ts = "";
            for (Statement<Type, Phase, Name> t: trueBranch) {
                ts += t.toString();
            }
            String fs = "";
            for (Statement<Type, Phase, Name> f: falseBranch) {
                fs += f.toString();
            }
            return "(IfS " + loc.toString() + " " + cond.toString() + " " + ts + " " + fs + ")";
        }
    }
    
    public static final class Let<Type, Phase, Name> extends Statement<Type, Phase, Name> {
        
        public final ArrayList<Binding<Type, Phase, Name>> bindings;
        
        public Let(Location loc, ArrayList<Binding<Type, Phase, Name>> bindings) {
            super(loc, StatementTag.Let);
            this.bindings = bindings;
        }
        
        public String toString() {
            String bs = "";
            for (Binding<Type, Phase, Name> b: bindings) {
                bs += b.toString();
            }
            return "(Let " + loc.toString() + " " + bs + ")";
        }
    }
    
    public static final class Nested<Type, Phase, Name> extends Statement<Type, Phase, Name> {
        
        public final ArrayList<Statement<Type, Phase, Name>> body;
        
        public Nested(Location loc, ArrayList<Statement<Type, Phase, Name>> statements) {
            super(loc, StatementTag.Nested);
            this.body = statements;
        }
        
        public String toString() {
            String ss = "";
            for (Statement<Type, Phase, Name> s: body) {
                ss += s.toString();
            }
            return "(Nested " + loc.toString() + " " + ss + ")";
        }
    }
    
    public static final class Return<Type, Phase, Name> extends Statement<Type, Phase, Name> {
        
        public Expression<Type, Phase, Name> value;
        
        public Return(Location loc, Expression<Type, Phase, Name> value) {
            super(loc, StatementTag.Return);
            this.value = value;
        }
        
        public String toString() {
            return "(Return " + loc.toString() + " " + value.toString() + ")";
        }
    }
}
