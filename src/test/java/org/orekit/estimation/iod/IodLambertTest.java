/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

package org.orekit.estimation.iod;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

/**
 *
 * Source: http://ccar.colorado.edu/asen5050/projects/projects_2012/kemble/gibbs_derivation.htm
 *
 * @author Joris Olympio
 * @since 7.1
 *
 */
public class IodLambertTest {

    @Test
    public void testLambert() {
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final double mu = context.initialOrbit.getMu();
        final Frame frame = context.initialOrbit.getFrame();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final List<ObservedMeasurement<?>> measurements =
                EstimationTestUtils.createMeasurements(propagator,
                                                       new PVMeasurementCreator(),
                                                       0.0, 1.0, 60.0);

        // measurement data 1
        final int idMeasure1 = 0;
        final AbsoluteDate date1 = measurements.get(idMeasure1).getDate();
        /*final Vector3D stapos1 = context.stations.get(0)  // FIXME we need to access the station of the measurement
                                    .getBaseFrame()
                                    .getPVCoordinates(date1, frame)
                                    .getPosition();*/
        final Vector3D position1 = new Vector3D(
                                                measurements.get(idMeasure1).getObservedValue()[0],
                                                measurements.get(idMeasure1).getObservedValue()[1],
                                                measurements.get(idMeasure1).getObservedValue()[2]);

        // measurement data 2
        final int idMeasure2 = 10;
        final AbsoluteDate date2 = measurements.get(idMeasure2).getDate();
        /*final Vector3D stapos2 = context.stations.get(0)  // FIXME we need to access the station of the measurement
                        .getBaseFrame()
                        .getPVCoordinates(date2, frame)
                        .getPosition();*/
        final Vector3D position2 = new Vector3D(
                                                measurements.get(idMeasure2).getObservedValue()[0],
                                                measurements.get(idMeasure2).getObservedValue()[1],
                                                measurements.get(idMeasure2).getObservedValue()[2]);

        final int nRev = 0;

        // instantiate the IOD method
        final IodLambert iod = new IodLambert(mu);

        final KeplerianOrbit orbit = iod.estimate(frame,
                                            true,
                                            nRev,
                                            /*stapos1.add*/(position1), date1,
                                            /*stapos2.add*/(position2), date2);

        Assert.assertEquals(orbit.getA(), context.initialOrbit.getA(), 1.0e-9 * context.initialOrbit.getA());
        Assert.assertEquals(orbit.getE(), context.initialOrbit.getE(), 1.0e-9 * context.initialOrbit.getE());
        Assert.assertEquals(orbit.getI(), context.initialOrbit.getI(), 1.0e-9 * context.initialOrbit.getI());
    }

    /** Testing IOD Lambert estimation for several orbital periods.
     *  @author Maxime Journot
     */ 
    @Test
    public void testLambert2() {
        
        // Initialize context - "eccentric orbit" built-in test bench context
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final double mu = context.initialOrbit.getMu();
        final Frame frame = context.initialOrbit.getFrame();

        // Use a simple Keplerian propagator (no perturbation)
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create propagator
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        // Save initial state and orbit (ie. reference orbit)
        final SpacecraftState initialState = propagator.getInitialState();
        final KeplerianOrbit refOrbit = new KeplerianOrbit(initialState.getOrbit());
        double[] refOrbitArray = new double[6];
        OrbitType.KEPLERIAN.mapOrbitToArray(refOrbit, PositionAngle.TRUE, refOrbitArray, null);
        final Vector3D position1 = refOrbit.getPVCoordinates().getPosition();
        final AbsoluteDate date1 = refOrbit.getDate();
        
        // Orbit period
        final double T = context.initialOrbit.getKeplerianPeriod();
        
        // Always check the orbit at t0 wrt refOrbit
        // Create a list of samples to treat several cases
        // 0: dt = T/4       - nRev = 0 - posigrade = true
        // 1: dt = T/2       - nRev = 0 - posigrade = true
        // 2: dt = 3T/4      - nRev = 0 - posigrade = false
        // 3: dt = 2T + T/4  - nRev = 2 - posigrade = true
        // 4: dt = 3T + 3T/4 - nRev = 3 - posigrade = false
        final double[] dts = new double[] {
            T/4, T/2, 3*T/4, 2*T + T/4, 3*T + 3*T/4};
        final int[]   nRevs = new int[] {0, 0, 0, 2, 3};
        final boolean[] posigrades = new boolean[] {true, true, false, true, false}; 
        
        for (int i = 0; i < dts.length; i++) {
            // Reset to ref state
            propagator.resetInitialState(initialState);
            
            // Propagate to test date
            final AbsoluteDate date2 = date1.shiftedBy(dts[i]);
            final Vector3D position2 = propagator.propagate(date2).getPVCoordinates().getPosition();
            
            // Instantiate the IOD method
            final IodLambert iod = new IodLambert(mu);

            // Estimate the orbit
            final KeplerianOrbit orbit = iod.estimate(frame, posigrades[i], nRevs[i], position1, date1, position2, date2);
           
            // Test relative values
            final double relTol = 1e-12;
            Assert.assertEquals(refOrbit.getA(),                             orbit.getA(),                             relTol * refOrbit.getA());
            Assert.assertEquals(refOrbit.getE(),                             orbit.getE(),                             relTol * refOrbit.getE());
            Assert.assertEquals(refOrbit.getI(),                             orbit.getI(),                             relTol * refOrbit.getI());
            Assert.assertEquals(refOrbit.getPerigeeArgument(),               orbit.getPerigeeArgument(),               relTol * refOrbit.getPerigeeArgument());
            Assert.assertEquals(refOrbit.getRightAscensionOfAscendingNode(), orbit.getRightAscensionOfAscendingNode(), relTol * refOrbit.getRightAscensionOfAscendingNode());
            Assert.assertEquals(refOrbit.getTrueAnomaly(),                   orbit.getTrueAnomaly(),                   relTol * refOrbit.getTrueAnomaly());
        }
    }


    @Test
    public void testMultiRevolutions() {

        Utils.setDataRoot("regular-data");
        TLE aussatB1 = new TLE("1 22087U 92054A   17084.21270512 -.00000243 +00000-0 +00000-0 0  9999",
                               "2 22087 008.5687 046.5717 0005960 022.3650 173.1619 00.99207999101265");
        final Propagator propagator = TLEPropagator.selectExtrapolator(aussatB1);
        final Frame teme = FramesFactory.getTEME();

        final AbsoluteDate t1 = new AbsoluteDate("2017-03-25T23:48:31.282", TimeScalesFactory.getUTC());
        final Vector3D p1 = propagator.propagate(t1).getPVCoordinates(teme).getPosition();
        final AbsoluteDate t2 = t1.shiftedBy(40000);
        final Vector3D p2 = propagator.propagate(t2).getPVCoordinates(teme).getPosition();
        final AbsoluteDate t3 = t1.shiftedBy(115200.0);
        final Vector3D p3 = propagator.propagate(t3).getPVCoordinates(teme).getPosition();

        IodLambert lambert = new IodLambert(Constants.WGS84_EARTH_MU);
        KeplerianOrbit k0 = lambert.estimate(teme, true, 0, p1, t1, p2, t2);
        Assert.assertEquals(6.08e-4, k0.getE(), 1.0e-6);
        Assert.assertEquals(8.55, FastMath.toDegrees(k0.getI()), 0.01);
        Assert.assertEquals(0.0, Vector3D.distance(p1, k0.getPVCoordinates(t1, teme).getPosition()), 2.0e-8);
        Assert.assertEquals(0.0, Vector3D.distance(p2, k0.getPVCoordinates(t2, teme).getPosition()), 2.0e-7);

        KeplerianOrbit k1 = lambert.estimate(teme, true, 1, p1, t1, p3, t3);
        Assert.assertEquals(5.97e-4, k1.getE(), 1.0e-6);
        Assert.assertEquals(8.55, FastMath.toDegrees(k1.getI()), 0.01);
        Assert.assertEquals(0.0, Vector3D.distance(p1, k1.getPVCoordinates(t1, teme).getPosition()), 1.4e-8);
        Assert.assertEquals(0.0, Vector3D.distance(p3, k1.getPVCoordinates(t3, teme).getPosition()), 3.0e-7);

    }

    @Test
    public void testNonChronologicalObservations() {
        
        // Initialize context - "eccentric orbit" built-in test bench context
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final double mu = context.initialOrbit.getMu();
        final Frame frame = context.initialOrbit.getFrame();

        // Use a simple Keplerian propagator (no perturbation)
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create propagator
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        // Save initial state and orbit (ie. reference orbit)
        final SpacecraftState initialState = propagator.getInitialState();
        final KeplerianOrbit refOrbit = new KeplerianOrbit(initialState.getOrbit());
        double[] refOrbitArray = new double[6];
        OrbitType.KEPLERIAN.mapOrbitToArray(refOrbit, PositionAngle.TRUE, refOrbitArray, null);

        final Vector3D position1 = refOrbit.getPVCoordinates().getPosition();
        final AbsoluteDate date1 = refOrbit.getDate();
        
        // Orbit period
        final double T = context.initialOrbit.getKeplerianPeriod();

        final double  dts = -T/4;
        final int     nRevs = 0;
        final boolean posigrades = true;
        
        // Reset to ref state
        propagator.resetInitialState(initialState);
        
        // Propagate to test date
        final AbsoluteDate date2 = date1.shiftedBy(dts);
        final Vector3D position2 = propagator.propagate(date2).getPVCoordinates().getPosition();
        
        // Instantiate the IOD method
        final IodLambert iod = new IodLambert(mu);

        // Estimate the orbit
        try {
            iod.estimate(frame, posigrades, nRevs, position1, date1, position2, date2);
            Assert.fail("An exception should have been thrown");
            
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_CHRONOLOGICAL_DATES_FOR_OBSERVATIONS, oe.getSpecifier());
        }
    }

    /** Testing out <a href="https://gitlab.orekit.org/orekit/orekit/issues/533"> issue #533</a> on Orekit forge.
     *  <p>Issue raised after unexpected results were found with the IodLambert class.
     *  <br> Solution for Δν = 270° gives a retrograde orbit (i = 180° instead of 0°) with Δν = 90°</br>
     *  <br> Not respecting at all the Δt constraint placed in input. </br>
     *  </p>
     *  @author Nicola Sullo
     */ 
    @Test
    public void testIssue533() {
        
        //Utils.setDataRoot("regular-data");
        
        double mu = Constants.EGM96_EARTH_MU;
        Frame j2000 = FramesFactory.getEME2000();

        // According to the Orekit documentation for IodLambert.estimate:
        /*
         * "As an example, if t2 is less than half a period after t1, then posigrade should be true and nRev
         * should be 0. If t2 is more than half a period after t1 but less than one period after t1,
         * posigrade should be false and nRev should be 1."
         */
        // Implementing 2 test cases to validate this
        // 1 - Δν = 90° , posigrade = true , nRev = 0
        // 2 - Δν = 270°, posigrade = false, nRev = 1
        for (int testCase = 1; testCase < 3; testCase++) {
    
            double trueAnomalyDifference;
            boolean posigrade;
            int nRev;
            if (testCase == 1) {
                posigrade = true;
                nRev = 0;
                trueAnomalyDifference = 0.5*FastMath.PI; // 90 degrees;
            } else {
                posigrade = false;
                nRev = 0;
                trueAnomalyDifference = 1.5*FastMath.PI; // 270 degrees;
            }
            
            // Tested orbit
            double a = 24000*1e3;
            double e = 0.72;
            double i = FastMath.toRadians(0.);
            double aop = 0.;
            double raan = 0.;
            double nu0 = 0.;
    
            // Assign position and velocity @t1 (defined as the position and velocity vectors and the periapse)
            AbsoluteDate t1 = new AbsoluteDate(2010, 1, 1, 0, 0, 0, TimeScalesFactory.getUTC());
            KeplerianOrbit kep1 = new KeplerianOrbit(a, e, i, aop, raan, nu0, PositionAngle.TRUE, j2000, t1, mu);
            Vector3D p1 = kep1.getPVCoordinates().getPosition();
            Vector3D v1 = kep1.getPVCoordinates().getVelocity();
    
            // Assign t2 date (defined as the date after a swept angle of "trueAnomalyDifference" after periapsis)
            KeplerianOrbit kep2 = new KeplerianOrbit(a, e, i, aop, raan, trueAnomalyDifference, PositionAngle.TRUE, j2000, t1, mu);
            double n = kep2.getKeplerianMeanMotion();
            double M2 = kep2.getMeanAnomaly();
            double delta_t = M2 / n; // seconds
            AbsoluteDate t2 = t1.shiftedBy(delta_t);
    
            // Assign position and velocity @t2
            PVCoordinates pv2 = kep1.getPVCoordinates(t2, j2000);
            Vector3D p2 = pv2.getPosition();
            Vector3D v2 = pv2.getVelocity();
    
            // Solve Lambert problem
            IodLambert iodLambert = new IodLambert(mu);
            KeplerianOrbit resultOrbit1 = iodLambert.estimate(j2000, posigrade, nRev, p1, t1, p2, t2);
    
            // Get position and velocity coordinates @t1 and @t2 from the output Keplerian orbit of iodLambert.estimate
            PVCoordinates resultPv1 = resultOrbit1.getPVCoordinates(t1, j2000);
            PVCoordinates resultPv2 = resultOrbit1.getPVCoordinates(t2, j2000);
    
            Vector3D resultP1 = resultPv1.getPosition();
            Vector3D resultV1 = resultPv1.getVelocity();
            Vector3D resultP2 = resultPv2.getPosition();
            Vector3D resultV2 = resultPv2.getVelocity();
            
            // Compare PVs
            double dP1 = Vector3D.distance(p1, resultP1);
            double dV1 = Vector3D.distance(v1, resultV1);
            double dP2 = Vector3D.distance(p2, resultP2);
            double dV2 = Vector3D.distance(v2, resultV2);
            
            // Tolerances
            double dP1Tol, dP2Tol, dV1Tol, dV2Tol;
            if (testCase == 1) {
                dP1Tol = 4.28e-25;
                dV1Tol = 5.97e-12;
                dP2Tol = 7.57e-9;
                dV2Tol = 7.50e-12;
            } else {
                dP1Tol = 2.81e-25;
                dV1Tol = 3.03e-12;
                dP2Tol = 9.86e-7;
                dV2Tol = 4.01e-10;
            }
            
            // Check results
            Assert.assertEquals(0., dP1, dP1Tol);
            Assert.assertEquals(0., dV1, dV1Tol);
            Assert.assertEquals(0., dP2, dP2Tol);
            Assert.assertEquals(0., dV2, dV2Tol);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
