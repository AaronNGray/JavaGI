package javagi.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class ImplementationInfo {

    TypeVariable<?>[] parameters;
    Type interfaceType;
    Type[] implementingTypes;
    Class<?>[] rawImplementingTypes;
    Constraint[] constraints;
    Class<?> dictionaryInterfaceClass;
    int[] dispatchPositions;
    Type[] nonDispatchTypes;
    boolean hasAbstractMethods;
    
    // the following two fields are set in the implementation constructor
    String packageName;
    String explicitName; // null for anonymous implementations
    
    public ImplementationInfo(String[] parameterNames,
                              String interfaceTypeSig,
                              Class<?> dictionaryInterfaceClass,
                              int[] dispatchPositions,
                              String[] implementingTypeSigs,
                              String[] constraintSigs,
                              boolean hasAbstractMethods) {
        parameters = new TypeVariable<?>[parameterNames.length];
        for (int i = 0; i < parameterNames.length; i++) {
            parameters[i] = Types.mkTyvar(parameterNames[i]);
        }
        this.interfaceType = GenericSignatureParser.parseTypeSig(interfaceTypeSig);
        this.dictionaryInterfaceClass = dictionaryInterfaceClass;
        this.dispatchPositions = dispatchPositions;
        this.implementingTypes = new Type[implementingTypeSigs.length];
        for (int i = 0; i < implementingTypeSigs.length; i++) {
            this.implementingTypes[i] = GenericSignatureParser.parseTypeSig(implementingTypeSigs[i]);
        }
        this.constraints = new Constraint[constraintSigs.length];
        for (int i = 0; i < constraintSigs.length; i++) {
            this.constraints[i] = GenericSignatureParser.parseConstraintSig(constraintSigs[i]);
        }
        this.nonDispatchTypes = new Type[this.implementingTypes.length - this.dispatchPositions.length];
        for (int i = 0, k = 0; i < implementingTypes.length; i++) {
            if (Utils.arraySearch(dispatchPositions, i) < 0) {
                this.nonDispatchTypes[k++] = implementingTypes[i];
            }
        }
        this.rawImplementingTypes = new Class<?>[implementingTypes.length];
        for (int i = 0; i < this.rawImplementingTypes.length; i++) {
            this.rawImplementingTypes[i] = getClass(implementingTypes[i]);
        }
        this.hasAbstractMethods = hasAbstractMethods;
    }
    
    Class<?> rawDictionaryInterfaceType() {
        return dictionaryInterfaceClass;
    }

    Class<?>[] rawImplementingTypes() {
        return rawImplementingTypes;
    }

    private Class<?> getClass(Type t) {
        if (t instanceof Class) {
            return (Class<?>) t;
        } else if (t instanceof ParameterizedType) {
            return getClass(((ParameterizedType) t).getRawType());
        } else {
            throw new JavaGIRuntimeBug("Cannot extract class from type " + t + " (class: " + t.getClass());
        }
    }

    public TypeVariable[] getParameters() {
        TypeVariable[] res = new TypeVariable[parameters.length];
        System.arraycopy(this.parameters, 0, res, 0, parameters.length);
        return res;
    }
    
    public Type getInterfaceType() {
        return this.interfaceType;
    }
 
    public String getInterfaceTypeName() {
        Class<?> c = getClass(this.interfaceType);
        return c.getName();
    }
    
    public Type[] getInterfaceTyargs() {
        Type t = this.interfaceType;
        if (t instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) t;
            return p.getActualTypeArguments();
        } else {
            return Types.NO_TYPES;
        }
    }
    
    public Class<?> getInterfaceClass() {
        return getClass(this.interfaceType);
    }
    
    public Type[] getImplementingTypes() {
        Type[] res = new Type[implementingTypes.length];
        System.arraycopy(this.implementingTypes, 0, res, 0, implementingTypes.length);
        return res;       
    }
    
    public Constraint[] getConstraints() {
        Constraint[] res = new Constraint[constraints.length];
        System.arraycopy(this.constraints, 0, res, 0, constraints.length);
        return res;       
    }
 
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("implementation");
        if (this.parameters.length > 0) {
            sb.append('<');
            for (int i = 0; i < this.parameters.length; i++) {
                sb.append(Types.toString(this.parameters[i]));
                if (i != this.parameters.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append('>');
        }
        sb.append(' ');
        sb.append(Types.toString(this.interfaceType));
        sb.append(" [");
        for (int i = 0; i < this.implementingTypes.length; i++) {
            sb.append(Types.toString(implementingTypes[i]));
            if (i != this.implementingTypes.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        if (this.constraints.length > 0) {
            sb.append(" where ");
            for (int i = 0; i < this.constraints.length; i++) {
                sb.append(this.constraints[i].toString());
                if (i != this.constraints.length - 1) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

}
