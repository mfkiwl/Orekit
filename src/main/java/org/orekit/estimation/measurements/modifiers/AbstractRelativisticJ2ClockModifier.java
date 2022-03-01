/* Copyright 2002-2022 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orekit.estimation.measurements.modifiers;

import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

import org.hipparchus.util.FastMath;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.SpacecraftState;

/**
 * Class modifying theoretical measurements with relativistic J2 clock correction.
 * <p>
 * Relativistic clock correction of the effects caused by the oblateness of Earth on
 * the gravity potential.
 * </p>
 * <p>
 * The time delay caused by this effect is computed based on the orbital parameters of the
 * emitter's orbit.
 * </p>
 *
 * @author Louis Aucouturier
 * @since 11.2
 *
 * @see "Teunissen, Peter, and Oliver Montenbruck, eds. Springer handbook of global navigation
 * satellite systems. Chapter 19.2. Equation 19.18 Springer, 2017."
 */
public class AbstractRelativisticJ2ClockModifier {

    /**
     * Relativistic J2 effect constant.
     */
    private final double cJ2;

    /** Simple constructor. */
    public AbstractRelativisticJ2ClockModifier() {
        this.cJ2 = 1.5 * (Constants.WGS84_EARTH_C20) * (Constants.WGS84_EARTH_EQUATORIAL_RADIUS * Constants.WGS84_EARTH_EQUATORIAL_RADIUS) /
                (Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT);
    }

    /**
     * Computes the relativistic J2 clock time delay correction.
     * @param estimated
     * @return dt_relJ2clk
     */
    protected double relativisticJ2Correction(final EstimatedMeasurement<?> estimated) {
        // Extracting the state of the receiver to determine the frame and mu
        final SpacecraftState localState = estimated.getStates()[0];
        final double orbitMu = localState.getMu();
        final Frame localFrame = localState.getFrame();

        // Getting Participants to extract the remote PV
        final TimeStampedPVCoordinates[] pvs = estimated.getParticipants();
        final TimeStampedPVCoordinates pvRemote;

        // Checking if the correction is applied on a two-way GNSS problem
        // In that case the emitter is at index 1, else index 0
        if (pvs.length < 3) {
            pvRemote = pvs[0];
        } else {
            pvRemote = pvs[1];
        }

        // Define a Keplerian orbit to extract the orbital parameters needed to compute the correction
        final KeplerianOrbit remoteOrbit = new KeplerianOrbit(pvRemote, localFrame, orbitMu);
        final double orbitInclination = remoteOrbit.getI();
        final double orbitA = remoteOrbit.getA();

        // u = perigee argument + true anomaly
        final double orbitU = remoteOrbit.getTrueAnomaly() + remoteOrbit.getPerigeeArgument();

        // Returning the value of the time delay
        return cJ2 * FastMath.sqrt(orbitMu / (orbitA * orbitA * orbitA)) * FastMath.sin(2 * orbitU) *
                FastMath.sin(orbitInclination) * FastMath.sin(orbitInclination);
    }

}
