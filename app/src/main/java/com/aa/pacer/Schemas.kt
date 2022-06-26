package com.aa.pacer

import kotlin.collections.ArrayList

class Schemas()  {
    private var schemas : ArrayList<String> = arrayListOf()

    fun get(): ArrayList<String> {
        return schemas
    }

    // For saving in preferences
    fun getAsMutableSet(): MutableSet<String> {
        return schemas.toMutableSet()
    }

    fun set(s: Set<String>?) {
        if (s != null) {
            schemas.addAll(s)
        }
    }

    fun add(s: String) {
        if (!schemas.contains(s)) {
            schemas.add(s)
        }
    }

    fun add(min: String, sec: String, times: String) {
        var m = min
        var s = sec
        if (m.isEmpty()) m = "0"
        if (s.isEmpty()) s = "0"
        s = s.padStart(2, '0')
        val set = "$m:$s x $times"
        add("$m:$s x $times")
    }

    fun parse(s: String): Triple<String, String, String>  {
        val tokens = s.split("x", ":")
        return Triple(tokens[0], tokens[1].trim(), tokens[2].trim())
    }
}