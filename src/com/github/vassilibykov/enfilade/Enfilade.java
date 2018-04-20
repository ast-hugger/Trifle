// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade;

import com.github.vassilibykov.enfilade.core.Callable;

import java.util.ArrayList;
import java.util.List;

public class Enfilade {
    public static final Enfilade INSTANCE = new Enfilade();

    private final List<Callable> callables = new ArrayList<>();

//    public Callable findCallable(int id) {
//        synchronized (callables) {
//            return callables.get(id);
//        }
//    }
//
//    public int addCallable(Callable callable) {
//        synchronized (callables) {
//            var id = callables.size();
//            callables.add(callable);
//            return callables.size() - 1;
//        }
//    }
}
