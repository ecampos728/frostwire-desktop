/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.frostwire.httpserver;

/**
 * Parameters that users will not likely need to set
 * but are useful for debugging
 */
public class ServerConfig {

    static int clockTick;

    static int defaultClockTick = 10000; // 10 sec.

    /* These values must be a reasonable multiple of clockTick */
    static long defaultReadTimeout = 20; // 20 sec.
    static long defaultWriteTimeout = 60; // 60 sec.
    static long defaultIdleInterval = 300; // 5 min
    static long defaultSelCacheTimeout = 120; // seconds
    static int defaultMaxIdleConnections = 200;

    static long defaultDrainAmount = 64 * 1024;

    static long readTimeout;
    static long writeTimeout;
    static long idleInterval;
    static long selCacheTimeout;
    static long drainAmount; // max # of bytes to drain from an inputstream
    static int maxIdleConnections;
    static boolean debug = false;

    static {

        idleInterval = defaultIdleInterval * 1000;
        clockTick = defaultClockTick;
        maxIdleConnections = defaultMaxIdleConnections;
        readTimeout = defaultReadTimeout * 1000;
        selCacheTimeout = defaultSelCacheTimeout * 1000;
        writeTimeout = defaultWriteTimeout * 1000;
        drainAmount = defaultDrainAmount;
        debug = false;
    }

    static long getReadTimeout() {
        return readTimeout;
    }

    static long getSelCacheTimeout() {
        return selCacheTimeout;
    }

    static boolean debugEnabled() {
        return debug;
    }

    static long getIdleInterval() {
        return idleInterval;
    }

    static int getClockTick() {
        return clockTick;
    }

    static int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    static long getWriteTimeout() {
        return writeTimeout;
    }

    static long getDrainAmount() {
        return drainAmount;
    }
}
