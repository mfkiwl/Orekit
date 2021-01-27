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

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.adm.ADMMetadataKey;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.KVNStructureProcessingState;
import org.orekit.files.ccsds.section.XMLStructureProcessingState;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.state.AbstractMessageParser;
import org.orekit.files.ccsds.utils.state.ErrorState;
import org.orekit.files.ccsds.utils.state.ProcessingState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * A parser for the CCSDS AEM (Attitude Ephemeris Message).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMParser extends AbstractMessageParser<AEMFile, AEMParser> {

    /** Root element for XML files. */
    private static final String ROOT = "aem";

    /** Key for format version. */
    private static final String FORMAT_VERSION_KEY = "CCSDS_AEM_VERS";

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** AEM file being read. */
    private AEMFile file;

    /** Metadata for current observation block. */
    private AEMMetadata metadata;

    /** Parsing context valid for current metadata. */
    private ParsingContext context;

    /** Current Ephemerides block being parsed. */
    private AEMData currentEphemeridesBlock;

    /** Default interpolation degree. */
    private int interpolationDegree;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /**
     * Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * (may be null if time system is absolute)
     * @param interpolationDegree default interpolation degree
     */
    public AEMParser(final IERSConventions conventions, final boolean simpleEOP,
                     final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate, final int interpolationDegree) {
        super(FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext);
        this.interpolationDegree  = interpolationDegree;
        this.missionReferenceDate = missionReferenceDate;
    }

    /**
     * Get reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @return the reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** Get default interpolation degree.
     * @return interpolationDegree default interpolation degree to use while parsing
     * @see #withInterpolationDegree(int)
     * @since 10.3
     */
    public int getInterpolationDegree() {
        return interpolationDegree;
    }

    /** {@inheritDoc} */
    @Override
    public Header getHeader() {
        return file.getHeader();
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        file     = new AEMFile(getConventions(), isSimpleEOP(), getDataContext(), missionReferenceDate);
        metadata = null;
        context  = null;
        if (getFileFormat() == FileFormat.XML) {
            structureProcessor = new XMLStructureProcessingState(ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new KVNStructureProcessingState(this);
            reset(fileFormat, new HeaderProcessingState(this));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void prepareHeader() {
        setFallback(new HeaderProcessingState(this));
    }

    /** {@inheritDoc} */
    @Override
    public void inHeader() {
        setFallback(structureProcessor);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeHeader() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void prepareMetadata() {
        metadata  = new AEMMetadata();
        context   = new ParsingContext(this::getConventions,
                                       this::isSimpleEOP,
                                       this::getDataContext,
                                       this::getMissionReferenceDate,
                                       metadata::getTimeSystem);
        setFallback(this::processMetadataToken);
    }

    /** {@inheritDoc} */
    @Override
    public void inMetadata() {
        setFallback(structureProcessor);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeMetadata() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void prepareData() {
        currentEphemeridesBlock = new AEMData();
        setFallback(this::processDataToken);
    }

    /** {@inheritDoc} */
    @Override
    public void inData() {
        setFallback(structureProcessor);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeData() {
        if (metadata != null) {
            if ("A2B".equals(metadata.getAttitudeDirection())) {
                // TODO
            }
            file.addSegment(new AEMSegment(metadata, currentEphemeridesBlock,
                                           getConventions(), getDataContext()));
        }
        metadata = null;
        context  = null;
    }

    /** {@inheritDoc} */
    @Override
    public AEMFile build() {
        file.checkTimeSystems();
        return file;
    }

    /** Process one metadata token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processMetadataToken(final ParseToken token) {
        try {
            inMetadata();
            final ADMMetadataKey generalKey = ADMMetadataKey.valueOf(token.getName());
            generalKey.process(token, context, metadata);
            return true;
        } catch (IllegalArgumentException iaeG) {
            try {
                final AEMMetadataKey specificKey = AEMMetadataKey.valueOf(token.getName());
                specificKey.process(token, context, metadata);
                return true;
            } catch (IllegalArgumentException iaeS) {
                // token has not been recognized, it is most probably the end of the metadata section
                // we push the token back into next queue and let the structure parser handle it
                setFallback(new ErrorState());
                return structureProcessor;
            }
        }
    }

    /** Process one data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processDataToken(final ParseToken token) {
        if (token.getType() == TokenType.RAW_LINE) {
            // TODO
            inData();
            return true;
        } else {
            // not a raw line, it is most probably the end of the data section
            // we push the token back into next queue and let the structure parser handle it
            setFallback(new ErrorState());
            return structureProcessor;
        }
    }

}
