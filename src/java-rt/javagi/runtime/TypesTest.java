package javagi.runtime;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

class CTest<X> {}
class DTest<X> {}
class BTest<X,Y> extends DTest<X> {}
class ATest<X,Y> extends BTest<Y, String> {}

interface TypesTest {
    TypeVariable<?> x = Types.mkTyvar("X");
    TypeVariable<?> y = Types.mkTyvar("Y");
    Type cx = Types.mkParametric(CTest.class, x);
    Type string = Types.mkParametric(String.class);
    Type integer = Types.mkParametric(Integer.class);
    Type cString = Types.mkParametric(CTest.class, string);
    Type dString = Types.mkParametric(DTest.class, string);
    Type cdString = Types.mkParametric(CTest.class, dString);
    Type dy = Types.mkParametric(DTest.class, y);
    Type ddString = Types.mkParametric(DTest.class, dString);
    Type dx = Types.mkParametric(DTest.class, x);
    Type ccx = Types.mkParametric(CTest.class, cx);
    Type cdx = Types.mkParametric(CTest.class, dx);
    Type arrCx = Types.mkArray(cx);
    Type arrCString = Types.mkArray(cString);
    Type cLowerDx = Types.mkParametric(CTest.class, Types.mkLowerWildcard(dx));
    Type cLowerDString = Types.mkParametric(CTest.class, Types.mkLowerWildcard(dString));
    Type dCLowerDString = Types.mkParametric(DTest.class, cLowerDString);
    Type aIntegerCLowerDX = Types.mkParametric(ATest.class, 
                                               integer,
                                               cLowerDx);
}
