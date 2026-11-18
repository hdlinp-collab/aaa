package com.example.trickcalc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * 計算核心（純 Java，不依賴 Android，方便單元測試）。
 *
 * evaluate()  —— 正確計算算式。
 * calculate() —— 依照 wrongRate 機率，「部分」結果會被故意改錯。
 *                同一個算式每次都得到同樣的（錯誤）答案，看起來更像真的。
 */
public final class Calculator {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final int MAX_SIGNIFICANT = 12;

    /** 答錯的機率 (0.0 ~ 1.0)。 */
    private final double wrongRate;

    public Calculator(double wrongRate) {
        this.wrongRate = wrongRate;
    }

    /** 計算結果：value 為顯示的字串，tampered 表示是否被改錯。 */
    public static final class Result {
        public final String value;
        public final String correctValue;
        public final boolean tampered;

        Result(String value, String correctValue, boolean tampered) {
            this.value = value;
            this.correctValue = correctValue;
            this.tampered = tampered;
        }
    }

    /** 計算並（可能）故意給出錯誤答案。honest=true 時永遠正確。 */
    public Result calculate(String expression, boolean honest) {
        BigDecimal correct = evaluate(expression);
        String correctText = format(correct);
        if (honest || !shouldLie(correct) || correctText.contains("e")) {
            return new Result(correctText, correctText, false);
        }
        // 用算式本身當種子：同一題永遠錯成同一個答案
        Random rnd = new Random(normalize(expression).hashCode() * 31L + 7);
        if (rnd.nextDouble() >= wrongRate) {
            return new Result(correctText, correctText, false);
        }
        String wrong = corrupt(correct, rnd);
        return new Result(wrong, correctText, !wrong.equals(correctText));
    }

    /** 太簡單的答案（例如 1+1）就不改，免得一下就被看穿。 */
    private static boolean shouldLie(BigDecimal value) {
        return value.abs().compareTo(BigDecimal.TEN) >= 0;
    }

    private static String corrupt(BigDecimal correct, Random rnd) {
        String text = format(correct);
        switch (rnd.nextInt(3)) {
            case 0: { // 差一點點：最後一位 ±1~3
                BigDecimal unit = BigDecimal.ONE.movePointLeft(Math.max(0, correct.stripTrailingZeros().scale()));
                int delta = (rnd.nextInt(3) + 1) * (rnd.nextBoolean() ? 1 : -1);
                String r = format(correct.add(unit.multiply(BigDecimal.valueOf(delta))));
                if (!r.equals(text)) return r;
                break;
            }
            case 1: { // 相鄰兩位數字對調
                String r = swapAdjacentDigits(text, rnd);
                if (!r.equals(text)) return r;
                break;
            }
            default:
                break;
        }
        // 改掉其中一位數字（保底一定會不同）
        return changeOneDigit(text, rnd);
    }

    private static String swapAdjacentDigits(String text, Random rnd) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i + 1 < text.length(); i++) {
            char a = text.charAt(i), b = text.charAt(i + 1);
            boolean leadingZero = (i == 0 || text.charAt(i - 1) == '-') && b == '0';
            if (Character.isDigit(a) && Character.isDigit(b) && a != b && !leadingZero) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) return text;
        int i = candidates.get(rnd.nextInt(candidates.size()));
        char[] c = text.toCharArray();
        char t = c[i];
        c[i] = c[i + 1];
        c[i + 1] = t;
        return new String(c);
    }

    private static String changeOneDigit(String text, Random rnd) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) positions.add(i);
        }
        // 優先改後面的位數，比較不容易被發現
        int i = positions.get(positions.size() - 1 - rnd.nextInt(Math.min(3, positions.size())));
        char[] c = text.toCharArray();
        int old = c[i] - '0';
        int digit = (old + 1 + rnd.nextInt(9)) % 10;
        boolean isLeading = i == 0 || c[i - 1] == '-';
        if (isLeading && digit == 0 && positions.size() > 1) digit = old == 9 ? 8 : old + 1;
        c[i] = (char) ('0' + digit);
        return new String(c);
    }

    // ------------------------------------------------------------------
    // 正確的算式解析（四則運算優先順序、小數、負號、百分比）
    // ------------------------------------------------------------------

    public static String normalize(String expr) {
        return expr.replace('×', '*').replace('÷', '/').replace('−', '-').replace(" ", "");
    }

    public static BigDecimal evaluate(String expression) {
        String s = normalize(expression);
        if (s.isEmpty()) throw new IllegalArgumentException("empty");
        Deque<BigDecimal> values = new ArrayDeque<>();
        Deque<Character> ops = new ArrayDeque<>();
        boolean expectNumber = true;
        int i = 0;
        while (i < s.length()) {
            char ch = s.charAt(i);
            if (expectNumber) {
                boolean negative = false;
                while (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
                    if (s.charAt(i) == '-') negative = !negative;
                    i++;
                }
                int start = i;
                while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
                if (start == i) throw new IllegalArgumentException("number expected");
                BigDecimal num = new BigDecimal(s.substring(start, i));
                while (i < s.length() && s.charAt(i) == '%') {
                    num = num.movePointLeft(2);
                    i++;
                }
                values.push(negative ? num.negate() : num);
                expectNumber = false;
            } else {
                if ("+-*/".indexOf(ch) < 0) throw new IllegalArgumentException("bad char " + ch);
                while (!ops.isEmpty() && prec(ops.peek()) >= prec(ch)) apply(values, ops.pop());
                ops.push(ch);
                expectNumber = true;
                i++;
            }
        }
        if (expectNumber) throw new IllegalArgumentException("incomplete");
        while (!ops.isEmpty()) apply(values, ops.pop());
        return values.pop();
    }

    private static int prec(char op) {
        return (op == '*' || op == '/') ? 2 : 1;
    }

    private static void apply(Deque<BigDecimal> values, char op) {
        BigDecimal b = values.pop(), a = values.pop();
        switch (op) {
            case '+': values.push(a.add(b, MC)); break;
            case '-': values.push(a.subtract(b, MC)); break;
            case '*': values.push(a.multiply(b, MC)); break;
            default:
                if (b.signum() == 0) throw new ArithmeticException("divide by zero");
                values.push(a.divide(b, MC));
        }
    }

    public static String format(BigDecimal v) {
        BigDecimal r = v.round(new MathContext(MAX_SIGNIFICANT, RoundingMode.HALF_UP)).stripTrailingZeros();
        if (r.signum() == 0) return "0";
        if (r.abs().compareTo(new BigDecimal("1e15")) >= 0 || r.abs().compareTo(new BigDecimal("1e-9")) < 0) {
            return r.toString().replace("E+", "e").replace("E", "e");
        }
        return r.toPlainString();
    }
}
