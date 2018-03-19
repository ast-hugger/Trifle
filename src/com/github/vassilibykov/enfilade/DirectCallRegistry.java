package com.github.vassilibykov.enfilade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectCallRegistry {

    public static final DirectCallRegistry INSTANCE = new DirectCallRegistry();

    private final List<Function> functionsById = new ArrayList<>();
    private final Map<Function, Integer> functionIds = new HashMap<>();

    public synchronized Integer lookup(Function function) {
        Integer id = functionIds.get(function);
        if (id != null) {
            return id;
        } else {
            int newId = functionsById.size();
            functionsById.add(function);
            functionIds.put(function, newId);
            return newId;
        }
    }

    public synchronized Function lookup(int id) {
        try {
            return functionsById.get(id);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
