package com.example.trickcalc;

import static org.junit.Assert.*;

import org.junit.Test;

public class CalculatorTest {

    private static String eval(String e) {
        return Calculator.format(Calculator.evaluate(e));
    }

    @Test
    public void evaluatesCorrectly() {
        assertEquals("2", eval("1+1"));
        assertEquals("14", eval("2+3×4"));
        assertEquals("0.3", eval("0.1+0.2"));
        assertEquals("0.333333333333", eval("1÷3"));
        assertEquals("-6", eval("−2×3"));
        assertEquals("-6", eval("2×−3"));
        assertEquals("25", eval("50%×50"));
    }

    @Test(expected = ArithmeticException.class)
    public void divideByZero() {
        Calculator.evaluate("5÷0");
    }

    @Test
    public void honestModeIsAlwaysCorrect() {
        Calculator c = new Calculator(1.0);
        for (int a = 0; a < 200; a++) {
            String e = a + "×37";
            assertEquals(eval(e), c.calculate(e, true).value);
        }
    }

    @Test
    public void someResultsAreWrongAndStable() {
        Calculator c = new Calculator(0.35);
        int wrong = 0, total = 0;
        for (int a = 10; a < 400; a++) {
            String e = a + "+" + (a * 7);
            Calculator.Result r = c.calculate(e, false);
            assertEquals(r.value, c.calculate(e, false).value); // 同一題答案固定
            assertEquals(eval(e), r.correctValue);
            if (r.tampered) {
                wrong++;
                assertNotEquals(r.correctValue, r.value);
            } else {
                assertEquals(r.correctValue, r.value);
            }
            total++;
        }
        double ratio = wrong / (double) total;
        assertTrue("ratio=" + ratio, ratio > 0.2 && ratio < 0.5);
    }

    @Test
    public void simpleAnswersStayCorrect() {
        Calculator c = new Calculator(1.0);
        assertEquals("2", c.calculate("1+1", false).value);
        assertEquals("9", c.calculate("3×3", false).value);
    }
}
