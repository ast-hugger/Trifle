// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

public interface Invocable {
    Object invoke();
    Object invoke(Object arg);
    Object invoke(Object arg1, Object arg2);
}
