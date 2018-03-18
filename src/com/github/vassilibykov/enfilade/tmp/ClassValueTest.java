package com.github.vassilibykov.enfilade.tmp;

public class ClassValueTest {
    private static class StringValue extends ClassValue<String> {
        @Override
        protected String computeValue(Class<?> type) {
            return type.getSimpleName();
        }
    }

    public static void main(String[] args) {
        long start = System.nanoTime();
        StringValue v = new StringValue();
        Integer iv = 3;
        Object sv = new Object();
        String s;
        for (int i = 0; i < 1_000_000_000; i++) {
            x(lookup(iv)); // 14ms
            x(lookup(""));
            x(lookup(sv));
//            v.get(iv.getClass()); // 5293ms
//            v.get("".getClass());
//            v.get(sv.getClass());
        }
        long elapsed = System.nanoTime() - start;
        System.out.println("Time: " + elapsed / 1_000_000);
    }

    private static String lookup(Object o) {
        if (o.getClass() == Integer.class) {
            return "Integer";
        } else if (o.getClass() == String.class) {
            return "String";
        } else {
            return "Object";
        }
    }

    private static void x(String x) {

    }
}
