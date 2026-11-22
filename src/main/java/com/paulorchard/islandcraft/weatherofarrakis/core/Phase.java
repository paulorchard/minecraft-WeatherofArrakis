package com.paulorchard.islandcraft.weatherofarrakis.core;

/** Coriolis phase of one level. MAJOR_RESET and MAJOR_WAIT belong to the Major storm (Part 6). */
public enum Phase {
    CALM, APPROACH, STORM, CLEARING, MAJOR_RESET, MAJOR_WAIT;

    public boolean isActiveStorm() {
        return this == APPROACH || this == STORM || this == CLEARING;
    }
}
