/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm.apm;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for Attitude Parameter Message data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class SpinStabilized extends CommentsContainer {

    /** Frame A. */
    private FrameFacade frameA;

    /** Frame B. */
    private FrameFacade frameB;

    /** Flag for frames direction. */
    private Boolean a2b;

    /** Right ascension of spin axis vector (rad). */
    private double spinAlpha;

    /** Declination of the spin axis vector (rad). */
    private double spinDelta;

    /** Phase of the satellite about the spin axis (rad). */
    private double spinAngle;

    /** Angular velocity of satellite around spin axis (rad/s). */
    private double spinAngleVel;

    /** Nutation angle of spin axis (rad). */
    private double nutation;

    /** Body nutation period of the spin axis (s). */
    private double nutationPer;

    /** Inertial nutation phase (rad). */
    private double nutationPhase;

    /** Simple constructor.
     */
    public SpinStabilized() {
        spinAlpha      = Double.NaN;
        spinDelta      = Double.NaN;
        spinAngle      = Double.NaN;
        spinAngleVel   = Double.NaN;
        nutation       = Double.NaN;
        nutationPer    = Double.NaN;
        nutationPhase  = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        checkNotNull(frameA,      SpinStabilizedKey.SPIN_FRAME_A);
        checkNotNull(frameB,      SpinStabilizedKey.SPIN_FRAME_B);
        checkNotNull(a2b,         SpinStabilizedKey.SPIN_DIR);
        checkNotNaN(spinAlpha,    SpinStabilizedKey.SPIN_ALPHA);
        checkNotNaN(spinDelta,    SpinStabilizedKey.SPIN_DELTA);
        checkNotNaN(spinAngle,    SpinStabilizedKey.SPIN_ANGLE);
        checkNotNaN(spinAngleVel, SpinStabilizedKey.SPIN_ANGLE_VEL);
        if (Double.isNaN(nutation + nutationPer + nutationPhase)) {
            // if at least one is NaN, all must be NaN (i.e. not initialized)
            if (!(Double.isNaN(nutation) && Double.isNaN(nutationPer) && Double.isNaN(nutationPhase))) {
                throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "NUTATION*");
            }
        }
    }

    /** Set frame A.
     * @param frameA frame A
     */
    public void setFrameA(final FrameFacade frameA) {
        this.frameA = frameA;
    }

    /** Get frame A.
     * @return frame A
     */
    public FrameFacade getFrameA() {
        return frameA;
    }

    /** Set frame B.
     * @param frameB frame B
     */
    public void setFrameB(final FrameFacade frameB) {
        this.frameB = frameB;
    }

    /** Get frame B.
     * @return frame B
     */
    public FrameFacade getFrameB() {
        return frameB;
    }

    /** Set rotation direction.
     * @param a2b if true, rotation is from {@link #getFrameA() frame A}
     * to {@link #getFrameB() frame B}
     */
    public void setA2b(final boolean a2b) {
        this.a2b = a2b;
    }

    /** Check if rotation direction is from {@link #getFrameA() frame A} to {@link #getFrameB() frame B}.
     * @return true if rotation direction is from {@link #getFrameA() frame A} to {@link #getFrameB() frame B}
     */
    public boolean isA2b() {
        return a2b == null ? true : a2b;
    }

    /**
     * Get the right ascension of spin axis vector (rad).
     * @return the right ascension of spin axis vector
     */
    public double getSpinAlpha() {
        return spinAlpha;
    }

    /**
     * Set the right ascension of spin axis vector (rad).
     * @param spinAlpha value to be set
     */
    public void setSpinAlpha(final double spinAlpha) {
        refuseFurtherComments();
        this.spinAlpha = spinAlpha;
    }

    /**
     * Get the declination of the spin axis vector (rad).
     * @return the declination of the spin axis vector (rad).
     */
    public double getSpinDelta() {
        return spinDelta;
    }

    /**
     * Set the declination of the spin axis vector (rad).
     * @param spinDelta value to be set
     */
    public void setSpinDelta(final double spinDelta) {
        refuseFurtherComments();
        this.spinDelta = spinDelta;
    }

    /**
     * Get the phase of the satellite about the spin axis (rad).
     * @return the phase of the satellite about the spin axis
     */
    public double getSpinAngle() {
        return spinAngle;
    }

    /**
     * Set the phase of the satellite about the spin axis (rad).
     * @param spinAngle value to be set
     */
    public void setSpinAngle(final double spinAngle) {
        refuseFurtherComments();
        this.spinAngle = spinAngle;
    }

    /**
     * Get the angular velocity of satellite around spin axis (rad/s).
     * @return the angular velocity of satellite around spin axis
     */
    public double getSpinAngleVel() {
        return spinAngleVel;
    }

    /**
     * Set the angular velocity of satellite around spin axis (rad/s).
     * @param spinAngleVel value to be set
     */
    public void setSpinAngleVel(final double spinAngleVel) {
        refuseFurtherComments();
        this.spinAngleVel = spinAngleVel;
    }

    /**
     * Get the nutation angle of spin axis (rad).
     * @return the nutation angle of spin axis
     */
    public double getNutation() {
        return nutation;
    }

    /**
     * Set the nutation angle of spin axis (rad).
     * @param nutation the nutation angle to be set
     */
    public void setNutation(final double nutation) {
        refuseFurtherComments();
        this.nutation = nutation;
    }

    /**
     * Get the body nutation period of the spin axis (s).
     * @return the body nutation period of the spin axis
     */
    public double getNutationPeriod() {
        return nutationPer;
    }

    /**
     * Set the body nutation period of the spin axis (s).
     * @param period the nutation period to be set
     */
    public void setNutationPeriod(final double period) {
        refuseFurtherComments();
        this.nutationPer = period;
    }

    /**
     * Get the inertial nutation phase (rad).
     * @return the inertial nutation phase
     */
    public double getNutationPhase() {
        return nutationPhase;
    }

    /**
     * Set the inertial nutation phase (rad).
     * @param nutationPhase the nutation phase to be set
     */
    public void setNutationPhase(final double nutationPhase) {
        refuseFurtherComments();
        this.nutationPhase = nutationPhase;
    }

}
