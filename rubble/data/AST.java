package rubble.data;

import java.util.ArrayList;

public final class AST {
    
    public static enum ExpressionTag { AddressOf, Apply, AsType, BufferLiteral, IfE, Index, Number, Tuple, ValueAt, Variable }
    
    public static final class Binding<Type> {
        
        public final Location loc;
        public final ArrayList<Reference> references;
        public final Expression<Type> value;
        
        public Binding(Location loc, ArrayList<Reference> references, Expression<Type> value) {
            this.loc = loc;
            this.references = references;
            this.value = value;
        }
    }
    
    public static final class Reference {
        
        public final String name;
        public final Types.ModalType modalType;
        
        public Reference(String name, Types.ModalType modalType) {
            this.name = name;
            this.modalType = modalType;
        }
    }
    
    
    public static enum DeclarationTag { Def, GlobalLet }
    
    public abstract static class Declaration<Type> {
        
        public Location loc;
        public DeclarationTag tag;
        
        public Declaration(Location loc, DeclarationTag tag) {
            this.loc = loc;
            this.tag = tag;
        }
    }
    
    public static final class Def<Type> extends Declaration<Type> {
        
        public final String name;
        public final ArrayList<Reference> arguments;
        public final Types.Type returnType;
        public final ArrayList<Statement<Type>> body;
        
        public Def(Location loc, String name, ArrayList<Reference> arguments, Types.Type returnType, ArrayList<Statement<Type>> body) {
            super(loc, DeclarationTag.Def);
            this.name = name;
            this.arguments = arguments;
            this.returnType = returnType;
            this.body = body;
        }
    }
    
    public static final class GlobalLet<Type> extends Declaration<Type> {
        
        public final ArrayList<Binding<Type>> bindings;
        
        public GlobalLet(Location loc, ArrayList<Binding<Type>> bindings) {
            super(loc, DeclarationTag.GlobalLet);
            this.bindings = bindings;
        }
    }
    
    
    public abstract static class Expression<Tau> {
        
        public final Location loc;
        public final Tau tau;
        public final ExpressionTag tag;
        
        public Expression(Location loc, Tau tau, ExpressionTag tag) {
            this.loc = loc;
            this.tau = tau;
            this.tag = tag;
        }
    }

    public static final class AddressOf<Tau> extends Expression<Tau> {
        
        Expression<Tau> value;
        
        public AddressOf(Location loc, Tau tau, Expression<Tau> value) {
            super(loc, tau, ExpressionTag.AddressOf);
            this.value = value;
        }
        
    }
    
    public static final class Apply<Tau> extends Expression<Tau> {
        
        public final Expression<Tau> function;
        public final ArrayList<Expression<Tau>> arguments;
        public final boolean untupleArgument;
        
        public Apply(Location loc, Tau tau, Expression<Tau> function, ArrayList<Expression<Tau>> arguments, boolean untupleArgument) {
            super(loc, tau, ExpressionTag.Apply);
            this.function = function;
            this.arguments = arguments;
            this.untupleArgument = untupleArgument;
        }
    }
    
    public static final class AsType<Tau> extends Expression<Tau> {
        
        public final Expression<Tau> value;
        public final Types.Type type;
        
        public AsType(Location loc, Tau tau, Expression<Tau> value, Types.Type type) {
            super(loc, tau, ExpressionTag.AsType);
            this.value = value;
            this.type = type;
        }
    }
    
    public static final class BufferLiteral<Tau> extends Expression<Tau> {
        
        public final ArrayList<Expression<Tau>> es;
        
        public BufferLiteral(Location loc, Tau tau, ArrayList<Expression<Tau>> es) {
            super(loc, tau, ExpressionTag.BufferLiteral);
            this.es = es;
        }
    }
    
    public static final class IfE<Tau> extends Expression<Tau> {
        
        public final Expression<Tau> cond;
        public final Expression<Tau> trueBranch;
        public final Expression<Tau> falseBranch;
        
        public IfE(Location loc, Tau tau, Expression<Tau> cond, Expression<Tau> trueBranch, Expression<Tau> falseBranch) {
            super(loc, tau, ExpressionTag.IfE);
            this.cond = cond;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }
    }
    
    public static final class Index<Tau> extends Expression<Tau> {
        
        public final Expression<Tau> base;
        public final Expression<Tau> offset;
        
        public Index(Location loc, Tau tau, Expression<Tau> base, Expression<Tau> offset) {
            super(loc, tau, ExpressionTag.Index);
            this.base = base;
            this.offset = offset;
        }
    }
    
    public static final class Number<Tau> extends Expression<Tau> {
        
        public final String number;
        
        public Number(Location loc, Tau tau, String number) {
            super(loc, tau, ExpressionTag.Number);
            this.number = number;
        }
        
        public boolean isPositive() {
            return number.charAt(0) != '-' && !(number.equals("0"));
        }
    }
    
    public static final class Tuple<Tau> extends Expression<Tau> {
        
        public final ArrayList<Expression<Tau>> es;
        
        public Tuple(Location loc, Tau tau, ArrayList<Expression<Tau>> es) {
            super(loc, tau, ExpressionTag.Tuple);
            this.es = es;
        }
    }
    
    public static final class ValueAt<Tau> extends Expression<Tau> {
        
        public final Expression<Tau> value;
        
        public ValueAt(Location loc, Tau tau, Expression<Tau> value) {
            super(loc, tau, ExpressionTag.ValueAt);
            this.value = value;
        }
    }
    
    public static final class Variable<Tau> extends Expression<Tau> {
        
        public final String name;
        
        public Variable(Location loc, Tau tau, String name) {
            super(loc, tau, ExpressionTag.Variable);
            this.name = name;
        }
    }
    
    
    public static enum LValueTag { Direct, IndexL, Indirect, TupleL };
    
    public abstract static class LValue<Type>{
        
        public final Location loc;
        public final Type tau;
        public final LValueTag tag;
        
        public LValue(Location loc, Type tau, LValueTag tag) {
            this.loc = loc;
            this.tau = tau;
            this.tag = tag;
        }
    }
    
    public static final class Direct<Type> extends LValue<Type> {
        
        public final String name;
        
        public Direct(Location loc, Type tau, String name) {
            super(loc, tau, LValueTag.Direct);
            this.name = name;
        }
    }
    
    public static final class IndexL<Type> extends LValue<Type>{
        
        public final LValue<Type> base;
        public final Expression<Type> offset;
        
        public IndexL(Location loc, Type tau, LValue<Type> base, Expression<Type> offset) {
            super(loc, tau, LValueTag.IndexL);
            this.base = base;
            this.offset = offset;
        }
    }
    
    public static final class Indirect<Type> extends LValue<Type> {
        
        public final Expression<Type> address;
        
        public Indirect(Location loc, Type tau, Expression<Type> address) {
            super(loc, tau, LValueTag.Indirect);
            this.address = address;
        }
    }
    
    public static final class TupleL<Type> extends LValue<Type> {
        
        public final ArrayList<LValue<Type>> lValues;
        
        public TupleL(Location loc, Type tau, ArrayList<LValue<Type>> lValues) {
            super(loc, tau, LValueTag.TupleL);
            this.lValues = lValues;
        }
    }
    
    
    public static enum StatementTag { Assign, Break, Call, Forever, IfS, Let, Nested, Return }
    
    public abstract static class Statement<Type> {
        
        public final Location loc;
        public final StatementTag tag;
        
        public Statement(Location loc, StatementTag tag) {
            this.loc = loc;
            this.tag = tag;
        }
    }
    
    public static final class Assign<Type> extends Statement<Type> {
        
        public LValue<Type> lValue;
        public Expression<Type> value;
        
        public Assign(Location loc, LValue<Type> lValue, Expression<Type> value) {
            super(loc, StatementTag.Assign);
            this.lValue = lValue;
            this.value = value;
        }
    }
    
    public static final class Break<Type> extends Statement<Type> {
        
        public final int depth;
        
        public Break(Location loc, int depth) {
            super(loc, StatementTag.Break);
            this.depth = depth;
        }
    }
    
    public static final class Call<Type> extends Statement<Type> {
        
        public final Expression<Type> function;
        public final ArrayList<Expression<Type>> arguments;
        
        public Call(Location loc, Expression<Type> function, ArrayList<Expression<Type>> arguments) {
            super(loc, StatementTag.Call);
            this.function = function;
            this.arguments = arguments;
        }
    }
    
    public static final class Forever<Type> extends Statement<Type> {
        
        public final String label;
        public final ArrayList<Statement<Type>> block;
        
        public Forever(Location loc, String label, ArrayList<Statement<Type>> block) {
            super(loc, StatementTag.Forever);
            this.label = label;
            this.block = block;
        }
    }
    
    public static final class IfS<Type> extends Statement<Type> {
        
        public final Expression<Type> cond;
        public final ArrayList<Statement<Type>> trueBranch;
        public final ArrayList<Statement<Type>> falseBranch;
        
        public IfS(Location loc, Expression<Type> cond, ArrayList<Statement<Type>> trueBranch, ArrayList<Statement<Type>> falseBranch) {
            super(loc, StatementTag.IfS);
            this.cond = cond;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }
    }
    
    public static final class Let<Type> extends Statement<Type> {
        
        public final ArrayList<Binding<Type>> bindings;
        
        public Let(Location loc, ArrayList<Binding<Type>> bindings) {
            super(loc, StatementTag.Let);
            this.bindings = bindings;
        }
    }
    
    public static final class Nested<Type> extends Statement<Type> {
        
        public final ArrayList<Statement<Type>> statements;
        
        public Nested(Location loc, ArrayList<Statement<Type>> statements) {
            super(loc, StatementTag.Nested);
            this.statements = statements;
        }
    }
    
    public static final class Return<Type> extends Statement<Type> {
        
        public Expression<Type> value;
        
        public Return(Location loc, Expression<Type> value) {
            super(loc, StatementTag.Return);
            this.value = value;
        }
    }
}
