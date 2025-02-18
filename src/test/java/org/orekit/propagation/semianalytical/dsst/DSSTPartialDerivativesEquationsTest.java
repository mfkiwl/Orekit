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
package org.orekit.propagation.semianalytical.dsst;

import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.HarrisPriester;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;

/** Unit tests for {@link DSSTPartialDerivativesEquations}. */
@Deprecated
public class DSSTPartialDerivativesEquationsTest {

    /** arbitrary date */
    private static final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
    /** Earth gravitational parameter */
    private static final double gm = Constants.EIGEN5C_EARTH_MU;
    /** arbitrary inertial frame */
    private static final Frame eci = FramesFactory.getGCRF();

    /** unused propagator */
    private DSSTPropagator propagator;
    /** mock force model */
    private MockForceModel forceModel;
    /** arbitrary state */
    private SpacecraftState state;
    /** subject under test */
    private DSSTPartialDerivativesEquations pde;

    /**
     * set up {@link #pde} and dependencies.
     *
     */
    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
        propagator = new DSSTPropagator(new DormandPrince54Integrator(1, 500, 0.001, 0.001));
        forceModel = new MockForceModel();
        propagator.addForceModel(forceModel);
        pde = new DSSTPartialDerivativesEquations("pde", propagator, PropagationType.MEAN);
        final Orbit orbit = new EquinoctialOrbit(4.2163393E7,
                                                 -0.25925449177598586,
                                                 -0.06946703170551687,
                                                 0.15995912655021305,
                                                 -0.5969755874197339,
                                                 15.47576793123677,
                                                 PositionAngle.MEAN,
                                                 eci, date, gm);
        state = new SpacecraftState(orbit).addAdditionalState("pde", new double[2 * 3 * 6]);
        pde.setInitialJacobians(state);

    }

    @Test
    public void testDragParametersDerivatives() throws ParseException, IOException {
        doTestParametersDerivatives(DragSensitive.DRAG_COEFFICIENT,
                                    2.4e-3,
                                    PropagationType.MEAN,
                                    OrbitType.EQUINOCTIAL);
    }

    @Test
    public void testMuParametersDerivatives() throws ParseException, IOException {
        doTestParametersDerivatives(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                    5.e-3,
                                    PropagationType.MEAN,
                                    OrbitType.EQUINOCTIAL);
    }

    private void doTestParametersDerivatives(String parameterName, double tolerance,
                                             PropagationType type,
                                             OrbitType... orbitTypes) {

        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);

        DSSTForceModel drag = new DSSTAtmosphericDrag(new HarrisPriester(CelestialBodyFactory.getSun(), earth),
                                                      new IsotropicDrag(2.5, 1.2),
                                                      provider.getMu());

        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                         4, 4, 4, 8, 4, 4, 2);
        final DSSTForceModel zonal = new DSSTZonal(provider, 4, 3, 9);

        Orbit baseOrbit =
                new KeplerianOrbit(7000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   provider.getMu());

        double dt = 900;
        double dP = 1.0;
        for (OrbitType orbitType : orbitTypes) {
            final Orbit initialOrbit = orbitType.convertType(baseOrbit);

            DSSTPropagator propagator =
                            setUpPropagator(type, initialOrbit, dP, orbitType, zonal, tesseral, drag);
            propagator.setMu(provider.getMu());
            for (final DSSTForceModel forceModel : propagator.getAllForceModels()) {
                for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                    driver.setValue(driver.getReferenceValue());
                    driver.setSelected(driver.getName().equals(parameterName));
                }
            }

            DSSTPartialDerivativesEquations partials = new DSSTPartialDerivativesEquations("partials", propagator, type);
            final SpacecraftState initialState =
                    partials.setInitialJacobians(new SpacecraftState(initialOrbit));
            propagator.setInitialState(initialState, PropagationType.MEAN);
            final DSSTJacobiansMapper mapper = partials.getMapper();
            PickUpHandler pickUp = new PickUpHandler(mapper, null);
            propagator.setStepHandler(pickUp);
            propagator.propagate(initialState.getDate().shiftedBy(dt));

            // compute reference Jacobian using finite differences
            double[][] dYdPRef = new double[6][1];
            DSSTPropagator propagator2 = setUpPropagator(type, initialOrbit, dP, orbitType, zonal, tesseral, drag);
            propagator2.setMu(provider.getMu());
            ParameterDriversList bound = new ParameterDriversList();
            for (final DSSTForceModel forceModel : propagator2.getAllForceModels()) {
                for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                    if (driver.getName().equals(parameterName)) {
                        driver.setSelected(true);
                        bound.add(driver);
                    } else {
                        driver.setSelected(false);
                    }
                }
            }
            ParameterDriver selected = bound.getDrivers().get(0);
            double p0 = selected.getReferenceValue();
            double h  = selected.getScale();
            selected.setValue(p0 - 4 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sM4h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 - 3 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sM3h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 - 2 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sM2h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 - 1 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sM1h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 + 1 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sP1h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 + 2 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sP2h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 + 3 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sP3h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            selected.setValue(p0 + 4 * h);
            propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType),
                                                       orbitType,
                                                       initialState.getFrame(), initialState.getDate(),
                                                       propagator2.getMu(), // the mu may have been reset above
                                                       initialState.getAttitude()));
            SpacecraftState sP4h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
            fillJacobianColumn(dYdPRef, 0, orbitType, h,
                               sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);

            for (int i = 0; i < 6; ++i) {
                Assertions.assertEquals(dYdPRef[i][0], pickUp.dYdP.getEntry(i, 0), FastMath.abs(dYdPRef[i][0] * tolerance));
            }

        }

    }

    @Test
    public void testPropagationTypesElliptical() throws FileNotFoundException, UnsupportedEncodingException, OrekitException {
        doTestPropagation(PropagationType.MEAN, 7.0e-16);
    }

    @Test
    public void testPropagationTypesEllipticalWithShortPeriod() throws FileNotFoundException, UnsupportedEncodingException, OrekitException {
        doTestPropagation(PropagationType.OSCULATING, 3.3e-4);
    }

    private void doTestPropagation(PropagationType type,
                                  double tolerance)
        throws FileNotFoundException, UnsupportedEncodingException {

        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);

        Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                         4, 4, 4, 8, 4, 4, 2);

        DSSTForceModel zonal = new DSSTZonal(provider, 4, 3, 9);
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            provider.getMu());

        DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), provider.getMu());

        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   provider.getMu());
        final EquinoctialOrbit orbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(initialOrbit);

        double dt = 900;
        double dP = 0.001;
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;

        // compute state Jacobian using PartialDerivatives
        DSSTPropagator propagator = setUpPropagator(type, orbit, dP, orbitType, srp, tesseral, zonal, moon);
        propagator.setMu(provider.getMu());
        DSSTPartialDerivativesEquations partials = new DSSTPartialDerivativesEquations("partials", propagator, type);
        final SpacecraftState initialState =
                partials.setInitialJacobians(new SpacecraftState(orbit));
        final double[] stateVector = new double[6];
        OrbitType.EQUINOCTIAL.mapOrbitToArray(initialState.getOrbit(), PositionAngle.MEAN, stateVector, null);
        final AbsoluteDate target = initialState.getDate().shiftedBy(dt);
        propagator.setInitialState(initialState, type);
        final DSSTJacobiansMapper mapper = partials.getMapper();
        PickUpHandler pickUp = new PickUpHandler(mapper, null);
        propagator.setStepHandler(pickUp);
        propagator.propagate(target);

        // compute reference state Jacobian using finite differences
        double[][] dYdY0Ref = new double[6][6];
        DSSTPropagator propagator2 = setUpPropagator(type, orbit, dP, orbitType, srp, tesseral, zonal, moon);
        propagator2.setMu(provider.getMu());
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        for (int i = 0; i < 6; ++i) {
            propagator2.setInitialState(shiftState(initialState, orbitType, -4 * steps[i], i), type);
            SpacecraftState sM4h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType, -3 * steps[i], i), type);
            SpacecraftState sM3h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType, -2 * steps[i], i), type);
            SpacecraftState sM2h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType, -1 * steps[i], i), type);
            SpacecraftState sM1h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType,  1 * steps[i], i), type);
            SpacecraftState sP1h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType,  2 * steps[i], i), type);
            SpacecraftState sP2h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType,  3 * steps[i], i), type);
            SpacecraftState sP3h = propagator2.propagate(target);
            propagator2.setInitialState(shiftState(initialState, orbitType,  4 * steps[i], i), type);
            SpacecraftState sP4h = propagator2.propagate(target);
            fillJacobianColumn(dYdY0Ref, i, orbitType, steps[i],
                               sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
        }

        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                if (stateVector[i] != 0) {
                    double error = FastMath.abs((pickUp.dYdY0.getEntry(i, j) - dYdY0Ref[i][j]) / stateVector[i]) * steps[j];
                    Assertions.assertEquals(0, error, tolerance);
                }
            }
        }
    }

    @Test
    public void testNotInitialized() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Orbit initialOrbit =
                    new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.MEAN,
                            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                            Constants.EIGEN5C_EARTH_MU);
            final Orbit orbit = OrbitType.EQUINOCTIAL.convertType(initialOrbit);

            double dP = 0.001;
            DSSTPropagator propagator =
                    setUpPropagator(PropagationType.MEAN, orbit, dP, OrbitType.EQUINOCTIAL);
            new DSSTPartialDerivativesEquations("partials", propagator, PropagationType.MEAN).getMapper();
        });
     }

    @Test
    public void testTooSmallDimension() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Orbit initialOrbit =
                    new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.MEAN,
                            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                            Constants.EIGEN5C_EARTH_MU);
            final Orbit orbit = OrbitType.EQUINOCTIAL.convertType(initialOrbit);

            double dP = 0.001;
            DSSTPropagator propagator =
                    setUpPropagator(PropagationType.MEAN, orbit, dP, OrbitType.EQUINOCTIAL);
            DSSTPartialDerivativesEquations partials = new DSSTPartialDerivativesEquations("partials", propagator, PropagationType.MEAN);
            partials.setInitialJacobians(new SpacecraftState(orbit),
                    new double[5][6], new double[6][2]);
        });
     }

    @Test
    public void testTooLargeDimension() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Orbit initialOrbit =
                    new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.MEAN,
                            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                            Constants.EIGEN5C_EARTH_MU);
            final Orbit orbit = OrbitType.EQUINOCTIAL.convertType(initialOrbit);

            double dP = 0.001;
            DSSTPropagator propagator =
                    setUpPropagator(PropagationType.MEAN, orbit, dP, OrbitType.EQUINOCTIAL);
            DSSTPartialDerivativesEquations partials = new DSSTPartialDerivativesEquations("partials", propagator, PropagationType.MEAN);
            partials.setInitialJacobians(new SpacecraftState(orbit),
                    new double[8][6], new double[6][2]);
        });
     }

    @Test
    public void testMismatchedDimensions() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Orbit initialOrbit =
                    new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.MEAN,
                            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                            Constants.EIGEN5C_EARTH_MU);
            final Orbit orbit = OrbitType.EQUINOCTIAL.convertType(initialOrbit);

            double dP = 0.001;
            DSSTPropagator propagator =
                    setUpPropagator(PropagationType.MEAN, orbit, dP, OrbitType.EQUINOCTIAL);
            DSSTPartialDerivativesEquations partials = new DSSTPartialDerivativesEquations("partials", propagator, PropagationType.MEAN);
            partials.setInitialJacobians(new SpacecraftState(orbit),
                    new double[6][6], new double[7][2]);
        });
     }

    @Test
    public void testWrongParametersDimension() {
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);
        final Orbit orbit = OrbitType.EQUINOCTIAL.convertType(initialOrbit);

        double dP = 0.001;
        DSSTForceModel sunAttraction  = new DSSTThirdBody(CelestialBodyFactory.getSun(), Constants.EIGEN5C_EARTH_MU);
        DSSTForceModel moonAttraction = new DSSTThirdBody(CelestialBodyFactory.getMoon(), Constants.EIGEN5C_EARTH_MU);
        DSSTPropagator propagator =
                setUpPropagator(PropagationType.MEAN, orbit, dP, OrbitType.EQUINOCTIAL,
                                sunAttraction, moonAttraction);
        DSSTPartialDerivativesEquations partials = new DSSTPartialDerivativesEquations("partials", propagator, PropagationType.MEAN);
        try {
            partials.setInitialJacobians(new SpacecraftState(orbit),
                                         new double[6][6], new double[6][3]);
            partials.derivatives(new SpacecraftState(orbit));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INITIAL_MATRIX_AND_PARAMETERS_NUMBER_MISMATCH,
                                oe.getSpecifier());
        }
    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, double h,
                                    SpacecraftState sM4h, SpacecraftState sM3h,
                                    SpacecraftState sM2h, SpacecraftState sM1h,
                                    SpacecraftState sP1h, SpacecraftState sP2h,
                                    SpacecraftState sP3h, SpacecraftState sP4h) {
        double[] aM4h = stateToArray(sM4h, orbitType)[0];
        double[] aM3h = stateToArray(sM3h, orbitType)[0];
        double[] aM2h = stateToArray(sM2h, orbitType)[0];
        double[] aM1h = stateToArray(sM1h, orbitType)[0];
        double[] aP1h = stateToArray(sP1h, orbitType)[0];
        double[] aP2h = stateToArray(sP2h, orbitType)[0];
        double[] aP3h = stateToArray(sP3h, orbitType)[0];
        double[] aP4h = stateToArray(sP4h, orbitType)[0];
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (aP4h[i] - aM4h[i]) +
                                    32 * (aP3h[i] - aM3h[i]) -
                                   168 * (aP2h[i] - aM2h[i]) +
                                   672 * (aP1h[i] - aM1h[i])) / (840 * h);
        }
    }

    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType);
        array[0][column] += delta;

        return arrayToState(array, orbitType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType) {
          double[][] array = new double[2][6];

          orbitType.mapOrbitToArray(state.getOrbit(), PositionAngle.MEAN, array[0], array[1]);
          return array;
      }

      private SpacecraftState arrayToState(double[][] array, OrbitType orbitType,
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
          EquinoctialOrbit orbit = (EquinoctialOrbit) orbitType.mapArrayToOrbit(array[0], array[1], PositionAngle.MEAN, date, mu, frame);
          return new SpacecraftState(orbit, attitude);
      }

    private DSSTPropagator setUpPropagator(PropagationType type, Orbit orbit, double dP,
                                           OrbitType orbitType,
                                           DSSTForceModel... models) {

        final double minStep = 6000.0;
        final double maxStep = 86400.0;

        double[][] tol = NumericalPropagator.tolerances(dP, orbit, orbitType);
        DSSTPropagator propagator =
            new DSSTPropagator(new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]), type);
        for (DSSTForceModel model : models) {
            propagator.addForceModel(model);
        }
        return propagator;
    }

    private static class PickUpHandler implements OrekitStepHandler {

        private final DSSTJacobiansMapper mapper;
        private final AbsoluteDate pickUpDate;
        private RealMatrix dYdY0;
        private RealMatrix dYdP;

        public PickUpHandler(DSSTJacobiansMapper mapper, AbsoluteDate pickUpDate) {
            this.mapper = mapper;
            this.pickUpDate = pickUpDate;
        }

        public void handleStep(OrekitStepInterpolator interpolator) {
            if (pickUpDate != null) {
                // we want to pick up some intermediate Jacobians
                double dt0 = pickUpDate.durationFrom(interpolator.getPreviousState().getDate());
                double dt1 = pickUpDate.durationFrom(interpolator.getCurrentState().getDate());
                if (dt0 * dt1 > 0) {
                    // the current step does not cover the pickup date
                    return;
                } else {
                    checkState(interpolator.getInterpolatedState(pickUpDate));
                }
            }
        }

        public void finish(SpacecraftState finalState) {
            checkState(finalState);
        }

        private void checkState(final SpacecraftState state) {
            Assertions.assertEquals(1, state.getAdditionalStates().size());
            Assertions.assertTrue(state.getAdditionalStates().containsKey(mapper.getName()));
            mapper.setReferenceState(state);
            dYdY0 = mapper.getStateTransitionMatrix(state);
            dYdP  = mapper.getParametersJacobian(state);

        }

    }

    /** Test to ensure correct Jacobian values.
     * In MEAN case, Jacobian should be a 6x6 identity matrix.
     * In OSCULATING cas, first and last lines are compared to reference values.
     */
    @Test
    public void testIssue713() {
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);

        Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                         4, 4, 4, 8, 4, 4, 2);

        DSSTForceModel zonal = new DSSTZonal(provider, 4, 3, 9);
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            provider.getMu());

        DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), provider.getMu());

        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   provider.getMu());
        final EquinoctialOrbit orbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(initialOrbit);

        double dP = 0.001;
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;

        // Test MEAN case
        DSSTPropagator propagatorMEAN = setUpPropagator(PropagationType.MEAN, orbit, dP, orbitType, srp, tesseral, zonal, moon);
        propagatorMEAN.setMu(provider.getMu());
        DSSTPartialDerivativesEquations partialsMEAN = new DSSTPartialDerivativesEquations("partials", propagatorMEAN, PropagationType.MEAN);
        final SpacecraftState initialStateMEAN =
                partialsMEAN.setInitialJacobians(new SpacecraftState(orbit));
        final DSSTJacobiansMapper mapperMEAN = partialsMEAN.getMapper();
        mapperMEAN.setReferenceState(initialStateMEAN);
        RealMatrix dYdY0MEAN = mapperMEAN.getStateTransitionMatrix(initialStateMEAN);
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                Assertions.assertEquals(i == j ? 1.0 : 0.0, dYdY0MEAN.getEntry(i, j), 1e-9);
            }
        }

        // Test OSCULATING case
        DSSTPropagator propagatorOSC = setUpPropagator(PropagationType.OSCULATING, orbit, dP, orbitType, srp, tesseral, zonal, moon);
        propagatorOSC.setMu(provider.getMu());
        DSSTPartialDerivativesEquations partialsOSC = new DSSTPartialDerivativesEquations("partials", propagatorOSC, PropagationType.OSCULATING);
        final SpacecraftState initialStateOSC =
                partialsOSC.setInitialJacobians(new SpacecraftState(orbit));
        final DSSTJacobiansMapper mapperOSC = partialsOSC.getMapper();
        mapperOSC.setReferenceState(initialStateOSC);
        RealMatrix dYdY0OSC =   mapperOSC.getStateTransitionMatrix(initialStateOSC);
        final double[] refLine1 = new double[] {1.0000, -5750.3478, 15270.6488, -2707.1208, -2165.0148, -178.3653};
        final double[] refLine6 = new double[] {0.0000, 0.0035, 0.0013, -0.0005, 0.0005, 1.0000};
        for (int i = 0; i < 6; ++i) {
            Assertions.assertEquals(refLine1[i], dYdY0OSC.getEntry(0, i), 1e-4);
            Assertions.assertEquals(refLine6[i], dYdY0OSC.getEntry(5, i), 1e-4);
        }

    }

    /**
     * check {@link DSSTPartialDerivativesEquations#derivatives(SpacecraftState)}.
     *
     */
    @Test
    public void testDerivatives() {

        //action
       pde.derivatives(state);

        //verify
        MatcherAssert.assertThat(forceModel.sma.getReal(), is(state.getA()));
        MatcherAssert.assertThat(forceModel.ex.getReal(),  is(state.getEquinoctialEx()));
        MatcherAssert.assertThat(forceModel.ey.getReal(),  is(state.getEquinoctialEy()));
        MatcherAssert.assertThat(forceModel.hx.getReal(),  is(state.getHx()));
        MatcherAssert.assertThat(forceModel.hy.getReal(),  is(state.getHy()));
        MatcherAssert.assertThat(forceModel.l.getReal(),   is(state.getLv()));

    }

    /** Mock {@link DSSTForceModel}. */
    private static class MockForceModel implements DSSTForceModel {

        /** semi major axis. */
        public Gradient sma;

        /**  first component of the eccentricity vector. */
        public Gradient ex;

        /** second component of the eccentricity vector. */
        public Gradient ey;

        /** first component of the inclination vector. */
        public Gradient hx;

        /** second component of the inclination vector. */
        public Gradient hy;

        /** true latitude argument. */
        public Gradient l;

        @Override
        public List<ShortPeriodTerms> initializeShortPeriodTerms(AuxiliaryElements auxiliaryElements,
                                                 PropagationType type,
                                                 double[] parameters) {
            return new ArrayList<ShortPeriodTerms>();
        }

        @Override
        public <T extends CalculusFieldElement<T>> List<FieldShortPeriodTerms<T>> initializeShortPeriodTerms(FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                         PropagationType type,
                                                                                         T[] parameters) {
            return new ArrayList<FieldShortPeriodTerms<T>>();
        }

        @Override
        public double[] getMeanElementRate(SpacecraftState state,
                                           AuxiliaryElements auxiliaryElements,
                                           double[] parameters) {
            return new double[] {state.getA(),
                                 state.getEquinoctialEx(),
                                 state.getEquinoctialEy(),
                                 state.getHx(),
                                 state.getHy(),
                                 state.getLv()};
        }

        @Override
        public <T extends CalculusFieldElement<T>> T[] getMeanElementRate(FieldSpacecraftState<T> state,
                                                                      FieldAuxiliaryElements<T> auxiliaryElements,
                                                                      T[] parameters) {

            final Field<T> field = state.getDate().getField();

            this.sma = (Gradient) state.getA();
            this.ex  = (Gradient) state.getEquinoctialEx();
            this.ey  = (Gradient) state.getEquinoctialEy();
            this.hx  = (Gradient) state.getHx();
            this.hy  = (Gradient) state.getHy();
            this.l   = (Gradient) state.getLv();

            final T[] elements = MathArrays.buildArray(field, 6);
            elements[0] = state.getA();
            elements[1] = state.getEquinoctialEx();
            elements[2] = state.getEquinoctialEy();
            elements[3] = state.getHx();
            elements[4] = state.getHy();
            elements[5] = state.getLv();

            return elements;

        }

        @Override
        public EventDetector[] getEventsDetectors() {
            return new EventDetector[0];
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldEventDetector<T>[] getFieldEventsDetectors(Field<T> field) {
            return null;
        }

        @Override
        public void registerAttitudeProvider(AttitudeProvider provider) {
        }

        @Override
        public void updateShortPeriodTerms(double[] parameters, SpacecraftState... meanStates) {
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends CalculusFieldElement<T>> void updateShortPeriodTerms(T[] parameters,
                                                                           FieldSpacecraftState<T>... meanStates) {
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }

    }
}
