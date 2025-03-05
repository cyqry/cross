package com.ytycc;


import com.google.common.reflect.TypeToken;

import java.lang.reflect.Field;

public abstract class AbstractAnalyzer<T> implements Analyzer<T> {

    private final TypeToken<T> matcher = new TypeToken<T>(getClass()) {};


    @Override
    public final boolean acceptAnalyzer(Object o) {
        return matcher.getRawType().isInstance(o);
    }



    @Override
    public final Result analysis(T obj) throws Exception {
        return doAnalysis(new ObjectView<>(obj));
    }

    abstract Result doAnalysis(ObjectView<T> view) throws Exception;


    public static class ObjectView<O> {
        private final O inner;

        private final Class<?> innerClass;


        ObjectView(O inner) {
            this.inner = inner;
            this.innerClass = inner.getClass();
        }

        public <F> F getValue(String fieldName) throws NoSuchFieldException, IllegalAccessException {
            Field field = innerClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (F) field.get(inner);
        }
    }
}
