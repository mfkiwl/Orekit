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
package org.orekit.estimation.sequential;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Force;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Arrays;
import java.util.Locale;

public class UnivariateprocessNoiseTest {

    /** Basic test for getters. */
    @Test
    public void testUnivariateProcessNoiseGetters() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Define the univariate functions for the standard deviations
        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];
        // Evolution for position error
        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {100., 0., 1e-4});
        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {100., 1e-1, 0.});
        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {100., 0., 0.});
        // Evolution for velocity error
        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1.e-6});
        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 0.});

        // Evolution for propagation parameters error
        final UnivariateFunction[] propagationParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {10., 1., 1e-4}),
                                                  new PolynomialFunction(new double[] {1000., 0., 0.})};

        // Evolution for measurements parameters error
        final UnivariateFunction[] measurementsParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {100., 1., 1e-3})};

        // Create a dummy initial covariance matrix
        final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(9);

        // Set the process noise object
        // Define input LOF and output position angle
        final LOFType lofType = LOFType.TNW;
        final UnivariateProcessNoise processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                               lofType,
                                                                               PositionAngle.TRUE,
                                                                               lofCartesianOrbitalParametersEvolution,
                                                                               propagationParametersEvolution,
                                                                               measurementsParametersEvolution);

        Assertions.assertEquals(LOFType.TNW, processNoise.getLofType());
        Assertions.assertEquals(PositionAngle.TRUE, processNoise.getPositionAngle());
        Assertions.assertEquals(initialCovarianceMatrix,
                            processNoise.getInitialCovarianceMatrix(new SpacecraftState(context.initialOrbit)));
        Assertions.assertArrayEquals(lofCartesianOrbitalParametersEvolution, processNoise.getLofCartesianOrbitalParametersEvolution());
        Assertions.assertArrayEquals(propagationParametersEvolution, processNoise.getPropagationParametersEvolution());
        Assertions.assertArrayEquals(measurementsParametersEvolution, processNoise.getMeasurementsParametersEvolution());

        final UnivariateProcessNoise processNoiseOld = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                                  lofType,
                                                                                  PositionAngle.TRUE,
                                                                                  lofCartesianOrbitalParametersEvolution,
                                                                                  propagationParametersEvolution);

        Assertions.assertEquals(LOFType.TNW, processNoiseOld.getLofType());
        Assertions.assertEquals(PositionAngle.TRUE, processNoiseOld.getPositionAngle());
        Assertions.assertEquals(initialCovarianceMatrix,
                            processNoiseOld.getInitialCovarianceMatrix(new SpacecraftState(context.initialOrbit)));
        Assertions.assertArrayEquals(lofCartesianOrbitalParametersEvolution, processNoiseOld.getLofCartesianOrbitalParametersEvolution());
        Assertions.assertArrayEquals(propagationParametersEvolution, processNoiseOld.getPropagationParametersEvolution());
    }

    /** Test UnivariateProcessNoise class.
     * - Initialize with different univariate functions for orbital/propagation parameters variation
     * - Check that the inertial process noise covariance matrix is consistent with the inputs
     * - Propagation in Cartesian formalism
     */
    @Test
    public void testProcessNoiseMatrixCartesian() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE; // Not used here
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder = context.createBuilder(orbitType, positionAngle, perfectStart,
                                                                                   minStep, maxStep, dP,
                                                                                   Force.POTENTIAL, Force.THIRD_BODY_MOON,
                                                                                   Force.THIRD_BODY_SUN,
                                                                                   Force.SOLAR_RADIATION_PRESSURE);
        // Create a propagator
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        // Define the univariate functions for the standard deviations
        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];
        // Evolution for position error
        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {100., 0., 1e-4});
        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {100., 1e-1, 0.});
        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {100., 0., 0.});
        // Evolution for velocity error
        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1.e-6});
        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 0.});

        // Evolution for propagation parameters error
        final UnivariateFunction[] propagationParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {10., 1., 1e-4}),
                                                  new PolynomialFunction(new double[] {1000., 0., 0.})};


        // Create a dummy initial covariance matrix
        final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(8);

        // Set the process noise object
        // Define input LOF and output position angle
        final LOFType lofType = LOFType.TNW;
        final UnivariateProcessNoise processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                               lofType,
                                                                               positionAngle,
                                                                               lofCartesianOrbitalParametersEvolution,
                                                                               propagationParametersEvolution);
        // Test on initial value, after 1 hour and after one orbit
        final SpacecraftState state0 = propagator.getInitialState();
        final SpacecraftState state1 = propagator.propagate(context.initialOrbit.getDate().shiftedBy(3600.));
        final SpacecraftState state2 = propagator.propagate(context.initialOrbit.getDate()
                                                            .shiftedBy(2*context.initialOrbit.getKeplerianPeriod()));

        // Number of samples for the statistics
        final int sampleNumber = 10000;

        // Relative tolerance on final standard deviations observed (< 1.5% for 10000 samples)
        final double relativeTolerance = 1.5e-2;

        if (print) {
            System.out.println("Orbit Type    : " + orbitType);
            System.out.println("Position Angle: " + positionAngle + "\n");
        }
        checkCovarianceValue(print, state0, state0, processNoise, 2, sampleNumber, relativeTolerance);
        checkCovarianceValue(print, state0, state1, processNoise, 2, sampleNumber, relativeTolerance);
        checkCovarianceValue(print, state0, state2, processNoise, 2, sampleNumber, relativeTolerance);
    }

    /** Test UnivariateProcessNoise class with its new constructor.
     * - Initialize with different univariate functions for orbital/propagation parameters variation
     * - Check that the inertial process noise covariance matrix is consistent with the inputs
     * - Propagation in Cartesian formalism
     */
    @Test
    public void testProcessNoiseMatrixCartesianNew() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE; // Not used here
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder = context.createBuilder(orbitType, positionAngle, perfectStart,
                                                                                   minStep, maxStep, dP,
                                                                                   Force.POTENTIAL, Force.THIRD_BODY_MOON,
                                                                                   Force.THIRD_BODY_SUN,
                                                                                   Force.SOLAR_RADIATION_PRESSURE);
        // Create a propagator
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        // Define the univariate functions for the standard deviations
        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];
        // Evolution for position error
        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {500., 0., 1e-4});
        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {400., 1e-1, 0.});
        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {200., 2e-1, 0.});
        // Evolution for velocity error
        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1e-6});
        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 1e-5});

        // Evolution for propagation parameters error
        final UnivariateFunction[] propagationParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {100., 1., 1e-4}),
                                                  new PolynomialFunction(new double[] {2000., 3., 0.})};

        // Evolution for measurements parameters error
        final UnivariateFunction[] measurementsParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {100., 1., 1e-3})};

        // Create a dummy initial covariance matrix
        final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(9);

        // Set the process noise object
        // Define input LOF and output position angle
        final LOFType lofType = LOFType.TNW;
        final UnivariateProcessNoise processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                               lofType,
                                                                               positionAngle,
                                                                               lofCartesianOrbitalParametersEvolution,
                                                                               propagationParametersEvolution,
                                                                               measurementsParametersEvolution);
        // Test on initial value, after 1 hour and after one orbit
        final SpacecraftState state0 = propagator.getInitialState();
        final SpacecraftState state1 = propagator.propagate(context.initialOrbit.getDate().shiftedBy(3600.));
        final SpacecraftState state2 = propagator.propagate(context.initialOrbit.getDate()
                                                            .shiftedBy(2*context.initialOrbit.getKeplerianPeriod()));

        // Number of samples for the statistics
        final int sampleNumber = 10000;

        // Relative tolerance on final standard deviations observed (< 1.5% for 10000 samples)
        final double relativeTolerance = 1.5e-2;

        if (print) {
            System.out.println("Orbit Type    : " + orbitType);
            System.out.println("Position Angle: " + positionAngle + "\n");
        }
        checkCovarianceValue(print, state0, state0, processNoise, 2, sampleNumber, relativeTolerance);
        checkCovarianceValue(print, state0, state1, processNoise, 2, sampleNumber, relativeTolerance);
        checkCovarianceValue(print, state0, state2, processNoise, 2, sampleNumber, relativeTolerance);
    }

      // Note: This test does not work, probably due to the high values of the univariate functions when time increases.
      // The jacobian matrices used to convert from Keplerian (equinoctial, circular) to Cartesian and back are based on first oder
      // derivatives. When differences to the reference orbit are very large, these first order derivatives are not enough to physically
      // represent the deviation of the orbit.
      // Investigations are to be done on this test
//    /** Test process noise matrix computation.
//     * - Initialize with different univariate functions for orbital/propagation parameters variation
//     * - Check that the inertial process noise covariance matrix is consistent with the inputs
//     * - Propagation in Non-Cartesian formalism (Keplerian, Circular or Equinoctial)
//     *  TO DO: Find out why position in LOF frame is off after one hour of propagation
//     */
//    @Ignore
//    @Test
//    public void testProcessNoiseMatrixNonCartesian() {
//
//        // Create context
//        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
//
//        // Print result on console ?
//        final boolean print = true;
//
//        // Create initial orbit and propagator builder
//        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
//        final PositionAngle positionAngle = PositionAngle.TRUE;
//        final boolean       perfectStart  = true;
//        final double        minStep       = 1.e-6;
//        final double        maxStep       = 60.;
//        final double        dP            = 1.;
//        final NumericalPropagatorBuilder propagatorBuilder = context.createBuilder(orbitType, positionAngle, perfectStart,
//                                                                                   minStep, maxStep, dP);//
////                                                                                   Force.POTENTIAL, Force.THIRD_BODY_MOON,
////                                                                                   Force.THIRD_BODY_SUN,
////                                                                                   Force.SOLAR_RADIATION_PRESSURE);
//
//        // Create a propagator
//        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
//                                                                           propagatorBuilder);
//
//        // Define the univariate functions for the standard deviations
//        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];
//        // Evolution for position error
//        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {100., 0., 1e-4});
//        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {100., 1e-1, 0.});
//        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {100., 0., 0.});
//        // Evolution for velocity error
//        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1.e-6});
//        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
//        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 0.});
//
//        final UnivariateFunction[] propagationParametersEvolution =
//                        new UnivariateFunction[] {new PolynomialFunction(new double[] {10, 1., 1e-4}),
//                                                  new PolynomialFunction(new double[] {1000., 0., 0.})};
//
//        // Create a dummy initial covariance matrix
//        final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(7);
//
//        // Set the process noise object
//        // Define input LOF and output position angle
//        final LOFType lofType = LOFType.TNW;
//        final UnivariateProcessNoise processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
//                                                                               lofType,
//                                                                               positionAngle,
//                                                                               lofCartesianOrbitalParametersEvolution,
//                                                                               propagationParametersEvolution);
//        // Test on initial value, after 1 hour and after 2 orbits
//        final SpacecraftState state0 = propagator.getInitialState();
//        final SpacecraftState state1 = propagator.propagate(context.initialOrbit.getDate().shiftedBy(3600.));
////        final SpacecraftState state2 = propagator.propagate(context.initialOrbit.getDate()
////                                                            .shiftedBy(2*context.initialOrbit.getKeplerianPeriod()));
//
//        // Number of samples for the statistics
//        final int sampleNumber = 10000;
//
//        // Relative tolerance on final standard deviations observed
//        final double relativeTolerance = 0.02;
//
//        if (print) {
//            System.out.println("Orbit Type    : " + orbitType);
//            System.out.println("Position Angle: " + positionAngle + "\n");
//        }
//        checkCovarianceValue(print, state0, state0, processNoise, sampleNumber, relativeTolerance);
//        checkCovarianceValue(print, state0, state1, processNoise, sampleNumber, relativeTolerance);
//
//        // Orbit too far off after 2 orbital periods
//        // It becomes inconsistent when applying random vector values
//        //checkCovarianceValue(print, state0, state2, processNoise, sampleNumber, relativeTolerance);
//    }


    /** Check the values of the covariance given in inertial frame by the UnivariateProcessNoise object.
     *  - Get the process noise covariance matrix P in inertial frame
     *  - Use a random vector generator based on P to produce a set of N error vectors in inertial frame
     *  - Compare the standard deviations of the noisy data to the univariate functions given in input of the UnivariateProcessNoise class.
     *  - For orbit parameters this involve converting the inertial orbital vector to LOF frame and Cartesian formalism first
     * @param print print results in console ?
     * @param previous previous state
     * @param current current state
     * @param univariateProcessNoise UnivariateProcessNoise object to test
     * @param propagationParametersSize size of propagation parameters submatrix in UnivariateProcessNoise
     * @param sampleNumber number of sample vectors for the statistics
     * @param relativeTolerance relative tolerance on the standard deviations for the tests
     */
    private void checkCovarianceValue(final boolean print,
                                      final SpacecraftState previous,
                                      final SpacecraftState current,
                                      final UnivariateProcessNoise univariateProcessNoise,
                                      final int propagationParametersSize,
                                      final int sampleNumber,
                                      final double relativeTolerance) {

        // Get the process noise matrix
        final RealMatrix processNoiseMatrix = univariateProcessNoise.getProcessNoiseMatrix(previous, current);

        // Initialize a random vector generator
        final CorrelatedRandomVectorGenerator randomVectorGenerator = createSampler(processNoiseMatrix);

        // Propagation parameters length
        int measurementsParametersSize = processNoiseMatrix.getColumnDimension() - (6 + propagationParametersSize);
        if ( measurementsParametersSize < 0) {
            measurementsParametersSize = 0;
        }

        // Prepare the statistics
        final StreamingStatistics[] orbitalStatistics = new StreamingStatistics[6];
        for (int i = 0; i < 6; i++) {
            orbitalStatistics[i] = new StreamingStatistics();
        }
        StreamingStatistics[] propagationStatistics;
        if (propagationParametersSize > 0) {
            propagationStatistics = new StreamingStatistics[propagationParametersSize];
            for (int i = 0; i < propagationParametersSize; i++) {
                propagationStatistics[i] = new StreamingStatistics();
            }
        } else {
          propagationStatistics = null;
        }
        StreamingStatistics[] measurementsStatistics;
        if (propagationParametersSize > 0) {
            measurementsStatistics = new StreamingStatistics[measurementsParametersSize];
            for (int i = 0; i < measurementsParametersSize; i++) {
                measurementsStatistics[i] = new StreamingStatistics();
            }
        } else {
          measurementsStatistics = null;
        }

        // Current orbit stored in an array
        // With the position angle defined in the univariate process noise function
        final double[] currentOrbitArray = new double[6];
        current.getOrbit().getType().mapOrbitToArray(current.getOrbit(),
                                                     univariateProcessNoise.getPositionAngle(),
                                                     currentOrbitArray,
                                                     null);
        // Transform from inertial to current spacecraft LOF frame
        final Transform inertialToLof = univariateProcessNoise.getLofType().transformFromInertial(current.getDate(),
                                                                                                  current.getOrbit().getPVCoordinates());
        // Create the vectors and compute the states
        for (int i = 0; i < sampleNumber; i++) {

            // Create a random vector
            final double[] randomVector = randomVectorGenerator.nextVector();

            // Orbital parameters values
            // -------------------------

            // Get the full inertial orbit by adding up the values of current orbit and orbit error (first 6 components of random vector)
            final double[] modifiedOrbitArray = new double[6];
            for (int j = 0; j < 6; j++) {
                modifiedOrbitArray[j] = currentOrbitArray[j] + randomVector[j];
            }

            // Get the corresponding PV coordinates
            TimeStampedPVCoordinates inertialPV = null;
            try {
                inertialPV = current.getOrbit().getType().mapArrayToOrbit(modifiedOrbitArray,
                                                                          null,
                                                                          univariateProcessNoise.getPositionAngle(),
                                                                          current.getDate(),
                                                                          current.getMu(),
                                                                          current.getFrame()).getPVCoordinates();
            } catch (OrekitIllegalArgumentException e) {
                // If orbit becomes inconsistent due to errors that are too large, print orbital values
                System.out.println("i = " + i + " / Inconsistent Orbit");
                System.out.println("\tCurrent Orbit  = " + Arrays.toString(currentOrbitArray));
                System.out.println("\tModified Orbit = " + Arrays.toString(modifiedOrbitArray));
                System.out.println("\tDelta Orbit    = " + Arrays.toString(randomVector));
                e.printStackTrace();
                System.err.println(e.getMessage());
            }

            // Transform from inertial to current LOF
            // The value obtained is the Cartesian error vector in LOF (w/r to current spacecraft position)
            final PVCoordinates lofPV = inertialToLof.transformPVCoordinates(inertialPV);

            // Store the LOF PV values in the statistics summaries
            orbitalStatistics[0].addValue(lofPV.getPosition().getX());
            orbitalStatistics[1].addValue(lofPV.getPosition().getY());
            orbitalStatistics[2].addValue(lofPV.getPosition().getZ());
            orbitalStatistics[3].addValue(lofPV.getVelocity().getX());
            orbitalStatistics[4].addValue(lofPV.getVelocity().getY());
            orbitalStatistics[5].addValue(lofPV.getVelocity().getZ());


            // Propagation parameters values
            // -----------------------------

            // Extract the propagation parameters random vector and store them in the statistics
            if (propagationParametersSize > 0) {
                for (int j = 6; j < randomVector.length - measurementsParametersSize; j++) {
                    propagationStatistics[j - 6].addValue(randomVector[j]);
                }
            }

            // Measurements parameters values
            // -----------------------------

            // Extract the measurements parameters random vector and store them in the statistics
            if (measurementsParametersSize > 0) {
                for (int j = 6 + propagationParametersSize ; j < randomVector.length; j++) {
                    measurementsStatistics[j - (6 + propagationParametersSize)].addValue(randomVector[j]);
                }
            }
        }

        // Get the real values and the statistics
        // --------------------------------------

        // DT
        final double dt = current.getDate().durationFrom(previous.getDate());

        // Get the values of the orbital functions and the statistics
        final double[] orbitalValues = new double[6];
        final double[] orbitalStatisticsValues = new double[6];
        final double[] orbitalRelativeValues = new double[6];

        for (int i = 0; i < 6; i++) {
            orbitalValues[i] = FastMath.abs(univariateProcessNoise.getLofCartesianOrbitalParametersEvolution()[i].value(dt));
            orbitalStatisticsValues[i] = orbitalStatistics[i].getStandardDeviation();

            if (FastMath.abs(orbitalValues[i]) > 1e-15) {
                orbitalRelativeValues[i] = FastMath.abs(1. - orbitalStatisticsValues[i]/orbitalValues[i]);
            } else {
                orbitalRelativeValues[i] = orbitalStatisticsValues[i];
            }
        }

        // Get the values of the propagation functions and statistics
        final double[] propagationValues = new double[propagationParametersSize];
        final double[] propagationStatisticsValues = new double[propagationParametersSize];
        final double[] propagationRelativeValues = new double[propagationParametersSize];
        for (int i = 0; i < propagationParametersSize; i++) {
            propagationValues[i] = FastMath.abs(univariateProcessNoise.getPropagationParametersEvolution()[i].value(dt));
            propagationStatisticsValues[i] = propagationStatistics[i].getStandardDeviation();
            if (FastMath.abs(propagationValues[i]) > 1e-15) {
                propagationRelativeValues[i] = FastMath.abs(1. - propagationStatisticsValues[i]/propagationValues[i]);
            } else {
                propagationRelativeValues[i] = propagationStatisticsValues[i];
            }
        }

        // Get the values of the measurements functions and statistics
        final double[] measurementsValues = new double[measurementsParametersSize];
        final double[] measurementsStatisticsValues = new double[measurementsParametersSize];
        final double[] measurementsRelativeValues = new double[measurementsParametersSize];
        for (int i = 0; i < measurementsParametersSize; i++) {
            measurementsValues[i] = FastMath.abs(univariateProcessNoise.getMeasurementsParametersEvolution()[i].value(dt));
            measurementsStatisticsValues[i] = measurementsStatistics[i].getStandardDeviation();
            if (FastMath.abs(propagationValues[i]) > 1e-15) {
                measurementsRelativeValues[i] = FastMath.abs(1. - measurementsStatisticsValues[i]/measurementsValues[i]);
            } else {
                measurementsRelativeValues[i] = measurementsStatisticsValues[i];
            }
        }

        // Print values
        if (print) {
            System.out.println("\tdt = " + dt + " / N = " + sampleNumber + "\n");
            System.out.println("\tσ orbit ref   = " + Arrays.toString(orbitalValues));
            System.out.println("\tσ orbit stat  = " + Arrays.toString(orbitalStatisticsValues));
            System.out.println("\tσ orbit %     = " + Arrays.toString(Arrays.stream(orbitalRelativeValues).map(i -> i*100.).toArray()) + "\n");

            System.out.println("\tσ propag ref   = " + Arrays.toString(propagationValues));
            System.out.println("\tσ propag stat  = " + Arrays.toString(propagationStatisticsValues));
            System.out.println("\tσ propag %     = " + Arrays.toString(Arrays.stream(propagationRelativeValues).map(i -> i*100.).toArray()) + "\n");

            System.out.println("\tσ meas ref   = " + Arrays.toString(propagationValues));
            System.out.println("\tσ meas stat  = " + Arrays.toString(propagationStatisticsValues));
            System.out.println("\tσ meas %     = " + Arrays.toString(Arrays.stream(propagationRelativeValues).map(i -> i*100.).toArray()) + "\n");
        }

        // Test the values
        Assertions.assertArrayEquals(new double[6],
                          orbitalRelativeValues,
                          relativeTolerance);
        Assertions.assertArrayEquals(new double[propagationParametersSize],
                          propagationRelativeValues,
                          relativeTolerance);
        Assertions.assertArrayEquals(new double[measurementsParametersSize],
                          measurementsRelativeValues,
                          relativeTolerance);
    }

    /** Create a gaussian random vector generator based on an input covariance matrix.
     * @param covarianceMatrix input covariance matrix
     * @return correlated gaussian random vectors generator
     */
    private CorrelatedRandomVectorGenerator createSampler(final RealMatrix covarianceMatrix) {
        double small = 10e-20 * covarianceMatrix.getNorm1();
        return new CorrelatedRandomVectorGenerator(
                covarianceMatrix,
                small,
                new GaussianRandomGenerator(new Well1024a(0x366a26b94e520f41l)));
    }

    /** Test LOF orbital covariance matrix value against reference.
     * 1. Initialize with different univariate functions for orbital/propagation parameters variation
     * 2. Get the inertial process noise covariance matrix
     * 3. Convert the inertial covariance from 2 in Cartesian and LOF
     * 4. Compare values of 3 with reference values from 1
     */
    @Test
    public void testLofCartesianOrbitalCovarianceFormal() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder = context.createBuilder(orbitType, positionAngle, perfectStart,
                                                                                   minStep, maxStep, dP,
                                                                                   Force.POTENTIAL, Force.THIRD_BODY_MOON,
                                                                                   Force.THIRD_BODY_SUN,
                                                                                   Force.SOLAR_RADIATION_PRESSURE);

        // Create a propagator
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        // Define the univariate functions for the standard deviations
        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];
        // Evolution for position error
        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {100., 0., 1e-4});
        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {100., 1e-1, 0.});
        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {100., 0., 0.});
        // Evolution for velocity error
        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1.e-6});
        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 0.});

        // Evolution for propagation parameters error
        final UnivariateFunction[] propagationParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {10., 1., 1e-4}),
                                                  new PolynomialFunction(new double[] {1000., 0., 0.})};


        // Create a dummy initial covariance matrix
        final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(8);

        // Set the process noise object
        // Define input LOF and output position angle
        final LOFType lofType = LOFType.TNW;
        final UnivariateProcessNoise processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                               lofType,
                                                                               positionAngle,
                                                                               lofCartesianOrbitalParametersEvolution,
                                                                               propagationParametersEvolution);
        // Initial value
        final SpacecraftState state0 = propagator.getInitialState();

        // 1 orbit
        final SpacecraftState state1 = propagator.propagate(context.initialOrbit.getDate()
                                                            .shiftedBy(context.initialOrbit.getKeplerianPeriod()));
        // 1 day
        final SpacecraftState state2 = propagator.propagate(context.initialOrbit.getDate().shiftedBy(86400.));

        if (print) {
            System.out.println("Orbit Type    : " + orbitType);
            System.out.println("Position Angle: " + positionAngle);
        }

        // Worst case, KEPLERIAN MEAN - 1.62e-11
        checkLofOrbitalCovarianceMatrix(print, state0, state0, processNoise, 1.62e-11);
        // Worst case, CIRCULAR ECCENTRIC - 2.78e-10
        checkLofOrbitalCovarianceMatrix(print, state0, state1, processNoise, 2.78e-10);
        // Worst case, CIRCULAR TRUE - 9.15e-9
        checkLofOrbitalCovarianceMatrix(print, state0, state2, processNoise, 9.15e-9);
    }

    /** Check orbital covariance matrix in LOF wrt reference.
     * Here we do the reverse calculcation
     */
    private void checkLofOrbitalCovarianceMatrix(final boolean print,
                                                 final SpacecraftState previous,
                                                 final SpacecraftState current,
                                                 final UnivariateProcessNoise univariateProcessNoise,
                                                 final double relativeTolerance) {

        // Get the process noise matrix in inertial frame
        final RealMatrix inertialQ = univariateProcessNoise.getProcessNoiseMatrix(previous, current);

        // Extract the orbit part
        final RealMatrix inertialOrbitalQ = inertialQ.getSubMatrix(0, 5, 0, 5);

        // Jacobian from parameters to Cartesian
        final double[][] dCdY = new double[6][6];
        current.getOrbit().getJacobianWrtParameters(univariateProcessNoise.getPositionAngle(), dCdY);
        final RealMatrix jacOrbToCar = new Array2DRowRealMatrix(dCdY, false);

        // Jacobian from inertial to LOF
        final double[][] dLOFdI = new double[6][6];
        univariateProcessNoise.getLofType().transformFromInertial(current.getDate(),
                                                                  current.getOrbit().getPVCoordinates()).getJacobian(CartesianDerivativesFilter.USE_PV, dLOFdI);
        final RealMatrix jacIToLOF = new Array2DRowRealMatrix(dLOFdI, false);

        // Complete Jacobian
        final RealMatrix jac = jacIToLOF.multiply(jacOrbToCar);

        // Q in LOF and Cartesian
        final RealMatrix lofQ = jac.multiply(inertialOrbitalQ).multiplyTransposed(jac);

        // Build reference LOF Cartesian orbital sigmas
        final RealVector refLofSig = new ArrayRealVector(6);
        double dt = current.getDate().durationFrom(previous.getDate());
        for (int i = 0; i < 6; i++) {
            refLofSig.setEntry(i, univariateProcessNoise.getLofCartesianOrbitalParametersEvolution()[i].value(dt));
        }

        // Compare diagonal values with reference (relative difference) and suppress them from the matrix
        // Ensure non-diagonal values are into numerical noise sensitivity
        RealVector dLofDiag = new ArrayRealVector(6);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                double refI = refLofSig.getEntry(i);
                if (i == j) {
                    // Diagonal term
                    dLofDiag.setEntry(i, FastMath.abs(1. - lofQ.getEntry(i, i) / refI / refI));
                    lofQ.setEntry(i, i, 0.);
                } else {
                    // Non-diagonal term, ref is 0.
                    lofQ.setEntry(i, j, FastMath.abs(lofQ.getEntry(i,j) / refI / refLofSig.getEntry(j)));
                }
            }

        }

        // Norm of diagonal and non-diagonal
        double dDiag = dLofDiag.getNorm();
        double dNonDiag = lofQ.getNorm1();

        // Print values ?
        if (print) {
            System.out.println("\tdt = " + dt);
            System.out.format(Locale.US, "\tΔDiagonal norm in Cartesian LOF     = %10.4e%n", dDiag);
            System.out.format(Locale.US, "\tΔNon-Diagonal norm in Cartesian LOF = %10.4e%n%n", dNonDiag);
        }
        Assertions.assertEquals(0.,  dDiag   , relativeTolerance);
        Assertions.assertEquals(0.,  dNonDiag, relativeTolerance);
    }
}
