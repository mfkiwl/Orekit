/* Copyright 2010-2011 Centre National d'Études Spatiales
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
package org.orekit.propagation.numerical;

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.CunninghamAttractionModel;
import org.orekit.forces.gravity.DrozinerAttractionModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/** Class helping implementation of partial derivatives in {@link ForceModel force models} implementations.
 * <p>
 * For better performances, {@link ForceModel force models} implementations should compute their
 * partial derivatives analytically. However, in some cases, it may be difficult. This class
 * allows to compute the derivatives by finite differences relying only on the basic acceleration.
 * </p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 * @deprecated as of 9.0, this class is used only by deprecated classes {@link CunninghamAttractionModel}
 * ans {@link DrozinerAttractionModel}
 */
public class Jacobianizer {

    /** Wrapped force model instance. */
    private final ForceModel forceModel;

    /** Central attraction coefficient (m³/s²). */
    private final double mu;

    /** Step used for finite difference computation with respect to spacecraft position. */
    private double hPos;

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;

    /** Simple constructor.
     * <p>
     * The step size used for partial derivatives with respect to parameters is
     * the {@link ParameterDriver#getScale() scale} of the corresponding
     * {@link ParameterDriver}.
     * </p>
     * @param forceModel force model instance to wrap
     * @param mu central attraction coefficient (m³/s²)
     * @param hPos step used for finite difference computation with respect to spacecraft position (m)
     */
    public Jacobianizer(final ForceModel forceModel, final double mu, final double hPos) {

        this.forceModel = forceModel;
        this.mu         = mu;
        this.hPos       = hPos;
        this.factory    = new DSFactory(1, 1);

    }

    /** Compute acceleration.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param velocity velocity of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass spacecraft mass
     * @return acceleration
     * @exception OrekitException if the underlying force models cannot compute the acceleration
     */
    private Vector3D computeShiftedAcceleration(final AbsoluteDate date, final Frame frame,
                                                final Vector3D position, final Vector3D velocity,
                                                final Rotation rotation, final double mass)
        throws OrekitException {
        final Orbit shiftedORbit = new CartesianOrbit(new PVCoordinates(position, velocity), frame, date, mu);
        return forceModel.acceleration(new SpacecraftState(shiftedORbit,
                                                       new Attitude(date, frame, rotation, Vector3D.ZERO, Vector3D.ZERO),
                                                       mass));
    }

    /** Compute acceleration and derivatives with respect to state.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param velocity velocity of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass spacecraft mass
     * @return acceleration with derivatives
     * @exception OrekitException if the underlying force models cannot compute the acceleration
     */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                                                      final FieldVector3D<DerivativeStructure> position,
                                                                      final FieldVector3D<DerivativeStructure> velocity,
                                                                      final FieldRotation<DerivativeStructure> rotation,
                                                                      final DerivativeStructure mass)
        throws OrekitException {

        // estimate the scalar velocity step, assuming energy conservation
        // and hence differentiating equation V = sqrt(mu (2/r - 1/a))
        final Vector3D      p0    = position.toVector3D();
        final Vector3D      v0    = velocity.toVector3D();
        final double        r2    = p0.getNormSq();
        final double        hVel  = mu * hPos / (v0.getNorm() * r2);

        // estimate mass step, applying the same relative value as position
        final double hMass = mass.getValue() * hPos / FastMath.sqrt(r2);

        // compute nominal acceleration
        final Vector3D nominal = computeShiftedAcceleration(date, frame, p0, v0, rotation.toRotation(), mass.getValue());
        final double[] a0 = nominal.toArray();

        // shift position by hPos alon x, y and z
        Vector3D shifted = computeShiftedAcceleration(date, frame, shift(position, 0, hPos), v0, shift(rotation, 0, hPos), shift(mass, 0, hPos));
        final double[] derPx = new Vector3D(1 / hPos, shifted, -1 / hPos, nominal).toArray();
        shifted = computeShiftedAcceleration(date, frame, shift(position, 1, hPos), v0, shift(rotation, 1, hPos), shift(mass, 1, hPos));
        final double[] derPy = new Vector3D(1 / hPos, shifted, -1 / hPos, nominal).toArray();
        shifted = computeShiftedAcceleration(date, frame, shift(position, 2, hPos), v0, shift(rotation, 2, hPos), shift(mass, 2, hPos));
        final double[] derPz = new Vector3D(1 / hPos, shifted, -1 / hPos, nominal).toArray();

        // shift velocity by hVel alon x, y and z
        shifted = computeShiftedAcceleration(date, frame, p0, shift(velocity, 3, hVel), shift(rotation, 3, hVel), shift(mass, 3, hVel));
        final double[] derVx = new Vector3D(1 / hVel, shifted, -1 / hVel, nominal).toArray();
        shifted = computeShiftedAcceleration(date, frame, p0, shift(velocity, 4, hVel), shift(rotation, 4, hVel), shift(mass, 4, hVel));
        final double[] derVy = new Vector3D(1 / hVel, shifted, -1 / hVel, nominal).toArray();
        shifted = computeShiftedAcceleration(date, frame, p0, shift(velocity, 5, hVel), shift(rotation, 5, hVel), shift(mass, 5, hVel));
        final double[] derVz = new Vector3D(1 / hVel, shifted, -1 / hVel, nominal).toArray();

        final double[] derM;
        if (mass.getFreeParameters() < 7) {
            derM = null;
        } else {
            // shift mass by hMass
            shifted = computeShiftedAcceleration(date, frame, p0, v0, shift(rotation, 6, hMass), shift(mass, 6, hMass));
            derM = new Vector3D(1 / hMass, shifted, -1 / hMass, nominal).toArray();

        }
        final double[] derivatives = new double[1 + mass.getFreeParameters()];
        final DerivativeStructure[] accDer = new DerivativeStructure[3];
        for (int i = 0; i < 3; ++i) {

            // first element is value of acceleration
            derivatives[0] = a0[i];

            // next three elements are derivatives with respect to position
            derivatives[1] = derPx[i];
            derivatives[2] = derPy[i];
            derivatives[3] = derPz[i];

            // next three elements are derivatives with respect to velocity
            derivatives[4] = derVx[i];
            derivatives[5] = derVy[i];
            derivatives[6] = derVz[i];

            if (derM != null) {
                derivatives[7] = derM[i];
            }

            accDer[i] = mass.getFactory().build(derivatives);

        }

        return new FieldVector3D<>(accDer);


    }

    /** Shift a vector.
     * @param nominal nominal vector
     * @param index index of the variable with respect to which we shift
     * @param h shift step
     * @return shifted vector
     */
    private Vector3D shift(final FieldVector3D<DerivativeStructure> nominal, final int index, final double h) {
        final double[] delta = new double[nominal.getX().getFreeParameters()];
        delta[index] = h;
        return new Vector3D(nominal.getX().taylor(delta),
                            nominal.getY().taylor(delta),
                            nominal.getZ().taylor(delta));
    }

    /** Shift a rotation.
     * @param nominal nominal rotation
     * @param index index of the variable with respect to which we shift
     * @param h shift step
     * @return shifted rotation
     */
    private Rotation shift(final FieldRotation<DerivativeStructure> nominal, final int index, final double h) {
        final double[] delta = new double[nominal.getQ0().getFreeParameters()];
        delta[index] = h;
        return new Rotation(nominal.getQ0().taylor(delta),
                            nominal.getQ1().taylor(delta),
                            nominal.getQ2().taylor(delta),
                            nominal.getQ3().taylor(delta),
                            true);
    }

    /** Shift a scalar.
     * @param nominal nominal scalar
     * @param index index of the variable with respect to which we shift
     * @param h shift step
     * @return shifted scalar
     */
    private double shift(final DerivativeStructure nominal, final int index, final double h) {
        final double[] delta = new double[nominal.getFreeParameters()];
        delta[index] = h;
        return nominal.taylor(delta);
    }

    /** Compute acceleration and derivatives with respect to parameter.
     * @param s current state
     * @param paramName parameter with respect to which derivation is desired
     * @return acceleration with derivatives
     * @exception OrekitException if the underlying force models cannot compute the acceleration
     * or the parameter is not supported
     */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s,
                                                                      final String paramName)
        throws OrekitException {

        final double hP = forceModel.getParameterDriver(paramName).getScale();

        final Vector3D nominal = forceModel.acceleration(s);
        final double nx = nominal.getX();
        final double ny = nominal.getY();
        final double nz = nominal.getZ();

        ParameterDriver driver = null;
        for (final ParameterDriver pd : forceModel.getParametersDrivers()) {
            if (pd.getName().equals(paramName)) {
                driver = pd;
            }
        }
        final double paramValue = driver.getValue();
        driver.setValue(paramValue + hP);
        final double realhP = driver.getValue() - paramValue;
        final Vector3D shifted = forceModel.acceleration(s);
        final double sx = shifted.getX();
        final double sy = shifted.getY();
        final double sz = shifted.getZ();

        driver.setValue(paramValue);

        return new FieldVector3D<>(factory.build(nx, (sx - nx) / realhP),
                                   factory.build(ny, (sy - ny) / realhP),
                                   factory.build(nz, (sz - nz) / realhP));

    }

}
