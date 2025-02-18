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
package org.orekit.time;

import java.util.Collection;
import java.util.stream.Stream;

/** This interface represents objects that can be interpolated in time.
 * @param <T> Type of the object.
 * @author Luc Maisonobe
 */
public interface TimeInterpolable<T extends TimeInterpolable<T>> {

    /** Get an interpolated instance.
     * <p>
     * Note that the state of the current instance may not be used
     * in the interpolation process, only its type and non interpolable
     * fields are used (for example central attraction coefficient or
     * frame when interpolating orbits). The interpolable fields taken
     * into account are taken only from the states of the sample points.
     * So if the state of the instance must be used, the instance should
     * be included in the sample points.
     * </p>
     * <p>
     * Note that this method is designed for small samples only (say up
     * to about 10-20 points) so it can be implemented using polynomial
     * interpolation (typically Hermite interpolation). Using too much
     * points may induce <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * </p>
     * @param date interpolation date
     * @param sample sample points on which interpolation should be done
     * @return a new instance, interpolated at specified date
     */
    default T interpolate(AbsoluteDate date, Collection<T> sample) {
        return interpolate(date, sample.stream());
    }

    /** Get an interpolated instance.
     * <p>
     * Note that the state of the current instance may not be used
     * in the interpolation process, only its type and non interpolable
     * fields are used (for example central attraction coefficient or
     * frame when interpolating orbits). The interpolable fields taken
     * into account are taken only from the states of the sample points.
     * So if the state of the instance must be used, the instance should
     * be included in the sample points.
     * </p>
     * <p>
     * Note that this method is designed for small samples only (say up
     * to about 10-20 points) so it can be implemented using polynomial
     * interpolation (typically Hermite interpolation). Using too much
     * points may induce <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * </p>
     * @param date interpolation date
     * @param sample sample points on which interpolation should be done
     * @return a new instance, interpolated at specified date
          * @since 9.0
     */
    T interpolate(AbsoluteDate date, Stream<T> sample);

}
