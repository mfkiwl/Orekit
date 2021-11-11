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

package org.orekit.forces.maneuvers.triggers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.trigger.AbstractManeuverTriggers;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/**
 * Tests the functionality of starting a propagation in between and on start and end times
 * of a Constant Thrust Maneuver
 *
 * @author Greg Carbott
 */
public abstract class AbstractManeuverTriggersInitializationTest<T extends AbstractManeuverTriggers> {

    protected abstract T createTrigger(AbsoluteDate start, AbsoluteDate stop);

    private NumericalPropagator propagator;
    private AbsoluteDate startDate;
    private SpacecraftState initialState;
    /** Propagation duration in seconds */
    private double propDuration = 1200.;

    /** Maneuver duration in seconds */
    private double duration = 60.;

    /** Maneuver Thrust Force in Newtons */
    private double thrust = 1e3;

    /** Maneuver Specific Impulse in seconds */
    private double isp = 100.;

    /** Mass of Spacecraft in kilograms */
    private double mass = 2e3;

    /** Tolerance of deltaV (m/s) difference */
    private double dvTolerance = 3e-6;

    /** Tolerance of mass (kg) difference */
    private double massTolerance = 5e-6;

    /** Direction of Maneuver */
    private Vector3D direction = new Vector3D(1, 0, 1);

    /** Control Items */
    private double deltaVControlFullForward;
    private double massControlFullForward;
    private double massControlHalfForward;
    private double deltaVControlFullReverse;
    private double deltaVControlHalfReverse;
    private double massControlFullReverse;
    private double massControlHalfReverse;

    /** trigger dates. */
    private AbsoluteDate triggerStart;
    private AbsoluteDate triggerStop;

    private T configureTrigger(final AbsoluteDate start, final AbsoluteDate stop) {
        T trigger = createTrigger(start, stop);
        trigger.addObserver((state, isStart) -> {
            if (isStart) {
                triggerStart = state.getDate();
            } else {
                triggerStop  = state.getDate();
            }
        });
        return trigger;
    }

    /** set orekit data */
    @BeforeClass
    public static void setUpBefore() {
        Utils.setDataRoot("regular-data");
    }

    @Before
    public void setUp() {
        startDate = new AbsoluteDate();
        double a = Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 400e3;
        double e = 0.003;
        double i = (Math.PI / 4);
        double pa = 0.0;
        double raan = 0.0;
        double anomaly = 0.0;
        PositionAngle type = PositionAngle.MEAN;
        Frame frame = FramesFactory.getEME2000();
        double mu = Constants.EGM96_EARTH_MU;
        Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, anomaly,
                type, frame, startDate, mu);
        initialState = new SpacecraftState(orbit, mass);

        //Numerical Propagator
        double minStep = 1.0e-5;
        double maxStep = 1000.0;
        double positionTolerance = 1.0e-4;
        OrbitType propagationType = OrbitType.KEPLERIAN;
        double[][] tolerances =
                NumericalPropagator.tolerances(positionTolerance, orbit, propagationType);
        DormandPrince853Integrator integrator =
                new DormandPrince853Integrator(minStep, maxStep,
                        tolerances[0], tolerances[1]);
        integrator.setInitialStepSize(10.0);
        integrator.setMaxGrowth(2.0);
        //Set up propagator
        propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(propagationType);

        //Control deltaVs and mass changes
        double flowRate = -thrust / (Constants.G0_STANDARD_GRAVITY * isp);
        massControlFullForward = mass + (flowRate * duration);
        deltaVControlFullForward = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(mass / massControlFullForward);

        massControlHalfForward = mass + (flowRate * duration / 2);

        massControlFullReverse = mass - (flowRate * duration);
        deltaVControlFullReverse = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(massControlFullReverse / mass);

        massControlHalfReverse = mass - (flowRate * duration / 2);
        deltaVControlHalfReverse = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(massControlHalfReverse / mass);

        triggerStart = null;
        triggerStop  = null;
    }

    @Test
    public void testInBetween() {
        //Create test Thrust Maneuver
        Maneuver ctm = new Maneuver(null,
                                    configureTrigger(startDate.shiftedBy(-(duration / 2)),
                                                     startDate.shiftedBy( (duration / 2))),
                                    new BasicConstantThrustPropulsionModel(thrust, isp, direction, ""));

        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(propDuration));

        Assert.assertEquals(massControlHalfForward, finalStateTest.getMass(), massTolerance);

        Assert.assertNull(triggerStart);
        Assert.assertEquals(duration / 2, triggerStop.durationFrom(startDate), 1.0e-10);

    }

    @Test
    public void testOnStart() {
        //Create test Thrust Maneuver
        Maneuver ctm = new Maneuver(null,
                                    configureTrigger(startDate,
                                                     startDate.shiftedBy(duration)),
                                    new BasicConstantThrustPropulsionModel(thrust, isp, direction, ""));
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(mass / finalStateTest.getMass());

        Assert.assertEquals(deltaVControlFullForward, deltaVTest, dvTolerance);
        Assert.assertEquals(massControlFullForward, finalStateTest.getMass(), massTolerance);

        Assert.assertEquals(0.0,      triggerStart.durationFrom(startDate), 1.0e-10);
        Assert.assertEquals(duration, triggerStop.durationFrom(startDate),  1.0e-10);

    }

    @Test
    public void testOnEnd() {
        //Create test Thrust Maneuver
        Maneuver ctm = new Maneuver(null,
                                    configureTrigger(startDate.shiftedBy(-duration),
                                                     startDate),
                                    new BasicConstantThrustPropulsionModel(thrust, isp, direction, ""));
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(mass / finalStateTest.getMass());

        Assert.assertTrue(deltaVTest == 0.0);
        Assert.assertTrue(finalStateTest.getMass() == mass);

        Assert.assertNull(triggerStart);
        Assert.assertEquals(0.0, triggerStop.durationFrom(startDate), 1.0e-10);

    }

    @Test
    public void testOnEndReverse() {
        //Create test Thrust Maneuver
        Maneuver ctm = new Maneuver(null,
                                    configureTrigger(startDate.shiftedBy(-duration),
                                                     startDate),
                                    new BasicConstantThrustPropulsionModel(thrust, isp, direction, ""));
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(-propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(finalStateTest.getMass() / mass);

        Assert.assertEquals(deltaVControlFullReverse, deltaVTest, dvTolerance);
        Assert.assertEquals(massControlFullReverse, finalStateTest.getMass(), massTolerance);

        Assert.assertEquals(-duration, triggerStart.durationFrom(startDate), 1.0e-10);
        Assert.assertEquals(0.0,       triggerStop.durationFrom(startDate),  1.0e-10);

    }

    @Test
    public void testOnStartReverse() {
        //Create test Thrust Maneuver
        Maneuver ctm = new Maneuver(null,
                                    configureTrigger(startDate,
                                                     startDate.shiftedBy(duration)),
                                    new BasicConstantThrustPropulsionModel(thrust, isp, direction, ""));
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(-propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(finalStateTest.getMass() / mass);

        Assert.assertTrue(deltaVTest == 0.0);
        Assert.assertTrue(finalStateTest.getMass() == mass);

        Assert.assertEquals(0.0, triggerStart.durationFrom(startDate), 1.0e-10);
        Assert.assertNull(triggerStop);
    }

    @Test
    public void testInBetweenReverse() {
        //Create test Thrust Maneuver
        Maneuver ctm = new Maneuver(null,
                                    configureTrigger(startDate.shiftedBy(-duration / 2),
                                                     startDate.shiftedBy( duration / 2)),
                                    new BasicConstantThrustPropulsionModel(thrust, isp, direction, ""));
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(-propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(finalStateTest.getMass() / mass);

        Assert.assertEquals(deltaVControlHalfReverse, deltaVTest, dvTolerance);
        Assert.assertEquals(massControlHalfReverse, finalStateTest.getMass(), massTolerance);

        Assert.assertEquals(-0.5 * duration, triggerStart.durationFrom(startDate), 1.0e-10);
        Assert.assertNull(triggerStop);

    }

    @Test
    public void testControlForward() {
        //Create test Thrust Maneuver
        Maneuver ctm = new Maneuver(null,
                                    configureTrigger(startDate.shiftedBy(1.0),
                                                     startDate.shiftedBy(1.0 + duration)),
                                    new BasicConstantThrustPropulsionModel(thrust, isp, direction, ""));
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(mass / finalStateTest.getMass());

        Assert.assertEquals(deltaVControlFullForward, deltaVTest, dvTolerance);
        Assert.assertEquals(massControlFullForward, finalStateTest.getMass(), massTolerance);

        Assert.assertEquals(1.0,            triggerStart.durationFrom(startDate), 1.0e-10);
        Assert.assertEquals(1.0 + duration, triggerStop.durationFrom(startDate),  1.0e-10);
    }

    @Test
    public void testControlReverse() {
        //Create test Thrust Maneuver
        Maneuver ctm = new Maneuver(null,
                                    configureTrigger(startDate.shiftedBy(-1.0 - duration),
                                                     startDate.shiftedBy(-1.0)),
                                    new BasicConstantThrustPropulsionModel(thrust, isp, direction, ""));
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(-propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(finalStateTest.getMass() / mass);

        Assert.assertEquals(deltaVControlFullReverse, deltaVTest, dvTolerance);
        Assert.assertEquals(massControlFullReverse, finalStateTest.getMass(), massTolerance);

        Assert.assertEquals(-1.0 - duration, triggerStart.durationFrom(startDate), 1.0e-10);
        Assert.assertEquals(-1.0,            triggerStop.durationFrom(startDate),  1.0e-10);

    }

}
