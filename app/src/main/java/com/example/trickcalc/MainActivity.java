package com.example.trickcalc;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    /** 約 35% 的（非簡單）算式會得到錯誤答案。 */
    private static final double WRONG_RATE = 0.35;

    private final Calculator calculator = new Calculator(WRONG_RATE);
    private final StringBuilder expr = new StringBuilder();

    private TextView exprView;
    private TextView resultView;
    private View honestDot;

    /** 隱藏開關：長按「=」切換誠實模式（右上角會出現一個小點）。 */
    private boolean honest = false;
    private boolean justEvaluated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        exprView = findViewById(R.id.expression);
        resultView = findViewById(R.id.result);
        honestDot = findViewById(R.id.honest_dot);

        int[] inputIds = {
                R.id.b0, R.id.b1, R.id.b2, R.id.b3, R.id.b4, R.id.b5, R.id.b6, R.id.b7, R.id.b8, R.id.b9,
                R.id.bDot, R.id.bAdd, R.id.bSub, R.id.bMul, R.id.bDiv, R.id.bPercent
        };
        for (int id : inputIds) {
            Button b = findViewById(id);
            b.setOnClickListener(v -> append(((Button) v).getText().toString()));
        }
        findViewById(R.id.bClear).setOnClickListener(v -> clear());
        findViewById(R.id.bDel).setOnClickListener(v -> backspace());
        View eq = findViewById(R.id.bEq);
        eq.setOnClickListener(v -> equals());
        eq.setOnLongClickListener(v -> {
            honest = !honest;
            honestDot.setVisibility(honest ? View.VISIBLE : View.INVISIBLE);
            return true;
        });

        if (savedInstanceState != null) {
            expr.append(savedInstanceState.getString("expr", ""));
            honest = savedInstanceState.getBoolean("honest", false);
            resultView.setText(savedInstanceState.getString("result", ""));
            honestDot.setVisibility(honest ? View.VISIBLE : View.INVISIBLE);
        }
        refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("expr", expr.toString());
        out.putString("result", resultView.getText().toString());
        out.putBoolean("honest", honest);
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '−' || c == '×' || c == '÷';
    }

    private void append(String key) {
        char k = key.charAt(0);
        if (justEvaluated) {
            justEvaluated = false;
            String last = resultView.getText().toString();
            expr.setLength(0);
            // 接著上一個答案繼續算
            if ((isOperator(k) || k == '%') && !last.isEmpty() && !last.equals(getString(R.string.error))) {
                expr.append(last.replace('-', '−'));
            }
        }
        char prev = expr.length() > 0 ? expr.charAt(expr.length() - 1) : 0;

        if (isOperator(k)) {
            if (expr.length() == 0) {
                if (k == '−') expr.append(k); // 允許負號開頭
                refresh();
                return;
            }
            if (isOperator(prev)) {
                // 「×−」可以表示乘以負數，其他情況取代原本的運算子
                if (k == '−' && (prev == '×' || prev == '÷')) expr.append(k);
                else if (expr.length() > 1 || prev != '−') {
                    while (expr.length() > 0 && isOperator(expr.charAt(expr.length() - 1))) {
                        expr.setLength(expr.length() - 1);
                    }
                    if (expr.length() > 0) expr.append(k);
                }
            } else if (prev != '.') {
                expr.append(k);
            }
        } else if (k == '.') {
            String cur = currentNumber();
            if (cur.contains(".") || prev == '%') return;
            if (cur.isEmpty()) expr.append('0');
            expr.append('.');
        } else if (k == '%') {
            if (Character.isDigit(prev)) expr.append('%');
        } else { // 數字
            if (prev == '%') return;
            String cur = currentNumber();
            if (cur.equals("0")) expr.setLength(expr.length() - 1);
            expr.append(k);
        }
        refresh();
    }

    private String currentNumber() {
        int i = expr.length();
        while (i > 0 && (Character.isDigit(expr.charAt(i - 1)) || expr.charAt(i - 1) == '.')) i--;
        return expr.substring(i);
    }

    private void backspace() {
        justEvaluated = false;
        if (expr.length() > 0) expr.setLength(expr.length() - 1);
        refresh();
    }

    private void clear() {
        justEvaluated = false;
        expr.setLength(0);
        resultView.setText("");
        refresh();
    }

    private void equals() {
        if (expr.length() == 0) return;
        try {
            Calculator.Result r = calculator.calculate(expr.toString(), honest);
            resultView.setText(r.value);
            justEvaluated = true;
        } catch (ArithmeticException e) {
            resultView.setText(R.string.error);
            justEvaluated = true;
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.incomplete, Toast.LENGTH_SHORT).show();
        }
    }

    private void refresh() {
        exprView.setText(expr.length() == 0 ? "0" : expr);
    }
}
