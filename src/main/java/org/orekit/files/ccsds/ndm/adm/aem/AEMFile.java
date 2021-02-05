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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.utils.IERSConventions;

/**
 * This class stores all the information of the Attitude Ephemeris Message (AEM) File parsed
 * by AEMParser. It contains the header and a list of Attitude Ephemerides Blocks each
 * containing metadata and a list of attitude ephemerides data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMFile extends NDMFile<Header, AEMSegment> implements AttitudeEphemerisFile {

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_AEM_VERS";

    /** Simple constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     */
    public AEMFile(final Header header, final List<AEMSegment> segments,
                   final IERSConventions conventions, final DataContext dataContext) {
        super(header, segments, conventions, dataContext);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, AEMSatelliteEphemeris> getSatellites() {
        final Map<String, List<AEMSegment>> byId = new HashMap<>();
        for (final AEMSegment segment : getSegments()) {
            final String id = segment.getMetadata().getObjectID();
            byId.putIfAbsent(id, new ArrayList<>());
            byId.get(id).add(segment);
        }
        final Map<String, AEMSatelliteEphemeris> ret = new HashMap<>();
        for (final Map.Entry<String, List<AEMSegment>> entry : byId.entrySet()) {
            ret.put(entry.getKey(), new AEMSatelliteEphemeris(entry.getKey(), entry.getValue()));
        }
        return ret;
    }

    /**
     * Check that, according to the CCSDS standard, every AEMBlock has the same time system.
     */
    public void checkTimeSystems() {
        CcsdsTimeScale referenceTimeSystem = null;
        for (final AEMSegment segment : getSegments()) {
            final CcsdsTimeScale timeSystem = segment.getMetadata().getTimeSystem();
            if (referenceTimeSystem == null) {
                referenceTimeSystem = timeSystem;
            } else if (!referenceTimeSystem.equals(timeSystem)) {
                throw new OrekitException(OrekitMessages.CCSDS_AEM_INCONSISTENT_TIME_SYSTEMS,
                                          referenceTimeSystem, timeSystem);
            }
        }
    }

}
