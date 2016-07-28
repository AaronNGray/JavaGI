import java.util.*;

interface Intersect [Shape1, Shape2] {
    receiver Shape1 {
        boolean intersect(Shape2 that);
    }
}

class Shape {}
class Rectangle extends Shape {}
class Circle extends Shape {}

implementation Intersect [Shape, Shape] {
    receiver Shape {
        boolean intersect (Shape that) {
            System.out.println ("[Shape, Shape]");
            return true;
        }
    }
}

implementation Intersect [Rectangle, Rectangle] {
    receiver Rectangle {
        boolean intersect (Rectangle that) {
            System.out.println ("[Rectangle, Rectangle]");
            return true;
        }
    }
}

implementation Intersect [Rectangle, Circle] {
    receiver Rectangle {
        boolean intersect (Circle that) {
            System.out.println ("[Rectangle, Circle]");
            return true;
        }
    }
}

implementation Intersect [Circle, Rectangle] {
    receiver Circle {
        boolean intersect (Rectangle r) {
            return r.intersect(this);
        }
    }
}

public class ShapeTest {

    public static void main(String[] args) {
        new ShapeTest().runTest();
    }

    static <X,Y> void foo(X x, Y y) where X*Y implements Intersect {
        System.out.println(x.intersect(y));
    }

    static <X> void bar(List<X> list) where X*X implements Intersect {
        for (int i = 0; i < list.size() - 1; i++) {
            X x1 = list.get(i);
            X x2 = list.get(i+1);
            foo(x1, x2);
        }
    }

    public void runTest() {
        Shape s1 = new Circle();
        Shape s2 = new Rectangle();
        Shape s3 = new Circle();
        Shape s4 = new Shape () ;

	s1.intersect (s1); // [Shape, Shape]
	s1.intersect (s2); // [Rectangle, Circle]
	s1.intersect (s4); // [Shape, Shape]
	s2.intersect (s2); // [Rectangle, Rectangle]
	s2.intersect (s3); // [Rectangle, Circle]
	s3.intersect (s4); // [Shape, Shape]
	s4.intersect (s1); // [Shape, Shape]
	s4.intersect (s2); // [Shape, Shape]
	s4.intersect (s4); // [Shape, Shape]

        System.out.println("---");

        foo(s1, s2); // [Rectangle, Circle]
                     // true
        foo(s3, s4); // [Shape, Shape]
                     // true
        
        List<Shape> list = new ArrayList<Shape>();
        list.add(s1);
        list.add(s2);
        list.add(s3);
        list.add(s4);

        System.out.println("---");

        bar(list); /*
                    * [Rectangle, Circle]
                    * true
                    * [Rectangle, Circle]
                    * true
                    * [Shape, Shape]
                    * true
                    */

        System.out.println("---");

        bar(Arrays.asList(s1, s2, s3, s4));
    }
}
