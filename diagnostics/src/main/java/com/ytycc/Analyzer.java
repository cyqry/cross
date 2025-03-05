package com.ytycc;

public interface Analyzer<T> {
    Result analysis(T obj) throws Exception;

    boolean acceptAnalyzer(Object o);
}
