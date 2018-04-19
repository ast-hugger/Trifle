// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.function.Function;

/**
 * A mutable call site with inline cache management support. An instance counts
 * the number of linked fast path guard-with-tests already established. If their
 * number exceeds {@link #CACHE_LIMIT}, the instance marks itself as megamorphic
 * and discards any previously established cache entries, so future calls go
 * directly to the original dispatch method.
 */
class InlineCachingCallSite extends MutableCallSite {
    static final int CACHE_LIMIT = 2;

    private final MethodHandle dispatch;
    private int cacheSize = 0;

    /**
     * Because the dispatch method needs to be bound to this call site so it can talk
     * to it about the cache status, but the call site initialization needs the dispatch
     * handle in the constructor if it wants to keep it nicely final and immutable, the
     * chicken-and-egg conundrum is unwound by giving the constructor a function which
     * will produce the dispatch method given the call site.
     */
    InlineCachingCallSite(MethodType type, Function<InlineCachingCallSite, MethodHandle> dispatchMaker) {
        super(type);
        dispatch = dispatchMaker.apply(this);
        setTarget(dispatch);
    }

    synchronized boolean isMegamorphic() {
        return cacheSize > CACHE_LIMIT;
    }

    /**
     * Invoked by the dispatch method to establish an inline cache entry. If the
     * site is megamorphic, this method does nothing. The dispatch method should
     * check if the site is already megamorphic before computing the arguments
     * and calling this method. In megamorphic state dispatch becomes the
     * standard handler, and we don't wan't to compute a guard and a guarded
     * path on every call just to have them ignored.
     */
    synchronized void addCacheEntry(MethodHandle guard, MethodHandle guardedPath) {
        if (cacheSize < CACHE_LIMIT) {
            cacheSize++;
            MethodHandle entry = MethodHandles.guardWithTest(guard, guardedPath, getTarget());
            setTarget(entry);
        } else if (cacheSize == CACHE_LIMIT) {
            cacheSize++;
            setTarget(dispatch);
        }
    }
}
